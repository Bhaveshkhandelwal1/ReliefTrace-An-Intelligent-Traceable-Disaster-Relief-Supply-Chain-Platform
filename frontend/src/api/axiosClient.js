import axios from 'axios';

let currentToken = null;
let onTokenExpired = null;
let onForbidden = null;

export const setAuthToken = (token) => {
  currentToken = token;
};

export const setAuthCallbacks = ({ onExpired, onAccessDenied }) => {
  onTokenExpired = onExpired;
  onForbidden = onAccessDenied;
};

const axiosClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 15000,
});

axiosClient.interceptors.request.use(
  (config) => {
    // Inject Authorization header if token exists
    if (currentToken) {
      config.headers.Authorization = `Bearer ${currentToken}`;
    }

    // Set correlation ID for distributed tracing
    if (!config.headers['X-Correlation-Id']) {
      config.headers['X-Correlation-Id'] =
        typeof crypto !== 'undefined' && crypto.randomUUID
          ? crypto.randomUUID()
          : `req-${Date.now()}-${Math.random().toString(36).substring(2, 9)}`;
    }

    return config;
  },
  (error) => Promise.reject(error)
);

axiosClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const status = error.response ? error.response.status : null;

    if (status === 401) {
      if (typeof onTokenExpired === 'function') {
        onTokenExpired();
      }
    } else if (status === 403) {
      if (typeof onForbidden === 'function') {
        onForbidden();
      }
    }

    return Promise.reject(error);
  }
);

export default axiosClient;
