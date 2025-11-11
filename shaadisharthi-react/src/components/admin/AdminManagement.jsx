import React, { useState } from "react";
import { toast } from "react-toastify";

// Mock data simulating backend response
const mockAdmins = [
  {
    id: "1",
    name: "Super Admin",
    email: "super@lottery.com",
    role: "SUPER_ADMIN",
    status: "active",
  },
  {
    id: "2",
    name: "Finance Manager",
    email: "finance@lottery.com",
    role: "FINANCE",
    status: "active",
  },
  {
    id: "3",
    name: "Support Agent",
    email: "support@lottery.com",
    role: "SUPPORT",
    status: "suspended",
  },
  {
    id: "4",
    name: "New Admin",
    email: "newadmin@lottery.com",
    role: "SUPPORT",
    status: "invited",
  },
];

const rolePermissions = {
  SUPER_ADMIN: [
    "USER_READ",
    "USER_WRITE",
    "DRAW_READ",
    "DRAW_WRITE",
    "RESULT_PUBLISH",
    "FINANCE_READ",
    "FINANCE_WRITE",
    "SUPPORT_HANDLE",
    "SETTINGS_WRITE",
    "AUDIT_VIEW",
    "ADMIN_MANAGE",
  ],
  FINANCE: ["FINANCE_READ", "FINANCE_WRITE"],
  SUPPORT: ["SUPPORT_HANDLE"],
};

const permissionLabels = {
  USER_READ: "View Users",
  USER_WRITE: "Manage Users",
  DRAW_READ: "View Draws",
  DRAW_WRITE: "Manage Draws",
  RESULT_PUBLISH: "Publish Results",
  FINANCE_READ: "View Financials",
  FINANCE_WRITE: "Manage Financials",
  SUPPORT_HANDLE: "Handle Support",
  SETTINGS_WRITE: "Modify Settings",
  AUDIT_VIEW: "View Audit Logs",
  ADMIN_MANAGE: "Manage Admins",
};

const AdminManagementMock = () => {
  const [admins] = useState(mockAdmins);
  const [totalPages] = useState(1);
  const [currentPage] = useState(1);
  const [selectedAdmin, setSelectedAdmin] = useState(null);
  const [roleMatrixOpen, setRoleMatrixOpen] = useState(false);
  const [editRole, setEditRole] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleEditRole = (admin) => {
    setSelectedAdmin(admin);
    setEditRole(admin.role);
  };

  const handleRoleSubmit = (e) => {
    e.preventDefault();
    if (!editRole) {
      toast.error("Role is required");
      return;
    }

    setIsSubmitting(true);
    setTimeout(() => {
      toast.success("Role updated successfully");
      setSelectedAdmin(null);
      setEditRole("");
      setIsSubmitting(false);
    }, 2000); // Simulate backend delay
  };

  return (
    <main id="main" className="main">
      <div className="pagetitle">
        <h1>Admin Management</h1>
        <nav className="d-flex justify-content-between">
          <ol className="breadcrumb">
            <li className="breadcrumb-item">Home</li>
            <li className="breadcrumb-item active">Admin Management</li>
          </ol>
          <button
            className="btn btn-primary"
            onClick={() => setRoleMatrixOpen(true)}
          >
            <i className="bi bi-shield-lock me-2"></i>View Role Matrix
          </button>
        </nav>
      </div>

      <section className="section">
        <div className="card">
          <div className="card-body">
            <h5 className="card-title">Administrators</h5>
            <div className="table-responsive">
              <table className="table table-hover">
                <thead>
                  <tr>
                    <th>Name</th>
                    <th>Email</th>
                    <th>Role</th>
                    <th>Status</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {admins.length > 0 ? (
                    admins.map((admin) => (
                      <tr key={admin.id}>
                        <td>{admin.name}</td>
                        <td>{admin.email}</td>
                        <td
                          style={{
                            fontWeight: 700,
                            color:
                              admin.role === "SUPER_ADMIN"
                                ? "#d32f2f"
                                : "#1976d2",
                          }}
                        >
                          {admin.role}
                        </td>
                        <td
                          style={{
                            color:
                              admin.status === "active" ? "green" : "gray",
                          }}
                        >
                          {admin.status}
                        </td>
                        <td>
                          <button
                            className="btn btn-primary btn-sm"
                            onClick={() => handleEditRole(admin)}
                          >
                            <i className="bi bi-pencil"></i> Edit Role
                          </button>
                        </td>
                      </tr>
                    ))
                  ) : (
                    <tr>
                      <td colSpan="5">No admins found.</td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>

            {totalPages > 1 && (
              <nav aria-label="Page navigation" className="mt-3">
                <ul className="pagination justify-content-center">
                  <li className="page-item disabled">
                    <button className="page-link">Previous</button>
                  </li>
                  <li className="page-item active">
                    <button className="page-link">1</button>
                  </li>
                  <li className="page-item disabled">
                    <button className="page-link">Next</button>
                  </li>
                </ul>
              </nav>
            )}
          </div>
        </div>
      </section>

      {selectedAdmin && (
        <>
          <div className="custom-backdrop" onClick={() => setSelectedAdmin(null)}></div>
          <div
            className="modal d-block fade show"
            tabIndex="-1"
            role="dialog"
            style={{ zIndex: 1055 }}
          >
            <div className="modal-dialog modal-dialog-centered">
              <div className="modal-content">
                <div className="modal-header">
                  <h5 className="modal-title">Edit Role for {selectedAdmin.name}</h5>
                  <button
                    type="button"
                    className="btn-close"
                    onClick={() => setSelectedAdmin(null)}
                  ></button>
                </div>
                <form onSubmit={handleRoleSubmit}>
                  <div className="modal-body">
                    <div className="form-group">
                      <label htmlFor="role">Admin Role</label>
                      <select
                        className="form-control"
                        id="role"
                        value={editRole}
                        onChange={(e) => setEditRole(e.target.value)}
                      >
                        <option value="">Select Role</option>
                        {Object.keys(rolePermissions).map((role) => (
                          <option key={role} value={role}>
                            {role}
                          </option>
                        ))}
                      </select>
                    </div>
                  </div>
                  <div className="modal-footer">
                    <button
                      type="button"
                      className="btn btn-secondary"
                      onClick={() => setSelectedAdmin(null)}
                    >
                      Cancel
                    </button>
                    <button
                      type="submit"
                      className="btn btn-primary"
                      disabled={isSubmitting}
                    >
                      {isSubmitting ? (
                        <>
                          <span
                            className="spinner-border spinner-border-sm me-2"
                            role="status"
                            aria-hidden="true"
                          ></span>
                          Saving...
                        </>
                      ) : (
                        "Save Changes"
                      )}
                    </button>
                  </div>
                </form>
              </div>
            </div>
          </div>
        </>
      )}

      {roleMatrixOpen && (
        <>
          <div className="custom-backdrop" onClick={() => setRoleMatrixOpen(false)}></div>
          <div
            className="modal d-block fade show"
            tabIndex="-1"
            role="dialog"
            style={{ zIndex: 1055 }}
          >
            <div className="modal-dialog modal-dialog-centered modal-lg">
              <div className="modal-content">
                <div className="modal-header">
                  <h5 className="modal-title">Role-Permission Matrix</h5>
                  <button
                    type="button"
                    className="btn-close"
                    onClick={() => setRoleMatrixOpen(false)}
                  ></button>
                </div>
                <div className="modal-body">
                  <div className="table-responsive">
                    <table className="table table-bordered">
                      <thead>
                        <tr>
                          <th>Permission</th>
                          {Object.keys(rolePermissions).map((role) => (
                            <th key={role} className="text-center">
                              {role}
                            </th>
                          ))}
                        </tr>
                      </thead>
                      <tbody>
                        {Object.keys(permissionLabels).map((permission) => (
                          <tr key={permission}>
                            <td>{permissionLabels[permission]}</td>
                            {Object.keys(rolePermissions).map((role) => (
                              <td key={`${role}-${permission}`} className="text-center">
                                {rolePermissions[role].includes(permission) ? "✓" : "✗"}
                              </td>
                            ))}
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
                <div className="modal-footer">
                  <button
                    type="button"
                    className="btn btn-primary"
                    onClick={() => setRoleMatrixOpen(false)}
                  >
                    Close
                  </button>
                </div>
              </div>
            </div>
          </div>
        </>
      )}
    </main>
  );
};

export default AdminManagementMock;
