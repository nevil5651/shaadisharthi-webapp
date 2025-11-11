// BookingCardSkeleton Component: Loading skeleton for booking cards
// Provides visual feedback while content is loading with dark mode support
const BookingCardSkeleton = () => (
  <div className="bg-white dark:bg-gray-800 rounded-xl shadow-sm p-6">
    {/* Animated pulse effect for loading state */}
    <div className="animate-pulse space-y-4">
      {/* Header section skeleton */}
      <div className="flex justify-between">
        {/* Service name placeholder */}
        <div className="h-6 bg-gray-200 dark:bg-gray-700 rounded w-3/4"></div>
        {/* Status badge placeholder */}
        <div className="h-6 bg-gray-200 dark:bg-gray-700 rounded w-1/4"></div>
      </div>
      {/* Details section skeleton */}
      <div className="space-y-3">
        {/* Provider info placeholder */}
        <div className="flex items-center">
          <div className="h-4 w-4 bg-gray-200 dark:bg-gray-700 rounded-full mr-3"></div>
          <div className="h-4 bg-gray-200 dark:bg-gray-700 rounded w-1/2"></div>
        </div>
        {/* Date info placeholder */}
        <div className="flex items-center">
          <div className="h-4 w-4 bg-gray-200 dark:bg-gray-700 rounded-full mr-3"></div>
          <div className="h-4 bg-gray-200 dark:bg-gray-700 rounded w-1/2"></div>
        </div>
        {/* Time info placeholder */}
        <div className="flex items-center">
          <div className="h-4 w-4 bg-gray-200 dark:bg-gray-700 rounded-full mr-3"></div>
          <div className="h-4 bg-gray-200 dark:bg-gray-700 rounded w-1/2"></div>
        </div>
      </div>
    </div>
  </div>
);

export { BookingCardSkeleton };