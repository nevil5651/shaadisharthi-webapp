import React from 'react';

// Skeleton loader component for VendorCard
// Used as placeholder while vendor data is loading
const VendorCardSkeleton = () => {
  return (
    <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md overflow-hidden animate-pulse">
      {/* Image placeholder */}
      <div className="w-full h-48 bg-gray-300 dark:bg-gray-700"></div>
      <div className="p-4">
        {/* Service name placeholder */}
        <div className="h-4 bg-gray-300 dark:bg-gray-700 rounded w-3/4 mb-2"></div>
        {/* Location placeholder */}
        <div className="h-3 bg-gray-300 dark:bg-gray-700 rounded w-1/2 mb-4"></div>
        {/* Price and button placeholders */}
        <div className="flex items-center justify-between">
          <div className="h-4 bg-gray-300 dark:bg-gray-700 rounded w-1/4"></div>
          <div className="h-4 bg-gray-300 dark:bg-gray-700 rounded w-1/4"></div>
        </div>
      </div>
    </div>
  );
};

export default VendorCardSkeleton;