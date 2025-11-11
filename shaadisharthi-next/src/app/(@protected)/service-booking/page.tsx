'use client'

import { useForm, UseFormRegister, FieldErrors } from 'react-hook-form';
import { useEffect, Suspense, useCallback } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { useServiceBooking } from '@/hooks/useServiceBooking';
import { BookingFormData } from '@/lib/serviceBooking';
import { toast } from 'react-toastify';
import { FaSpinner, FaCalendarCheck } from 'react-icons/fa';

// =============================================================================
// Loading Component
// =============================================================================

/**
 * Loading state component with consistent styling
 * Used for both initial load and Suspense fallback
 */
const LoadingState = () => (
  <div className="bg-gray-50 dark:bg-gray-900 font-['Poppins'] text-black dark:text-white flex justify-center items-center h-screen">
    <div className="text-center">
      <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-pink-600 dark:border-pink-400 mx-auto"></div>
      <p className="mt-4">Loading Service Details...</p>
    </div>
  </div>
);

// =============================================================================
// Service Header Component
// =============================================================================

interface ServiceHeaderProps {
  serviceName: string;
}

/**
 * Displays the service booking header with gradient background
 * Consistent padding and spacing with main form
 */
const ServiceHeader = ({ serviceName }: ServiceHeaderProps) => (
  <div className="bg-gradient-to-r text-center mb-4 from-primary/5 to-secondary/5 dark:from-primary/10 dark:to-secondary/10 p-6 border-b border-gray-100 dark:border-gray-700">
    <h1 className="text-3xl font-bold font-serif text-gray-800 dark:text-white">
      Book {serviceName}
    </h1>
    <p className="opacity-90 text-gray-600 dark:text-gray-400">
      Complete your booking details below
    </p>
  </div>
);

// =============================================================================
// Service Info Component
// =============================================================================

interface ServiceInfoProps {
  service: {
    serviceName: string;
    businessName: string;
    price: number;
  };
}

/**
 * Displays service information in a 3-column grid
 * Shows service name, provider, and price with consistent spacing
 */
const ServiceInfo = ({ service }: ServiceInfoProps) => (
  <div className="bg-gradient-to-r from-primary/5 to-secondary/5 dark:from-primary/10 dark:to-secondary/10 p-6 border-b border-gray-100 dark:border-gray-700">
    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
      <div>
        <p className="text-sm text-gray-500 dark:text-gray-400">Service</p>
        <p className="font-medium text-gray-800 dark:text-white">{service.serviceName}</p>
      </div>
      <div>
        <p className="text-sm text-gray-500 dark:text-gray-400">Provider</p>
        <p className="font-medium text-gray-800 dark:text-white">{service.businessName}</p>
      </div>
      <div>
        <p className="text-sm text-gray-500 dark:text-gray-400">Price</p>
        <p className="font-medium text-gray-800 dark:text-white">₹{service.price.toFixed(2)}</p>
      </div>
    </div>
  </div>
);

// =============================================================================
// Form Field Components
// =============================================================================

interface FormFieldProps {
  label: string;
  error?: string;
  children: React.ReactNode;
}

/**
 * Reusable form field wrapper with consistent spacing and error handling
 */
const FormField = ({ label, error, children }: FormFieldProps) => (
  <div>
    <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
      {label}
    </label>
    {children}
    {error && (
      <p className="mt-1 text-sm text-red-600 dark:text-red-400">{error}</p>
    )}
  </div>
);

// =============================================================================
// Customer Information Section
// =============================================================================

interface CustomerInfoSectionProps {
  register: UseFormRegister<BookingFormData>;
  errors: FieldErrors<BookingFormData>;
}

/**
 * Customer information section with name and phone fields
 * Uses grid layout for responsive design
 */
const CustomerInfoSection = ({ register, errors }: CustomerInfoSectionProps) => (
  <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
    <FormField 
      label="Full Name" 
      error={errors.customer_name?.message}
    >
      <input
        id="customer_name"
        {...register('customer_name', { required: 'Full name is required' })}
        className="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary dark:focus:ring-pink-400 focus:border-transparent transition-all bg-white dark:bg-gray-700 text-gray-800 dark:text-white"
      />
    </FormField>
    
    <FormField 
      label="Phone Number" 
      error={errors.phone?.message}
    >
      <input
        id="phone"
        type="tel"
        {...register('phone', { 
          required: 'Phone number is required',
          pattern: {
            value: /^\d{10}$/,
            message: 'Please enter a valid 10-digit phone number'
          }
        })}
        className="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary dark:focus:ring-pink-400 focus:border-transparent transition-all bg-white dark:bg-gray-700 text-gray-800 dark:text-white"
      />
    </FormField>
  </div>
);

// =============================================================================
// Event Details Section
// =============================================================================

interface EventDetailsSectionProps {
  register: UseFormRegister<BookingFormData>;
  errors: FieldErrors<BookingFormData>;
}

/**
 * Event details section with date, time, and venue fields
 * Includes optional reception date field
 */
const EventDetailsSection = ({ register, errors }: EventDetailsSectionProps) => (
  <>
    <FormField 
      label="Event Venue Address" 
      error={errors.event_address?.message}
    >
      <input
        id="event_address"
        {...register('event_address', { required: 'Venue address is required' })}
        className="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary dark:focus:ring-pink-400 focus:border-transparent transition-all bg-white dark:bg-gray-700 text-gray-800 dark:text-white"
      />
    </FormField>

    <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
      <FormField 
        label="Event Date" 
        error={errors.event_start_date?.message}
      >
        <input
          id="event_start_date"
          type="date"
          {...register('event_start_date', { required: 'Wedding date is required' })}
          className="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary dark:focus:ring-pink-400 focus:border-transparent transition-all bg-white dark:bg-gray-700 text-gray-800 dark:text-white"
        />
      </FormField>
      
      <FormField 
        label="Time" 
        error={errors.event_time?.message}
      >
        <input
          id="event_time"
          type="time"
          {...register('event_time', { required: 'Event time is required' })}
          className="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary dark:focus:ring-pink-400 focus:border-transparent transition-all bg-white dark:bg-gray-700 text-gray-800 dark:text-white"
        />
      </FormField>
      
      <FormField label="Reception Date (Optional)">
        <input
          id="event_end_date"
          type="date"
          {...register('event_end_date')}
          className="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary dark:focus:ring-pink-400 focus:border-transparent transition-all bg-white dark:bg-gray-700 text-gray-800 dark:text-white"
        />
      </FormField>
    </div>
  </>
);

// =============================================================================
// Submit Button Component
// =============================================================================

interface SubmitButtonProps {
  isSubmitting: boolean;
}

/**
 * Animated submit button with loading states
 * Maintains consistent padding and hover effects
 */
const SubmitButton = ({ isSubmitting }: SubmitButtonProps) => (
  <div className="pt-4">
    <button
      type="submit"
      className="w-full bg-gradient-to-r from-pink-600 to-orange-500 dark:from-pink-500 dark:to-orange-400 text-white py-3 px-6 rounded-lg font-medium hover:from-pink-700 hover:to-orange-600 dark:hover:from-pink-600 dark:hover:to-orange-500 transition-all disabled:opacity-50"
      disabled={isSubmitting}
    >
      {isSubmitting ? (
        <span className="flex items-center justify-center">
          <FaSpinner className="animate-spin mr-2" /> Processing...
        </span>
      ) : (
        <span className="flex items-center justify-center">
          <FaCalendarCheck className="mr-2" /> Confirm Booking
        </span>
      )}
    </button>
  </div>
);

// =============================================================================
// Main Booking Form Component
// =============================================================================

const ServiceBookingForm = () => {
  const router = useRouter();
  const searchParams = useSearchParams();
  const serviceId = searchParams.get('serviceId') || '';
  
  // Custom hook for service booking logic
  const { service, loading, submitBooking } = useServiceBooking(serviceId);

  // React Hook Form initialization
  const {
    register,
    handleSubmit,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<BookingFormData>();

  // ===========================================================================
  // Side Effects
  // ===========================================================================

  /**
   * Redirect to services page if no serviceId is provided
   * Runs when serviceId or loading state changes
   */
  useEffect(() => {
    if (!loading && !serviceId) {
      toast.error("No service was selected for booking.");
      router.replace('/services');
    }
  }, [serviceId, loading, router]);

  /**
   * Initialize form values and document title when service data loads
   * Batches updates to prevent multiple re-renders
   */
  useEffect(() => {
    if (service) {
      // Set form default values from service data
      setValue('service_id', service.serviceId);
      setValue('service_name', service.serviceName);
      setValue('service_price', service.price);
      setValue('email', service.email || '');
      
      // Update page title for better UX
      document.title = `Book ${service.serviceName} | ShaadiSharthi`;
    }
  }, [service, setValue]);

  // ===========================================================================
  // Event Handlers
  // ===========================================================================

  /**
   * Form submission handler
   * Uses useCallback to prevent unnecessary re-renders
   */
  const onSubmit = useCallback(async (data: BookingFormData) => {
    try {
      const result = await submitBooking(data);
      
      if (result.success) {
        toast.success('Booking request sent! You will be notified upon confirmation or rejection.');
        router.push('/bookings?fromBooking=true');
      } else {
        toast.error(result.error || 'Booking failed. Please check your details and try again.');
      }
    } catch (error) {
      toast.error('An unexpected error occurred. Please try again.');
    }
  }, [submitBooking, router]);

  // ===========================================================================
  // Render Logic
  // ===========================================================================

  // Show loading state while fetching service data
  if (loading) return <LoadingState />;  
  
  // Show not found state if no service data
  if (!service) return <div className="text-center py-8 text-gray-800 dark:text-gray-200">Service not found</div>;

  return (
    <main className="bg-gray-50 dark:bg-gray-900 min-h-screen">
      <div className="container mx-auto px-4 py-8">
        {/* Main booking card with consistent shadow and border */}
        <div className="bg-white dark:bg-gray-800 rounded-xl overflow-hidden max-w-3xl mx-auto shadow-[0_10px_25px_-5px_rgba(0,0,0,0.1)] border border-gray-100 dark:border-gray-700">
          
          {/* Service header section */}
          <ServiceHeader serviceName={service.serviceName} />
          
          {/* Service information section */}
          <ServiceInfo service={service} />
          
          {/* Booking form with consistent spacing */}
          <form onSubmit={handleSubmit(onSubmit)} className="p-6 space-y-6">
            {/* Hidden form fields for service data */}
            <input type="hidden" {...register('service_id')} />
            <input type="hidden" {...register('service_name')} />

            {/* Customer information section */}
            <CustomerInfoSection register={register} errors={errors} />
            
            {/* Email field (disabled) */}
            <FormField label="Email">
              <input
                id="email"
                {...register('email')}
                className="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-gray-100 dark:bg-gray-700 text-gray-800 dark:text-gray-300 cursor-not-allowed"
                disabled
              />
            </FormField>

            {/* Editable price field */}
            <FormField 
              label="Service Price (₹)" 
              error={errors.service_price?.message}
            >
              <input
                id="service_price"
                type="number"
                {...register('service_price', { 
                  required: 'Price is required',
                  min: {
                    value: 0,
                    message: 'Price must be positive'
                  },
                  valueAsNumber: true
                })}
                className="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary dark:focus:ring-pink-400 focus:border-transparent transition-all bg-white dark:bg-gray-700 text-gray-800 dark:text-white"
              />
            </FormField>

            {/* Event details section */}
            <EventDetailsSection register={register} errors={errors} />

            {/* Special requirements textarea */}
            <FormField label="Special Requirements">
              <textarea
                id="notes"
                rows={3}
                {...register('notes')}
                className="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary dark:focus:ring-pink-400 focus:border-transparent transition-all bg-white dark:bg-gray-700 text-gray-800 dark:text-white"
                placeholder="Any specific requirements or customization needed..."
              />
            </FormField>

            {/* Submit button */}
            <SubmitButton isSubmitting={isSubmitting} />
          </form>
        </div>
      </div>
    </main>
  );
};

// =============================================================================
// Main Page Component with Suspense Boundary
// =============================================================================

/**
 * Main page component wrapped in Suspense for streaming support
 * Handles loading state during service data fetching
 */
const ServiceBookingPage = () => {
  return (
    <Suspense fallback={<LoadingState />}>
      <ServiceBookingForm />
    </Suspense>
  );
};

export default ServiceBookingPage;