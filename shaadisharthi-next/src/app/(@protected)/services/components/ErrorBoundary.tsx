'use client';
import React from 'react';

// Props interface for ErrorBoundary component
interface ErrorBoundaryProps {
  children: React.ReactNode;    // Child components to wrap
  fallback?: React.ReactNode;   // Custom fallback UI (optional)
  onReset?: () => void;         // Reset callback function (optional)
}

// State interface for ErrorBoundary component
interface ErrorBoundaryState {
  hasError: boolean;    // Whether an error has been caught
  error?: Error;        // The error that was caught (optional)
}

// ErrorBoundary component to catch and handle errors in child components
class ErrorBoundary extends React.Component<ErrorBoundaryProps, ErrorBoundaryState> {
  constructor(props: ErrorBoundaryProps) {
    super(props);
    this.state = { hasError: false };
  }

  // Static method to update state when an error is caught
  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { hasError: true, error };
  }

  // Lifecycle method to log errors
  componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
    console.error('Component Error:', error, errorInfo);
  }

  // Reset error state and call optional onReset callback
  resetError = () => {
    this.setState({ hasError: false, error: undefined });
    this.props.onReset?.();
  };

  render() {
    // If there's an error, show fallback UI
    if (this.state.hasError) {
      if (this.props.fallback) {
        return this.props.fallback;
      }
      
      // Default error UI
      return (
        <div className="min-h-screen flex items-center justify-center p-4 bg-white dark:bg-gray-900">
          <div className="text-center max-w-md">
            <div className="text-4xl mb-4">😔</div>
            <h2 className="text-xl font-semibold mb-2 text-gray-800 dark:text-white">Something went wrong</h2>
            <p className="text-gray-600 dark:text-gray-300 mb-4">
              We&apos;re having trouble loading this content. Please try refreshing the page.
            </p>
            {/* Error recovery buttons */}
            <button
              onClick={this.resetError}
              className="px-4 py-2 bg-primary text-white rounded-lg hover:bg-primary-dark transition-colors mr-2"
            >
              Try Again
            </button>
            <button
              onClick={() => window.location.reload()}
              className="px-4 py-2 bg-gray-200 dark:bg-gray-700 text-gray-800 dark:text-white rounded-lg hover:bg-gray-300 dark:hover:bg-gray-600 transition-colors"
            >
              Reload Page
            </button>
          </div>
        </div>
      );
    }

    // If no error, render children normally
    return this.props.children;
  }
}

export default ErrorBoundary;