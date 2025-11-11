import React, { memo } from 'react';
import * as Icons from './Icons';

// Props interface for CategoryCard component
interface CategoryCardProps {
  category: string;        // Category name to display
  icon: string;           // Icon identifier from the icon map
  colorClass: { bg: string; text: string };  // Color classes for background and text
  isSelected: boolean;    // Whether this category is currently selected
  onClick: (category: string) => void;  // Click handler function
}

// Map icon names to their corresponding React components
const iconComponentMap: Record<string, React.FC<{ className?: string }>> = {
  'fa-camera': Icons.CameraIcon,
  'fa-building': Icons.BuildingIcon,
  'fa-volume-up': Icons.VolumeUpIcon,
  'fa-utensils': Icons.UtensilsIcon,
  'fa-paint-brush': Icons.PaintBrushIcon,
  'fa-tshirt': Icons.TShirtIcon,
  'fa-gem': Icons.GemIcon,
  'fa-gift': Icons.GiftIcon,
  'fa-clipboard-list': Icons.ClipboardListIcon,
  'fa-magic': Icons.MagicIcon,
  'fa-video': Icons.VideoIcon,
  'fa-user-tie': Icons.UserTieIcon,
  'fa-birthday-cake': Icons.BirthdayCakeIcon,
  'fa-envelope': Icons.EnvelopeIcon,
  'fa-music': Icons.MusicIcon,
  'fa-star': Icons.StarIcon,
};

// CategoryCard component displays a single service category as a clickable card
const CategoryCard: React.FC<CategoryCardProps> = ({
  category,
  icon,
  colorClass,
  isSelected,
  onClick,
}) => {
  // Get the appropriate icon component or fallback to StarIcon
  const IconComponent = iconComponentMap[icon] || Icons.StarIcon;
  
  return (
    <button
      onClick={() => onClick(category)}
      className={`flex items-center p-3 rounded-lg hover:shadow-md transition-all w-full ${
        isSelected 
          ? 'bg-primary text-white shadow-md'  // Selected state styles
          : `bg-white dark:bg-gray-800 text-gray-800 dark:text-gray-200 hover:bg-gray-50 dark:hover:bg-gray-700 ${colorClass.text}`  // Default state styles
      }`}
      aria-pressed={isSelected}  // Accessibility attribute for selected state
    >
      {/* Icon container with dynamic background color */}
      <div className={`w-10 h-10 rounded-full flex items-center justify-center mr-3 ${
        isSelected ? 'bg-white/20' : colorClass.bg  // Use white background when selected
      }`}>
        <IconComponent className={isSelected ? 'text-white' : ''} />
      </div>
      {/* Category name */}
      <span className="font-medium text-sm truncate">{category}</span>
    </button>
  );
};

// Custom equality function for React.memo to prevent unnecessary re-renders
const areEqual = (prevProps: CategoryCardProps, nextProps: CategoryCardProps) => {
  return prevProps.category === nextProps.category &&
         prevProps.icon === nextProps.icon &&
         prevProps.colorClass.bg === nextProps.colorClass.bg &&
         prevProps.colorClass.text === nextProps.colorClass.text &&
         prevProps.isSelected === nextProps.isSelected &&
         prevProps.onClick === nextProps.onClick; // Critical: compare function reference
};

// Export memoized component with custom equality check
export default memo(CategoryCard, areEqual);