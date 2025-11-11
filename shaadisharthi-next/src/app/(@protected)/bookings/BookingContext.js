import { createContext, useContext, useState, useCallback } from 'react';
import { useQueryClient } from '@tanstack/react-query';

// Create React Context for managing booking filters state
const BookingContext = createContext();

export const BookingProvider = ({ children }) => {
    // State for managing filter values
    const [filters, setFilters] = useState({
        status: 'all',
        search: '',
        dateFrom: '',
        dateTo: ''
    });
    const queryClient = useQueryClient();

    // Update filters and invalidate queries to trigger refetch
    const updateFilters = useCallback((newFilters) => {
        setFilters(prev => ({ ...prev, ...newFilters }));
        // Invalidate and refetch bookings when filters change
        queryClient.invalidateQueries(['bookings']);
    }, [queryClient]);

    // Reset all filters to default values
    const resetFilters = useCallback(() => {
        setFilters({
            status: 'all',
            search: '',
            dateFrom: '',
            dateTo: ''
        });
        queryClient.invalidateQueries(['bookings']);
    }, [queryClient]);

    return (
        <BookingContext.Provider value={{ filters, updateFilters, resetFilters }}>
            {children}
        </BookingContext.Provider>
    );
};

// Custom hook to access booking context
export const useBookingContext = () => useContext(BookingContext);