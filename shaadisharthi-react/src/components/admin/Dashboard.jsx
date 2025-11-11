// Admin dashboard with stats, user trends line chart, order comparison bar chart
// Uses ApexCharts for visualization, fallback data on API failure

import React, { useState, useEffect, useRef } from "react";
import ApexCharts from "apexcharts"; // Charting library
import axiosInstance from "../../utils/http"; // API client
import { handleAxiosError } from "../../utils/handleAxiosError"; // Error handler

/**
 * Dashboard - Main admin overview component
 */
const Dashboard = () => {
  // Stats state (customers/providers)
  const [stats, setStats] = useState({ totalCustomers: 0, totalProviders: 0 });
  // User trends data for line chart
  const [userTrends, setUserTrends] = useState(null);
  // Order comparison data for bar chart
  const [orderComparison, setOrderComparison] = useState(null);
  // Per-section loading states
  const [loading, setLoading] = useState({
    stats: true,
    userTrends: true,
    orderComparison: true,
  });

  const lineChartRef = useRef(null); // Ref for line chart container
  const barChartRef = useRef(null); // Ref for bar chart container
  const lineChartInstance = useRef(null); // Chart instance for cleanup
  const barChartInstance = useRef(null); // Chart instance for cleanup

  // Fetch Dashboard Stats
  useEffect(() => {
    const fetchDashboardStats = async () => {
      try {
        const res = await axiosInstance.get("admin/dashboardstats");
        setStats(res.data);
        setLoading((prev) => ({ ...prev, stats: false }));
      } catch (err) {
        handleAxiosError(err, "Failed to load dashboard stats");
        setLoading((prev) => ({ ...prev, stats: false }));
      }
    };

    fetchDashboardStats();
  }, []); // Empty deps: Fetch once on mount

  // Fetch User Trends Data
  useEffect(() => {
    const fetchUserTrends = async () => {
      try {
        const res = await axiosInstance.get("admin/usertrends");
        setUserTrends(res.data);
        setLoading((prev) => ({ ...prev, userTrends: false }));
      } catch (err) {
        handleAxiosError(err, "Failed to load user trends");
        // Set fallback data if API fails — Ensures chart renders
        setUserTrends({
          customerJoining: [
            15, 30, 45, 50, 65, 80, 90, 100, 120, 130, 140, 150,
          ],
          customerLeaving: [5, 10, 15, 20, 18, 25, 30, 35, 40, 45, 50, 55],
          providerJoining: [8, 20, 30, 40, 50, 55, 65, 75, 85, 95, 105, 115],
          providerLeaving: [3, 8, 12, 15, 20, 22, 28, 30, 35, 40, 45, 50],
        });
        setLoading((prev) => ({ ...prev, userTrends: false }));
      }
    };

    fetchUserTrends();
  }, []); // Empty deps: Fetch once

  // Fetch Order Comparison Data
  useEffect(() => {
    const fetchOrderComparison = async () => {
      try {
        const res = await axiosInstance.get("admin/ordercomparison");
        setOrderComparison(res.data);
        setLoading((prev) => ({ ...prev, orderComparison: false }));
      } catch (err) {
        handleAxiosError(err, "Failed to load order comparison");
        // Set fallback data if API fails
        setOrderComparison({
          categories: [
            "Decoration",
            "Photography",
            "Sound",
            "Beauty",
            "Catering",
          ],
          currentYearData: [320, 280, 150, 200, 180],
          previousYearData: [300, 260, 130, 180, 160],
        });
        setLoading((prev) => ({ ...prev, orderComparison: false }));
      }
    };

    fetchOrderComparison();
  }, []); // Empty deps: Fetch once

  // Render Line Chart when userTrends data is available
  useEffect(() => {
    if (userTrends && lineChartRef.current) {
      const months = [
        "Jan",
        "Feb",
        "Mar",
        "Apr",
        "May",
        "Jun",
        "Jul",
        "Aug",
        "Sep",
        "Oct",
        "Nov",
        "Dec",
      ];

      // Destroy previous chart instance if it exists — Prevents memory leaks
      if (lineChartInstance.current) {
        lineChartInstance.current.destroy();
      }

      // Initialize new chart
      lineChartInstance.current = new ApexCharts(lineChartRef.current, {
        series: [
          { name: "Customer Joining", data: userTrends.customerJoining },
          { name: "Customer Leaving", data: userTrends.customerLeaving },
          {
            name: "Service Provider Joining",
            data: userTrends.providerJoining,
          },
          {
            name: "Service Provider Leaving",
            data: userTrends.providerLeaving,
          },
        ],
        chart: {
          height: 350,
          type: "line",
          zoom: { enabled: false },
          animations: { enabled: true },
          toolbar: { show: true },
        },
        dataLabels: { enabled: false },
        stroke: { curve: "smooth", width: 2 },
        grid: {
          row: {
            colors: ["#f3f3f3", "transparent"],
            opacity: 0.5,
          },
        },
        xaxis: {
          categories: months,
          labels: { style: { fontSize: "12px" } },
        },
        yaxis: {
          title: { text: "Number of Users", style: { fontSize: "12px" } },
        },
        colors: ["#008FFB", "#FF4560", "#00E396", "#FEB019"],
        legend: {
          position: "top",
          fontSize: "12px",
        },
        responsive: [
          {
            breakpoint: 768,
            options: {
              chart: { height: 250 },
              xaxis: { labels: { style: { fontSize: "10px" } } },
              legend: { fontSize: "10px" },
            },
          },
        ],
      });
      lineChartInstance.current.render();
    }

    // Cleanup on unmount or data change
    return () => {
      if (lineChartInstance.current) {
        lineChartInstance.current.destroy();
      }
    };
  }, [userTrends]); // Dep: Re-render on data change

  // Render Bar Chart when orderComparison data is available
  useEffect(() => {
    if (orderComparison && barChartRef.current) {
      // Destroy previous chart instance if it exists
      if (barChartInstance.current) {
        barChartInstance.current.destroy();
      }

      // Initialize new chart
      barChartInstance.current = new ApexCharts(barChartRef.current, {
        series: [
          { name: "Current Year", data: orderComparison.currentYearData },
          { name: "Previous Year", data: orderComparison.previousYearData },
        ],
        chart: {
          type: "bar",
          height: 350,
          stacked: false,
          toolbar: { show: true },
          zoom: { enabled: true },
        },
        responsive: [
          {
            breakpoint: 480,
            options: {
              legend: {
                position: "bottom",
                offsetX: -10,
                offsetY: 0,
              },
            },
          },
        ],
        plotOptions: {
          bar: {
            horizontal: false,
            borderRadius: 10,
            dataLabels: {
              total: {
                enabled: true,
                style: { fontSize: "13px", fontWeight: 900 },
              },
            },
          },
        },
        xaxis: {
          type: "category",
          categories: orderComparison.categories,
        },
        legend: { position: "right", offsetY: 40 },
        fill: { opacity: 1 },
      });

      barChartInstance.current.render();
    }

    // Cleanup
    return () => {
      if (barChartInstance.current) {
        barChartInstance.current.destroy();
      }
    };
  }, [orderComparison]); // Dep: Re-render on data change

  return (
    <main id="main" className="main">
      {/* Page Title */}
      <div className="pagetitle">
        <h1>Dashboard</h1>
        <nav>
          <ol className="breadcrumb">
            <li className="breadcrumb-item">Home</li>
            <li className="breadcrumb-item active">Dashboard</li>
          </ol>
        </nav>
      </div>

      <section className="section dashboard">
        <div className="row">
          {/* Customers Card */}
          <div className="col-12 col-md-6 col-lg-3">
            <div className="card info-card customers-card">
              <div className="card-body">
                <h5 className="card-title">Total Customers</h5>
                <div className="d-flex align-items-center">
                  <div className="card-icon rounded-circle d-flex align-items-center justify-content-center">
                    <i className="bi bi-people"></i>
                  </div>
                  <div className="ps-3">
                    <h6>
                      {loading.stats ? "Loading..." : stats.totalCustomers}
                    </h6>
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Providers Card */}
          <div className="col-12 col-md-6 col-lg-3">
            <div className="card info-card providers-card">
              <div className="card-body">
                <h5 className="card-title">Total Providers</h5>
                <div className="d-flex align-items-center">
                  <div className="card-icon rounded-circle d-flex align-items-center justify-content-center">
                    <i className="bi bi-people"></i>
                  </div>
                  <div className="ps-3">
                    <h6>
                      {loading.stats ? "Loading..." : stats.totalProviders}
                    </h6>
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Orders Card (Static) */}
          <div className="col-12 col-md-6 col-lg-3">
            <div className="card info-card sales-card">
              <div className="card-body">
                <h5 className="card-title">
                  Total Orders <span>| This Month</span>
                </h5>
                <div className="d-flex align-items-center">
                  <div className="card-icon rounded-circle d-flex align-items-center justify-content-center">
                    <i className="bi bi-cart"></i>
                  </div>
                  <div className="ps-3">
                    <h6>145</h6>
                    <span className="text-success small pt-1 fw-bold">12%</span>
                    <span className="text-muted small pt-2 ps-1">increase</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Revenue Card (Static) */}
          <div className="col-12 col-md-6 col-lg-3">
            <div className="card info-card revenue-card">
              <div className="card-body">
                <h5 className="card-title">Total Balance</h5>
                <div className="d-flex align-items-center">
                  <div className="card-icon rounded-circle d-flex align-items-center justify-content-center">
                    <i className="bi bi-currency-dollar"></i>
                  </div>
                  <div className="ps-3">
                    <h6>$3,264</h6>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Charts Row */}
        <div className="row">
          {/* User Trends Chart */}
          <div className="col-12 col-lg-6">
            <div className="card">
              <div className="card-body">
                <h5 className="card-title">User Trends</h5>
                {loading.userTrends ? (
                  <div className="text-center py-5">
                    <div className="spinner-border text-primary" role="status">
                      <span className="visually-hidden">Loading...</span>
                    </div>
                    <p className="mt-2">Loading user trends...</p>
                  </div>
                ) : (
                  <div id="lineChart" ref={lineChartRef}></div>
                )}
              </div>
            </div>
          </div>

          {/* Order Comparison Chart */}
          <div className="col-12 col-lg-6">
            <div className="card">
              <div className="card-body">
                <h5 className="card-title">Order Comparison</h5>
                {loading.orderComparison ? (
                  <div className="text-center py-5">
                    <div className="spinner-border text-primary" role="status">
                      <span className="visually-hidden">Loading...</span>
                    </div>
                    <p className="mt-2">Loading order comparison...</p>
                  </div>
                ) : (
                  <div id="barChart" ref={barChartRef}></div>
                )}
              </div>
            </div>
          </div>
        </div>
      </section>
    </main>
  );
};

export default Dashboard;
