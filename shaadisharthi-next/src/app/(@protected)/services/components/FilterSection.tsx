import React from 'react';
import { useDebouncedCallback } from 'use-debounce';
import { ServiceFilters } from '@/hooks/useServicesQuery';
import { SlidersIcon, TimesIcon } from './Icons';

// Props interface for FilterSection component
interface FilterSectionProps {
  filters: ServiceFilters;                    // Current filter values
  setFilters: (filters: ServiceFilters) => void;  // Function to update filters
  onResetFilters: () => void;                // Function to reset all filters
}

// FilterSection component that provides filtering options for services
const FilterSection: React.FC<FilterSectionProps> = ({ filters, setFilters, onResetFilters }) => {
  // State to control visibility of advanced filters
  const [showFilters, setShowFilters] = React.useState(false);

  // Static data for filter options
  const categories = [
    "Photography", "Venues", "Sound", "Catering", "Decoration",
    "Bridal Wear", "Jewellery", "Favors", "Planners", "Bridal Makeup",
    "Videographers", "Groom Wear", "Mehendi Artists", "Cakes", "Cards",
    "Choreographers", "Beauty", "Entertainment"
  ];

  const locations = ["Vadodara", "Rajkot", "Jamnagar", "Hyderabad", "Chennai"];
  const ratings = ["5", "4", "3", "2", "1"];
  const sortOptions = [
    { value: "popular", label: "Most Popular" },
    { value: "rating", label: "Highest Rated" },
    { value: "price_low", label: "Price: Low to High" },
    { value: "price_high", label: "Price: High to Low" }
  ];

  // Debounce filter updates to prevent excessive API calls
  const debouncedSetFilters = useDebouncedCallback((newFilters: ServiceFilters) => {
    setFilters(newFilters);
  }, 300);

  // Handle changes to filter inputs
  const handleFilterChange = (e: React.ChangeEvent<HTMLSelectElement | HTMLInputElement>) => {
    const { name, value } = e.target;
    const newFilters = { ...filters, [name]: value };
    debouncedSetFilters(newFilters);
  };

  // Toggle visibility of advanced filters
  const toggleFilters = () => {
    setShowFilters(!showFilters);
  };

  return (
    <div className="filter-section bg-white dark:bg-gray-800 rounded-lg shadow-sm dark:shadow-md p-6 mb-8">
      {/* Filter header with toggle button */}
      <div className="flex justify-between items-center">
        <h2 className="text-xl font-semibold text-gray-800 dark:text-gray-200">Filters</h2>
        <button
          id="filterToggle"
          className="filter-toggle flex items-center text-secondary dark:text-pink-400 font-medium hover:text-opacity-80 dark:hover:text-opacity-80"
          onClick={toggleFilters}
          aria-expanded={showFilters}  // Accessibility attribute
        >
          {showFilters ? <TimesIcon className="mr-2" /> : <SlidersIcon className="mr-2" />}
          <span>Advanced Filters</span>
        </button>
      </div>

      {/* Filter form - conditionally rendered based on showFilters state */}
      <form
        id="filterForm"
        className={`filter-content ${showFilters ? 'block' : 'hidden'} mt-4 transition-all duration-300`}
        onSubmit={(e) => e.preventDefault()}  // Prevent form submission
      >
        <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
          {/* Category Filter Dropdown */}
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              Category
            </label>
            <select
              name="category"
              value={filters.category}
              onChange={handleFilterChange}
              className="w-full rounded-lg border border-gray-300 dark:border-gray-600 focus:border-primary dark:focus:border-pink-500 focus:outline-none focus:ring-2 focus:ring-primary dark:focus:ring-pink-500 focus:ring-opacity-50 dark:focus:ring-opacity-50 p-2 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-200"
            >
              <option value="">All Categories</option>
              {categories.map((cat) => (
                <option key={cat} value={cat}>
                  {cat}
                </option>
              ))}
            </select>
          </div>

          {/* Location Filter Dropdown */}
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              Location
            </label>
            <select
              name="location"
              value={filters.location}
              onChange={handleFilterChange}
              className="w-full rounded-lg border border-gray-300 dark:border-gray-600 focus:border-primary dark:focus:border-pink-500 focus:outline-none focus:ring-2 focus:ring-primary dark:focus:ring-pink-500 focus:ring-opacity-50 dark:focus:ring-opacity-50 p-2 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-200"
            >
              <option value="">All Locations</option>
              {locations.map((loc) => (
                <option key={loc} value={loc}>
                  {loc}
                </option>
              ))}
            </select>
          </div>

          {/* Price Range Inputs */}
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              Price Range
            </label>
            <div className="flex items-center space-x-2">
              <input
                type="number"
                name="minPrice"
                placeholder="Min"
                value={filters.minPrice || ''}
                onChange={handleFilterChange}
                className="w-1/2 rounded-lg border border-gray-300 dark:border-gray-600 focus:border-primary dark:focus:border-pink-500 focus:outline-none focus:ring-2 focus:ring-primary dark:focus:ring-pink-500 focus:ring-opacity-50 dark:focus:ring-opacity-50 p-2 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-200"
              />
              <span className="text-gray-500 dark:text-gray-400">-</span>
              <input
                type="number"
                name="maxPrice"
                placeholder="Max"
                value={filters.maxPrice || ''}
                onChange={handleFilterChange}
                className="w-1/2 rounded-lg border border-gray-300 dark:border-gray-600 focus:border-primary dark:focus:border-pink-500 focus:outline-none focus:ring-2 focus:ring-primary dark:focus:ring-pink-500 focus:ring-opacity-50 dark:focus:ring-opacity-50 p-2 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-200"
              />
            </div>
          </div>

          {/* Rating Filter Dropdown */}
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              Minimum Rating
            </label>
            <select
              name="rating"
              value={filters.rating}
              onChange={handleFilterChange}
              className="w-full rounded-lg border border-gray-300 dark:border-gray-600 focus:border-primary dark:focus:border-pink-500 focus:outline-none focus:ring-2 focus:ring-primary dark:focus:ring-pink-500 focus:ring-opacity-50 dark:focus:ring-opacity-50 p-2 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-200"
            >
              <option value="">Any Rating</option>
              {ratings.map((r) => (
                <option key={r} value={r}>
                  {r} {r === '5' ? 'Stars' : '+ Stars'}
                </option>
              ))}
            </select>
          </div>

          {/* Sort and Reset Controls */}
          <div className="md:col-span-4 flex justify-between items-center">
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Sort By
              </label>
              <select
                name="sortBy"
                value={filters.sortBy}
                onChange={handleFilterChange}
                className="rounded-lg border border-gray-300 dark:border-gray-600 focus:border-primary dark:focus:border-pink-500 focus:outline-none focus:ring-2 focus:ring-primary dark:focus:ring-pink-500 focus:ring-opacity-50 dark:focus:ring-opacity-50 p-2 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-200"
              >
                {sortOptions.map((opt) => (
                  <option key={opt.value} value={opt.value}>
                    {opt.label}
                  </option>
                ))}
              </select>
            </div>
            <div className="flex space-x-2">
              {/* Reset Filters Button */}
              <button
                type="button"
                onClick={onResetFilters}
                className="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-600 transition-colors text-gray-900 dark:text-gray-200"
              >
                Reset
              </button>
            </div>
          </div>
        </div>
      </form>
    </div>
  );
};

export default FilterSection;