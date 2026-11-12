import axios from 'axios';

const api = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor to attach JWT token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('ledgerline_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor to handle unauthorized access
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      // Don't auto-logout if the error was a failed login attempt
      if (!error.config.url.includes('/auth/login')) {
        localStorage.removeItem('ledgerline_token');
        localStorage.removeItem('ledgerline_user');
        window.location.href = '/';
      }
    }
    return Promise.reject(error);
  }
);

export default api;
