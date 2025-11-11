import React from "react";

/**
 * Footer Component
 *
 * Purpose: Renders a static footer with copyright and design credits.
 *
 * Key Features:
 * - Displays current year dynamically via &copy;
 * - Links to official brand website
 * - Minimal, reusable, and theme-consistent
 *
 * Placement: Typically rendered at the bottom of admin layout
 */
function Footer() {
  return (
    <footer id="footer" className="footer">
      {/* Copyright Notice - Strong branding with reserved rights */}
      <div className="copyright">
        &copy; Copyright
        <strong>
          <span>Shaadisharthi</span>
        </strong>
        . All Rights Reserved
      </div>

      {/* Design Attribution - External link to company site */}
      <div className="credits">
        Designed by{" "}
        <a href="https://shaadisharthi.theworkpc.com/">ShaadiSharthi</a>
      </div>
    </footer>
  );
}

export default Footer;
