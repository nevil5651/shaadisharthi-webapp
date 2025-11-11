import { useInfiniteQuery, useQuery } from '@tanstack/react-query';
import api from '@/lib/axios';
import { Service } from '@/lib/types';
import { ServiceFilters } from './useServices';

// Interface for the services API response
interface ServicesResponse {
  services: Service[];  // Array of services
  hasMore: boolean;     // Whether there are more pages to load
  nextPage: number;     // Next page number
}

// Custom hook to fetch services with infinite scrolling and filtering
export const useServices = (filters: ServiceFilters) => {
  return useInfiniteQuery<ServicesResponse>({
    initialPageParam: 1,  // Start from page 1
    // Query key includes all filters for proper caching
    queryKey: ['services', 
      filters.category, 
      filters.location, 
      filters.sortBy,
      filters.minPrice,
      filters.maxPrice, 
      filters.rating
    ],
    // Query function to fetch services from API
    queryFn: async ({ pageParam }) => {
      // Build query parameters from filters
      const query = new URLSearchParams({
        page: String(pageParam),
        limit: '12',  // 12 items per page
        ...(filters.category && { category: filters.category }),
        ...(filters.location && { location: filters.location }),
        ...(filters.minPrice && { minPrice: filters.minPrice }),
        ...(filters.maxPrice && { maxPrice: filters.maxPrice }),
        ...(filters.rating && { rating: filters.rating }),
        ...(filters.sortBy && { sortBy: filters.sortBy }),
      }).toString();

      // API call to fetch services
      const response = await api.get(`/Customer/services?${query}`);
      
      return {
        services: response.data.services,
        hasMore: response.data.hasMore,
        nextPage: (pageParam as number) + 1,
      };
    },
    // Determine if there's a next page to fetch
    getNextPageParam: (lastPage) => {
      return lastPage.hasMore ? lastPage.nextPage : undefined;
    },
    staleTime: 2 * 60 * 1000, // Data is fresh for 2 minutes
    gcTime: 10 * 60 * 1000, // Cache persists for 10 minutes (formerly cacheTime)
    refetchOnWindowFocus: false, // Don't refetch when window gains focus
  });
};

// Custom hook to fetch service categories
export const useServiceCategories = () => {
  return useQuery({
    queryKey: ['categories'],  // Unique key for categories cache
    queryFn: async () => {
      // Return static categories data (in real app, this would be an API call)
      return [
        { name: 'Photography', icon: 'fa-camera', color: { bg: 'bg-indigo-100 dark:bg-indigo-900', text: 'text-indigo-600 dark:text-indigo-300' } },
        { name: 'Venues', icon: 'fa-building', color: { bg: 'bg-purple-100 dark:bg-purple-900', text: 'text-purple-600 dark:text-purple-300' } },
        { name: 'Sound', icon: 'fa-volume-up', color: { bg: 'bg-purple-100 dark:bg-purple-900', text: 'text-purple-600 dark:text-purple-300' } },
        { name: 'Catering', icon: 'fa-utensils', color: { bg: 'bg-green-100 dark:bg-green-900', text: 'text-green-600 dark:text-green-300' } },
        { name: 'Decoration', icon: 'fa-paint-brush', color: { bg: 'bg-pink-100 dark:bg-pink-900', text: 'text-pink-600 dark:text-pink-300' } },
        { name: 'Bridal Wear', icon: 'fa-tshirt', color: { bg: 'bg-red-100 dark:bg-red-900', text: 'text-red-600 dark:text-red-300' } },
        { name: 'Jewellery', icon: 'fa-gem', color: { bg: 'bg-yellow-100 dark:bg-yellow-900', text: 'text-yellow-600 dark:text-yellow-300' } },
        { name: 'Favors', icon: 'fa-gift', color: { bg: 'bg-blue-100 dark:bg-blue-900', text: 'text-blue-600 dark:text-blue-300' } },
        { name: 'Planners', icon: 'fa-clipboard-list', color: { bg: 'bg-teal-100 dark:bg-teal-900', text: 'text-teal-600 dark:text-teal-300' } },
        { name: 'Bridal Makeup', icon: 'fa-magic', color: { bg: 'bg-pink-100 dark:bg-pink-900', text: 'text-pink-600 dark:text-pink-300' } },
        { name: 'Videographers', icon: 'fa-video', color: { bg: 'bg-indigo-100 dark:bg-indigo-900', text: 'text-indigo-600 dark:text-indigo-300' } },
        { name: 'Groom Wear', icon: 'fa-user-tie', color: { bg: 'bg-blue-100 dark:bg-blue-900', text: 'text-blue-600 dark:text-blue-300' } },
        { name: 'Mehendi Artists', icon: 'fa-paint-brush', color: { bg: 'bg-orange-100 dark:bg-orange-900', text: 'text-orange-600 dark:text-orange-300' } },
        { name: 'Cakes', icon: 'fa-birthday-cake', color: { bg: 'bg-pink-100 dark:bg-pink-900', text: 'text-pink-600 dark:text-pink-300' } },
        { name: 'Cards', icon: 'fa-envelope', color: { bg: 'bg-red-100 dark:bg-red-900', text: 'text-red-600 dark:text-red-300' } },
        { name: 'Choreographers', icon: 'fa-music', color: { bg: 'bg-purple-100 dark:bg-purple-900', text: 'text-purple-600 dark:text-purple-300' } },
        { name: 'Entertainment', icon: 'fa-star', color: { bg: 'bg-yellow-100 dark:bg-yellow-900', text: 'text-yellow-600 dark:text-yellow-300' } },
      ];
    },
    staleTime: Infinity,  // Categories data never becomes stale
  });
};

export type { ServiceFilters };