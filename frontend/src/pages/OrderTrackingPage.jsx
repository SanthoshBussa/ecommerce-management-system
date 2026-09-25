import React, { useState } from 'react';
import {
  Stepper,
  Step,
  StepLabel,
  TextField,
  Button,
  Paper,
  Typography,
  Chip,
  Alert,
} from '@mui/material';
import LocalShippingIcon from '@mui/icons-material/LocalShipping';
import axiosClient from '../api/axiosClient';

export default function OrderTrackingPage() {
  const [orderNumber, setOrderNumber] = useState('');
  const [trackingData, setTrackingData] = useState(null);
  const [error, setError] = useState('');

  const handleTrackOrder = async (e) => {
    e.preventDefault();
    setError('');
    try {
      const response = await axiosClient.get(`/orders/track/${orderNumber.trim()}`);
      setTrackingData(response.data);
    } catch (err) {
      setTrackingData(null);
      setError(err.response?.data?.message || 'Order not found. Please verify your Order ID.');
    }
  };

  const activeStepIndex = trackingData
    ? trackingData.timeline.findIndex((step) => step.current)
    : 0;

  return (
    <div className="max-w-4xl mx-auto px-4 py-10">
      <Paper className="p-8 rounded-2xl shadow-sm border border-slate-200">
        <div className="flex items-center gap-3 mb-6">
          <LocalShippingIcon color="primary" fontSize="large" />
          <div>
            <Typography variant="h5" className="font-bold text-slate-800">
              Real-Time Order Tracking
            </Typography>
            <Typography variant="body2" className="text-slate-500">
              Enter your Order Number (e.g., ORD-8F92A1B3) to view live shipment milestones
            </Typography>
          </div>
        </div>

        <form onSubmit={handleTrackOrder} className="flex gap-4 mb-8">
          <TextField
            fullWidth
            label="Order Number"
            placeholder="ORD-XXXXXXXX"
            value={orderNumber}
            onChange={(e) => setOrderNumber(e.target.value)}
            required
          />
          <Button type="submit" variant="contained" size="large" className="px-8">
            Track
          </Button>
        </form>

        {error && <Alert severity="error" className="mb-6">{error}</Alert>}

        {trackingData && (
          <div className="space-y-8">
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 bg-slate-50 p-4 rounded-xl border border-slate-200">
              <div>
                <span className="text-xs uppercase text-slate-400 font-semibold">Order ID</span>
                <p className="font-bold text-slate-800">{trackingData.orderNumber}</p>
              </div>
              <div>
                <span className="text-xs uppercase text-slate-400 font-semibold">Courier & AWB</span>
                <p className="font-medium text-slate-700">
                  {trackingData.courierPartner} ({trackingData.trackingNumber})
                </p>
              </div>
              <div>
                <span className="text-xs uppercase text-slate-400 font-semibold">Payment Status</span>
                <div className="mt-1">
                  <Chip
                    label={trackingData.paymentStatus}
                    color={trackingData.paymentStatus === 'PAID' ? 'success' : 'warning'}
                    size="small"
                  />
                </div>
              </div>
            </div>

            <Stepper activeStep={activeStepIndex} alternativeLabel>
              {trackingData.timeline.map((step) => (
                <Step key={step.status} completed={step.completed}>
                  <StepLabel>{step.label}</StepLabel>
                </Step>
              ))}
            </Stepper>

            <div className="text-sm text-slate-600 border-t pt-4 flex justify-between">
              <span>Shipping To: <strong>{trackingData.shippingAddress}</strong></span>
              <span>Order Total: <strong>₹{Number(trackingData.totalAmount).toLocaleString()}</strong></span>
            </div>
          </div>
        )}
      </Paper>
    </div>
  );
}
