import React from 'react';
import { HeartIcon } from './Icons';

// Props interface for EmptyState component
interface EmptyStateProps {
  onReset?: () => void;  // Optional reset filters callback
}

// EmptyState component displayed when no vendors match the current filters
const EmptyState = ({ onReset }: EmptyStateProps) => {
  return (
    <div className="text-center py-12">
      {/* Heart icon for visual representation */}
      <HeartIcon className="text-4xl text-gray-300 dark:text-gray-600 mb-4" />
      {/* Empty state message */}
      <h3 className="text-xl font-medium text-gray-700 dark:text-gray-300">No vendors found</h3>
      <p className="text-gray-500 dark:text-gray-400 mt-2">
        We couldn&apos;t find any vendors matching your criteria
      </p>
      {/* Reset filters button - only shown if onReset callback is provided */}
      {onReset && (
        <button
          onClick={onReset}
          className="mt-4 px-4 py-2 bg-primary text-white rounded-lg hover:bg-opacity-90 transition-colors"
        >
          Reset Filters
        </button>
      )}
    </div>
  );
};

export default EmptyState;