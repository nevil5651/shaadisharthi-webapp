'use client'
import { Booking } from '@/lib/bookings';
import { FaUserTie, FaCalendarAlt, FaClock, FaRupeeSign, FaTimes, FaCheck } from 'react-icons/fa';
import { normalizeBookingStatus, isCancellable, showPaymentButton } from '../bookingUtils';

// Define possible booking status types
type BookingStatus = 'Pending' | 'Confirmed' | 'Cancelled' | 'Completed';
type BookingCardProps = {
  booking: Booking;
  onCancel: (id: string) => void;
  onPay: (id: string) => void;
};

// Color mapping for different booking statuses
const statusColors: Record<BookingStatus, string> = {
  Pending: 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-black-200',
  Confirmed: 'bg-green-200 text-green-800 dark:bg-green-900 dark:text-green-100',
  Cancelled: 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-500',
  Completed: 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-500',
};

export const BookingCard = ({ 
  booking,
  onCancel,
  onPay,
}: BookingCardProps) => {
  // Handle case where booking data is invalid
  if (!booking) {
    return <div className="p-4 text-red-500">Error: Invalid booking data</div>;
  }

  // Normalize status from backend to frontend format and get corresponding color
  const normalizedStatus = normalizeBookingStatus(booking.status) as BookingStatus;
  const statusColor = statusColors[normalizedStatus] || 'bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-200';

  return (
    <div className="bg-white dark:bg-gray-800 rounded-xl shadow-lg border border-gray-100 dark:border-gray-700 overflow-hidden hover:shadow-xl transition duration-300 ease-in-out">
      <div className="p-6 sm:p-8">
        {/* Header Section: Service Name and Status Badge */}
        <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between mb-4">
          <h3 className="text-2xl font-extrabold text-gray-900 dark:text-white mb-3 sm:mb-0 leading-snug">
            {booking.serviceName}
          </h3>
          {/* Status badge that changes color based on booking status */}
          <span className={`self-start flex-shrink-0 px-3 py-1 rounded-full text-sm font-semibold tracking-wide ${statusColor}`}>
            {normalizedStatus.toUpperCase()}
          </span>
        </div>

        {/* Details Grid - Shows booking information in a responsive layout */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-y-4 gap-x-6 mb-6 pt-4 border-t border-gray-100 dark:border-gray-700">
          {/* Service Provider Name */}
          <div className="flex items-center text-gray-700 dark:text-gray-300">
            <FaUserTie className="text-pink-600 dark:text-pink-400 mr-3 text-lg flex-shrink-0 w-5 h-5" />
            <span className="font-medium text-sm sm:text-base whitespace-nowrap">Provider:</span>
            <span className="ml-1 text-sm sm:text-base truncate">{booking.providerName}</span>
          </div>
          {/* Booking Amount */}
          <div className="flex items-center text-gray-900 dark:text-white sm:justify-end order-first sm:order-none">
            <FaRupeeSign className="text-pink-600 dark:text-pink-400 mr-2 text-xl font-bold flex-shrink-0 w-6 h-6" />
            <span className="font-extrabold text-2xl">
              {booking.amount.toLocaleString()}
            </span>
          </div>
          {/* Booking Date */}
          <div className="flex items-center text-gray-700 dark:text-gray-300">
            <FaCalendarAlt className="text-pink-600 dark:text-pink-400 mr-3 text-lg flex-shrink-0 w-5 h-5" />
            <span className="font-medium text-sm sm:text-base whitespace-nowrap">Date:</span>
            <span className="ml-1 text-sm sm:text-base">
              {new Date(booking.date).toLocaleDateString('en-US', {
                weekday: 'short',
                year: 'numeric',
                month: 'short',
                day: 'numeric',
              })}
            </span>
          </div>
          {/* Booking Time */}
          <div className="flex items-center text-gray-700 dark:text-gray-300">
            <FaClock className="text-pink-600 dark:text-pink-400 mr-3 text-lg flex-shrink-0 w-5 h-5" />
            <span className="font-medium text-sm sm:text-base whitespace-nowrap">Time:</span>
            <span className="ml-1 text-sm sm:text-base">{booking.time}</span>
          </div>
        </div>

        {/* Action Buttons - Conditionally show based on booking status */}
        <div className="flex flex-wrap justify-end gap-3 pt-4 border-t border-gray-100 dark:border-gray-700">
          {/* Show cancel button only for cancellable statuses */}
          {isCancellable(normalizedStatus) && (
            <button
              onClick={() => onCancel(booking.id)}
              aria-label={`Cancel booking for ${booking.serviceName}`}
              className="flex items-center px-4 py-2 border border-red-500 text-red-500 dark:text-red-400 dark:border-red-400 rounded-lg font-medium hover:bg-red-50 dark:hover:bg-red-900/30 transition duration-150"
            >
              <FaTimes className="mr-2 w-4 h-4" /> Cancel
            </button>
          )}
          {/* Show payment button only when payment is required and status allows it */}
          {showPaymentButton(booking.paymentStatus, normalizedStatus) && (
            <button
              onClick={() => onPay(booking.id)}
              aria-label={`Pay for ${booking.serviceName}`}
              className="flex items-center px-5 py-2 bg-pink-600 text-white rounded-lg font-medium hover:bg-pink-700 shadow-md hover:shadow-lg transition duration-150 transform hover:scale-[1.02]"
            >
              <FaCheck className="mr-2 w-4 h-4" /> Pay Now
            </button>
          )}
        </div>
      </div>
    </div>
  );
};