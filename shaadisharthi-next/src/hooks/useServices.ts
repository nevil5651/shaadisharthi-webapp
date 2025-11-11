// useServices.ts (with reset filter added)
import { useState, useEffect, useRef, useCallback, useMemo } from 'react';
import api from '@/lib/axios';
import { isAxiosError } from 'axios';
import debounce from 'lodash/debounce';
import { toast } from 'react-toastify';
import { Service } from '@/lib/types';

export interface ServiceFilters {
  category: string;
  location: string;
  minPrice: string;
  maxPrice: string;
  rating: string;
  sortBy: string;
}

// This interface represents the shape of the service object coming directly from the backend API.
export interface BackendService {
  providerId?: string;
  serviceId: number;
  serviceName: string;
  description?: string;
  price: number;
  rating: number;
  reviewCount: number;
  location: string;
  imageUrl?: string;
  category: string;
  businessName: string;
  email?: string | null;
  phone?: string | null;
}

export const useServices = (initialFilters: ServiceFilters) => {
  const [filters, setFilters] = useState(initialFilters);
  const [services, setServices] = useState<Service[]>([]);
  const [page, setPage] = useState(1);
  const [hasMore, setHasMore] = useState(true);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const loaderRef = useRef<HTMLDivElement>(null);
  
  
  // Refs to avoid recreating fetchServices
  const isFetchingRef = useRef(false);
  const abortControllerRef = useRef<AbortController | null>(null);
  const filtersRef = useRef(filters);
  const hasMoreRef = useRef(hasMore);
  const pageRef = useRef(page);
  const filterChangeCount = useRef(0);  

  // Keep refs in sync
  useEffect(() => {
    filtersRef.current = filters;
    hasMoreRef.current = hasMore;
    pageRef.current = page;
  }, [filters, hasMore, page]);

  useEffect(() => {
  // Increment counter when key filters change
  filterChangeCount.current += 1;
}, [filters.category, filters.location, filters.sortBy]);

  const fetchServices = useCallback(async (isNewSearch = false) => {

    const currentFilterCount = filterChangeCount.current;
    if (isFetchingRef.current) return;
    
    // Don't fetch if no more pages and not a new search
    if (!isNewSearch && !hasMoreRef.current) return;
    
    // Cancel previous request if it exists
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
    }
    
    abortControllerRef.current = new AbortController();
    const signal = abortControllerRef.current.signal;
    
    isFetchingRef.current = true;
    setIsLoading(true);
    setError('');
    
    const currentFilters = filtersRef.current;
    const currentPage = pageRef.current;
    const targetPage = isNewSearch ? 1 : currentPage + 1;

    if (isNewSearch) {
      setPage(1);
      setServices([]);
      setHasMore(true);
      pageRef.current = 1;
      hasMoreRef.current = true;
    } else {
      setPage(targetPage);
      pageRef.current = targetPage;
    }

    const query = new URLSearchParams({
      page: String(targetPage),
      limit: '12',
      ...(currentFilters.category && { category: currentFilters.category }),
      ...(currentFilters.location && { location: currentFilters.location }),
      ...(currentFilters.minPrice && { minPrice: currentFilters.minPrice }),
      ...(currentFilters.maxPrice && { maxPrice: currentFilters.maxPrice }),
      ...(currentFilters.rating && { rating: currentFilters.rating }),
      ...(currentFilters.sortBy && { sortBy: currentFilters.sortBy }),
    }).toString();

    try {
      const response = await api.get(`/Customer/services?${query}`, {
        signal,
        timeout: 10000
      });
      
      if (signal.aborted) return;

      if (currentFilterCount !== filterChangeCount.current) {
      return; // Discard results if filters changed during fetch
  }

      const { services: backendServices, hasMore: more } = response.data;

      const formattedServices: Service[] = backendServices.map((service: BackendService) => ({
        providerId: service.providerId || String(service.serviceId), // Fallback for providerId
        serviceId: service.serviceId,
        name: service.serviceName, // Map serviceName to name
        description: service.description || '', // Provide a default
        price: service.price,
        rating: service.rating,
        reviewCount: service.reviewCount,
        location: service.location,
        imageUrl: service.imageUrl || '/img/default-vendor.jpg',
        category: service.category,
        businessName: service.businessName,
        email: service.email || null,
        phone: service.phone || null,
      }));

      setServices(prev => isNewSearch ? formattedServices : [...prev, ...formattedServices]);
      setHasMore(more);
      hasMoreRef.current = more;
      
    } catch (err: unknown) {
      if (signal.aborted || (err instanceof Error && err.name === 'AbortError')) {
        return;
      }
      
      if (isAxiosError(err)) {
        if (err.response?.status === 401) return;
        const message = err.response?.status === 429 ? 'Too many requests.' : 'Failed to load services.';
        setError(message);
        toast.error(message);
      } else {
        const message = 'An unexpected error occurred.';
        setError(message);
        toast.error(message);
      }
    } finally {
      if (!signal.aborted) {
        isFetchingRef.current = false;
        setIsLoading(false);
      }
    }
  }, []); // Empty dependencies - using refs instead

  // Effect for initial fetch and filter changes
  useEffect(() => {
    fetchServices(true);
  }, [filters, fetchServices]);

  // Debounced filter updates
  const debouncedSetFilters = useMemo(
    () => debounce((newFilters: ServiceFilters) => {
      setFilters(newFilters);
    }, 300),
    []
  );

  // Reset filters function
  const resetFilters = useCallback(() => {
    setFilters(initialFilters);
  }, [initialFilters]);

  // Effect for infinite scroll
  useEffect(() => {
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting && hasMore && !isLoading && !error) {
          fetchServices();
        }
      },
      { threshold: 0.5, rootMargin: '100px' }
    );

    const currentLoader = loaderRef.current;
    if (currentLoader) {
      observer.observe(currentLoader);
    }

    return () => {
      if (currentLoader) {
        observer.unobserve(currentLoader);
      }
    };
  }, [hasMore, isLoading, error, fetchServices]);

  const retryFetch = useCallback(() => {
    setError('');
    fetchServices(true);
  }, [fetchServices]);

  

  return { 
    services, 
    filters, 
    setFilters: debouncedSetFilters, 
    resetFilters, // Added resetFilters
    isLoading, 
    error, 
    hasMore, 
    loaderRef,
    retryFetch
  };
};