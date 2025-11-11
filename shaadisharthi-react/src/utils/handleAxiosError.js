import { toast } from "react-toastify";

/**
 * handleAxiosError - Centralized Axios Error Handler
 *
 * Purpose:
 * - Provide consistent error messaging across the app
 * - Handle common HTTP status codes with appropriate user feedback
 * - Auto-logout on token-related 401 errors (if logout function provided)
 * - Log full error to console for debugging
 *
 * @param {Object} err - Axios error object
 * @param {string} fallbackMessage - Default message if none from server
 * @param {Function} [logout] - Optional logout callback (e.g., clear auth)
 *
 * Security Notes:
 * - Avoid exposing sensitive server details
 * - Generic messages for 404/500 to prevent info leakage
 */
export const handleAxiosError = (
  err,
  fallbackMessage = "Something went wrong",
  logout
) => {
  // Extract server-provided message, fallback if not present
  const message =
    err.response?.data?.message || err.response?.data?.error || fallbackMessage;

  // Case 1: Server responded with error status
  if (err.response) {
    const status = err.response.status;

    switch (status) {
      case 400:
        // Bad Request - usually validation errors
        toast.error(message); // Server-defined validation errors
        break;

      case 401:
        // Unauthorized - wrong credentials or expired token
        toast.error(message); // e.g., "Incorrect password"

        // Auto-logout if token is invalid/expired and logout fn provided
        // Double condition is redundant but kept for backward compatibility
        if (
          status === 401 &&
          logout &&
          message.toLowerCase().includes("token")
        ) {
          setTimeout(() => logout(), 1000); // Delay to show toast first
        }
        if (logout && message.toLowerCase().includes("token")) {
          setTimeout(() => logout(), 1000);
        }
        break;

      case 403:
        // Forbidden - user lacks permission
        toast.error(message || "You do not have permission.");
        break;

      case 404:
        // Not Found - endpoint or resource missing
        toast.error(message || "Resource not found.");
        break;

      case 500:
        // Internal Server Error
        toast.error(message || "Server error.");
        break;

      default:
        // Any other status: show server message
        toast.error(message);
    }
  }
  // Case 2: No response (network/down server)
  else if (err.request) {
    toast.error("No response from server. Please check your internet.");
  }
  // Case 3: Request setup error (e.g., invalid URL)
  else {
    toast.error(err.message || fallbackMessage);
  }

  // Always log full error for developer debugging
  console.error("Axios error:", err);
};
