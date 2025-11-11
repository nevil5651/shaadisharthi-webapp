import axios from 'axios';

const api = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL || '',
  headers: {
    'Content-Type': 'application/json',
  },
  // This is the crucial part for httpOnly cookies.
  // It tells the browser to send cookies along with cross-origin requests.
  withCredentials: true,
});

// Response interceptor to handle common error cases, like 401 Unauthorized
api.interceptors.response.use(
  (response) => response,
  (error) => {
    // The 401 error is now handled globally in the AuthContext.
    // This interceptor's job is just to pass the error along so that
    // the AuthContext or other calling functions can handle it appropriately.
    // By removing the redirect here, we prevent base path issues and
    // ensure a single, consistent way of handling session expiry.
    return Promise.reject(error);
  }
);

export default api;
