// Purpose: Admin interface for managing registered user queries (Customer/Service Provider)

import React, { useEffect, useState } from "react";
import {
  fetchQueryList, // Fetches paginated queries from registered users
  assignQuery, // Assigns query to current admin
  replyToQuery, // Sends reply and updates status
} from "../../utils/queryService";
import { toast } from "react-toastify";
import { handleAxiosError } from "../../utils/handleAxiosError";

/**
 * QueryPage - Manages queries from registered users
 * - Simpler than GuestQueryPage (no normalization needed)
 * - Assumes consistent backend response shape
 */
const QueryPage = () => {
  const [queryList, setQueryList] = useState([]);
  const [selectedQuery, setSelectedQuery] = useState(null);
  const [showModal, setShowModal] = useState(false);
  const [replyMsg, setReplyMsg] = useState("");
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const pageSize = 10; // Number of queries per page — Matches backend

  /**
   * loadQueries - Fetches paginated queries
   * Assumes backend returns { queries: [], totalCount: N }
   */
  const loadQueries = async (page = 1) => {
    try {
      const res = await fetchQueryList(page, pageSize);
      setQueryList(res.data.queries || []);
      setTotalPages(Math.ceil(res.data.totalCount / pageSize) || 1);
    } catch (err) {
      handleAxiosError(err, "Failed to fetch queries");
    }
  };

  /**
   * useEffect - Reload on page change
   */
  useEffect(() => {
    loadQueries(currentPage);
  }, [currentPage]);

  /**
   * handleView - Assigns query and opens modal
   * Uses query_id from list
   */
  const handleView = async (query) => {
    try {
      await assignQuery(query.query_id);
      setSelectedQuery(query);
      setShowModal(true);
    } catch (err) {
      handleAxiosError(err, "Failed to assign query");
    }
  };

  /**
   * handleCloseModal - Reset modal state
   */
  const handleCloseModal = () => {
    setShowModal(false);
    setSelectedQuery(null);
    setReplyMsg("");
    setIsSubmitting(false);
  };

  /**
   * handleReplySubmit - Send reply with validation
   */
  const handleReplySubmit = async (e) => {
    if (e) e.preventDefault();

    if (!selectedQuery?.query_id || !replyMsg.trim()) {
      toast.error("Reply message cannot be empty.");
      return;
    }

    if (replyMsg.length > 500) {
      toast.error("Reply message exceeds 500 characters.");
      return;
    }

    try {
      setIsSubmitting(true);
      const res = await replyToQuery(selectedQuery.query_id, replyMsg);

      if (res.data.success) {
        toast.success(
          res.data.message ||
            "Reply sent successfully and email notification sent"
        );
        handleCloseModal();
        loadQueries(currentPage); // Refresh current page
      } else {
        toast.error(res.data.error || "Unknown error occurred.");
        setIsSubmitting(false);
      }
    } catch (err) {
      handleAxiosError(err, "Failed to send reply");
      setIsSubmitting(false);
    }
  };

  /**
   * handlePageChange - Safe page navigation
   */
  const handlePageChange = (newPage) => {
    if (newPage >= 1 && newPage <= totalPages) {
      setCurrentPage(newPage);
    }
  };

  return (
    <main id="main" className="main">
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
            <h5 className="card-title">Pending Queries</h5>
            <div className="table-responsive">
              <table className="table table-hover">
                <thead>
                  <tr>
                    <th>Sr.No</th>
                    <th>User ID</th>
                    <th>Subject</th>
                    <th>User Type</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {queryList.length > 0 ? (
                    queryList.map((q, idx) => (
                      <tr key={q.query_id}>
                        <th>{(currentPage - 1) * pageSize + idx + 1}</th>
                        <td>{q.userId}</td>
                        <td>{q.subject}</td>
                        <td>
                          {q.userType === "ServiceProvider"
                            ? "Service Provider"
                            : "Customer"}
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
                      <td colSpan="5">No queries found.</td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>

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

      {showModal && selectedQuery && (
        <>
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
                      <h6>User ID</h6>
                      <p>{selectedQuery.userId}</p>
                    </div>
                    <div className="col-md-6">
                      <h6>Date</h6>
                      <p>
                        {new Date(selectedQuery.timestamp).toLocaleString(
                          "en-IN",
                          { timeZone: "Asia/Kolkata" }
                        )}
                      </p>
                    </div>
                    <div className="col-12">
                      <h6>Subject</h6>
                      <p>{selectedQuery.subject}</p>
                    </div>
                    <div className="col-12">
                      <h6>Message</h6>
                      <p>{selectedQuery.message}</p>
                    </div>
                  </div>

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

export default QueryPage;
