import React, { useState } from 'react';
import { AppBar, Toolbar, Typography, Button, Badge, IconButton, Container } from '@mui/material';
import ShoppingBagIcon from '@mui/icons-material/ShoppingBag';
import ShoppingCartIcon from '@mui/icons-material/ShoppingCart';
import ProductCatalogPage from './pages/ProductCatalogPage';
import OrderTrackingPage from './pages/OrderTrackingPage';

export default function App() {
  const [activeTab, setActiveTab] = useState('catalog');
  const [cart, setCart] = useState([]);

  const handleAddToCart = (product) => {
    setCart((prev) => [...prev, product]);
  };

  return (
    <div className="min-h-screen flex flex-col bg-slate-50">
      <AppBar position="sticky" color="default" elevation={1} className="bg-white">
        <Container maxWidth="xl">
          <Toolbar disableGutters className="flex justify-between">
            <div className="flex items-center gap-2 cursor-pointer" onClick={() => setActiveTab('catalog')}>
              <ShoppingBagIcon color="primary" fontSize="large" />
              <Typography variant="h6" className="font-extrabold tracking-tight text-slate-900">
                ShopSphere
              </Typography>
            </div>

            <div className="flex items-center gap-4">
              <Button
                color={activeTab === 'catalog' ? 'primary' : 'inherit'}
                onClick={() => setActiveTab('catalog')}
              >
                Products
              </Button>
              <Button
                color={activeTab === 'track' ? 'primary' : 'inherit'}
                onClick={() => setActiveTab('track')}
              >
                Track Order
              </Button>
              <IconButton color="primary">
                <Badge badgeContent={cart.length} color="error">
                  <ShoppingCartIcon />
                </Badge>
              </IconButton>
            </div>
          </Toolbar>
        </Container>
      </AppBar>

      <main className="flex-grow">
        {activeTab === 'catalog' ? (
          <ProductCatalogPage onAddToCart={handleAddToCart} />
        ) : (
          <OrderTrackingPage />
        )}
      </main>
    </div>
  );
}
