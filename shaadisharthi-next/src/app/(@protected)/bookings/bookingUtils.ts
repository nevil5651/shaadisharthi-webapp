// Utility functions for booking management

// Maps backend statuses to frontend display statuses for consistency
export const normalizeBookingStatus = (status: string): string => {
  const statusMap: Record<string, string> = {
    'pending': 'Pending',
    'accepted': 'Confirmed',  // Map backend "Accepted" to frontend "Confirmed"
    'rejected': 'Cancelled',  // Map backend "Rejected" to frontend "Cancelled"
    'confirmed': 'Confirmed',
    'cancelled': 'Cancelled',
    'completed': 'Completed'
  };
  
  return statusMap[status.toLowerCase()] || status;
};

// Determines if booking can be cancelled based on its status
export const isCancellable = (status: string): boolean => {
  return ['Pending', 'Confirmed'].includes(status);
};

// Determines if payment button should be shown 
export const showPaymentButton = (paymentStatus: string, status: string): boolean => {
  return paymentStatus === 'unpaid' && 
         ['Confirmed', 'Completed'].includes(status);
};

// Formats date for API requests in YYYY-MM-DD format
export const formatDateForAPI = (dateString: string): string => {
  if (!dateString) return '';
  try {
    const date = new Date(dateString);
    return date.toISOString().split('T')[0];
  } catch {
    console.error('Invalid date format:', dateString);
    return '';
  }
};