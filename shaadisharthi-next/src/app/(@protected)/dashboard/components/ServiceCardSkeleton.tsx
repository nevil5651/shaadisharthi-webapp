// ServiceCardSkeleton Component: Loading skeleton for service cards
// Provides visual placeholder while service data is being fetched
const ServiceCardSkeleton = () => (
  <div className="bg-white dark:bg-gray-800 rounded-xl shadow-sm overflow-hidden">
    {/* Animated pulse effect for loading state */}
    <div className="animate-pulse">
      {/* Image placeholder */}
      <div className="bg-gray-300 dark:bg-gray-700 h-48 w-full"></div>
      {/* Content placeholder */}
      <div className="p-4">
        {/* Header with service name and category placeholders */}
        <div className="flex justify-between items-start mb-2">
          <div className="h-5 bg-gray-300 dark:bg-gray-700 rounded w-3/4"></div>
          <div className="h-4 bg-gray-300 dark:bg-gray-700 rounded w-1/4"></div>
        </div>
        {/* Footer with rating and price placeholders */}
        <div className="flex items-center justify-between">
          <div className="flex items-center">
            <div className="h-4 w-4 bg-gray-300 dark:bg-gray-700 rounded-full mr-1"></div>
            <div className="h-4 bg-gray-300 dark:bg-gray-700 rounded w-8"></div>
          </div>
          <div className="h-5 bg-gray-300 dark:bg-gray-700 rounded w-1/4"></div>
        </div>
      </div>
    </div>
  </div>
);

export default ServiceCardSkeleton;