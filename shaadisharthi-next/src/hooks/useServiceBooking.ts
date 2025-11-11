import { useState, useEffect } from 'react';
import { BookingFormData } from '@/lib/serviceBooking';
import { Service } from '@/lib/types';
import { fetchService } from '@/lib/service';
import axios from 'axios'; 
import api from '@/lib/axios';

export const useServiceBooking = (serviceId: string, preloadedServiceData: string | null = null) => {
  const [service, setService] = useState<Service | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const loadService = async () => {
      setLoading(true);
      setError(null);

      if (serviceId) {
        try {
          const data = await fetchService(serviceId);
          setService(data);
        } catch  {
          setError('Failed to load service details');
        } finally {
          setLoading(false);
        }
      } else {
        setError('No service ID provided');
        setLoading(false);
      }
    };

    loadService();
  }, [serviceId, preloadedServiceData]);

  const submitBooking = async (formData: BookingFormData) => {
    try {
      // Remove provider_id before sending
      const dataToSend = formData;
      
      const response = await api.post('/Customer/process-bookings', dataToSend);
      return { success: true, data: response.data };
    } catch (err) {
      let errorMessage = 'Booking submission failed. Please try again.';
      if (axios.isAxiosError(err) && err.response) {
        // Prioritize the message from the backend if it's sent as a plain string.
        if (typeof err.response.data === 'string' && err.response.data) {
          errorMessage = err.response.data;
        } else {
          errorMessage =  err.response.data.error || 'An unknown error occurred on the server.';
        }
      }
      return { success: false, error: errorMessage };
    }
  };

  return { service, loading, error, submitBooking };
};