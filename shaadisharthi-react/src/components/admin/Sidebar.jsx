import { useCallback, useEffect } from "react";
import { NavLink } from "react-router-dom";

/**
 * Sidebar Component
 *
 * Purpose: Responsive vertical navigation for admin dashboard
 *
 * Key Features:
 * - Highlights active route with custom styles
 * - Auto-collapses on mobile after link click
 * - Handles window resize edge cases
 * - Uses react-router-dom for SPA navigation
 *
 * Responsive Behavior:
 * - < 992px (lg breakpoint): Sidebar collapses by default
 * - Clicking link on mobile → closes sidebar
 * - Resizing from mobile → desktop while open → auto-close
 */
const Sidebar = ({ isOpen }) => {
  // Active link styling - matches theme's primary color and background
  const activeLinkStyle = {
    color: "#4154f1",
    backgroundColor: "#f6f9ff",
  };

  /**
   * getLinkStyle
   *
   * @param {object} navData - { isActive: boolean }
   * @returns {object} - Inline styles for NavLink
   *
   * Used to dynamically apply active state styles
   */
  const getLinkStyle = ({ isActive }) => {
    return isActive ? activeLinkStyle : {};
  };

  /**
   * handleLinkClick
   *
   * Closes sidebar on mobile after navigation
   * Only runs if screen width < 992px (Bootstrap lg)
   *
   * Memoized with useCallback to prevent unnecessary re-renders
   */
  const handleLinkClick = useCallback(() => {
    if (window.innerWidth < 992) {
      document.body.classList.remove("toggle-sidebar");
    }
  }, []);

  /**
   * useEffect: Handle window resize
   *
   * If user resizes from mobile (sidebar open) to desktop,
   * ensure sidebar is closed to avoid layout conflict
   */
  useEffect(() => {
    const handleResize = () => {
      if (window.innerWidth >= 992) {
        document.body.classList.remove("toggle-sidebar");
      }
    };

    window.addEventListener("resize", handleResize);

    // Cleanup listener on unmount
    return () => {
      window.removeEventListener("resize", handleResize);
    };
  }, []);

  return (
    <aside id="sidebar" className={`sidebar ${isOpen ? "active" : ""}`}>
      <ul className="sidebar-nav" id="sidebar-nav">
        {/* Dashboard */}
        <li className="nav-item">
          <NavLink
            to="/dashboard"
            className="nav-link"
            style={getLinkStyle}
            onClick={handleLinkClick}
          >
            <i className="bi bi-grid"></i>
            <span>Dashboard</span>
          </NavLink>
        </li>

        {/* Account */}
        <li className="nav-item">
          <NavLink
            to="/account"
            className="nav-link"
            style={getLinkStyle}
            onClick={handleLinkClick}
          >
            <i className="bi bi-person"></i>
            <span>Account</span>
          </NavLink>
        </li>

        {/* Customers */}
        <li className="nav-item">
          <NavLink
            to="/customers"
            className="nav-link"
            style={getLinkStyle}
            onClick={handleLinkClick}
          >
            <i className="bi bi-people"></i>
            <span>Customers</span>
          </NavLink>
        </li>

        {/* Service Providers */}
        <li className="nav-item">
          <NavLink
            to="/serviceproviders"
            className="nav-link"
            style={getLinkStyle}
            onClick={handleLinkClick}
          >
            <i className="bi bi-people"></i>
            <span>Service Providers</span>
          </NavLink>
        </li>

        {/* Query Page */}
        <li className="nav-item">
          <NavLink
            to="/querypage"
            className="nav-link"
            style={getLinkStyle}
            onClick={handleLinkClick}
          >
            <i className="bi bi-filter"></i>
            <span>Query </span>
          </NavLink>
        </li>

        {/* Guest Query */}
        <li className="nav-item">
          <NavLink
            to="/guestquerypage"
            className="nav-link"
            style={getLinkStyle}
            onClick={handleLinkClick}
          >
            <i className="bi bi-filter"></i>
            <span>Guest Query</span>
          </NavLink>
        </li>

        {/* Admin Management */}
        <li className="nav-item">
          <NavLink
            to="/adminmanagement"
            className="nav-link"
            style={getLinkStyle}
            onClick={handleLinkClick}
          >
            <i className="bi bi-filter"></i>
            <span>Admin Management </span>
          </NavLink>
        </li>
      </ul>
    </aside>
  );
};

export default Sidebar;
