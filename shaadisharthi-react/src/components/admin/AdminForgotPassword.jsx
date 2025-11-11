import { useNavigate, Link } from "react-router-dom";
import { useState } from "react";
import logoImg from "@assets/admin/imgg/logo.png";
import ValidatedForm from "../ValidatedForm";
import axiosInstance from "../../utils/http";
import { toast } from "react-toastify";
import { handleAxiosError } from "../../utils/handleAxiosError";

/**
 * AdminForgotPassword Component
 *
 * Purpose: Allows admin users to request a password reset link via email.
 * Flow:
 * 1. User enters registered email
 * 2. On submit → POST to `/admin/admin-auth/forgot-password`
 * 3. Show success toast (even if email not found — security best practice)
 * 4. Redirect to login page
 *
 * Uses ValidatedForm for client-side validation (HTML5 + custom feedback)
 */
const AdminForgotPassword = () => {
  // Hook for programmatic navigation after successful submission
  const navigate = useNavigate();

  // Loading state to disable button and show feedback during API call
  const [loading, setLoading] = useState(false);

  /**
   * handleForgotPassword
   *
   * @param {SubmitEvent} e - Form submission event
   *
   * Responsibilities:
   * - Prevent default form submission
   * - Extract email from form data
   * - Make secure API call to trigger password reset email
   * - Show non-revealing success message (prevents email enumeration)
   * - Handle errors gracefully via centralized error handler
   * - Manage loading state in finally block
   */
  const handleForgotPassword = async (e) => {
    e.preventDefault();
    setLoading(true);

    // FormData API used to easily extract named input values
    const formData = new FormData(e.target);
    const email = formData.get("email");

    try {
      // Axios POST request to backend forgot-password endpoint
      const res = await axiosInstance.post(
        "/admin/admin-auth/forgot-password",
        { email }
      );

      // Success: Show generic message to avoid revealing if email exists
      toast.success(
        res.data.message || "If registered, a reset link has been sent"
      );

      // Redirect to admin login after success
      navigate("/adminlogin");
    } catch (err) {
      // Centralized error handling with toast + console logging
      handleAxiosError(err, "Forgot password failed");
    } finally {
      // Always reset loading state regardless of success/failure
      setLoading(false);
    }
  };

  return (
    <main>
      {/* Outer container for layout consistency */}
      <div className="container">
        {/* Full-viewport section with flex centering */}
        <section className="section register min-vh-10 d-flex flex-column align-items-center justify-content-center py-4">
          <div className="container">
            <div className="row justify-content-center">
              {/* Responsive card width: 4 cols on lg, 6 on md */}
              <div className="col-lg-4 col-md-6 d-flex flex-column align-items-center justify-content-center">
                {/* Logo Section - Hidden text span for future branding */}
                <div className="d-flex justify-content-center">
                  <a className="logo d-flex align-items-center w-auto mb-3">
                    <img src={logoImg} alt="ShaadiSharthi" />
                    <span className="d-none d-lg-block"></span>
                  </a>
                </div>

                {/* Main Card Container */}
                <div className="card mb-3">
                  <div className="card-body">
                    {/* Card Header: Title + Instructions */}
                    <div className="pb-2">
                      <h5 className="card-title text-center pb-0 fs-4">
                        Forgot Password
                      </h5>
                      <p className="text-center small">
                        Enter your email to receive a reset link
                      </p>
                    </div>

                    {/* Form with client-side validation via ValidatedForm HOC */}
                    <ValidatedForm
                      onSubmit={handleForgotPassword}
                      className="row g-3 needs-validation"
                      noValidate // Let ValidatedForm handle validation, not browser
                    >
                      {/* Email Input Field */}
                      <div className="col-12">
                        <label htmlFor="yourEmail" className="form-label">
                          Your Email
                        </label>
                        <input
                          type="email"
                          name="email"
                          className="form-control"
                          id="yourEmail"
                          required // HTML5 validation trigger
                        />
                        {/* Bootstrap invalid feedback shown by ValidatedForm */}
                        <div className="invalid-feedback">
                          Please enter a valid Email address!
                        </div>
                      </div>

                      {/* Submit Button with loading state */}
                      <div className="col-12">
                        <button className="btn btn-primary w-100" type="submit">
                          {loading ? "Sending..." : "Send Reset Link"}
                        </button>
                      </div>

                      {/* Back to Login Link */}
                      <div className="col-12">
                        <p className="small mb-0">
                          Back to <Link to="/adminlogin">Login</Link>
                        </p>
                      </div>
                    </ValidatedForm>
                  </div>
                </div>

                {/* Footer Credits */}
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

export default AdminForgotPassword;
