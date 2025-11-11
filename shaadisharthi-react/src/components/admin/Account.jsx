// Admin account management page: Profile view/edit, password change
// Uses auth context for state management

import { useState, useEffect } from "react";
import profileImg from "@assets/admin/imgg/profile-img.jpg"; // Default profile image
import { useAuth } from "../../auth/admin/useAuth"; // Auth context hook
import axiosInstance from "../../utils/http"; // Configured Axios instance
import { toast } from "react-toastify"; // Notification library
import "react-toastify/dist/ReactToastify.css"; // Toast styles
import { handleAxiosError } from "../../utils/handleAxiosError"; // Error handler

/**
 * Account - Component for admin profile management
 */
const Account = () => {
  const { account, updateAccount, fetchAccount, logout } = useAuth(); // Auth actions from context

  // Local form state for editing
  const [formData, setFormData] = useState({
    name: "",
    phone: "",
    address: "",
    email: "",
  });

  // Sync formData with account from context
  useEffect(() => {
    if (account) {
      setFormData(account); // Update form with fetched account
    } else {
      fetchAccount(); // Fetch if not available
    }
  }, [account]); // Dep: Re-run on account change

  // Generic input change handler
  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
  };

  // Profile update submit handler
  const handleSubmit = async (e) => {
    e.preventDefault();

    const token = sessionStorage.getItem("admin_jwt");
    if (!token) {
      toast.error("Unauthorized: Please log in again.");
      logout(); // Trigger logout if no token
      return;
    }

    try {
      const res = await axiosInstance.post("admin/account", formData); // POST updated data
      const data = res.data;

      if (data.success) {
        toast.success("Profile updated successfully!");
        updateAccount(formData); // Update context
      } else {
        toast.error(data.message || "Failed to update profile");
      }
    } catch (err) {
      handleAxiosError(err, "Failed to update profile"); // Centralized error
    }
  };

  // Password change handler
  const handleChangePassword = async (e) => {
    e.preventDefault();
    const form = e.target;
    const currentPassword = form.password.value;
    const newPassword = form.newpassword.value;
    const renewPassword = form.renewpassword.value;

    // Client-side validations
    if (newPassword !== renewPassword) {
      toast.error("New passwords do not match");
      return;
    }

    if (currentPassword === newPassword) {
      toast.error("New password cannot be the same as current password");
      return;
    }

    const passwordRegex =
      /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/;
    if (!passwordRegex.test(newPassword)) {
      toast.error(
        "New password must include uppercase, lowercase, number, special char, and be at least 8 chars."
      );
      return;
    }

    try {
      const admin = sessionStorage.getItem("admin_id");
      const token = sessionStorage.getItem("admin_jwt");
      if (!admin || !token) {
        toast.error("Unauthorized: Please log in again.");
        logout();
        return;
      }

      // POST password change
      const res = await axiosInstance.post("admin/changepassword", {
        currentPassword,
        newPassword,
        adminId: admin,
      });

      if (res.status === 200) {
        toast.success("Password changed successfully. Logging out...");

        // Attempt backend logout
        try {
          await axiosInstance.post("admin/adminlogout");
        } catch (logoutErr) {
          console.error("Logout error:", logoutErr); // Log but continue
        }

        // Delayed frontend logout
        setTimeout(() => {
          logout(); // clears sessionStorage and redirects
        }, 2000);
      } else {
        toast.error(res.data.message || "Failed to change password");
      }
    } catch (err) {
      handleAxiosError(err, "Failed to change password");
    }
  };

  return (
    <main id="main" className="main">
      {/* Page Title & Breadcrumb */}
      <div className="pagetitle">
        <h1>Account</h1>
        <nav>
          <ol className="breadcrumb">
            <li className="breadcrumb-item">Home</li>
            <li className="breadcrumb-item active">Account</li>
          </ol>
        </nav>
      </div>

      <section className="section profile">
        <div className="row">
          {/* Profile Card */}
          <div className="col-xl-4">
            <div className="card">
              <div className="card-body profile-card pt-4 d-flex flex-column align-items-center">
                <img
                  src={profileImg}
                  alt="Profile"
                  className="rounded-circle"
                />
                <h2>{account.name || "UserName"}</h2> {/* Fallback name */}
              </div>
            </div>
          </div>

          {/* Tabs Container */}
          <div className="col-xl-8">
            <div className="card">
              <div className="card-body pt-3">
                {/* Tab Navigation */}
                <ul className="nav nav-tabs nav-tabs-bordered">
                  <li className="nav-item">
                    <button
                      className="nav-link active"
                      data-bs-toggle="tab"
                      data-bs-target="#profile-overview"
                    >
                      Overview
                    </button>
                  </li>
                  <li className="nav-item">
                    <button
                      className="nav-link"
                      data-bs-toggle="tab"
                      data-bs-target="#profile-edit"
                    >
                      Edit Profile
                    </button>
                  </li>
                  <li className="nav-item">
                    <button
                      className="nav-link"
                      data-bs-toggle="tab"
                      data-bs-target="#profile-change-password"
                    >
                      Change Password
                    </button>
                  </li>
                </ul>
                <div className="tab-content pt-2">
                  {/* Overview Tab */}
                  <div
                    className="tab-pane fade show active profile-overview"
                    id="profile-overview"
                  >
                    <h5 className="card-title">Profile Details</h5>
                    <div className="row">
                      <div className="col-lg-3 col-md-4 label">Full Name</div>
                      <div className="col-lg-9 col-md-8">{account.name}</div>
                    </div>
                    <div className="row">
                      <div className="col-lg-3 col-md-4 label">Phone</div>
                      <div className="col-lg-9 col-md-8">{account.phone}</div>
                    </div>
                    <div className="row">
                      <div className="col-lg-3 col-md-4 label">Email</div>
                      <div className="col-lg-9 col-md-8">{account.email}</div>
                    </div>
                    <div className="row">
                      <div className="col-lg-3 col-md-4 label">Address</div>
                      <div className="col-lg-9 col-md-8">{account.address}</div>
                    </div>
                  </div>

                  {/* Edit Profile Tab */}
                  <div
                    className="tab-pane fade profile-edit pt-3"
                    id="profile-edit"
                  >
                    <form onSubmit={handleSubmit}>
                      <div className="row mb-3">
                        <label
                          htmlFor="fullName"
                          className="col-md-4 col-lg-3 col-form-label"
                        >
                          Full Name
                        </label>
                        <div className="col-md-8 col-lg-9">
                          <input
                            name="name"
                            type="text"
                            className="form-control"
                            id="fullName"
                            onChange={handleChange}
                            value={formData.name}
                          />
                        </div>
                      </div>

                      <div className="row mb-3">
                        <label
                          htmlFor="Phone"
                          className="col-md-4 col-lg-3 col-form-label"
                        >
                          Phone
                        </label>
                        <div className="col-md-8 col-lg-9">
                          <input
                            name="phone"
                            type="text"
                            className="form-control"
                            id="Phone"
                            onChange={handleChange}
                            value={formData.phone}
                          />
                        </div>
                      </div>

                      <div className="row mb-3">
                        <label
                          htmlFor="Email"
                          className="col-md-4 col-lg-3 col-form-label"
                        >
                          Email
                        </label>
                        <div className="col-md-8 col-lg-9">
                          <input
                            name="email"
                            type="email"
                            className="form-control"
                            id="Email"
                            value={formData.email}
                            disabled // Email typically not editable
                          />
                        </div>
                      </div>

                      <div className="row mb-3">
                        <label
                          htmlFor="Address"
                          className="col-md-4 col-lg-3 col-form-label"
                        >
                          Address
                        </label>
                        <div className="col-md-8 col-lg-9">
                          <input
                            name="address"
                            type="text"
                            className="form-control"
                            id="Address"
                            onChange={handleChange}
                            value={formData.address}
                          />
                        </div>
                      </div>

                      <div className="text-center">
                        <button type="submit" className="btn btn-primary">
                          Save Changes
                        </button>
                      </div>
                    </form>
                  </div>

                  {/* Change Password Tab */}
                  <div
                    className="tab-pane fade pt-3"
                    id="profile-change-password"
                  >
                    <form onSubmit={handleChangePassword}>
                      <div className="row mb-3">
                        <label
                          htmlFor="currentPassword"
                          className="col-md-4 col-lg-3 col-form-label"
                        >
                          Current Password
                        </label>
                        <div className="col-md-8 col-lg-9">
                          <input
                            name="password"
                            type="password"
                            className="form-control"
                            id="currentPassword"
                            required
                          />
                        </div>
                      </div>

                      <div className="row mb-3">
                        <label
                          htmlFor="newPassword"
                          className="col-md-4 col-lg-3 col-form-label"
                        >
                          New Password
                        </label>
                        <div className="col-md-8 col-lg-9">
                          <input
                            name="newpassword"
                            type="password"
                            className="form-control"
                            id="newPassword"
                            required
                          />
                        </div>
                      </div>

                      <div className="row mb-3">
                        <label
                          htmlFor="renewPassword"
                          className="col-md-4 col-lg-3 col-form-label"
                        >
                          Re-enter New Password
                        </label>
                        <div className="col-md-8 col-lg-9">
                          <input
                            name="renewpassword"
                            type="password"
                            className="form-control"
                            id="renewPassword"
                            required
                          />
                        </div>
                      </div>

                      <div className="text-center">
                        <button type="submit" className="btn btn-primary">
                          Change Password
                        </button>
                      </div>
                    </form>
                  </div>
                </div>{" "}
                {/* End tab-content */}
              </div>{" "}
              {/* End card-body */}
            </div>{" "}
            {/* End card */}
          </div>
        </div>
      </section>
    </main>
  );
};

export default Account;
