'use client'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { toast } from 'react-toastify';
import { BookingProvider, useBookingContext } from './BookingContext';
import { BookingFilters } from './components/BookingFilters';
import { BookingsList } from './components/BookingsList';
import { useQuery } from '@tanstack/react-query';
import api from '@/lib/axios';
import { isAxiosError } from 'axios';
import { useEffect, useState, ComponentProps, useMemo } from 'react';
import { useSearchParams } from 'next/navigation';
import { debounce } from 'lodash';
import { normalizeBookingStatus, formatDateForAPI } from './bookingUtils';

import { Booking } from '@/lib/bookings';

// Configure React Query client with caching options
const queryClient = new QueryClient({
    defaultOptions: {
        queries: {
            staleTime: 5 * 60 * 1000, // Data stays fresh for 5 minutes
            gcTime: 10 * 60 * 1000, // Cache time 10 minutes
            refetchOnWindowFocus: false // Don't refetch when window gains focus
        }
    }
});

export default function BookingsPage() {
    return (
        <QueryClientProvider client={queryClient}>
            <BookingProvider>
                <BookingsContent />
            </BookingProvider>
        </QueryClientProvider>
    );
}

function BookingsContent() {
    // Get URL search parameters for deep linking
    const searchParams = useSearchParams();
    const { filters, updateFilters, resetFilters } = useBookingContext();
    const [currentPage, setCurrentPage] = useState(1);

    // Fetch bookings data with React Query
    const { data, isLoading, error, refetch } = useQuery<{ bookings: Booking[], totalPages: number }, Error>({
        queryKey: ['bookings', filters, currentPage], // Unique key for caching
        queryFn: () => {
            // Prepare API parameters from filters
            const params = {
                status: filters.status === 'all' ? undefined : filters.status,
                search: filters.search,
                dateFrom: formatDateForAPI(filters.dateFrom),
                dateTo: formatDateForAPI(filters.dateTo),
                page: currentPage,
                limit: 10 // 10 items per page
            };
            
            // Log request parameters for debugging
            //console.log('[API Request] Fetching bookings with params:', params);
            
            return api.get('/Customer/cstmr-bookings', { params })
                .then(res => {
                    // Log successful response for debugging
                    // console.log('[API Response] Bookings data:', {
                    //     status: res.status,
                    //     data: res.data
                    // });
                    
                    // Normalize statuses in API response for frontend consistency
                    const normalizedData = {
                        ...res.data,
                        bookings: (res.data.bookings || []).map((booking: Booking) => ({
                            ...booking,
                            status: normalizeBookingStatus(booking.status)
                        }))
                    };
                    
                    return normalizedData;
                })
                .catch(err => {
                    // Log error response for debugging
                    console.error('[API Error] Failed to fetch bookings:', {
                        status: err.response?.status,
                        message: err.message,
                        config: err.config
                    });
                    throw err;
                });
        },
        enabled: !!filters, // Only run query when filters are available
    });

    // Debounce filter updates to prevent excessive API calls
    const debouncedUpdateFilters = useMemo(() => debounce(updateFilters, 300), [updateFilters]);

    const handleFilterChange = (name: string, value: string) => {
    // Reset to first page only when actual filter values change
    const currentFilterValue = filters[name as keyof typeof filters];
    if (currentFilterValue !== value) {
        setCurrentPage(1);
    }
    
    // Prevent end date before start date by adjusting dates accordingly
    const newFilters = {...filters, [name]: value};
    if (name === 'dateFrom' && newFilters.dateTo && value > newFilters.dateTo) {
        newFilters.dateTo = value;
    }
    if (name === 'dateTo' && newFilters.dateFrom && value < newFilters.dateFrom) {
        newFilters.dateFrom = value;
    }
    
    debouncedUpdateFilters(newFilters);
};

    const handlePageChange = (page: number) => {
        setCurrentPage(page);
    };

    // Handle booking cancellation with reason prompt
    const onCancel: ComponentProps<typeof BookingsList>['onCancel'] = async (id) => {
        const reason = window.prompt('Please provide a reason for cancellation:');
        if (reason === null) { // User clicked "Cancel" on the prompt
            return;
        }

        const toastId = toast.loading('Submitting cancellation...');

        try {
            await api.post(`/Customer/cstmr-action/${id}`, {
                action: 'cancel',
                reason,
            });

            toast.update(toastId, {
                render: 'Booking cancelled successfully!',
                type: 'success',
                isLoading: false,
                autoClose: 3000,
            });

            refetch(); // Refresh the bookings list after cancellation
        } catch (err) {
            console.error('Cancellation failed:', err);
            let errorMessage = 'An unknown error occurred';
            if (isAxiosError(err)) {
                errorMessage = err.response?.data?.message || err.message;
            } else if (err instanceof Error) {
                errorMessage = err.message;
            }

            toast.update(toastId, {
                render: `Cancellation failed: ${errorMessage}`,
                type: 'error',
                isLoading: false,
                autoClose: 5000,
            });
        }
    };

    // Handle payment action - currently shows info message as payment is not implemented
    const handlePayNow = () => {
  toast.info(
    "We apologize, but online payment is not yet available. Please arrange payment directly with the service provider.",
    {
      autoClose: 8000, // Keep message on screen longer for readability
      position: "top-center",
    }
  );
};

    // Refetch bookings when navigating from booking creation
    useEffect(() => {
        if (searchParams.get('fromBooking')) {
            refetch();
        }
    }, [searchParams, refetch]);

    return (
        <div className="container mx-auto px-4 py-8 bg-gray-50 dark:bg-gray-900 min-h-screen">
            {/* Filter Component */}
            <BookingFilters 
                filters={filters}
                onFilterChange={handleFilterChange} 
                onReset={resetFilters} 
            />
            
            {/* Bookings List Component */}
            <BookingsList 
                bookings={data?.bookings || []}
                loading={isLoading}
                error={error?.message || null}
                onCancel={onCancel}
                currentPage={currentPage}
                onPay={() => handlePayNow()}
                totalPages={data?.totalPages || 1}
                onPageChange={handlePageChange}
            />
        </div>
    );
}