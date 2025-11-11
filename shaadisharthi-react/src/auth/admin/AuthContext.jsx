import { createContext, useState, useEffect } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import axiosInstance from "../../utils/http";

/**
 * Global authentication context for admin panel.
 * Centralizes auth state, token persistence, account data, and login/logout flows.
 * Used across the entire admin dashboard to enforce authentication and session management.
 */
export const AuthContext = createContext();

/**
 * AuthProvider wraps the entire admin app (via App.jsx) and provides:
 * - Persistent user session via sessionStorage (short-lived, cleared on tab close)
 * - Account data persistence via localStorage (long-lived, survives tab close)
 * - Automatic redirect to login on invalid/missing tokens
 * - Centralized login/logout with proper cleanup
 */
export const AuthProvider = ({ children }) => {
  const navigate = useNavigate();
  const location = useLocation(); // Tracks current route to prevent unauthorized access on refresh

  /**
   * Initialize user state from sessionStorage.
   * Using a function to avoid reading storage on every render (performance).
   * Only valid if both token AND adminId exist — prevents partial sessions.
   */
  const [user, setUser] = useState(() => {
    const token = sessionStorage.getItem("admin_jwt");
    const adminId = sessionStorage.getItem("admin_id");
    return token && adminId ? { token, adminId } : null;
  });

  /**
   * Account state persisted in localStorage.
   * Contains full admin profile (name, email, etc.) after successful login.
   * Survives page refresh and tab close — critical for UX.
   */
  const [account, setAccount] = useState(() => {
    const saved = localStorage.getItem("account");
    return saved ? JSON.parse(saved) : null;
  });

  // Controls initial loading state — prevents flash of protected content
  const [loading, setLoading] = useState(true);

  /**
   * Login handler with TOTP support.
   * Returns { data, error } for consistent error handling in UI.
   * Handles two-step flow:
   *   1. TOTP setup required → store token, return early
   *   2. Full login → store token + fetch account
   */
  const login = async ({ email, password, totpCode }) => {
    const result = { data: {}, error: null };

    try {
      const res = await axiosInstance.post(
        "/admin/admin-auth/login",
        {
          email,
          password,
          totpCode: totpCode || null,
        },
        { withCredentials: true }
      );

      const data = res.data;
      result.data = data;

      // Case 1: TOTP setup required — store partial session, redirect to setup
      if (data.setupTotp && data.token && data.adminId) {
        sessionStorage.setItem("admin_jwt", data.token);
        sessionStorage.setItem("admin_id", data.adminId);
        setUser({ token: data.token, adminId: data.adminId });
        return result;
      }

      // Case 2: Full login success — persist session and fetch account
      if (data.token && data.adminId) {
        sessionStorage.setItem("admin_jwt", data.token);
        sessionStorage.setItem("admin_id", data.adminId);
        setUser({ token: data.token, adminId: data.adminId });
        await fetchAccount(); // Populate account state immediately
      }
    } catch (error) {
      // Normalize error: use backend message if available, else fallback
      result.error = error.response?.data?.error || error.message;
      console.error("Login error:", result.error);
    }

    return result;
  };

  /**
   * Full logout: clear all auth state and redirect.
   * Ensures no residual data remains in memory or storage.
   */
  const logout = () => {
    setUser(null);
    setAccount(null);
    sessionStorage.removeItem("admin_jwt");
    sessionStorage.removeItem("admin_id");
    localStorage.removeItem("account");
    navigate("/adminlogin");
  };

  /**
   * Update account in both state and localStorage.
   * Also syncs name into user object to keep navbar/profile in sync without refetch.
   */
  const updateAccount = (data) => {
    setAccount(data);
    localStorage.setItem("account", JSON.stringify(data));
    setUser((prev) => (prev ? { ...prev, name: data.name } : prev));
  };

  /**
   * Fetch full account profile using stored JWT.
   * Called on login success or app init if account missing.
   */
  const fetchAccount = async () => {
    const token = sessionStorage.getItem("admin_jwt");
    if (!token) return;

    try {
      const res = await axiosInstance.get("/admin/account", {
        headers: { Authorization: `Bearer ${token}` },
        withCredentials: true,
      });
      if (res.status === 200) {
        updateAccount(res.data);
      }
    } catch (error) {
      console.error(
        "Fetch account error:",
        error.response?.data || error.message
      );
    }
  };

  /**
   * On mount or route change:
   * - Restore session from storage
   * - Auto-logout + redirect if on protected route without valid session
   * - Fetch account if missing
   * - Set loading false only after full init
   *
   * Dependency on [location] ensures re-check on navigation (e.g. browser back)
   */
  useEffect(() => {
    const init = async () => {
      const publicPaths = [
        "/adminlogin",
        "/adminforgotpassword",
        "/adminresetpassword",
        "/totp-setup",
      ];
      const token = sessionStorage.getItem("admin_jwt");
      const adminId = sessionStorage.getItem("admin_id");
      const storedAccount = localStorage.getItem("account");

      if (token && adminId) {
        const parsed = storedAccount ? JSON.parse(storedAccount) : null;
        setUser({ token, adminId, name: parsed?.name });
        setAccount(parsed);
        if (!parsed) await fetchAccount();
      } else if (!publicPaths.includes(location.pathname)) {
        console.log("Redirecting to /adminlogin from", location.pathname);
        logout(); // Triggers full cleanup + redirect
      }
      setLoading(false);
    };
    init();
  }, [location]); // Re-run on route change to enforce auth

  /**
   * Provide auth state and methods to entire app.
   * Children render only after loading completes — prevents protected content flash.
   */
  return (
    <AuthContext.Provider
      value={{
        user,
        loading,
        login,
        logout,
        account,
        updateAccount,
        fetchAccount,
      }}
    >
      {!loading && children}
    </AuthContext.Provider>
  );
};
