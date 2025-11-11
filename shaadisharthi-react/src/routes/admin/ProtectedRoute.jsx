// src/components/PrivateRoute.jsx

import { Navigate } from "react-router-dom";
import { AuthContext } from "../../auth/admin/AuthContext";
import Spinner from "../../components/Spinner";
import { useContext } from "react";

/**
 * ProtectedRoute wraps any route that requires authentication.
 *
 * Core Responsibilities:
 * 1. Check AuthContext.loading → show full-screen spinner (prevents content flash)
 * 2. Check AuthContext.user → allow access or redirect to /adminlogin
 *
 * Used in App.jsx to wrap AdminLayout and all protected child routes.
 *
 * Why not use <Outlet /> directly? Because we need to conditionally render children
 * or redirect *before* rendering layout — avoids layout flash on unauthorized access.
 */
const ProtectedRoute = ({ children }) => {
  const { user, loading } = useContext(AuthContext);

  // Show spinner during initial auth check (from AuthProvider useEffect)
  if (loading) {
    return <Spinner />; // Full-screen centered spinner — consistent with app UX
  }

  // If user is authenticated, render children (AdminLayout + nested routes)
  // Else redirect to login, preserving intended destination via state (optional enhancement)
  return user ? children : <Navigate to="/adminlogin" />;
};

export default ProtectedRoute;
