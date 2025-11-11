import React, { useState, useEffect } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import QRCode from "react-qr-code";
import axiosInstance from "../../utils/http";
import { toast } from "react-toastify";
import { handleAxiosError } from "../../utils/handleAxiosError";
import { useAuth } from "../../auth/admin/useAuth";

/**
 * ErrorBoundary: Class-based fallback UI for uncaught errors in TOTP flow.
 *
 * Why class component?
 * → Only class components can use componentDidCatch & getDerivedStateFromError
 * → Critical for production: prevents white screen if QRCode or API fails
 *
 * Renders clean error card with reload button — maintains branding
 */
class ErrorBoundary extends React.Component {
  state = { hasError: false };

  static getDerivedStateFromError(error) {
    return { hasError: true };
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="container">
          <section className="section register min-vh-10 d-flex flex-column align-items-center justify-content-center py-4">
            <div className="card mb-3">
              <div className="card-body text-center">
                <h5>Something went wrong</h5>
                <p>Please try again or contact support.</p>
                <button
                  className="btn btn-primary"
                  onClick={() => window.location.reload()}
                >
                  Reload
                </button>
              </div>
            </div>
          </section>
        </div>
      );
    }
    return this.props.children;
  }
}

/**
 * TOTPSetup: Intermediate step after partial login when 2FA is required.
 *
 * Flow:
 * 1. User lands here from AdminLogin with email in location.state
 * 2. Must have valid JWT in sessionStorage (set during login)
 * 3. Click "Generate QR" → backend returns secret + otpauth URL
 * 4. Scan QR → enter code via prompt → verify
 * 5. On success → logout + redirect to /login (force full re-auth with 2FA)
 *
 * Security:
 * - JWT required for all API calls
 * - No sensitive data in URL or localStorage
 * - Prompt for code (not input) → avoids form persistence
 */
const TOTPSetup = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const [email, setEmail] = useState("");
  const [qrCodeUrl, setQrCodeUrl] = useState("");
  const [totpSecret, setTotpSecret] = useState("");
  const [loading, setLoading] = useState(false);
  const token = sessionStorage.getItem("admin_jwt");
  const { logout } = useAuth();

  /**
   * On mount: validate session and email from navigation state
   * - No token → redirect to login
   * - No email in state → unsafe, redirect
   */
  useEffect(() => {
    if (!token) {
      navigate("/adminlogin");
    } else {
      const stateEmail = location.state?.email;
      if (stateEmail) {
        setEmail(stateEmail);
      } else {
        console.warn("Email not found in state, redirecting to login");
        navigate("/adminlogin");
      }
    }
  }, [navigate, token, location.state]);

  /**
   * Generate TOTP secret and QR code from backend.
   * Called once — enables QR display.
   */
  const handleSetup = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      const res = await axiosInstance.post(
        "/admin/admin-auth/totp",
        {
          action: "setup",
          adminId: sessionStorage.getItem("admin_id"),
        },
        {
          headers: { Authorization: `Bearer ${token}` },
        }
      );
      console.log("Response:", res.data);
      setTotpSecret(res.data.totpSecret);
      setQrCodeUrl(res.data.qrCodeUrl);
      toast.success(
        "TOTP setup initiated. Scan the QR code with a trusted authenticator app (e.g., Google Authenticator) now.",
        {
          autoClose: 5000,
        }
      );
    } catch (err) {
      handleAxiosError(err, "TOTP setup failed");
    } finally {
      setLoading(false);
    }
  };

  /**
   * Verify TOTP code entered in authenticator app.
   * Uses prompt() — avoids storing code in React state or DOM.
   * On success: logout + force full re-login with 2FA.
   */
  const handleVerify = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      const totpCode = prompt("Enter TOTP code from your authenticator app:");
      if (!totpCode) throw new Error("TOTP code is required");

      const res = await axiosInstance.post(
        "/admin/admin-auth/totp",
        {
          action: "verify",
          adminId: sessionStorage.getItem("admin_id"),
          totpCode,
        },
        {
          headers: { Authorization: `Bearer ${token}` },
        }
      );
      if (res.data.message === "2FA verified.") {
        toast.success("TOTP verified successfully!", {
          position: "top-center",
          autoClose: 3000,
        });
        logout(); // Clears all auth state
        navigate("/login"); // Force full login with 2FA now required
      }
    } catch (err) {
      handleAxiosError(err, "TOTP verification failed");
    } finally {
      setLoading(false);
    }
  };

  return (
    <ErrorBoundary>
      <main>
        <div className="container">
          <section className="section register min-vh-10 d-flex flex-column align-items-center justify-content-center py-4">
            <div className="container">
              <div className="row justify-content-center">
                <div className="col-lg-4 col-md-6 d-flex flex-column align-items-center justify-content-center">
                  <div className="card mb-3">
                    <div className="card-body">
                      <div className="pb-2">
                        <h5 className="card-title text-center pb-0 fs-4">
                          Admin, Confirm with TOTP
                        </h5>
                        <p className="text-center small">
                          Set up two-factor authentication for enhanced security
                        </p>
                        <p className="text-center text-warning small">
                          <strong>Warning:</strong> Use a trusted authenticator
                          app (e.g., Google Authenticator, Authy) and scan in a
                          secure environment.
                        </p>
                      </div>

                      {/* Step 1: Generate QR */}
                      <form
                        onSubmit={handleSetup}
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
                            value={email}
                            readOnly
                          />
                          <div className="invalid-feedback">
                            Please enter a valid Email address!
                          </div>
                        </div>

                        <div className="col-12">
                          <button
                            className="btn btn-primary w-100"
                            type="submit"
                            disabled={loading}
                          >
                            {loading ? "Setting up..." : "Generate QR Code"}
                          </button>
                        </div>
                      </form>

                      {/* Step 2: Show QR + Verify */}
                      {qrCodeUrl && (
                        <div className="text-center mt-3">
                          <h6>Scan this QR code with your authenticator app</h6>
                          <div
                            style={{
                              backgroundColor: "white",
                              padding: "16px",
                            }}
                          >
                            <QRCode value={qrCodeUrl} size={200} />
                          </div>
                          <p className="mt-2">Secret: {totpSecret}</p>
                          <button
                            className="btn btn-success mt-2"
                            onClick={handleVerify}
                            disabled={loading}
                          >
                            {loading ? "Verifying..." : "Verify TOTP"}
                          </button>
                          <p className="small">
                            After scanning, click Verify to complete setup.
                          </p>
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </section>
        </div>
      </main>
    </ErrorBoundary>
  );
};

export default TOTPSetup;
