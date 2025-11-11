// BookingCard Component: Displays individual booking information with status and details
export default function BookingCard({ booking }: {
  booking: {
    id: number;
    serviceName: string;
    date: string;
    time: string;
    providerName: string;
    status: string;
  }
}) {
  // Color mapping for different booking statuses with dark mode support
  const statusColors = {
    pending: 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-black-200', 
    confirmed: 'bg-green-200 text-green-800 dark:bg-green-900 dark:text-green-100',
    cancelled: 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-500', 
    completed: 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-500' 
  };

  // Normalize status to ensure consistent casing and terminology
  // Convert backend status to lowercase for consistency
  let normalizedStatus = booking.status.toLowerCase();
  // Map 'accepted' status to 'confirmed' for UI consistency
  if (normalizedStatus === 'accepted') {
    normalizedStatus = 'confirmed';
  }
  

  return (
    <div className="bg-white dark:bg-gray-800 rounded-xl shadow-lg p-6 sm:p-7 hover:shadow-xl transition-all duration-300 border border-gray-100 dark:border-gray-700">
          
    {/* Header Section: Service name and status badge */}
    {/* Uses responsive flex layout for mobile and desktop */}
    <div className="flex flex-col sm:flex-row justify-between items-start mb-4">
        {/* Service Name: Responsive text with proper spacing */}
        <h3 className="text-xl font-bold text-gray-900 dark:text-white mb-2 sm:mb-0 leading-snug flex-grow min-w-0 mr-4">
            {booking.serviceName}
        </h3>
        
        {/* Status Badge: Color-coded status indicator */}
        {/* Uses self-start to prevent stretching on small screens */}
        <span className={`self-start flex-shrink-0 px-3 py-1 rounded-full text-sm font-semibold tracking-wide ${statusColors[normalizedStatus as keyof typeof statusColors]}`}>
            {normalizedStatus.toUpperCase()}
        </span>
    </div>
    
    {/* Details Section: Booking information in vertical layout */}
    <div className="space-y-3">
        
        {/* Provider Information */}
        <div className="flex items-center text-gray-700 dark:text-gray-300 text-base">
            {/* User Icon */}
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="text-pink-600 dark:text-pink-400 mr-3 flex-shrink-0 w-5 h-5"><path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2"></path><circle cx="12" cy="7" r="4"></circle></svg>
            <span className="font-medium mr-1">Provider:</span>
            <span>{booking.providerName}</span>
        </div>
        
        {/* Date Information with formatted display */}
        <div className="flex items-center text-gray-700 dark:text-gray-300 text-base">
            {/* Calendar Icon */}
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="text-pink-600 dark:text-pink-400 mr-3 flex-shrink-0 w-5 h-5"><rect width="18" height="18" x="3" y="4" rx="2" ry="2"></rect><line x1="16" x2="16" y1="2" y2="6"></line><line x1="8" x2="8" y1="2" y2="6"></line><line x1="3" x2="21" y1="10" y2="10"></line></svg>
            <span className="font-medium mr-1">Date:</span>
            {/* Format date to readable format */}
            <span>{new Date(booking.date).toLocaleDateString('en-US', { 
                weekday: 'short', 
                year: 'numeric', 
                month: 'short', 
                day: 'numeric' 
            })}</span>
        </div>
        
        {/* Time Information */}
        <div className="flex items-center text-gray-700 dark:text-gray-300 text-base">
            {/* Clock Icon */}
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="text-pink-600 dark:text-pink-400 mr-3 flex-shrink-0 w-5 h-5"><circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline></svg>
            <span className="font-medium mr-1">Time:</span>
            <span>{booking.time}</span>
        </div>
    </div>
</div>
  );
}