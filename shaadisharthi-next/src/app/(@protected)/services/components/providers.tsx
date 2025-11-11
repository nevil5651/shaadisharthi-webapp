'use client';

import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';
import { useState } from 'react';

// Providers component that wraps the app with React Query context
export default function Providers({ children }: { children: React.ReactNode }) {
  // Create QueryClient instance with default options
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            staleTime: 5 * 60 * 1000, // Data is considered fresh for 5 minutes
            gcTime: 10 * 60 * 1000, // Cache persists for 10 minutes (formerly cacheTime)
            refetchOnWindowFocus: false, // Don't refetch when window gains focus
            retry: 2, // Retry failed requests twice
          },
        },
      })
  );

  return (
    // Provide React Query context to the entire app
    <QueryClientProvider client={queryClient}>
      {children}
      {/* React Query DevTools for development (initially closed) */}
      <ReactQueryDevtools initialIsOpen={false} />
    </QueryClientProvider>
  );
}