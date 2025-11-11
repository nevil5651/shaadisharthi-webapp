import { useState, useEffect, useRef, useCallback } from 'react';
import api from '@/lib/axios';
import { isAxiosError } from 'axios';
import { toast } from 'react-toastify';
import { Service } from '@/lib/types';

// Interface for service data coming from backend API
interface BackendService {
  providerId?: string;
  serviceId: number;
  serviceName: string;
  description?: string;
  price: number;
  rating: number;
  reviewCount?: number;
  location?: string;
  imageUrl?: string;
  category: string;
  businessName?: string;
  email?: string | null;
  phone?: string | null;
}

// Custom hook for fetching and managing recommended services
export const useRecommendedServices = (limit: number = 4) => {
  // State for services, loading status, and errors
  const [services, setServices] = useState<Service[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const abortControllerRef = useRef<AbortController | null>(null);

  // Main function to fetch recommended services
  const fetchRecommended = useCallback(async () => {
    // Abort previous request if still pending
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
    }
    abortControllerRef.current = new AbortController();
    const signal = abortControllerRef.current.signal;

    setIsLoading(true);
    setError(null);

    // Build query parameters for API call
    const query = new URLSearchParams({
      page: '1',
      limit: String(limit),
      sortBy: 'popular', // Use 'popular' for "good" services; change to 'rating' if preferred
    }).toString();

    try {
      // API call to get recommended services
      const response = await api.get(`/Customer/services?${query}`, { signal, timeout: 10000 });
      if (signal.aborted) return;

      const backendServices = response.data.services || [];

      // Transform backend data to frontend Service type
      const formattedServices: Service[] = backendServices.map((service: BackendService) => ({
        providerId: service.providerId || String(service.serviceId), // Fallback to serviceId if providerId missing
        serviceId: service.serviceId,
        name: service.serviceName,
        description: service.description || '', // Default empty string
        price: service.price,
        rating: service.rating,
        reviewCount: service.reviewCount || 0, // Default to 0
        location: service.location || '', // Default empty string
        imageUrl: service.imageUrl || '/img/default-service.jpg',
        category: service.category,
        businessName: service.businessName || service.serviceName, // Fallback to serviceName
        email: service.email || null,
        phone: service.phone || null,
      }));

      setServices(formattedServices);
    } catch (err: unknown) {
      // Handle request cancellation
      if (signal.aborted || (err instanceof Error && err.name === 'AbortError')) {
        return;
      }
      // Handle different error types with appropriate messages
      const message = isAxiosError(err) && err.response?.status === 429 
        ? 'Too many requests. Please try again later.' 
        : 'Failed to load recommended services.';
      setError(message);
      toast.error(message);
    } finally {
      // Only update loading state if request wasn't aborted
      if (!signal.aborted) setIsLoading(false);
    }
  }, [limit]);

  // Fetch services on mount and cleanup on unmount
  useEffect(() => {
    fetchRecommended();
    return () => {
      if (abortControllerRef.current) abortControllerRef.current.abort();
    };
  }, [fetchRecommended]);

  // Retry function for manual refetching
  const retryFetch = useCallback(() => {
    setError(null);
    fetchRecommended();
  }, [fetchRecommended]);

  return { services, isLoading, error, retryFetch };
};