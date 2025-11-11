// components/ui/RatingStars.tsx
'use client';

import { useState } from 'react';

// Props interface for RatingStars component
interface RatingStarsProps {
  rating: number;                    // Current rating value (0-5)
  setRating?: (rating: number) => void;  // Optional function to update rating
  interactive?: boolean;             // Whether stars are clickable
}

// RatingStars component displays and optionally allows setting a rating
export function RatingStars({ rating, setRating, interactive = false }: RatingStarsProps) {
  // State to track which star is being hovered over (for interactive mode)
  const [hoverRating, setHoverRating] = useState(0);

  return (
    <div className="flex">
      {[1, 2, 3, 4, 5].map((i) => (
        <button
          key={i}
          type="button"
          disabled={!interactive}  // Disable button if not interactive
          onClick={() => interactive && setRating?.(i)}  // Set rating on click
          onMouseEnter={() => interactive && setHoverRating(i)}  // Track hover
          onMouseLeave={() => interactive && setHoverRating(0)}  // Clear hover
          className="focus:outline-none"  // Remove default focus outline
        >
          {/* Individual star component */}
          <StarIcon filled={i <= (hoverRating || rating)} />
        </button>
      ))}
    </div>
  );
}

// Individual star icon component
function StarIcon({ filled }: { filled: boolean }) {
  return (
    <svg
      className={`w-5 h-5 ${filled ? 'text-yellow-400' : 'text-gray-300 dark:text-gray-600'}`}
      fill="currentColor"
      viewBox="0 0 20 20"
    >
      {/* Star SVG path */}
      <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
    </svg>
  );
}