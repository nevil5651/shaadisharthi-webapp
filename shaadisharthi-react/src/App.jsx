import { Routes, Route, BrowserRouter } from "react-router-dom";
import AdminLayout from "./layouts/admin/AdminLayout";
import Dashboard from "./components/admin/Dashboard";
import Account from "./components/admin/Account";
import AdminLogin from "./components/admin/AdminLogin";
import ProtectedRoute from "./routes/admin/ProtectedRoute";
import { AuthProvider } from "./auth/admin/AuthContext";
import Customers from "./components/admin/customers";
import ServiceProviders from "./components/admin/ServiceProviders";
import QueryPage from "./components/admin/QueryPage";
import { ToastContainer } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";
import AdminManagement from "./components/admin/AdminManagement";
import AdminForgotPassword from "./components/admin/AdminForgotPassword";
import AdminResetPassword from "./components/admin/AdminResetPassword";
import TOTPSetup from "./components/admin/TOTPSetup";
import GuestQueryPage from "./components/admin/GuestQueryPage";
/**
 * Root component of the admin panel.
 * Sets up:
 * - Client-side routing with base path `/admin`
 * - Global AuthProvider for session management
 * - Toast notifications for user feedback
 * - Route hierarchy: public vs protected
 */
const App = () => {
  return (
    <BrowserRouter basename="/admin">
      {/*
        AuthProvider must wrap everything that needs auth context.
        Includes toast container so notifications work globally.
      */}
      <AuthProvider>
        {/*
          ToastContainer configuration:
          - Top-right position
          - Auto-close after 3s
          - No progress bar (cleaner UX)
          - Light theme, pause on hover/focus
        */}
        <ToastContainer
          position="top-right"
          autoClose={3000}
          hideProgressBar={true}
          newestOnTop={false}
          closeOnClick
          rtl={false}
          pauseOnFocusLoss
          draggable
          pauseOnHover
          theme="light"
        />

        {/*
          Route Structure:
          - Public routes: login, forgot password, reset, TOTP setup
          - Protected routes: wrapped in ProtectedRoute + AdminLayout
          - Nested routes under "/" for shared layout (sidebar, header)
        */}
        <Routes>
          {/* Public Route */}
          <Route path="/adminlogin" element={<AdminLogin />} />
          <Route
            path="/adminforgotpassword"
            element={<AdminForgotPassword />}
          />
          <Route path="/adminresetpassword" element={<AdminResetPassword />} />
          <Route path="/totp-setup" element={<TOTPSetup />} />

          {/*
            Protected Layout and Nested Routes
            - ProtectedRoute checks AuthContext.user
            - AdminLayout provides sidebar, navbar, etc.
            - All child routes inherit layout
          */}
          <Route
            path="/"
            element={
              <ProtectedRoute>
                <AdminLayout />
              </ProtectedRoute>
            }
          >
            <Route index element={<Dashboard />} />
            <Route path="dashboard" element={<Dashboard />} />
            <Route path="account" element={<Account />} />
            <Route path="customers" element={<Customers />} />
            <Route path="serviceproviders" element={<ServiceProviders />} />
            <Route path="querypage" element={<QueryPage />} />
            <Route path="guestquerypage" element={<GuestQueryPage />} />
            <Route path="adminmanagement" element={<AdminManagement />} />
          </Route>
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
};

export default App;
