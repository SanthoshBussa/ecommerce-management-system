import React, { useState, useEffect } from 'react';
import {
  Box,
  Card,
  CardContent,
  CardMedia,
  Typography,
  Button,
  Chip,
  Slider,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  TextField,
  Rating,
  CircularProgress,
  Pagination,
} from '@mui/material';
import ShoppingCartIcon from '@mui/icons-material/ShoppingCart';
import FilterAltIcon from '@mui/icons-material/FilterAlt';
import axiosClient from '../api/axiosClient';

export default function ProductCatalogPage({ onAddToCart }) {
  const [products, setProducts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(false);

  // Filter & Sort states
  const [selectedCategory, setSelectedCategory] = useState('');
  const [keyword, setKeyword] = useState('');
  const [priceRange, setPriceRange] = useState([0, 150000]);
  const [minRating, setMinRating] = useState(0);
  const [sortBy, setSortBy] = useState('createdAt');
  const [sortDir, setSortDir] = useState('desc');
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);

  useEffect(() => {
    axiosClient.get('/categories')
      .then((res) => setCategories(res.data))
      .catch(() => {});
  }, []);

  useEffect(() => {
    fetchProducts();
  }, [selectedCategory, priceRange, minRating, sortBy, sortDir, page]);

  const fetchProducts = async () => {
    setLoading(true);
    try {
      const params = {
        category: selectedCategory || undefined,
        minPrice: priceRange[0],
        maxPrice: priceRange[1],
        minRating: minRating > 0 ? minRating : undefined,
        keyword: keyword || undefined,
        sortBy,
        sortDir,
        page: page - 1,
        size: 9,
      };
      const response = await axiosClient.get('/products', { params });
      setProducts(response.data.content || []);
      setTotalPages(response.data.totalPages || 1);
    } catch (error) {
      console.error('Failed to fetch filtered products:', error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 py-8 grid grid-cols-1 lg:grid-cols-4 gap-8">
      {/* Dynamic Filter Sidebar */}
      <aside className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200 h-fit space-y-6">
        <div className="flex items-center gap-2 border-b pb-3">
          <FilterAltIcon color="primary" />
          <Typography variant="h6" className="font-bold text-slate-800">
            Product Filters
          </Typography>
        </div>

        <TextField
          fullWidth
          size="small"
          label="Search Products..."
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && fetchProducts()}
        />

        <FormControl fullWidth size="small">
          <InputLabel>Category</InputLabel>
          <Select
            value={selectedCategory}
            label="Category"
            onChange={(e) => setSelectedCategory(e.target.value)}
          >
            <MenuItem value="">All Categories</MenuItem>
            {categories.map((cat) => (
              <MenuItem key={cat.id} value={cat.slug}>
                {cat.name}
              </MenuItem>
            ))}
          </Select>
        </FormControl>

        <div>
          <Typography variant="subtitle2" className="text-slate-600 mb-2">
            Price Range (₹{priceRange[0].toLocaleString()} - ₹{priceRange[1].toLocaleString()})
          </Typography>
          <Slider
            value={priceRange}
            onChange={(_, newValue) => setPriceRange(newValue)}
            valueLabelDisplay="auto"
            min={0}
            max={150000}
            step={1000}
          />
        </div>

        <div>
          <Typography variant="subtitle2" className="text-slate-600 mb-1">
            Minimum Customer Rating
          </Typography>
          <Rating
            value={minRating}
            onChange={(_, newValue) => setMinRating(newValue || 0)}
          />
        </div>

        <FormControl fullWidth size="small">
          <InputLabel>Sort By</InputLabel>
          <Select
            value={`${sortBy}-${sortDir}`}
            label="Sort By"
            onChange={(e) => {
              const [field, dir] = e.target.value.split('-');
              setSortBy(field);
              setSortDir(dir);
            }}
          >
            <MenuItem value="createdAt-desc">Newest Arrivals</MenuItem>
            <MenuItem value="price-asc">Price: Low to High</MenuItem>
            <MenuItem value="price-desc">Price: High to Low</MenuItem>
            <MenuItem value="rating-desc">Top Rated</MenuItem>
          </Select>
        </FormControl>

        <Button
          fullWidth
          variant="outlined"
          onClick={() => {
            setSelectedCategory('');
            setKeyword('');
            setPriceRange([0, 150000]);
            setMinRating(0);
          }}
        >
          Reset Filters
        </Button>
      </aside>

      {/* Product Grid */}
      <section className="lg:col-span-3">
        {loading ? (
          <Box className="flex justify-center items-center h-64">
            <CircularProgress />
          </Box>
        ) : (
          <>
            <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-6">
              {products.map((product) => (
                <Card
                  key={product.id}
                  className="rounded-2xl shadow-sm hover:shadow-md transition-shadow duration-200 flex flex-col justify-between"
                >
                  <div>
                    <CardMedia
                      component="img"
                      height="200"
                      image={product.imageUrl || 'https://placehold.co/600x400?text=Product'}
                      alt={product.name}
                      className="h-48 object-cover"
                    />
                    <CardContent>
                      <div className="flex justify-between items-center mb-2">
                        <Chip
                          label={product.category?.name || 'General'}
                          size="small"
                          color="primary"
                          variant="outlined"
                        />
                        <Rating value={Number(product.rating) || 4.5} precision={0.5} size="small" readOnly />
                      </div>
                      <Typography variant="h6" className="font-semibold text-slate-800 line-clamp-1">
                        {product.name}
                      </Typography>
                      <Typography variant="body2" className="text-slate-500 line-clamp-2 mt-1">
                        {product.description}
                      </Typography>
                      <div className="mt-4 flex items-baseline gap-2">
                        <span className="text-xl font-bold text-blue-600">
                          ₹{(product.discountPrice || product.price).toLocaleString()}
                        </span>
                        {product.discountPrice && (
                          <span className="text-sm text-slate-400 line-through">
                            ₹{product.price.toLocaleString()}
                          </span>
                        )}
                      </div>
                    </CardContent>
                  </div>

                  <div className="p-4 pt-0">
                    <Button
                      fullWidth
                      variant="contained"
                      startIcon={<ShoppingCartIcon />}
                      disabled={product.stockQuantity <= 0}
                      onClick={() => onAddToCart && onAddToCart(product)}
                    >
                      {product.stockQuantity > 0 ? 'Add to Cart' : 'Out of Stock'}
                    </Button>
                  </div>
                </Card>
              ))}
            </div>

            <div className="mt-8 flex justify-center">
              <Pagination
                count={totalPages}
                page={page}
                onChange={(_, value) => setPage(value)}
                color="primary"
              />
            </div>
          </>
        )}
      </section>
    </div>
  );
}
