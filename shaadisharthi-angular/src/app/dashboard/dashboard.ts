import { Component, OnInit } from '@angular/core';
import { ApiService } from '../core/services/api';    // Custom API service for data fetching
import { CommonModule } from '@angular/common';      // For Angular directives (*ngIf, etc.)
import { FormsModule } from '@angular/forms';         // For form bindings (if filters are enabled)
import { NgApexchartsModule } from 'ng-apexcharts';   // For ApexCharts integration

// Dashboard component — main page showing metrics and charts
@Component({
  selector: 'app-dashboard',               // Use <app-dashboard></app-dashboard>
  standalone: true,                        // Standalone (no module needed)
  imports: [CommonModule, FormsModule, NgApexchartsModule],  // Required modules
  templateUrl: './dashboard.html',         // HTML template
  styleUrl: './dashboard.scss'             // SCSS styles
})
export class Dashboard implements OnInit {
  // Key metrics displayed in cards (updated from API)
  upcomingOrders: number = 0;
  totalOrdersThisMonth: number = 0;
  revenueThisMonth: number = 0;
  customersThisYear: number = 0;
  // walletBalance: number = 0;            // Commented out, but ready if needed
  totalEarnings: number = 0;
  
  // Chart configuration objects (for ApexCharts)
  bookingAnalysisChart: any = {};          // Donut chart options
  performanceRatingChart: any = {};        // Bar chart options
  bookingCalendarChart: any = {};          // Line chart options
  financialChart: any = {};                // Area chart options
  
  // Filter selections (for month/year buttons — currently commented in HTML)
  selectedMonthFilter: string = 'thisMonth';
  selectedYearFilter: string = 'thisYear';
  
  // API endpoint for fetching dashboard data
  private dashboardApi = 'ServiceProvider/providerdashboardstats';

  // Inject ApiService
  constructor(private api: ApiService) {}

  // Lifecycle hook: runs on component init
  ngOnInit(): void {
    this.loadDashboardData();  // Fetch data immediately
  }

  // Fetch all dashboard data from API
  loadDashboardData(): void {
    this.api.get<any>(this.dashboardApi).subscribe({
      next: (data) => {
        // Update metrics from response
        this.upcomingOrders = data.upcomingOrders;
        this.totalOrdersThisMonth = data.totalOrdersThisMonth;
        this.revenueThisMonth = data.revenueThisMonth;
        this.customersThisYear = data.customersThisYear;
        // this.walletBalance = data.walletBalance;
        this.totalEarnings = data.totalEarnings;
        
        // Setup charts using data from API
        this.initBookingAnalysisChart(data.bookingAnalysis);
        this.initPerformanceRatingChart(data.performanceRating);
        this.initBookingCalendarChart(data.bookingCalendar);
        this.initFinancialChart(data.financialData);
      },
      error: (error) => {
        console.error('Error loading dashboard data:', error);  // Log errors
      }
    });
  }

  // Called when filters change (hooked to buttons in HTML if uncommented)
  onFilterChange(): void {
    // Reload data with new filters (add params to API if needed)
    this.loadDashboardData();
  }

  // Setup Donut chart for booking analysis
  initBookingAnalysisChart(data: any): void {
    this.bookingAnalysisChart = {
      series: data.series,    // Data points (e.g., [10, 20, 30])
      chart: {
        type: 'donut',        // Chart type
        height: 350           // Fixed height
      },
      labels: data.labels,    // Labels for segments
      colors: ['#008FFB', '#00E396', '#FEB019', '#FF4560', '#775DD0'],  // Color scheme
      responsive: [{          // Mobile responsive options
        breakpoint: 480,
        options: {
          chart: { width: 200 },
          legend: { position: 'bottom' }
        }
      }]
    };
  }

  // Setup Bar chart for performance ratings
  initPerformanceRatingChart(data: any): void {
    this.performanceRatingChart = {
      series: [{ name: 'Average Rating', data: data.ratings }],  // Data series
      chart: { height: 350, type: 'bar' },                       // Basic chart setup
      plotOptions: {                                             // Bar styling
        bar: { borderRadius: 10, dataLabels: { position: 'top' } }
      },
      dataLabels: {                                              // Labels on bars
        enabled: true,
        formatter: function(val: number) { return val.toFixed(1); },  // Format to 1 decimal
        offsetY: -20,
        style: { fontSize: '12px', colors: ["#304758"] }
      },
      xaxis: {                                                   // X-axis setup
        categories: data.categories,                             // Labels (e.g., months)
        position: 'bottom',
        axisBorder: { show: false },
        axisTicks: { show: false },
        crosshairs: {                                            // Hover effect
          fill: {
            type: 'gradient',
            gradient: { colorFrom: '#D8E3F0', colorTo: '#BED1E6', stops: [0, 100], opacityFrom: 0.4, opacityTo: 0.5 }
          }
        },
        tooltip: { enabled: true }
      },
      yaxis: {                                                   // Y-axis setup
        axisBorder: { show: false },
        axisTicks: { show: false },
        labels: { show: false, formatter: function(val: number) { return val.toFixed(1); } }
      },
      title: {                                                   // Chart title
        text: 'Service Performance Ratings',
        floating: true,
        offsetY: 0,
        align: 'center',
        style: { color: '#444' }
      }
    };
  }

  // Setup Line chart for booking calendar
  initBookingCalendarChart(data: any): void {
    this.bookingCalendarChart = {
      series: [{ name: 'Bookings', data: data.bookings }],  // Data series
      chart: { height: 350, type: 'line', zoom: { enabled: false } },  // No zoom
      dataLabels: { enabled: false },                       // No data labels
      stroke: { curve: 'straight' },                        // Straight lines
      title: { text: 'Bookings Over Time', align: 'left' }, // Title
      grid: {                                               // Grid styling
        row: { colors: ['#f3f3f3', 'transparent'], opacity: 0.5 }
      },
      xaxis: { categories: data.categories }                // X-axis labels (e.g., dates)
    };
  }

  // Setup Area chart for financial overview
  initFinancialChart(data: any): void {
    this.financialChart = {
      series: [{ name: 'Revenue', data: data.revenue }],    // Data series
      chart: { type: 'area', height: 350, zoom: { enabled: false } },  // Area type, no zoom
      dataLabels: { enabled: false },                       // No labels
      stroke: { curve: 'straight' },                        // Straight curve
      subtitle: { text: 'Revenue Movements', align: 'left' },  // Subtitle
      labels: data.categories,                              // Labels (shared for xaxis?)
      xaxis: { type: 'category' },                          // Category x-axis
      yaxis: {                                              // Y-axis formatting
        opposite: false,
        labels: { formatter: function(val: number) { return '$' + val.toFixed(2); } }
      },
      legend: { horizontalAlign: 'left' },                  // Legend position
      tooltip: {                                            // Tooltip formatting
        y: { formatter: function(val: number) { return '$' + val.toFixed(2); } }
      }
    };
  }
}