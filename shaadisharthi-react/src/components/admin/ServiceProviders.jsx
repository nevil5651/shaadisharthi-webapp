import React, { useEffect, useState } from "react";
import axiosInstance from "../../utils/http";
import { toast } from "react-toastify";

/**
 * Main component for managing service providers in the admin panel.
 * Supports listing, search, approval/rejection workflow, edit, delete.
 */
const ServiceProviders = () => {
  // List of service providers (filtered or all)
  const [serviceProviders, setServiceProviders] = useState([]);
  // Provider currently selected for viewing details
  const [selectedProvider, setSelectedProvider] = useState(null);
  // Provider being edited
  const [editProvider, setEditProvider] = useState(null);
  // Controls search modal visibility
  const [showSearchModal, setShowSearchModal] = useState(false);
  // Controls reject reason modal
  const [showRejectModal, setShowRejectModal] = useState(false); // New state for reject modal
  // Reason entered for rejecting a provider
  const [rejectReason, setRejectReason] = useState(""); // New state for rejection reason
  // ID of the provider being rejected
  const [rejectProviderId, setRejectProviderId] = useState(null); // New state for provider ID to reject
  // Search form fields
  const [searchForm, setSearchForm] = useState({
    provider_id: "",
    name: "",
    email: "",
    phone_no: "",
    alternate_phone: "",
    city: "",
    state: "",
    business_name: "",
    gst_number: "",
    aadhar_number: "",
    pan_number: "",
  });
  // Pagination state
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  // Whether current view is from search results
  const [isSearchActive, setIsSearchActive] = useState(false);
  // Toggle to show only providers pending approval
  const [showNotApproved, setShowNotApproved] = useState(false);
  // Items per page
  const limit = 15;

  /**
   * Updates search form state on input change.
   */
  const handleSearchInputChange = (e) => {
    const { name, value } = e.target;
    setSearchForm((prev) => ({ ...prev, [name]: value }));
  };

  /**
   * Fetches service providers with optional status filter.
   * Used for normal list and pending approval view.
   * @param {number} page - Page to fetch
   * @param {string|null} statusFilter - 'pending_approval' or null
   */
  const fetchServiceProviders = (page = 1, statusFilter = null) => {
    const url = statusFilter
      ? `admin/service-providers?status=${statusFilter}&page=${page}&limit=${limit}`
      : `admin/service-providers?page=${page}&limit=${limit}`;

    axiosInstance
      .get(url)
      .then((response) => {
        setServiceProviders(response.data.providers || []);
        const totalCount = response.data.totalCount || 0;
        setTotalPages(Math.ceil(totalCount / limit) || 1);
        setIsSearchActive(false);
      })
      .catch((err) => {
        handleAxiosError(err, "Failed to fetch service providers");
        setServiceProviders([]);
        setTotalPages(1);
        setIsSearchActive(false);
      });
  };

  /**
   * Effect to reload data when page, filter, or search state changes.
   */
  useEffect(() => {
    if (!isSearchActive) {
      fetchServiceProviders(
        currentPage,
        showNotApproved ? "pending_approval" : null
      );
    } else {
      handleSearch(currentPage);
    }
  }, [currentPage, showNotApproved, isSearchActive]);

  /**
   * Deletes a provider after confirmation.
   * Refreshes appropriate list after deletion.
   */
  const handleDelete = (providerId) => {
    if (
      window.confirm(
        `Are you sure you want to delete Provider with ID: ${providerId}?`
      )
    ) {
      const formData = new URLSearchParams();
      formData.append("provider_id", providerId);

      axiosInstance
        .post("admin/service-providers/delete", formData, {
          headers: { "Content-Type": "application/x-www-form-urlencoded" },
        })
        .then((response) => {
          if (response.data.message) {
            toast.success(response.data.message);
            if (isSearchActive) {
              handleSearch(currentPage);
            } else {
              fetchServiceProviders(
                currentPage,
                showNotApproved ? "pending_approval" : null
              );
            }
          } else {
            toast.error(response.data.error || "Failed to delete provider");
          }
        })
        .catch((err) => {
          handleAxiosError(err, "Error while deleting provider");
        });
    }
  };

  /**
   * Saves edited provider data.
   * Closes modal and refreshes list.
   */
  const handleEditSubmit = (e) => {
    e.preventDefault();
    const updated = {
      providerId: editProvider.providerId,
      name: editProvider.name,
      phoneNumber: editProvider.phoneNumber,
      alternatePhoneNumber: editProvider.alternatePhoneNumber,
      address: editProvider.address,
      city: editProvider.city,
      state: editProvider.state,
    };

    axiosInstance
      .post("admin/service-providers/edit", updated)
      .then((response) => {
        toast.success(
          response.data.message || "Provider updated successfully!"
        );
        if (isSearchActive) {
          handleSearch(currentPage);
        } else {
          fetchServiceProviders(
            currentPage,
            showNotApproved ? "pending_approval" : null
          );
        }
        setEditProvider(null);
        document.querySelector("#editProviderModal .btn-close").click();
      })
      .catch((err) => {
        toast.error(err.response?.data?.message || "Failed to update provider");
      });
  };

  /**
   * Executes search with current form values.
   * Only sends non-empty fields.
   */
  const handleSearch = (page = 1) => {
    const form = new URLSearchParams();
    Object.entries(searchForm).forEach(([k, v]) => {
      if (v.trim()) form.append(k, v);
    });

    axiosInstance
      .post(
        `admin/service-providers/search?page=${page}&limit=${limit}`,
        form,
        {
          headers: {
            "Content-Type": "application/x-www-form-urlencoded",
          },
        }
      )
      .then((response) => {
        if (response.data.providers && response.data.providers.length > 0) {
          setServiceProviders(response.data.providers);
          setTotalPages(Math.ceil(response.data.totalCount / limit) || 1);
          setIsSearchActive(true);
          toast.success(
            `${response.data.providers.length} matching providers found`
          );
          setShowSearchModal(false);
        } else {
          toast.info("No matching providers found");
          setServiceProviders([]);
          setTotalPages(1);
          setIsSearchActive(false);
        }
      })
      .catch((err) => {
        toast.error(err.response?.data?.message || "Search failed");
        setServiceProviders([]);
        setTotalPages(1);
        setIsSearchActive(false);
      });
  };

  /**
   * Approves a pending provider.
   * Sends status update to backend.
   */
  const handleApprove = (providerId) => {
    if (
      window.confirm(
        `Are you sure you want to approve Provider with ID: ${providerId}?`
      )
    ) {
      const formData = new URLSearchParams();
      formData.append("provider_id", providerId);
      formData.append("status", "approved");

      axiosInstance
        .post("admin/update-status", formData, {
          headers: { "Content-Type": "application/x-www-form-urlencoded" },
        })
        .then((response) => {
          if (response.data.message) {
            toast.success(response.data.message);
            fetchServiceProviders(currentPage, "pending_approval");
          } else {
            toast.error(response.data.error || "Failed to approve provider");
          }
        })
        .catch((err) => {
          toast.error(
            err.response?.data?.message || "Failed to approve provider"
          );
        });
    }
  };

  /**
   * Opens reject modal and stores provider ID.
   */
  const handleReject = (providerId) => {
    setRejectProviderId(providerId);
    setRejectReason("");
    setShowRejectModal(true);
  };

  /**
   * Submits rejection with reason.
   * Validates reason before sending.
   */
  const handleRejectSubmit = (e) => {
    e.preventDefault();
    if (!rejectReason.trim()) {
      toast.error("Please provide a rejection reason");
      return;
    }

    const formData = new URLSearchParams();
    formData.append("provider_id", rejectProviderId);
    formData.append("status", "rejected");
    formData.append("rejection_reason", rejectReason);

    axiosInstance
      .post("admin/update-status", formData, {
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
      })
      .then((response) => {
        if (response.data.message) {
          toast.success(response.data.message);
          fetchServiceProviders(currentPage, "pending_approval");
        } else {
          toast.error(response.data.error || "Failed to reject provider");
        }
        setShowRejectModal(false);
        setRejectReason("");
        setRejectProviderId(null);
      })
      .catch((err) => {
        toast.error(err.response?.data?.message || "Failed to reject provider");
        setShowRejectModal(false);
        setRejectReason("");
        setRejectProviderId(null);
      });
  };

  /**
   * Safely changes page within bounds.
   */
  const handlePageChange = (newPage) => {
    if (newPage >= 1 && newPage <= totalPages) {
      setCurrentPage(newPage);
    }
  };

  /**
   * Toggles between all providers and pending approval list.
   * Resets pagination and search state.
   */
  const handleToggleNotApproved = () => {
    setShowNotApproved((prev) => !prev);
    setCurrentPage(1);
    setIsSearchActive(false);
  };

  return (
    <main id="main" className="main">
      <div className="pagetitle">
        <h1>Service Providers</h1>
        <nav className="d-flex flex-column align-items-start flex-md-row justify-content-md-between align-items-md-center">
          <ol className="breadcrumb">
            <li className="breadcrumb-item">Home</li>
            <li className="breadcrumb-item active">Service Providers</li>
          </ol>
          <div className="d-flex flex-column flex-sm-row mt-3 mt-md-0 align-self-center align-items-center">
            {/* Search button */}
            <button
              className="btn btn-primary mb-2 mb-sm-0 me-sm-2"
              onClick={() => setShowSearchModal(true)}
            >
              Search user
            </button>
            {/* Toggle between all and pending */}
            <button
              className={`btn ${
                showNotApproved ? "btn-success" : "btn-secondary"
              }`}
              onClick={handleToggleNotApproved}
            >
              {showNotApproved
                ? "Show All Providers"
                : "Show Pending Approval Providers"}
            </button>
          </div>
        </nav>

        {/* Search Modal */}
        {showSearchModal && (
          <>
            <div className="custom-backdrop"></div>
            <div
              className="modal d-block"
              tabIndex="-1"
              role="dialog"
              style={{ zIndex: 1055 }}
            >
              <div
                className="modal-dialog modal-dialog-centered"
                role="document"
              >
                <div className="modal-content">
                  <div className="modal-header">
                    <h5 className="modal-title">Enter the fields</h5>
                    <button
                      type="button"
                      className="btn-close"
                      onClick={() => setShowSearchModal(false)}
                    ></button>
                  </div>
                  <div className="modal-body">
                    <form
                      onSubmit={(e) => {
                        e.preventDefault();
                        setCurrentPage(1);
                        setIsSearchActive(true);
                        handleSearch(1);
                      }}
                      className="row g-3"
                    >
                      {[
                        { label: "Service Provider ID", name: "provider_id" },
                        { label: "Name", name: "name" },
                        { label: "Email", name: "email" },
                        { label: "Phone Number", name: "phone_no" },
                        { label: "Alternate Phone", name: "alternate_phone" },
                        { label: "City", name: "city" },
                        { label: "State", name: "state" },
                        { label: "Business Name", name: "business_name" },
                        { label: "GST Number", name: "gst_number" },
                        { label: "Aadhaar Number", name: "aadhar_number" },
                        { label: "PAN Number", name: "pan_number" },
                      ].map((f) => (
                        <div className="col-md-4 col-md-6 col-12" key={f.name}>
                          <label>{f.label}</label>
                          <input
                            type="text"
                            className="form-control"
                            name={f.name}
                            value={searchForm[f.name]}
                            onChange={handleSearchInputChange}
                            placeholder={`Enter ${f.label}`}
                          />
                        </div>
                      ))}
                      <div className="col-12">
                        <button type="submit" className="btn btn-primary">
                          Search
                        </button>
                      </div>
                    </form>
                  </div>
                </div>
              </div>
            </div>
          </>
        )}

        {/* Reject Reason Modal */}
        {showRejectModal && (
          <>
            <div className="custom-backdrop"></div>
            <div
              className="modal d-block"
              tabIndex="-1"
              role="dialog"
              style={{ zIndex: 1055 }}
            >
              <div
                className="modal-dialog modal-dialog-centered"
                role="document"
              >
                <div className="modal-content">
                  <div className="modal-header">
                    <h5 className="modal-title">Reject Service Provider</h5>
                    <button
                      type="button"
                      className="btn-close"
                      onClick={() => {
                        setShowRejectModal(false);
                        setRejectReason("");
                        setRejectProviderId(null);
                      }}
                    ></button>
                  </div>
                  <form onSubmit={handleRejectSubmit}>
                    <div className="modal-body">
                      <div className="form-group">
                        <label>Reason for Rejection</label>
                        <textarea
                          className="form-control"
                          value={rejectReason}
                          onChange={(e) => setRejectReason(e.target.value)}
                          placeholder="Enter the reason for rejecting this provider"
                          rows="4"
                          required
                        />
                      </div>
                    </div>
                    <div className="modal-footer">
                      <button type="submit" className="btn btn-danger">
                        Submit Rejection
                      </button>
                      <button
                        type="button"
                        className="btn btn-secondary"
                        onClick={() => {
                          setShowRejectModal(false);
                          setRejectReason("");
                          setRejectProviderId(null);
                        }}
                      >
                        Cancel
                      </button>
                    </div>
                  </form>
                </div>
              </div>
            </div>
          </>
        )}
      </div>

      <section className="section">
        <div className="row">
          <div className="col-lg-12">
            <div className="card">
              <div className="card-body">
                <h5 className="card-title">
                  {showNotApproved
                    ? "Pending Approval Providers"
                    : "Recently Added Providers"}
                </h5>
                <div className="table-responsive">
                  <table className="table datatable">
                    <thead>
                      <tr>
                        <th>Sr No.</th>
                        <th>User ID</th>
                        <th>Name</th>
                        <th>Registration Date</th>
                        <th>Status</th>
                        <th>Actions</th>
                      </tr>
                    </thead>
                    <tbody>
                      {serviceProviders.length > 0 ? (
                        serviceProviders.map((p, i) => (
                          <tr key={p.providerId}>
                            <td>{(currentPage - 1) * limit + i + 1}</td>
                            <td>{p.providerId}</td>
                            <td>{p.name}</td>
                            <td>{p.createdAt}</td>
                            <td>{p.status}</td>
                            <td>
                              <ul className="list-inline m-0">
                                <li className="list-inline-item">
                                  {/* View details */}
                                  <button
                                    className="btn btn-primary btn-sm"
                                    title="Details"
                                    data-bs-toggle="modal"
                                    data-bs-target="#viewProviderModal"
                                    onClick={() => setSelectedProvider(p)}
                                  >
                                    <i className="bi bi-eye"></i>
                                  </button>
                                </li>
                                {showNotApproved ? (
                                  <>
                                    <li className="list-inline-item">
                                      {/* Approve action */}
                                      <button
                                        className="btn btn-success btn-sm"
                                        title="Approve"
                                        onClick={() =>
                                          handleApprove(p.providerId)
                                        }
                                      >
                                        <i className="bi bi-check-circle"></i>
                                      </button>
                                    </li>
                                    <li className="list-inline-item">
                                      {/* Reject action */}
                                      <button
                                        className="btn btn-danger btn-sm"
                                        title="Reject"
                                        onClick={() =>
                                          handleReject(p.providerId)
                                        }
                                      >
                                        <i className="bi bi-x-circle"></i>
                                      </button>
                                    </li>
                                  </>
                                ) : (
                                  <>
                                    <li className="list-inline-item">
                                      {/* Edit action */}
                                      <button
                                        className="btn btn-success btn-sm"
                                        title="Edit"
                                        data-bs-toggle="modal"
                                        data-bs-target="#editProviderModal"
                                        onClick={() => setEditProvider(p)}
                                      >
                                        <i className="bi bi-pencil"></i>
                                      </button>
                                    </li>
                                    <li className="list-inline-item">
                                      {/* Delete action */}
                                      <button
                                        className="btn btn-danger btn-sm"
                                        title="Delete"
                                        onClick={() =>
                                          handleDelete(p.providerId)
                                        }
                                      >
                                        <i className="bi bi-trash"></i>
                                      </button>
                                    </li>
                                  </>
                                )}
                              </ul>
                            </td>
                          </tr>
                        ))
                      ) : (
                        <tr>
                          <td colSpan="6">No providers found.</td>
                        </tr>
                      )}
                    </tbody>
                  </table>
                </div>

                {/* Pagination */}
                {totalPages > 1 && (
                  <nav aria-label="Page navigation" className="mt-3">
                    <ul className="pagination justify-content-center">
                      <li
                        className={`page-item ${
                          currentPage === 1 ? "disabled" : ""
                        }`}
                      >
                        <button
                          className="page-link"
                          onClick={() => handlePageChange(currentPage - 1)}
                        >
                          <span>« Previous</span>
                        </button>
                      </li>
                      {Array.from({ length: totalPages }, (_, i) => i + 1).map(
                        (page) => (
                          <li
                            key={page}
                            className={`page-item ${
                              page === currentPage ? "active" : ""
                            }`}
                          >
                            <button
                              className="page-link"
                              onClick={() => handlePageChange(page)}
                            >
                              {page}
                            </button>
                          </li>
                        )
                      )}
                      <li
                        className={`page-item ${
                          currentPage === totalPages ? "disabled" : ""
                        }`}
                      >
                        <button
                          className="page-link"
                          onClick={() => handlePageChange(currentPage + 1)}
                        >
                          <span>Next »</span>
                        </button>
                      </li>
                    </ul>
                  </nav>
                )}

                {/* View Provider Details Modal */}
                <div
                  className="modal fade"
                  id="viewProviderModal"
                  tabIndex="-1"
                >
                  <div className="modal-dialog modal-dialog-centered modal-lg">
                    <div className="modal-content">
                      <div className="modal-header">
                        <h5 className="modal-title">
                          Service Provider Details
                        </h5>
                        <button
                          type="button"
                          className="btn-close"
                          data-bs-dismiss="modal"
                        ></button>
                      </div>
                      <div className="modal-body">
                        {selectedProvider && (
                          <div className="d-flex flex-column gap-2">
                            {[
                              {
                                label: "Provider ID",
                                value: selectedProvider.providerId,
                              },
                              { label: "Name", value: selectedProvider.name },
                              {
                                label: "Created At",
                                value: selectedProvider.createdAt,
                              },
                              {
                                label: "Status",
                                value: selectedProvider.status,
                              },
                              {
                                label: "Phone No",
                                value: selectedProvider.phoneNumber,
                              },
                              {
                                label: "Alternate No",
                                value: selectedProvider.alternatePhoneNumber,
                              },
                              { label: "Email", value: selectedProvider.email },
                              {
                                label: "Address",
                                value: selectedProvider.address,
                              },
                              { label: "City", value: selectedProvider.city },
                              { label: "State", value: selectedProvider.state },
                              {
                                label: "Business Name",
                                value: selectedProvider.businessName,
                              },
                              {
                                label: "GST Number",
                                value: selectedProvider.gstNumber,
                              },
                              {
                                label: "Aadhaar Number",
                                value: selectedProvider.aadharNumber,
                              },
                              {
                                label: "PAN Number",
                                value: selectedProvider.panNumber,
                              },
                            ].map((f) => (
                              <div
                                className="d-flex justify-content-between align-items-center"
                                key={f.label}
                              >
                                <h6 className="m-0">{f.label}</h6>
                                <p className="m-0 text-wrap text-break text-end">
                                  {f.value || "N/A"}
                                </p>
                              </div>
                            ))}
                          </div>
                        )}
                      </div>
                      <div className="modal-footer">
                        <button
                          type="button"
                          className="btn btn-primary"
                          data-bs-dismiss="modal"
                        >
                          Go Back
                        </button>
                      </div>
                    </div>
                  </div>
                </div>

                {/* Edit Provider Modal */}
                <div
                  className="modal fade"
                  id="editProviderModal"
                  tabIndex="-1"
                >
                  <div className="modal-dialog modal-dialog-centered">
                    <form onSubmit={handleEditSubmit}>
                      <div className="modal-content">
                        <div className="modal-header">
                          <h5 className="modal-title">Edit Service Provider</h5>
                          <button
                            type="button"
                            className="btn-close"
                            data-bs-dismiss="modal"
                          ></button>
                        </div>
                        <div className="modal-body d-flex flex-column gap-2">
                          {editProvider && (
                            <>
                              <input
                                type="hidden"
                                value={editProvider.providerId}
                              />
                              <div>
                                <label>Name</label>
                                <input
                                  type="text"
                                  className="form-control"
                                  value={editProvider.name}
                                  onChange={(e) =>
                                    setEditProvider({
                                      ...editProvider,
                                      name: e.target.value,
                                    })
                                  }
                                  required
                                />
                              </div>
                              <div>
                                <label>Phone Number</label>
                                <input
                                  type="text"
                                  className="form-control"
                                  value={editProvider.phoneNumber}
                                  onChange={(e) =>
                                    setEditProvider({
                                      ...editProvider,
                                      phoneNumber: e.target.value,
                                    })
                                  }
                                  required
                                />
                              </div>
                              <div>
                                <label>Alternate Phone</label>
                                <input
                                  type="text"
                                  className="form-control"
                                  value={editProvider.alternatePhoneNumber}
                                  onChange={(e) =>
                                    setEditProvider({
                                      ...editProvider,
                                      alternatePhoneNumber: e.target.value,
                                    })
                                  }
                                />
                              </div>
                              <div>
                                <label>Email</label>
                                <input
                                  type="email"
                                  className="form-control"
                                  value={editProvider.email}
                                  disabled
                                />
                              </div>
                              <div>
                                <label>Address</label>
                                <textarea
                                  className="form-control"
                                  value={editProvider.address}
                                  onChange={(e) =>
                                    setEditProvider({
                                      ...editProvider,
                                      address: e.target.value,
                                    })
                                  }
                                  required
                                />
                              </div>
                              <div>
                                <label>City</label>
                                <input
                                  type="text"
                                  className="form-control"
                                  value={editProvider.city}
                                  onChange={(e) =>
                                    setEditProvider({
                                      ...editProvider,
                                      city: e.target.value,
                                    })
                                  }
                                />
                              </div>
                              <div>
                                <label>State</label>
                                <input
                                  type="text"
                                  className="form-control"
                                  value={editProvider.state}
                                  onChange={(e) =>
                                    setEditProvider({
                                      ...editProvider,
                                      state: e.target.value,
                                    })
                                  }
                                />
                              </div>
                            </>
                          )}
                        </div>
                        <div className="modal-footer">
                          <button type="submit" className="btn btn-success">
                            Save Changes
                          </button>
                          <button
                            type="button"
                            className="btn btn-secondary"
                            data-bs-dismiss="modal"
                          >
                            Cancel
                          </button>
                        </div>
                      </div>
                    </form>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>
    </main>
  );
};

export default ServiceProviders;
