import { useNavigate, Link } from "react-router-dom";
import { useAuth } from "../../auth/admin/useAuth";
import { useState } from "react";
import logoImg from "@assets/admin/imgg/logo.png";
import ValidatedForm from "../ValidatedForm";
import { toast } from "react-toastify";
import { handleAxiosError } from "../../utils/handleAxiosError";

/**
 * AdminLogin: Public entry point for admin authentication.
 *
 * Flow:
 * 1. User enters email/password (+ optional TOTP)
 * 2. Calls login() from AuthContext via useAuth hook
 * 3. Handles two response cases:
 *    - setupTotp: true → redirect to /totp-setup with email in state
 *    - full login → fetch account (via AuthProvider), show toast, go to dashboard
 *
 * Security:
 * - TOTP optional in UI but required if backend enforces it
 * - sessionStorage used for JWT (cleared on tab close)
 * - No sensitive data in localStorage
 */
const AdminLogin = () => {
  const navigate = useNavigate();
  const { login } = useAuth(); // Custom hook → useContext(AuthContext)

  // Local form state — not persisted
  const [formData, setFormData] = useState({
    email: "",
    password: "",
    totpCode: "",
  });
  const [loading, setLoading] = useState(false);

  // Generic input handler — keeps form controlled
  const handleInputChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  /**
   * Submit handler with full error handling and UX feedback.
   *
   * Why duplicate sessionStorage.setItem here?
   * → AuthContext.login already does this, but TOTP flow returns early.
   * → We must store token/adminId *immediately* to allow TOTPSetup access.
   * → Safe because backend only returns token on valid partial login.
   */
  const handleLogin = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      const result = await login({
        email: formData.email,
        password: formData.password,
        totpCode: formData.totpCode || null,
      });

      const { data, error } = result;

      if (error) throw new Error(error);

      // Case 1: TOTP setup required — store session + redirect
      if (data.setupTotp && data.token && data.adminId) {
        sessionStorage.setItem("admin_jwt", data.token);
        sessionStorage.setItem("admin_id", data.adminId);
        navigate("/totp-setup", { state: { email: data.email } });
        return;
      }

      // Case 2: Full login — AuthProvider will fetch account automatically
      if (data.token && data.adminId) {
        toast.success("Logged in successfully!");
        navigate("/"); // → ProtectedRoute → AdminLayout → Dashboard
      }
    } catch (err) {
      // Centralized Axios + network error handling with toast
      handleAxiosError(err, "Login failed");
    } finally {
      setLoading(false);
    }
  };

  return (
    <main>
      <div className="container">
        <section className="section register min-vh-10 d-flex flex-column align-items-center justify-content-center py-4">
          <div className="container">
            <div className="row justify-content-center">
              <div className="col-lg-4 col-md-6 d-flex flex-column align-items-center justify-content-center">
                <a className="logo d-flex align-items-center w-auto mb-3">
                  <img src={logoImg} alt="ShaadiSharthi" />
                </a>
                <div className="card mb-3">
                  <div className="card-body">
                    <h5 className="card-title text-center pb-0 fs-4">
                      Login to Your Account
                    </h5>
                    <p className="text-center small">
                      Enter credentials to login
                    </p>

                    {/*
                      ValidatedForm: Custom wrapper likely adds HTML5 validation + styling
                      noValidate disables browser default — we use custom validation
                    */}
                    <ValidatedForm
                      onSubmit={handleLogin}
                      className="row g-3 needs-validation"
                      noValidate
                    >
                      <div className="col-12">
                        <label htmlFor="yourEmail" className="form-label">
                          Your Email
                        </label>
                        <input
                          type="email"
                          name="email"
                          className="form-control"
                          id="yourEmail"
                          value={formData.email}
                          onChange={handleInputChange}
                          required
                        />
                      </div>

                      <div className="col-12">
                        <label htmlFor="yourPassword" className="form-label">
                          Password
                        </label>
                        <input
                          type="password"
                          name="password"
                          className="form-control"
                          id="yourPassword"
                          value={formData.password}
                          onChange={handleInputChange}
                          required
                        />
                      </div>

                      <div className="col-12">
                        <label htmlFor="totpCode" className="form-label">
                          TOTP Code (if enabled)
                        </label>
                        <input
                          type="text"
                          name="totpCode"
                          className="form-control"
                          id="totpCode"
                          value={formData.totpCode}
                          onChange={handleInputChange}
                          placeholder="Optional unless 2FA is required"
                        />
                      </div>

                      <div className="col-12">
                        <div className="form-check">
                          <input
                            className="form-check-input"
                            type="checkbox"
                            name="remember"
                            id="rememberMe"
                          />
                          <label
                            className="form-check-label"
                            htmlFor="rememberMe"
                          >
                            Remember me
                          </label>
                        </div>
                      </div>

                      <div className="col-12">
                        <button className="btn btn-primary w-100" type="submit">
                          {loading ? "Logging in..." : "Login"}
                        </button>
                      </div>

                      <div className="col-12">
                        <p className="small text-center mb-0 mt-2">
                          Forgot Password?
                          <Link to="/adminforgotpassword"> Reset it</Link>
                        </p>
                      </div>
                    </ValidatedForm>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>
      </div>
    </main>
  );
};

export default AdminLogin;
