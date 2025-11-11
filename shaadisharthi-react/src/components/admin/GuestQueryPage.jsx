// Purpose: Admin interface for managing guest (non-registered) user queries with assign-on-view and reply functionality

import { useEffect, useState } from "react";
import {
  fetchQueryList, // Fetches paginated list of pending guest queries
  assignQuery, // Assigns a query to current admin on view (idempotent)
  replyToQuery, // Sends reply + email notification, updates DB
} from "../../utils/guestQueryService";
import { toast } from "react-toastify"; // Global toast notifications
import { handleAxiosError } from "../../utils/handleAxiosError"; // Centralized error handling

/**
 * GuestQueryPage - Main component for admin to manage guest queries
 * Features:
 * - Paginated table with assign-on-view
 * - Modal for viewing full query + replying
 * - Real-time status updates (assigned, replied)
 * - Robust error handling with 409 conflict detection
 */
const GuestQueryPage = () => {
  // List of queries for current page
  const [queryList, setQueryList] = useState([]);
  // Currently selected query for modal view
  const [selectedQuery, setSelectedQuery] = useState(null);
  // Modal visibility toggle
  const [showModal, setShowModal] = useState(false);
  // Reply message input
  const [replyMsg, setReplyMsg] = useState("");
  // Current pagination page
  const [currentPage, setCurrentPage] = useState(1);
  // Total pages calculated from backend total count
  const [totalPages, setTotalPages] = useState(1);
  // Form submission state (disable buttons during reply)
  const [isSubmitting, setIsSubmitting] = useState(false);
  // Table loading state
  const [isLoading, setIsLoading] = useState(false);

  const pageSize = 10; // keep same as before — Consistent with backend limit

  /**
   * normalizeListResponse - Normalizes inconsistent backend response shapes
   * Handles: { total, queries } OR { totalCount, queries }
   * @param {Object} resData - Raw response.data from axios
   * @returns {{ queries: Array, total: number }} - Normalized shape
   */
  const normalizeListResponse = (resData) => {
    // backend may return { total, queries } or { totalCount, queries }
    const queries = resData.queries || [];
    const total = resData.total ?? resData.totalCount ?? 0;
    return { queries, total };
  };

  /**
   * loadQueries - Fetches queries for given page with normalization
   * - Sets loading state
   * - Normalizes list response
   * - Normalizes each query field to client-expected keys
   * - Calculates total pages
   * @param {number} page - Page to fetch (defaults to 1)
   */
  const loadQueries = async (page = 1) => {
    setIsLoading(true);
    try {
      const res = await fetchQueryList(page, pageSize);
      const { queries, total } = normalizeListResponse(res.data || {});
      // normalize each query to expected client fields:
      // backend should return fields like: id, name, subject, created_at, assigned_admin_id, assigned_at, status
      const normalized = queries.map((q) => ({
        id: q.id ?? q.query_id ?? q.id,
        name: q.name ?? q.fullName ?? q.userName ?? "N/A",
        subject: q.subject ?? "",
        created_at: q.created_at ?? q.timestamp ?? q.createdAt ?? null,
        assigned_admin_id: q.assigned_admin_id ?? q.assignedTo ?? null,
        assigned_at: q.assigned_at ?? q.assignedAt ?? null,
        status: q.status ?? "PENDING",
        // keep raw message/email available after assign-view returns full object
        message: q.message ?? q.msg ?? null,
        email: q.email ?? null,
      }));
      setQueryList(normalized);
      setTotalPages(Math.max(1, Math.ceil((total || 0) / pageSize)));
    } catch (err) {
      handleAxiosError(err, "Failed to fetch queries");
    } finally {
      setIsLoading(false);
    }
  };

  /**
   * useEffect - Load queries when currentPage changes
   * Dependency: [currentPage] — re-fetch on page change
   * eslint-disable-next-line intentionally suppressed: currentPage is stable
   */
  useEffect(() => {
    loadQueries(currentPage);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentPage]);

  /**
   * handleView - Assigns query to current admin and loads full details
   * - Calls assignQuery (backend returns full query on success)
   * - Normalizes returned query object
   * - Opens modal with full data
   * - Handles 409 conflict (already assigned to another admin)
   * @param {Object} query - Query row from table
   */
  const handleView = async (query) => {
    try {
      // attempt to assign (backend returns full query on success)
      const res = await assignQuery(query.id);
      // backend may return { success: true, query: {...} } or raw query; handle both
      const returned = res.data?.query ?? res.data;
      if (!returned) {
        toast.error("Failed to load query details.");
        return;
      }

      // normalize returned query object
      const full = {
        id: returned.id ?? returned.query_id ?? query.id,
        name: returned.name ?? query.name,
        subject: returned.subject ?? query.subject,
        message: returned.message ?? null,
        email: returned.email ?? null,
        created_at:
          returned.created_at ?? returned.timestamp ?? query.created_at,
        assigned_admin_id:
          returned.assigned_admin_id ?? returned.assignedTo ?? null,
        assigned_at: returned.assigned_at ?? null,
        status: returned.status ?? "PENDING",
        reply_message: returned.reply_message ?? null,
        replied_by_admin_id: returned.replied_by_admin_id ?? null,
        replied_at: returned.replied_at ?? null,
      };

      setSelectedQuery(full);
      setShowModal(true);
    } catch (err) {
      // if backend returns a 409 conflict, show friendlier message (handleAxiosError may already do)
      if (err && err.response && err.response.status === 409) {
        const info = err.response.data || {};
        const assignedTo = info.assigned_admin_id || info.assignedTo || null;
        const assignedAt = info.assigned_at || info.assignedAt || null;
        toast.info(
          `This query is currently assigned to admin ${
            assignedTo ?? "someone"
          }${
            assignedAt ? ` since ${new Date(assignedAt).toLocaleString()}` : ""
          }.`
        );
      } else {
        handleAxiosError(err, "Failed to assign query");
      }
    }
  };

  /**
   * handleCloseModal - Resets modal state on close
   * - Clears selected query, reply, submission state
   */
  const handleCloseModal = () => {
    setShowModal(false);
    setSelectedQuery(null);
    setReplyMsg("");
    setIsSubmitting(false);
  };

  /**
   * handleReplySubmit - Validates and sends reply
   * - Prevents empty or overly long replies
   * - Calls replyToQuery with validation
   * - Shows success/error toasts
   * - Refreshes current page on success
   * - Handles 409 (not assigned) gracefully
   * @param {Event} e - Form submit event
   */
  const handleReplySubmit = async (e) => {
    if (e) e.preventDefault();

    if (!selectedQuery?.id || !replyMsg.trim()) {
      toast.error("Reply message cannot be empty.");
      return;
    }

    if (replyMsg.length > 500) {
      toast.error("Reply message exceeds 500 characters.");
      return;
    }

    try {
      setIsSubmitting(true);
      const res = await replyToQuery(selectedQuery.id, replyMsg);

      if (res.data?.success) {
        toast.success(
          res.data.message ||
            "Reply sent successfully and email notification sent"
        );
        handleCloseModal();
        // reload current page to reflect status change
        loadQueries(currentPage);
      } else {
        // backend responded but indicated failure
        toast.error(
          res.data?.error || "Unknown error occurred while replying."
        );
        setIsSubmitting(false);
      }
    } catch (err) {
      // if backend returns 409 because another admin took it while replying, show message
      if (err && err.response && err.response.status === 409) {
        toast.error(
          err.response.data?.error ||
            "You are not the assigned admin for this query."
        );
      } else {
        handleAxiosError(err, "Failed to send reply");
      }
      setIsSubmitting(false);
    }
  };

  /**
   * handlePageChange - Safely updates current page within bounds
   * @param {number} newPage - Target page
   */
  const handlePageChange = (newPage) => {
    if (newPage >= 1 && newPage <= totalPages) {
      setCurrentPage(newPage);
    }
  };

  return (
    <main id="main" className="main">
      {/* Page Title & Breadcrumb */}
      <div className="pagetitle">
        <h1>Query Management</h1>
        <nav className="d-flex justify-content-between">
          <ol className="breadcrumb">
            <li className="breadcrumb-item">Home</li>
            <li className="breadcrumb-item active">Query Management</li>
          </ol>
        </nav>
      </div>

      <section className="section dashboard">
        <div className="card">
          <div className="card-body">
            <h5 className="card-title">Pending Guest Queries</h5>

            {/* Responsive Table */}
            <div className="table-responsive">
              <table className="table table-hover">
                <thead>
                  <tr>
                    <th>Sr.No</th>
                    <th>Name</th>
                    <th>Subject</th>
                    <th>Created At</th>
                    <th>Assigned</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {isLoading ? (
                    <tr>
                      <td colSpan="6" className="text-center py-4">
                        <div
                          className="spinner-border"
                          role="status"
                          aria-hidden="true"
                        ></div>
                        <div className="mt-2">Loading...</div>
                      </td>
                    </tr>
                  ) : queryList.length > 0 ? (
                    queryList.map((q, idx) => (
                      <tr key={q.id}>
                        {/* Serial number with pagination offset */}
                        <th>{(currentPage - 1) * pageSize + idx + 1}</th>
                        <td>{q.name}</td>
                        <td>{q.subject}</td>
                        <td>
                          {q.created_at
                            ? new Date(q.created_at).toLocaleString("en-IN", {
                                timeZone: "Asia/Kolkata",
                              })
                            : "-"}
                        </td>
                        <td>
                          {q.assigned_admin_id
                            ? q.assigned_admin_id === null
                              ? "Available"
                              : `Assigned (${q.assigned_admin_id})`
                            : "Available"}
                        </td>
                        <td>
                          <button
                            type="button"
                            className="btn btn-primary view-btn"
                            onClick={() => handleView(q)}
                          >
                            View
                          </button>
                        </td>
                      </tr>
                    ))
                  ) : (
                    <tr>
                      <td colSpan="6">No queries found.</td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>

            {/* Pagination - Only shown if >1 page */}
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
                      Previous
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
                      Next
                    </button>
                  </li>
                </ul>
              </nav>
            )}
          </div>
        </div>
      </section>

      {/* Modal - Conditionally rendered when showModal && selectedQuery */}
      {showModal && selectedQuery && (
        <>
          {/* Custom backdrop for click-to-close */}
          <div className="custom-backdrop" onClick={handleCloseModal}></div>
          <div
            className="modal d-block fade show"
            id="queryModal"
            tabIndex="-1"
            role="dialog"
            style={{ zIndex: 1055 }}
          >
            <div className="modal-dialog modal-dialog-centered modal-lg">
              <div className="modal-content">
                <div className="modal-header">
                  <h5 className="card-title">Query Details</h5>
                  <button
                    type="button"
                    className="btn-close"
                    onClick={handleCloseModal}
                  ></button>
                </div>
                <div className="modal-body">
                  <div className="row g-3">
                    <div className="col-md-6">
                      <h6>Name</h6>
                      <p>{selectedQuery.name}</p>
                    </div>
                    <div className="col-md-6">
                      <h6>Email</h6>
                      <p>{selectedQuery.email ?? "-"}</p>
                    </div>
                    <div className="col-md-6">
                      <h6>Date</h6>
                      <p>
                        {selectedQuery.created_at
                          ? new Date(selectedQuery.created_at).toLocaleString(
                              "en-IN",
                              { timeZone: "Asia/Kolkata" }
                            )
                          : "-"}
                      </p>
                    </div>
                    <div className="col-12">
                      <h6>Subject</h6>
                      <p>{selectedQuery.subject}</p>
                    </div>
                    <div className="col-12">
                      <h6>Message</h6>
                      <p style={{ whiteSpace: "pre-wrap" }}>
                        {selectedQuery.message}
                      </p>
                    </div>
                  </div>

                  {/* Reply Form */}
                  <form className="mt-4" onSubmit={handleReplySubmit}>
                    <div className="col-12">
                      <div className="form-floating">
                        <textarea
                          name="reply_msg"
                          className="form-control"
                          placeholder="Reply message"
                          id="reply-text"
                          style={{ height: "120px" }}
                          value={replyMsg}
                          onChange={(e) => {
                            setReplyMsg(e.target.value);
                            if (e.target.value.length > 500) {
                              toast.error(
                                "Reply message exceeds 500 characters."
                              );
                            }
                          }}
                          maxLength={500}
                        ></textarea>
                        <label htmlFor="reply-text">
                          Reply Message
                          <span className="text-muted">
                            ({replyMsg.length}/500)
                          </span>
                        </label>
                      </div>
                    </div>

                    <div className="d-flex justify-content-end mt-3">
                      <button
                        type="button"
                        className="btn btn-secondary me-2"
                        onClick={handleCloseModal}
                        disabled={isSubmitting}
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
                            Sending...
                          </>
                        ) : (
                          "Send Reply"
                        )}
                      </button>
                    </div>
                  </form>
                </div>
              </div>
            </div>
          </div>
        </>
      )}
    </main>
  );
};

export default GuestQueryPage;
