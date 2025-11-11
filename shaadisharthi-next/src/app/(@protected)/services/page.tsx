'use client';
import React, { useState, useRef, useMemo, Suspense, useEffect } from 'react';
import Head from 'next/head';
import { motion, AnimatePresence } from 'framer-motion';
import dynamic from 'next/dynamic';
import ErrorBoundary from './components/ErrorBoundary';
import VendorGridSkeleton from './components/VendorGridSkeleton';
import EmptyState from './components/EmptyState';
import { useServices, useServiceCategories } from '@/hooks/useServicesQuery';
import { useDebounce } from '@/hooks/useDebounce';
import { useIntersectionObserver } from '@/hooks/useIntersectionObserver';
import { ServiceFilters } from '@/hooks/useServicesQuery';
import { ChevronDownIcon, ChevronUpIcon } from './components/Icons';

// Dynamically import heavy components to improve initial load performance
const FilterSection = dynamic(() => import('./components/FilterSection'), {
  loading: () => <div className="h-32 bg-gray-100 dark:bg-gray-800 rounded-lg animate-pulse mb-8" />,
  ssr: false
});

const CategoryCard = dynamic(() => import('./components/CategoryCard'), {
  loading: () => <div className="h-16 bg-gray-100 dark:bg-gray-800 rounded animate-pulse" />,
});

const VendorCard = dynamic(() => import('./components/VendorCard'), {
  loading: () => <div className="h-64 bg-gray-100 dark:bg-gray-800 rounded-lg animate-pulse" />,
});

// Main service page component that displays wedding vendors with filtering and infinite scroll
const ServicePageContent: React.FC = () => {
  // State to control whether to show all categories or just first 6
  const [showAllCategories, setShowAllCategories] = useState(false);
  
  // Initial filter values
  const initialFilters: ServiceFilters = {
    category: '',
    location: '',
    minPrice: '',
    maxPrice: '',
    rating: '',
    sortBy: 'popular',
  };

  // State for current filters and debounced version to prevent too many API calls
  const [filters, setFilters] = useState(initialFilters);
  const debouncedFilters = useDebounce(filters, 300);
  const previousDebouncedFiltersRef = useRef(debouncedFilters);
  
  // Fetch service categories
  const { data: categories = [] } = useServiceCategories();

  // Fetch services using infinite query with debounced filters
  // React Query automatically handles caching based on query key
  const { data, isLoading, error, fetchNextPage, hasNextPage, isFetchingNextPage } = useServices(debouncedFilters);

  // Ref for main content area to enable scroll to top on filter changes
  const mainRef = useRef<HTMLElement>(null);

  // Flatten all pages of services into a single array
  const services = useMemo(() => {
    return data?.pages.flatMap(page => page.services) || [];
  }, [data]);

  // Reset all filters to initial state
  const resetFilters = () => {
    setFilters(initialFilters);
  };

  // Handle category selection - toggle category on/off
  const handleCategoryClick = (category: string) => {
    setFilters((prev: ServiceFilters) => ({
      ...prev,
      category: prev.category === category ? '' : category,
    }));
  };

  // Determine which categories to display based on showAllCategories state
  const displayedCategories = showAllCategories ? categories : categories.slice(0, 6);

  // Infinite scroll observer - detects when user scrolls near bottom
  const [observedLoaderRef, isLoaderIntersecting] = useIntersectionObserver({
    threshold: 0.5,
    rootMargin: '100px',
  });

  // Fetch next page when loader becomes visible and there are more pages
  useEffect(() => {
    if (isLoaderIntersecting && hasNextPage && !isFetchingNextPage) {
      fetchNextPage();
    }
  }, [isLoaderIntersecting, hasNextPage, isFetchingNextPage, fetchNextPage]);

  // Scroll to top only on major filter changes (category, location, sort)
  useEffect(() => {
    if (previousDebouncedFiltersRef.current !== debouncedFilters) {
      const prev = previousDebouncedFiltersRef.current;
      const isMajorChange = 
        prev.sortBy !== debouncedFilters.sortBy ||
        prev.category !== debouncedFilters.category || 
        prev.location !== debouncedFilters.location;
      
      if (isMajorChange) {
        mainRef.current?.scrollTo({ top: 0, behavior: 'smooth' });
      }
    }
    previousDebouncedFiltersRef.current = debouncedFilters;
  }, [debouncedFilters]);

  return (
    <div className="min-h-screen flex flex-col bg-gray-50 dark:bg-gray-900">
      <Head>
        <title>Wedding Vendors | ShaadiSharthi</title>
        <meta name="description" content="Discover the perfect vendors for your dream wedding" />
      </Head>

      {/* Main content area with scrollable reference */}
      <main ref={mainRef} className="flex-grow container mx-auto px-4 py-8">
        {/* Page Header */}
        <div className="text-center mb-12">
          <h1 className="text-3xl md:text-4xl font-serif font-bold text-gray-800 dark:text-gray-200">
            Wedding Vendors
          </h1>
          <p className="text-lg text-gray-600 dark:text-gray-300 max-w-2xl mx-auto">
            Discover the perfect vendors for your dream wedding
          </p>
        </div>

        {/* Filter Section - Sticky so it remains visible while scrolling */}
        <div className="sticky top-0 z-10 bg-gray-50 dark:bg-gray-900 pt-4 pb-2">
          <FilterSection
            filters={filters}
            setFilters={setFilters}
            onResetFilters={resetFilters}
          />
        </div>

        {/* Vendor Categories Section */}
        <div className="mb-8">
          <h2 className="text-xl font-semibold text-gray-800 dark:text-gray-200 mb-4">
            Browse by Category
          </h2>
          <div className="relative">
            {/* Categories grid */}
            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-6 gap-4">
              {displayedCategories.map((category) => (
                <CategoryCard
                  key={category.name}
                  category={category.name}
                  icon={category.icon}
                  colorClass={category.color}
                  isSelected={filters.category === category.name}
                  onClick={handleCategoryClick}
                />
              ))}
            </div>
            {/* Show More/Less toggle button */}
            <button
              onClick={() => setShowAllCategories(!showAllCategories)}
              className="absolute bottom-0 inset-x-0 mx-auto w-fit bg-white dark:bg-gray-800 px-3 py-1 text-primary dark:text-pink-400 font-medium rounded-md shadow-sm sm:right-0 sm:left-auto sm:mx-0"
              aria-expanded={showAllCategories}
              aria-label={showAllCategories ? 'Show fewer categories' : 'Show all categories'}
            >
              {showAllCategories ? 'See Less' : 'See More'}
              {showAllCategories ? <ChevronUpIcon className="ml-1" /> : <ChevronDownIcon className="ml-1" />}
            </button>
          </div>
        </div>

        {/* Results Header with count */}
        <div className="flex items-center justify-between mb-8">
          <h2 className="text-xl font-semibold text-gray-800 dark:text-gray-200">
            {filters.category ? `${filters.category} Vendors` : 'All Vendors'}
            <span className="text-sm font-normal text-gray-500 dark:text-gray-400 ml-2">
              ({services.length} results)
            </span>
          </h2>
        </div>

        {/* Main Vendors Grid with different states */}
        {isLoading && services.length === 0 ? (
          // Show skeleton loader on initial load
          <VendorGridSkeleton count={8} />
        ) : error ? (
          // Error state
          <div className="text-center py-12">
            <p className="text-red-600 dark:text-red-400 mb-4">Failed to load vendors</p>
            <button
              onClick={() => window.location.reload()}
              className="px-4 py-2 bg-primary text-white rounded hover:bg-primary-dark"
            >
              Try Again
            </button>
          </div>
        ) : services.length > 0 ? (
          // Success state with vendors
          <>
            {/* Background loading indicator for filter updates */}
            {isLoading && (
              <div className="mb-4 p-3 bg-blue-50 dark:bg-blue-900/20 rounded-lg">
                <div className="flex items-center justify-center gap-2">
                  <div className="animate-spin rounded-full w-4 h-4 border-b-2 border-blue-600"></div>
                  <span className="text-blue-600 dark:text-blue-400 text-sm">Updating results...</span>
                </div>
              </div>
            )}

            {/* Vendors grid with animation */}
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
              <AnimatePresence mode="popLayout">
                {services.map((vendor, index) => (
                  <motion.div
                    key={`${vendor.serviceId}-${filters.sortBy}-${index}`}
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0, scale: 0.95 }}
                    transition={{ duration: 0.3 }}
                    layout="position"
                  >
                    <VendorCard service={vendor} />
                  </motion.div>
                ))}
              </AnimatePresence>
            </div>

            {/* Infinite scroll loader indicator */}
            <div
              ref={observedLoaderRef}
              className="text-center py-8 min-h-[50px]"
            >
              {isFetchingNextPage ? (
                <div className="flex items-center justify-center gap-2">
                  <div className="animate-spin rounded-full w-6 h-6 border-b-2 border-pink-600"></div>
                  <span className="text-pink-600 font-medium">Loading more vendors...</span>
                </div>
              ) : hasNextPage ? (
                <span className="text-gray-600 dark:text-gray-400">Scroll to load more</span>
              ) : services.length > 0 ? (
                <span className="text-gray-600 dark:text-gray-400">All vendors loaded</span>
              ) : null}
            </div>
          </>
        ) : (
          // Empty state when no vendors found
          <EmptyState onReset={resetFilters} />
        )}
      </main>
    </div>
  );
};

// Wrapper component with error boundary and suspense fallback
const ServicePage: React.FC = () => {
  return (
    <ErrorBoundary>
      <Suspense fallback={
        <div className="min-h-screen flex items-center justify-center bg-gray-50 dark:bg-gray-900">
          <div className="animate-spin rounded-full w-12 h-12 border-b-2 border-pink-600"></div>
        </div>
      }>
        <ServicePageContent />
      </Suspense>
    </ErrorBoundary>
  );
};

export default ServicePage;