import axios from 'axios';

// The local development API is deliberately explicit.  Vite only exposes VITE_*
// values that were present when it started, so falling back to the Docker API
// would silently send a local browser to the wrong port after a restart.
const api = axios.create({baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api', headers: {Accept: 'application/json'}});
api.interceptors.request.use(config => {
  const token = localStorage.getItem('leaveflow.token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});
api.interceptors.response.use(response => response, error => {
  if (error.response?.status === 401) window.dispatchEvent(new Event('leaveflow:unauthorized'));
  return Promise.reject(error);
});
export default api;
