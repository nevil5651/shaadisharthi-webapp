import { useRef, useState } from 'react';
import { FaSearch, FaTimes, FaFilter } from 'react-icons/fa';
import { FiChevronDown } from 'react-icons/fi';

export const BookingFilters = ({
  filters,
  onFilterChange,
  onReset
}: {
  filters: {
    status: string;
    search: string;
    dateFrom: string;
    dateTo: string;
  };
  onFilterChange: (name: string, value: string) => void;
  onReset: () => void;
}) => {
  // Available status options for filtering
  const statusOptions = [
    { value: 'all', label: 'All Statuses' },
    { value: 'Pending', label: 'Pending' },
    { value: 'Confirmed', label: 'Confirmed' },
    { value: 'Cancelled', label: 'Cancelled' },
    { value: 'Completed', label: 'Completed' }
  ];

  // Track which date input is currently focused for label positioning
  const [focusedInput, setFocusedInput] = useState<string | null>(null); 
  const dateFromRef = useRef<HTMLInputElement>(null);
  const dateToRef = useRef<HTMLInputElement>(null);

  // Handle reset to also clear focus states
  const handleReset = () => {
    setFocusedInput(null);
    onReset();
  };

  // Handle date input changes with validation
  const handleDateChange = (name: 'dateFrom' | 'dateTo', value: string) => {
    onFilterChange(name, value);
    
    // If user clears the date, also remove focus state
    if (!value && focusedInput === name) {
      setFocusedInput(null);
    }
  };

  return (
    <div className="bg-white dark:bg-gray-800 rounded-xl shadow-lg p-6 mb-8 border border-gray-100 dark:border-gray-700">
      <div className="flex flex-col md:flex-row gap-4 items-end">
        {/* Search Input with clear button */}
        <div className="flex-1 w-full">
          <div className="relative">
            <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-pink-500">
              <FaSearch className="text-lg" />
            </div>
            <input
              type="text"
              value={filters.search}
              onChange={(e) => onFilterChange('search', e.target.value)}
              className="block w-full pl-10 pr-10 py-2.5 border border-gray-200 dark:border-gray-600 rounded-lg focus:ring-2 focus:outline-none focus:ring-pink-300 focus:border-pink-500 bg-gray-50 dark:bg-gray-700 dark:text-white transition-all"
              placeholder="Search bookings..."
            />
            {/* Show clear button only when there's search text */}
            {filters.search && (
              <button
                onClick={() => onFilterChange('search', '')}
                className="absolute inset-y-0 right-0 pr-3 flex items-center text-gray-400 hover:text-pink-600 transition"
              >
                <FaTimes />
              </button>
            )}
          </div>
        </div>

        {/* Status Filter Dropdown */}
        <div className="w-full md:w-48 relative">
          <div className="relative">
            <select
              value={filters.status}
              onChange={(e) => onFilterChange('status', e.target.value)}
              className="appearance-none block w-full pl-3 pr-8 py-2.5 border border-gray-200 dark:border-gray-600 focus:outline-none rounded-lg focus:ring-2 focus:ring-pink-300 focus:border-pink-500 bg-gray-50 dark:bg-gray-700 dark:text-white cursor-pointer transition-all"
            >
              {statusOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
            {/* Custom dropdown arrow */}
            <div className="pointer-events-none absolute inset-y-0 right-0 flex items-center pr-2 text-gray-400">
              <FiChevronDown className="text-lg" />
            </div>
          </div>
        </div>

        {/* Date Range Filter with floating labels */}
        <div className="grid grid-cols-2 gap-3 w-full md:w-72">
          <div className="relative">
            <input
              type="date"
              ref={dateFromRef}
              value={filters.dateFrom}
              onChange={(e) => handleDateChange('dateFrom', e.target.value)}
              onFocus={() => setFocusedInput('dateFrom')}
              onBlur={() => {
                // Only remove focus if no date is selected
                if (!filters.dateFrom) {
                  setFocusedInput(null);
                }
              }}
              placeholder="mm/dd/yyyy"
              max={filters.dateTo || undefined} // Prevent selecting date after "to" date
              className="block w-full pl-3 pr-3 py-2.5 border border-gray-200 dark:border-gray-600 rounded-lg focus:outline-none focus:ring-2 focus:ring-pink-300 focus:border-pink-500 bg-gray-50 dark:bg-gray-700 dark:text-white transition-all"
            />
            {/* Floating label for "From" date */}
            {(filters.dateFrom || focusedInput === 'dateFrom') && (
              <span className="absolute -top-2 left-2 px-1 text-xs bg-white dark:bg-gray-800 text-pink-600 dark:text-pink-400">From</span>
            )}
          </div>
          <div className="relative">
            <input
              type="date"
              ref={dateToRef}
              value={filters.dateTo}
              onChange={(e) => handleDateChange('dateTo', e.target.value)}
              onFocus={() => setFocusedInput('dateTo')}
              onBlur={() => {
                // Only remove focus if no date is selected
                if (!filters.dateTo) {
                  setFocusedInput(null);
                }
              }}
              placeholder="mm/dd/yyyy"
              min={filters.dateFrom || undefined} // Prevent selecting date before "from" date
              className="block w-full pl-3 pr-3 focus:outline-none py-2.5 border border-gray-200 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-pink-300 focus:border-pink-500 bg-gray-50 dark:bg-gray-700 dark:text-white transition-all"
            />
            {/* Floating label for "To" date */}
            {(filters.dateTo || focusedInput === 'dateTo') && (
              <span className="absolute -top-2 left-2 px-1 text-xs bg-white dark:bg-gray-800 text-pink-600 dark:text-pink-400">To</span>
            )}
          </div>
        </div>

        {/* Reset Filters Button */}
        <button
          onClick={handleReset}
          className="px-4 py-2.5 border border-pink-200 dark:border-pink-700 rounded-lg bg-pink-50 dark:bg-pink-900/30 text-pink-600 dark:text-pink-300 hover:bg-pink-100 dark:hover:bg-pink-900/50 transition-all flex items-center gap-2 whitespace-nowrap"
        >
          <FaFilter className="text-pink-500 dark:text-pink-400" />
          Reset Filters
        </button>
      </div>
    </div>
  );
};