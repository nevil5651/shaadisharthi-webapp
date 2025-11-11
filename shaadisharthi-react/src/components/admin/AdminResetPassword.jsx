import { useNavigate, useSearchParams } from "react-router-dom";
import { useState } from "react";
import logoImg from "@assets/admin/imgg/logo.png";
import ValidatedForm from "../ValidatedForm";
import axiosInstance from "../../utils/http";
import { toast } from "react-toastify";
import { handleAxiosError } from "../../utils/handleAxiosError";

/**
 * AdminResetPassword Component
 *
 * Purpose: Allows admin to set a new password using a secure token from email
 * Security: Validates presence of `token` in URL query params
 * Flow:
 * - Extract token from URL
 * - If missing → show "Invalid link" UI
 * - If present → render password form
 * - Submit { token, password } → backend validates & updates
 */
const AdminResetPassword = () => {
  // Navigation hook for post-success redirect
  const navigate = useNavigate();

  // Extract query parameters (e.g., ?token=abc123)
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token");

  // Loading state during password reset API call
  const [loading, setLoading] = useState(false);

  /**
   * handleResetPassword
   *
   * @param {SubmitEvent} e - Form submission event
   *
   * Responsibilities:
   * - Prevent default submission
   * - Extract new password
   * - Send token + password to backend
   * - Show success and redirect
   * - Use centralized error handling
   */
  const handleResetPassword = async (e) => {
    e.preventDefault();
    setLoading(true);

    const formData = new FormData(e.target);
    const password = formData.get("password");

    try {
      // POST to reset endpoint with token and new password
      const res = await axiosInstance.post("/admin/admin-auth/reset-password", {
        token,
        password,
      });

      // Success: Notify and redirect to login
      toast.success(res.data.message || "Password reset successfully");
      navigate("/adminlogin");
    } catch (err) {
      // Use shared error handler for consistent UX
      handleAxiosError(err, "Reset password failed");
    } finally {
      setLoading(false);
    }
  };

  // Early return: Invalid or missing token
  if (!token) {
    return (
      <main>
        <div className="container">
          <section className="section register min-vh-10 d-flex flex-column align-items-center justify-content-center py-4">
            <div className="container">
              <div className="row justify-content-center">
                <div className="col-lg-4 col-md-6">
                  {/* Security: Do not reveal why token is invalid */}
                  <p className="text-center">Invalid reset link.</p>
                  <a href="/adminlogin" className="btn btn-primary w-100">
                    Back to Login
                  </a>
                </div>
              </div>
            </div>
          </section>
        </div>
      </main>
    );
  }

  // Debug log (can be removed in production if not needed)
  console.log("Token present, rendering reset password form");

  return (
    <main>
      <div className="container">
        <section className="section register min-vh-10 d-flex flex-column align-items-center justify-content-center py-4">
          <div className="container">
            <div className="row justify-content-center">
              <div className="col-lg-4 col-md-6 d-flex flex-column align-items-center justify-content-center">
                {/* Logo */}
                <div className="d-flex justify-content-center">
                  <a className="logo d-flex align-items-center w-auto mb-3">
                    <img src={logoImg} alt="ShaadiSharthi" />
                    <span className="d-none d-lg-block"></span>
                  </a>
                </div>

                {/* Reset Form Card */}
                <div className="card mb-3">
                  <div className="card-body">
                    <div className="pb-2">
                      <h5 className="card-title text-center pb-0 fs-4">
                        Reset Password
                      </h5>
                      <p className="text-center small">
                        Enter your new password
                      </p>
                    </div>

                    <ValidatedForm
                      onSubmit={handleResetPassword}
                      className="row g-3 needs-validation"
                      noValidate
                    >
                      {/* Password Input with Strong Requirements */}
                      <div className="col-12">
                        <label htmlFor="yourPassword" className="form-label">
                          New Password
                        </label>
                        <input
                          type="password"
                          name="password"
                          className="form-control"
                          id="yourPassword"
                          required
                        />
                        {/* Custom validation message enforced by ValidatedForm */}
                        <div className="invalid-feedback">
                          Password must be at least 8 characters, with one
                          uppercase, one lowercase, one digit, and one special
                          character!
                        </div>
                      </div>

                      {/* Submit Button */}
                      <div className="col-12">
                        <button className="btn btn-primary w-100" type="submit">
                          {loading ? "Resetting..." : "Reset Password"}
                        </button>
                      </div>

                      {/* Login Link */}
                      <div className="col-12">
                        <p className="small mb-0">
                          Back to <a href="/adminlogin">Login</a>
                        </p>
                      </div>
                    </ValidatedForm>
                  </div>
                </div>

                {/* Credits */}
                <div className="credits">
                  Designed by{" "}
                  <a href="https://shaadisharthi.theworkpc.com/">
                    ShaadiSharthi
                  </a>
                </div>
              </div>
            </div>
          </div>
        </section>
      </div>
    </main>
  );
};

export default AdminResetPassword;
