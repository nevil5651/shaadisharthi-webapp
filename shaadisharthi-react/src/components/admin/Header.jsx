import { useNavigate } from "react-router-dom";
import logoImg from "@assets/admin/imgg/logo.png";
import profileImg from "@assets/admin/imgg/profile-img.jpg";
import { AuthContext } from "../../auth/admin/AuthContext";
import { useContext } from "react";
import axiosInstance from "../../utils/http";
import { toast } from "react-toastify";

/**
 * Header Component
 *
 * Purpose: Top navigation bar for admin panel
 *
 * Features:
 * - Brand logo (non-clickable in current design)
 * - Mobile-responsive sidebar toggle
 * - Search bar (UI only - no backend integration)
 * - User profile dropdown with:
 *   → Name from AuthContext
 *   → Profile & Settings links (stub routes)
 *   → Secure logout with API call + local cleanup
 *
 * Security:
 * - Calls backend logout endpoint
 * - Falls back to local logout even if API fails
 * - Uses centralized toast for consistent feedback
 */

const Header = () => {
  // Navigation hook for post-logout redirect
  const navigate = useNavigate();

  // Extract logout function and current account from AuthContext
  const { logout, account } = useContext(AuthContext);

  /**
   * handleLogout
   *
   * Responsibilities:
   * 1. Attempt server-side session termination
   * 2. Show success/error toast
   * 3. Always clear local auth state via context
   * 4. Redirect to login page
   *
   * Why call API? Ensures backend invalidates tokens/sessions
   */
  const handleLogout = async () => {
    try {
      // Attempt to logout on server (invalidates session/token)
      const response = await axiosInstance.post("admin/admin-auth/logout");
      toast.success(response.data.message || "Logged out successfully.");
    } catch (err) {
      // Even if server fails, inform user and proceed with local logout
      toast.error(
        err.response?.data?.message ||
          "Server error during logout. Logging out locally."
      );
      console.error("Logout error:", err);
    }

    // Critical: Always clear local auth state and redirect
    logout(); // Clears context, localStorage, etc.
    navigate("/adminlogin");
  };

  /**
   * handleSidebarToggle
   *
   * Toggles 'toggle-sidebar' class on <body>
   * Used by CSS to show/hide sidebar on mobile
   */
  const handleSidebarToggle = () => {
    document.body.classList.toggle("toggle-sidebar");
  };

  return (
    <header id="header" className="header fixed-top d-flex align-items-center">
      {/* Logo + Sidebar Toggle (Mobile) */}
      <div className="d-flex align-items-center justify-content-between">
        <a className="logo d-flex align-items-center">
          <img src={logoImg} alt="Logo" />
          <span className="d-none d-lg-block"></span>
        </a>
        {/* Hamburger icon - triggers sidebar on small screens */}
        <i
          className="bi bi-list toggle-sidebar-btn"
          onClick={handleSidebarToggle}
        ></i>
      </div>

      {/* Search Bar - UI Only (form action="#" → no submission) */}
      <div className="search-bar">
        <form
          className="search-form d-flex align-items-center"
          method="POST"
          action="#"
        >
          <input
            type="text"
            name="query"
            placeholder="Search"
            title="Enter search keyword"
          />
          <button type="submit" title="Search">
            <i className="bi bi-search"></i>
          </button>
        </form>
      </div>

      {/* Right-side Navigation */}
      <nav className="header-nav ms-auto">
        <ul className="d-flex align-items-center">
          {/* Mobile-only Search Toggle */}
          <li className="nav-item d-block d-lg-none">
            <a className="nav-link nav-icon search-bar-toggle" href="#">
              <i className="bi bi-search"></i>
            </a>
          </li>

          {/* User Profile Dropdown */}
          <li className="nav-item dropdown pe-3">
            <a
              className="nav-link nav-profile d-flex align-items-center pe-0"
              href="#"
              data-bs-toggle="dropdown"
            >
              <img src={profileImg} alt="Profile" className="rounded-circle" />
              {/* Fallback to "UserName" if account not loaded */}
              <span className="d-none d-md-block dropdown-toggle ps-2">
                {account?.name || "UserName"}
              </span>
            </a>

            {/* Dropdown Menu */}
            <ul className="dropdown-menu dropdown-menu-end dropdown-menu-arrow profile">
              <li className="dropdown-header">
                <h6>{account?.name || "UserName"}</h6>
              </li>
              <li>
                <hr className="dropdown-divider" />
              </li>

              {/* Profile Link */}
              <li>
                <a
                  className="dropdown-item d-flex align-items-center"
                  href="account"
                >
                  <i className="bi bi-person"></i>
                  <span>My Profile</span>
                </a>
              </li>
              <li>
                <hr className="dropdown-divider" />
              </li>

              {/* Settings Link */}
              <li>
                <a
                  className="dropdown-item d-flex align-items-center"
                  href="account"
                >
                  <i className="bi bi-gear"></i>
                  <span>Account Settings</span>
                </a>
              </li>
              <li>
                <hr className="dropdown-divider" />
              </li>

              {/* Logout Action */}
              <li>
                <a
                  className="dropdown-item d-flex align-items-center"
                  onClick={handleLogout}
                >
                  <i className="bi bi-box-arrow-right"></i>
                  <span>Sign Out</span>
                </a>
              </li>
            </ul>
          </li>
        </ul>
      </nav>
    </header>
  );
};

export default Header;
