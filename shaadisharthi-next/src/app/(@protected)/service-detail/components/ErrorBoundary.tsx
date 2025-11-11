// components/ErrorBoundary.tsx
'use client';

import React from 'react';

interface Props {
  children: React.ReactNode;
  fallback?: React.ReactNode;
}

interface State {
  hasError: boolean;
  error?: Error;
}

// Error Boundary component to catch JavaScript errors in child components
// and display a fallback UI instead of crashing the entire app
export class ErrorBoundary extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { hasError: false };
  }

  // Update state when an error is caught
  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  // Log error information for debugging
  componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
    console.error('Error caught by boundary:', error, errorInfo);
  }

  render() {
    // Display fallback UI if an error occurred
    if (this.state.hasError) {
      return this.props.fallback || (
        <div className="container mx-auto px-4 py-8 text-red-600">
          Something went wrong. Please try again later.
        </div>
      );
    }

    // Render children if no error occurred
    return this.props.children;
  }
}