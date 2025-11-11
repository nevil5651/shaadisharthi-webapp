// Senior-Level API Service for Guest Queries

import axiosInstance from "./http";

// Fetch list of all queries with pagination
export const fetchQueryList = (page = 1, limit = 10) => {
  // backend should accept page & limit and return { queries: [...], total: N } or { totalCount: N }
  return axiosInstance.get("admin/GuestQueryHandler", {
    params: { page, limit },
  });
};

// Assign query to admin (assign-on-view)
// backend should accept form data 'query_id' and return full query object or { success: true, query: {...} }
export const assignQuery = (query_id) => {
  const formData = new URLSearchParams();
  formData.append("query_id", query_id);
  formData.append("action", "assign");

  return axiosInstance.post("admin/GuestQueryHandler", formData, {
    headers: {
      "Content-Type": "application/x-www-form-urlencoded",
    },
  });
};

// Reply to a specific query
// backend should accept form data 'query_id' and 'reply_msg', verify assigned admin, send email and update DB,
// then return { success: true, message: "..." } or appropriate error + status code (409 if not assigned)
export const replyToQuery = (query_id, reply_msg) => {
  const formData = new URLSearchParams();
  formData.append("query_id", query_id);
  formData.append("reply_msg", reply_msg);
  formData.append("action", "reply");

  return axiosInstance.post("admin/GuestQueryHandler/reply", formData, {
    headers: {
      "Content-Type": "application/x-www-form-urlencoded",
    },
  });
};
