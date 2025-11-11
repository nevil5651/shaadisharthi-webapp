'use client';

import { useState, useEffect, useRef, useCallback } from 'react';
import { ErrorBoundary } from '../components/ErrorBoundary';
import { ServiceHeroSkeleton } from '../components/ServiceHeroSkeleton';
import { ReviewSkeleton } from '../components/ReviewSkeleton';
import ServiceHero from '../components/ServiceHero';
import { PricingCard } from '../components/PricingCard';
import { ReviewsList } from '../components/ReviewsList';
import { ReviewForm } from '../components/ReviewForm';
import { MediaGallery } from '../components/MediaGallery';
import { fetchService, fetchReviews, Service, Review } from '@/lib/service';
import React from 'react';

interface PageProps {
  params: Promise<{ id: string }>;
}

// Cache to prevent duplicate API requests - stores data for 30 seconds
const requestCache = new Map();

export default function ServiceDetailPage({ params }: PageProps) {
  // Extract service ID from URL parameters
  const resolvedParams = React.use(params);
  const serviceId = resolvedParams.id;

  // State for service data, reviews, and loading status
  const [service, setService] = useState<Service | null>(null);
  const [reviews, setReviews] = useState<Review[]>([]);
  const [hasMore, setHasMore] = useState(true); // Track if more reviews are available
  const [serviceLoading, setServiceLoading] = useState(false);
  const [reviewsLoading, setReviewsLoading] = useState(false);
  const [serviceError, setServiceError] = useState<string | null>(null);
  const [reviewsError, setReviewsError] = useState<string | null>(null);

  // Refs for tracking pagination and loading state
  const pageRef = useRef(1); // Current page number for reviews
  const isFetchingRef = useRef(false); // Prevent duplicate API calls
  const lastPageRef = useRef(0); // Track last fetched page to avoid duplicates

  // Separate refs for mobile and desktop loader elements
  // This fixes the issue where layout changes break the IntersectionObserver
  const mobileLoaderRef = useRef<HTMLDivElement>(null);
  const desktopLoaderRef = useRef<HTMLDivElement>(null);
  const observerRef = useRef<IntersectionObserver | null>(null); // Observer for infinite scroll

  // Load service details from API
  const loadService = useCallback(async () => {
    if (!serviceId || serviceId === 'undefined') {
      setServiceError('Invalid service ID');
      return;
    }

    // Check cache first to avoid unnecessary API calls
    const cacheKey = `service-${serviceId}`;
    if (requestCache.has(cacheKey)) {
      const cachedData = requestCache.get(cacheKey);
      // Use cached data if it's less than 30 seconds old
      if (cachedData.timestamp > Date.now() - 30000) {
        setService(cachedData.data);
        return;
      }
    }

    setServiceLoading(true);
    try {
      const serviceData = await fetchService(serviceId);
      if (!serviceData.serviceId) {
        throw new Error('Service ID missing in response');
      }
      
      // Cache the successful response
      requestCache.set(cacheKey, {
        data: serviceData,
        timestamp: Date.now()
      });
      
      setService(serviceData);
      setServiceError(null);
    } catch (err: unknown) {
      console.error('Error loading service:', err);
      setServiceError(err instanceof Error ? err.message : 'Failed to load service');
    } finally {
      setServiceLoading(false);
    }
  }, [serviceId]);

  // Load reviews with pagination support
  const loadReviews = useCallback(async (reset: boolean = false) => {
    // Prevent multiple simultaneous requests
    if (!serviceId || serviceId === 'undefined' || isFetchingRef.current) {
      return;
    }

    const targetPage = reset ? 1 : pageRef.current;
    
    // Don't fetch the same page again
    if (!reset && targetPage === lastPageRef.current) {
      return;
    }
    
    const cacheKey = `reviews-${serviceId}-${targetPage}`;

    // Check cache for reviews data
    if (requestCache.has(cacheKey) && !reset) {
      const cachedData = requestCache.get(cacheKey);
      if (cachedData.timestamp > Date.now() - 30000) {
        setReviews(prev => reset ? cachedData.data.reviews : [...prev, ...cachedData.data.reviews]);
        setHasMore(cachedData.data.hasMore);
        pageRef.current = targetPage + 1;
        lastPageRef.current = targetPage;
        return;
      }
    }

    isFetchingRef.current = true;
    setReviewsLoading(true);
    try {
      const reviewsData = await fetchReviews(serviceId, targetPage);
      
      // Cache the reviews response
      requestCache.set(cacheKey, {
        data: reviewsData,
        timestamp: Date.now()
      });
      
      // Add new reviews to existing list or replace for reset
      setReviews(prev => reset ? reviewsData.reviews : [...prev, ...reviewsData.reviews]);
      setHasMore(reviewsData.hasMore);
      pageRef.current = targetPage + 1;
      lastPageRef.current = targetPage;
      setReviewsError(null);
    } catch (err: unknown) {
      console.error('Error loading reviews:', err);
      setReviewsError(err instanceof Error ? err.message : 'Failed to load reviews');
      
      // Stop loading more if we hit rate limits
      if (err instanceof Error && err.message.includes('429')) {
        setHasMore(false);
      }
    } finally {
      setReviewsLoading(false);
      isFetchingRef.current = false;
    }
  }, [serviceId]);

  // Load initial data when service ID changes
  useEffect(() => {
    setReviews([]);
    pageRef.current = 1;
    lastPageRef.current = 0;
    setHasMore(true);
    loadService();
    loadReviews(true);
  }, [serviceId, loadService, loadReviews]);

  // Setup infinite scroll observer
  useEffect(() => {
    // Clean up previous observer
    if (observerRef.current) {
      observerRef.current.disconnect();
      observerRef.current = null;
    }

    // Determine which loader element is currently visible based on screen size
    const getCurrentLoader = () => {
      // Mobile layout (width < 1024px) uses mobileLoaderRef
      if (mobileLoaderRef.current && window.innerWidth < 1024) {
        return mobileLoaderRef.current;
      }
      // Desktop layout uses desktopLoaderRef
      if (desktopLoaderRef.current && window.innerWidth >= 1024) {
        return desktopLoaderRef.current;
      }
      return null;
    };

    const currentLoader = getCurrentLoader();

    // Set up new observer if conditions are met
    if (hasMore && !reviewsLoading && currentLoader) {
      const observer = new IntersectionObserver(
        (entries) => {
          const [entry] = entries;
          // Load more reviews when loader comes into view
          if (entry.isIntersecting && hasMore && !reviewsLoading && !isFetchingRef.current) {
            loadReviews();
          }
        },
        { 
          threshold: 0.1, // Trigger when 10% of loader is visible
          rootMargin: '50px 0px 50px 0px' // Start loading 50px before reaching loader
        }
      );

      observer.observe(currentLoader);
      observerRef.current = observer;
    }

    // Cleanup function - disconnect observer on unmount
    return () => {
      if (observerRef.current) {
        observerRef.current.disconnect();
        observerRef.current = null;
      }
    };
  }, [hasMore, reviewsLoading, loadReviews, serviceId]);

  // Reconnect observer when screen size changes (mobile/desktop switch)
  useEffect(() => {
    const handleResize = () => {
      // Only reconnect if we have an active observer
      if (observerRef.current) {
        observerRef.current.disconnect();
        observerRef.current = null;
        
        // Small delay to allow DOM to update after resize
        setTimeout(() => {
          const getCurrentLoader = () => {
            if (mobileLoaderRef.current && window.innerWidth < 1024) {
              return mobileLoaderRef.current;
            }
            if (desktopLoaderRef.current && window.innerWidth >= 1024) {
              return desktopLoaderRef.current;
            }
            return null;
          };

          const currentLoader = getCurrentLoader();
          
          // Recreate observer for the currently visible loader
          if (hasMore && !reviewsLoading && currentLoader && observerRef.current === null) {
            const observer = new IntersectionObserver(
              (entries) => {
                const [entry] = entries;
                if (entry.isIntersecting && hasMore && !reviewsLoading && !isFetchingRef.current) {
                  loadReviews();
                }
              },
              { 
                threshold: 0.1,
                rootMargin: '50px 0px 50px 0px'
              }
            );

            observer.observe(currentLoader);
            observerRef.current = observer;
          }
        }, 100);
      }
    };

    // Listen for window resize events
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, [hasMore, reviewsLoading, loadReviews]);

  // Helper to identify temporary reviews (optimistic updates)
  const isTemp = (r: Review) => typeof r.reviewId === 'string' && r.reviewId.startsWith('temp-');

  // Handle new review submission (optimistic update)
  const handleReviewSubmitted = useCallback((newReview: Review | null) => {
    if (newReview) {
      // Add new review and remove any temporary ones
      setReviews((prev) => [newReview, ...prev.filter(r => !isTemp(r))]);
    } else {
      // Remove temporary reviews on error
      setReviews((prev) => prev.filter(r => !isTemp(r)));
    }
  }, []);

  // Handle server response after review submission
  const handleServerReviews = useCallback((serverList: Review[]) => {
    // Replace local reviews with server authoritative list
    setReviews(serverList);
    pageRef.current = 2; // Reset to page 2 since we now have fresh page 1
    setHasMore(true);
    lastPageRef.current = 1;
    
    // Update service rating and review count
    setService((prev) => prev ? {
      ...prev,
      reviewCount: prev.reviewCount + 1,
      rating: calcNewAverage(prev.rating, prev.reviewCount, serverList[0]?.rating ?? 0)
    } : prev);
    
    // Clear review cache to ensure fresh data
    const cacheKeys = Array.from(requestCache.keys()).filter(key => key.startsWith(`reviews-${serviceId}-`));
    cacheKeys.forEach(key => requestCache.delete(key));
  }, [serviceId]);

  // Show error page if service fails to load
  if (serviceError) {
    return (
      <div className="container mx-auto px-4 py-8 text-red-600 dark:text-red-400">
        {serviceError}
      </div>
    );
  }

  return (
    <ErrorBoundary>
      <main className="container bg-gray-50 dark:bg-gray-900 font-['Poppins'] mx-auto px-4 py-8">
        {/* Service Hero Section */}
        {!service || serviceLoading ? (
          <ServiceHeroSkeleton />
        ) : (
          <ServiceHero service={service} />
        )}
        
        {/* Mobile Layout - Single Column Stack */}
        <div className="block lg:hidden space-y-8 mb-12">
          {/* Service Description */}
          <section className="bg-white dark:bg-gray-800 rounded-xl shadow-md p-6">
            <h2 className="text-2xl font-heading font-bold text-gray-900 dark:text-white mb-4">
              About This Service
            </h2>
            {!service || serviceLoading ? (
              // Skeleton loader for description
              <>
                <div className="h-4 bg-gray-300 dark:bg-gray-700 rounded w-full mb-2 animate-pulse"></div>
                <div className="h-4 bg-gray-300 dark:bg-gray-700 rounded w-full mb-2 animate-pulse"></div>
                <div className="h-4 bg-gray-300 dark:bg-gray-700 rounded w-3/4 animate-pulse"></div>
              </>
            ) : (
              <p className="text-gray-700 dark:text-gray-300">{service.description}</p>
            )}
          </section>
          
          {/* Pricing Card */}
          {!service || serviceLoading ? (
            <div className="bg-white dark:bg-gray-800 rounded-xl shadow-md p-6 animate-pulse">
              <div className="h-6 bg-gray-300 dark:bg-gray-700 rounded w-1/3 mb-4"></div>
              <div className="h-8 bg-gray-300 dark:bg-gray-700 rounded w-1/2 mb-4"></div>
              <div className="h-4 bg-gray-300 dark:bg-gray-700 rounded w-full mb-2"></div>
              <div className="h-4 bg-gray-300 dark:bg-gray-700 rounded w-full mb-4"></div>
              <div className="h-10 bg-gray-300 dark:bg-gray-700 rounded w-full"></div>
            </div>
          ) : (
            <PricingCard service={service} />
          )}
          
          {/* Media Gallery */}
          {service && service.media && service.media.length > 0 && (
            <MediaGallery media={service.media} />
          )}
          
          {/* Review Form */}
          {service && (
            <ReviewForm
              serviceId={service.serviceId}
              onSubmitSuccess={handleReviewSubmitted}
              onServerUpdate={handleServerReviews}
            />
          )}
          
          {/* Reviews List */}
          {reviewsError ? (
            <div className="text-red-600 dark:text-red-400 text-center py-4">{reviewsError}</div>
          ) : (
            <ReviewsList reviews={reviews} />
          )}
          
          {/* Infinite Scroll Loader for Mobile */}
          {hasMore && (
            <div ref={mobileLoaderRef} className="text-center py-4">
              {reviewsLoading ? (
                // Show loading skeletons when fetching
                <>
                  <ReviewSkeleton />
                  <ReviewSkeleton />
                  <div className="animate-spin rounded-full h-8 w-8 border-t-2 border-b-2 border-primary mx-auto mt-4"></div>
                </>
              ) : (
                <span className="text-gray-500 dark:text-gray-400">Scroll to load more reviews...</span>
              )}
            </div>
          )}
        </div>

        {/* Desktop Layout - 3 Column Grid */}
        <div className="hidden lg:grid grid-cols-1 lg:grid-cols-3 gap-8 mb-12">
          {/* Left Column - 2/3 width */}
          <div className="lg:col-span-2 space-y-8">
            {/* Service Description */}
            <section className="bg-white dark:bg-gray-800 rounded-xl shadow-md p-6">
              <h2 className="text-2xl font-heading font-bold text-gray-900 dark:text-white mb-4">
                About This Service
              </h2>
              {!service || serviceLoading ? (
                // Skeleton loader for description
                <>
                  <div className="h-4 bg-gray-300 dark:bg-gray-700 rounded w-full mb-2 animate-pulse"></div>
                  <div className="h-4 bg-gray-300 dark:bg-gray-700 rounded w-full mb-2 animate-pulse"></div>
                  <div className="h-4 bg-gray-300 dark:bg-gray-700 rounded w-3/4 animate-pulse"></div>
                </>
              ) : (
                <p className="text-gray-700 dark:text-gray-300">{service.description}</p>
              )}
            </section>
            
            {/* Media Gallery */}
            {service && service.media && service.media.length > 0 && (
              <MediaGallery media={service.media} />
            )}
            
            {/* Reviews List */}
            {reviewsError ? (
              <div className="text-red-600 dark:text-red-400 text-center py-4">{reviewsError}</div>
            ) : (
              <ReviewsList reviews={reviews} />
            )}
            
            {/* Infinite Scroll Loader for Desktop */}
            {hasMore && (
              <div ref={desktopLoaderRef} className="text-center py-4">
                {reviewsLoading ? (
                  // Show loading skeletons when fetching
                  <>
                    <ReviewSkeleton />
                    <ReviewSkeleton />
                    <div className="animate-spin rounded-full h-8 w-8 border-t-2 border-b-2 border-primary mx-auto mt-4"></div>
                  </>
                ) : (
                  <span className="text-gray-500 dark:text-gray-400">Scroll to load more reviews...</span>
                )}
              </div>
            )}
          </div>
          
          {/* Right Column - 1/3 width */}
          <div className="lg:col-span-1 space-y-6">
            {/* Pricing Card */}
            {!service || serviceLoading ? (
              <div className="bg-white dark:bg-gray-800 rounded-xl shadow-md p-6 animate-pulse">
                <div className="h-6 bg-gray-300 dark:bg-gray-700 rounded w-1/3 mb-4"></div>
                <div className="h-8 bg-gray-300 dark:bg-gray-700 rounded w-1/2 mb-4"></div>
                <div className="h-4 bg-gray-300 dark:bg-gray-700 rounded w-full mb-2"></div>
                <div className="h-4 bg-gray-300 dark:bg-gray-700 rounded w-full mb-4"></div>
                <div className="h-10 bg-gray-300 dark:bg-gray-700 rounded w-full"></div>
              </div>
            ) : (
              <PricingCard service={service} />
            )}
            
            {/* Review Form */}
            {service && (
              <ReviewForm
                serviceId={service.serviceId}
                onSubmitSuccess={handleReviewSubmitted}
                onServerUpdate={handleServerReviews}
              />
            )}
          </div>
        </div>
      </main>
    </ErrorBoundary>
  );
}

// Helper function to calculate new average rating after review
function calcNewAverage(oldAvg: number, oldCount: number, newRating: number) {
  if (oldCount === 0) return newRating;
  return ((oldAvg * oldCount) + newRating) / (oldCount + 1);
}