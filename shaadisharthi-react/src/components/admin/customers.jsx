import React, { useEffect, useState } from "react";
import axiosInstance from "../../utils/http";
import { toast } from "react-toastify";

/**
 * Main component for managing customers in the admin panel.
 * Handles listing, pagination, search, view, edit, and delete operations.
 */
const Customers = () => {
  // State for the list of customers fetched from the server
  const [customers, setCustomers] = useState([]);
  // State for the customer currently being viewed in the details modal
  const [selectedCustomer, setSelectedCustomer] = useState(null);
  // State for the customer being edited in the edit modal
  const [editCustomer, setEditCustomer] = useState(null);
  // Controls visibility of the search modal
  const [showSearchModal, setShowSearchModal] = useState(false);
  // Form state for search filters
  const [searchForm, setSearchForm] = useState({
    customer_id: "",
    name: "",
    email: "",
    phone_no: "",
    alternate_phone: "",
  });
  // Current page for pagination
  const [currentPage, setCurrentPage] = useState(1);
  // Total number of pages based on total count and limit
  const [totalPages, setTotalPages] = useState(1);
  // Flag to indicate if the current view is showing search results
  const [isSearchActive, setIsSearchActive] = useState(false);
  // Number of customers to display per page
  const limit = 15;

  /**
   * Handles changes in search input fields.
   * Updates the searchForm state immutably.
   */
  const handleSearchInputChange = (e) => {
    const { name, value } = e.target;
    setSearchForm((prev) => ({ ...prev, [name]: value }));
  };

  /**
   * Fetches customers from the backend with pagination.
   * Resets search state and updates customers, total pages.
   * @param {number} page - The page number to fetch (defaults to 1)
   */
  const fetchCustomers = (page = 1) => {
    axiosInstance
      .get(`admin/customers?page=${page}&limit=${limit}`)
      .then((response) => {
        setCustomers(response.data.customers);
        const totalCount = response.data.totalCount;
        setTotalPages(Math.ceil(totalCount / limit) || 1);
        setIsSearchActive(false);
      })
      .catch((err) => {
        toast.error(
          err.response?.data?.message || "Failed to fetch customers."
        );
        setCustomers([]);
        setTotalPages(1);
        setIsSearchActive(false);
      });
  };

  /**
   * Effect to refetch data when page changes or search mode toggles.
   * Chooses between normal fetch or search based on isSearchActive.
   */
  useEffect(() => {
    if (!isSearchActive) {
      fetchCustomers(currentPage);
    } else {
      handleSearch(currentPage);
    }
  }, [currentPage]);

  /**
   * Deletes a customer after user confirmation.
   * Sends a POST request with form-encoded data.
   * Refreshes the list (search or normal) after success.
   * @param {string} customerId - ID of the customer to delete
   */
  const handleDelete = (customerId) => {
    if (
      window.confirm(
        `Are you sure you want to delete customer with ID: ${customerId}?`
      )
    ) {
      const formData = new URLSearchParams();
      formData.append("customerId", customerId);

      axiosInstance
        .post("admin/customers/delete", formData, {
          headers: {
            "Content-Type": "application/x-www-form-urlencoded",
          },
        })
        .then((response) => {
          if (response.data.message) {
            toast.success(response.data.message);
            if (isSearchActive) {
              handleSearch(currentPage);
            } else {
              fetchCustomers(currentPage);
            }
          } else {
            toast.error(response.data.error || "Failed to delete customer");
          }
        })
        .catch((err) => {
          toast.error(
            err.response?.data?.message || "Error while deleting customer"
          );
        });
    }
  };

  /**
   * Submits edited customer data to the backend.
   * Closes modal and refreshes list on success.
   */
  const handleEditSubmit = (e) => {
    e.preventDefault();
    const updated = {
      customerId: editCustomer.customerId,
      name: editCustomer.name,
      phoneNumber: editCustomer.phoneNumber,
      alternatePhoneNumber: editCustomer.alternatePhoneNumber,
      address: editCustomer.address,
    };

    axiosInstance
      .post("admin/customers/edit", updated)
      .then((response) => {
        toast.success(
          response.data.message || "Customer updated successfully!"
        );
        if (isSearchActive) {
          handleSearch(currentPage);
        } else {
          fetchCustomers(currentPage);
        }
        setEditCustomer(null);
        document.querySelector("#editCustomerModal .btn-close").click();
      })
      .catch((err) => {
        toast.error(err.response?.data?.message || "Failed to update customer");
      });
  };

  /**
   * Performs a search with the current form values and pagination.
   * Only includes non-empty fields in the request.
   * Updates customers list and pagination state.
   * @param {number} page - Page number for search results
   */
  const handleSearch = (page = 1) => {
    const form = new URLSearchParams();
    Object.entries(searchForm).forEach(([k, v]) => {
      if (v.trim()) form.append(k, v);
    });

    axiosInstance
      .post(`admin/customers/search?page=${page}&limit=${limit}`, form, {
        headers: {
          "Content-Type": "application/x-www-form-urlencoded",
        },
      })
      .then((response) => {
        if (response.data.customers && response.data.customers.length > 0) {
          setCustomers(response.data.customers);
          setTotalPages(Math.ceil(response.data.totalCount / limit) || 1);
          setIsSearchActive(true);
          toast.success(
            `${response.data.customers.length} matching customers found`
          );
          setShowSearchModal(false);
        } else {
          toast.info("No matching customers found");
          setCustomers([]);
          setTotalPages(1);
          setIsSearchActive(false);
        }
      })
      .catch((err) => {
        toast.error(err.response?.data?.message || "Search failed");
        setCustomers([]);
        setTotalPages(1);
        setIsSearchActive(false);
      });
  };

  /**
   * Updates the current page if the new page is within valid range.
   * @param {number} newPage - Target page number
   */
  const handlePageChange = (newPage) => {
    if (newPage >= 1 && newPage <= totalPages) {
      setCurrentPage(newPage);
    }
  };

  return (
    <main id="main" className="main">
      <div className="pagetitle">
        <h1>Customers</h1>
        <nav className="d-flex flex-column align-items-start flex-md-row justify-content-md-between align-items-md-center">
          <ol className="breadcrumb">
            <li className="breadcrumb-item">Home</li>
            <li className="breadcrumb-item active">Customers</li>
          </ol>
          {/* Button to open the search modal */}
          <button
            className="btn btn-primary mt-3 mt-md-0 align-self-center"
            onClick={() => setShowSearchModal(true)}
          >
            Search User
          </button>
        </nav>

        {/* Search Modal - Manually controlled via state (not Bootstrap JS) */}
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
                        handleSearch(1);
                      }}
                      className="row g-3"
                    >
                      {[
                        { label: "Customer ID", name: "customer_id" },
                        { label: "Name", name: "name" },
                        { label: "Email", name: "email" },
                        { label: "Phone Number", name: "phone_no" },
                        { label: "Alternate Phone", name: "alternate_phone" },
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
      </div>

      <section className="section">
        <div className="row">
          <div className="col-lg-12">
            <div className="card">
              <div className="card-body">
                <h5 className="card-title">Recently Added Customers</h5>
                <div className="table-responsive">
                  <table className="table datatable">
                    <thead>
                      <tr>
                        <th>Sr No.</th>
                        <th>User ID</th>
                        <th>Name</th>
                        <th>Registration Date</th>
                        <th>Actions</th>
                      </tr>
                    </thead>
                    <tbody>
                      {customers.length > 0 ? (
                        customers.map((customer, i) => (
                          <tr key={customer.customerId}>
                            {/* Serial number calculated based on current page and limit */}
                            <td>{(currentPage - 1) * limit + i + 1}</td>
                            <td>{customer.customerId}</td>
                            <td>{customer.name}</td>
                            <td>{customer.createdAt}</td>
                            <td>
                              <ul className="list-inline m-0">
                                <li className="list-inline-item">
                                  {/* Opens view modal with selected customer */}
                                  <button
                                    className="btn btn-primary btn-sm"
                                    title="Details"
                                    data-bs-toggle="modal"
                                    data-bs-target="#viewCustomerModal"
                                    onClick={() =>
                                      setSelectedCustomer(customer)
                                    }
                                  >
                                    <i className="bi bi-eye"></i>
                                  </button>
                                </li>
                                <li className="list-inline-item">
                                  {/* Opens edit modal with customer data */}
                                  <button
                                    className="btn btn-success btn-sm"
                                    title="Edit"
                                    data-bs-toggle="modal"
                                    data-bs-target="#editCustomerModal"
                                    onClick={() => setEditCustomer(customer)}
                                  >
                                    <i className="bi bi-pencil"></i>
                                  </button>
                                </li>
                                <li className="list-inline-item">
                                  {/* Triggers delete confirmation */}
                                  <button
                                    className="btn btn-danger btn-sm"
                                    title="Delete"
                                    onClick={() =>
                                      handleDelete(customer.customerId)
                                    }
                                  >
                                    <i className="bi bi-trash"></i>
                                  </button>
                                </li>
                              </ul>
                            </td>
                          </tr>
                        ))
                      ) : (
                        <tr>
                          <td colSpan="5">No customers found.</td>
                        </tr>
                      )}
                    </tbody>
                  </table>
                </div>

                {/* Pagination controls - only shown if more than one page */}
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

                {/* View Customer Details Modal */}
                <div
                  className="modal fade"
                  id="viewCustomerModal"
                  tabIndex="-1"
                >
                  <div className="modal-dialog modal-dialog-centered modal-lg">
                    <div className="modal-content">
                      <div className="modal-header">
                        <h5 className="modal-title">Customer Details</h5>
                        <button
                          type="button"
                          className="btn-close"
                          data-bs-dismiss="modal"
                        ></button>
                      </div>
                      <div className="modal-body">
                        {selectedCustomer && (
                          <div className="d-flex flex-column gap-2">
                            {[
                              {
                                label: "Customer ID",
                                value: selectedCustomer.customerId,
                              },
                              { label: "Name", value: selectedCustomer.name },
                              { label: "Email", value: selectedCustomer.email },
                              {
                                label: "Phone Number",
                                value: selectedCustomer.phoneNumber,
                              },
                              {
                                label: "Alternate Phone",
                                value: selectedCustomer.alternatePhoneNumber,
                              },
                              {
                                label: "Address",
                                value: selectedCustomer.address,
                              },
                              {
                                label: "Created At",
                                value: selectedCustomer.createdAt,
                              },
                            ].map((f) => (
                              <div
                                className="d-flex justify-content-between align-items-center"
                                key={f.label}
                              >
                                <h6 className="m-0">{f.label}</h6>
                                <p className="m-0 text-wrap text-break text-end">
                                  {f.value}
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

                {/* Edit Customer Modal */}
                <div
                  className="modal fade"
                  id="editCustomerModal"
                  tabIndex="-1"
                >
                  <div className="modal-dialog modal-dialog-centered">
                    <form onSubmit={handleEditSubmit}>
                      <div className="modal-content">
                        <div className="modal-header">
                          <h5 className="modal-title">Edit Customer</h5>
                          <button
                            type="button"
                            className="btn-close"
                            data-bs-dismiss="modal"
                          ></button>
                        </div>
                        <div className="modal-body d-flex flex-column gap-2">
                          {editCustomer && (
                            <>
                              <input
                                type="hidden"
                                value={editCustomer.customerId}
                              />
                              <div>
                                <label>Name</label>
                                <input
                                  type="text"
                                  className="form-control"
                                  value={editCustomer.name}
                                  onChange={(e) =>
                                    setEditCustomer({
                                      ...editCustomer,
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
                                  value={editCustomer.phoneNumber}
                                  onChange={(e) =>
                                    setEditCustomer({
                                      ...editCustomer,
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
                                  value={editCustomer.alternatePhoneNumber}
                                  onChange={(e) =>
                                    setEditCustomer({
                                      ...editCustomer,
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
                                  value={editCustomer.email}
                                  disabled
                                />
                              </div>
                              <div>
                                <label>Address</label>
                                <textarea
                                  className="form-control"
                                  value={editCustomer.address}
                                  onChange={(e) =>
                                    setEditCustomer({
                                      ...editCustomer,
                                      address: e.target.value,
                                    })
                                  }
                                  required
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

export default Customers;
