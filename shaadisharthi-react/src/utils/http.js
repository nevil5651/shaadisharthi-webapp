// Axios Instance Configuration for Admin API Calls
// Handles base URL, credentials, JWT auth, and error logging

import axios from "axios";

// Create Axios instance with global config
const axiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL, // Environment-based API base URL
  withCredentials: true, // Needed for JSESSIONID cookie — Enables cookie-based sessions
});

// Request interceptor: Adds Authorization header if JWT exists
axiosInstance.interceptors.request.use(
  (config) => {
    const token = sessionStorage.getItem("admin_jwt"); // Retrieve admin JWT from sessionStorage
    if (token) {
      config.headers.Authorization = `Bearer ${token}`; // Attach Bearer token for authenticated requests
    } else {
      console.warn(
        "No JWT token found in sessionStorage. Public routes will proceed without Authorization header."
      ); // Warn for debugging — helps identify auth issues
    }
    return config;
  },
  (error) => {
    return Promise.reject(error); // Propagate request errors
  }
);

// Response interceptor: Logs errors, propagates for handling elsewhere
axiosInstance.interceptors.response.use(
  (response) => response, // Pass successful responses through
  (error) => {
    console.error("HTTP Error:", error); // Log full error for debugging
    // Rely on ProtectedRoute for 401 redirects — Decouples auth logic
    return Promise.reject(error); // Propagate for centralized handling (e.g., handleAxiosError)
  }
);

export default axiosInstance;
