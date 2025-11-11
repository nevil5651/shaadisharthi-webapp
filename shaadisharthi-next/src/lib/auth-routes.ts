// List of all routes that don't require authentication
// Users can access these even when not logged in
export const publicRoutes = [
  '/login',           // Login page
  '/signup',          // Sign up page  
  '/verify-email',    // Email verification
  '/register',        // Registration (usually with token)
  '/cstmr-verify-email', // Customer email verification
  '/forgot-password', // Password reset request
  '/reset-password'   // Actual password reset (with token)
];