// Handles registered user queries

import axiosInstance from "./http";

// Fetch list of all queries with pagination
export const fetchQueryList = (page = 1, limit = 10) => {
  return axiosInstance.get("admin/queries", {
    params: { page, limit },
  });
};

// Assign query to admin
export const assignQuery = (query_id) => {
  const formData = new URLSearchParams();
  formData.append("query_id", query_id);

  return axiosInstance.post("admin/queries/assign", formData, {
    params: { query_id },
    headers: {
      "Content-Type": "application/x-www-form-urlencoded",
    },
  });
};

// Reply to a specific query
export const replyToQuery = (query_id, reply_msg) => {
  const formData = new URLSearchParams();
  formData.append("query_id", query_id);
  formData.append("reply_msg", reply_msg);

  return axiosInstance.post("admin/queries/reply", formData, {
    headers: {
      "Content-Type": "application/x-www-form-urlencoded",
    },
  });
};
