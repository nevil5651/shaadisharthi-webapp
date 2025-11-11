import React from 'react';

// Props interface for all icon components
interface IconProps {
  className?: string;  // Optional CSS classes to apply to the icon
}

// Collection of icon components that render Font Awesome icons
// Each icon component accepts optional className prop for styling

export const CameraIcon: React.FC<IconProps> = ({ className = "" }) => (
  <i className={`fas fa-camera ${className}`} />
);

export const BuildingIcon: React.FC<IconProps> = ({ className = "" }) => (
  <i className={`fas fa-building ${className}`} />
);

export const VolumeUpIcon: React.FC<IconProps> = ({ className = "" }) => (
  <i className={`fas fa-volume-up ${className}`} />
);

export const UtensilsIcon: React.FC<IconProps> = ({ className = "" }) => (
  <i className={`fas fa-utensils ${className}`} />
);

export const PaintBrushIcon: React.FC<IconProps> = ({ className = "" }) => (
  <i className={`fas fa-paint-brush ${className}`} />
);

export const TShirtIcon: React.FC<IconProps> = ({ className = "" }) => (
  <i className={`fas fa-tshirt ${className}`} />
);

export const GemIcon: React.FC<IconProps> = ({ className = "" }) => (
  <i className={`fas fa-gem ${className}`} />
);

export const GiftIcon: React.FC<IconProps> = ({ className = "" }) => (
  <i className={`fas fa-gift ${className}`} />
);

export const ClipboardListIcon: React.FC<IconProps> = ({ className = "" }) => (
  <i className={`fas fa-clipboard-list ${className}`} />
);

export const MagicIcon: React.FC<IconProps> = ({ className = "" }) => (
  <i className={`fas fa-magic ${className}`} />
);

export const VideoIcon: React.FC<IconProps> = ({ className = "" }) => (
  <i className={`fas fa-video ${className}`} />
);

export const UserTieIcon: React.FC<IconProps> = ({ className = "" }) => (
  <i className={`fas fa-user-tie ${className}`} />
);

export const BirthdayCakeIcon: React.FC<IconProps> = ({ className = "" }) => (
  <i className={`fas fa-birthday-cake ${className}`} />
);

export const EnvelopeIcon: React.FC<IconProps> = ({ className = "" }) => (
  <i className={`fas fa-envelope ${className}`} />
);

export const MusicIcon: React.FC<IconProps> = ({ className = "" }) => (
  <i className={`fas fa-music ${className}`} />
);

export const StarIcon: React.FC<IconProps> = ({ className = "" }) => (
  <i className={`fas fa-star ${className}`} />
);

export const MapMarkerIcon: React.FC<IconProps> = ({ className = "" }) => (
  <i className={`fas fa-map-marker-alt ${className}`} />
);

export const ArrowRightIcon: React.FC<IconProps> = ({ className = "" }) => (
  <i className={`fas fa-arrow-right ${className}`} />
);

export const ChevronDownIcon: React.FC<IconProps> = ({ className = "" }) => (
  <i className={`fas fa-chevron-down ${className}`} />
);

export const ChevronUpIcon: React.FC<IconProps> = ({ className = "" }) => (
  <i className={`fas fa-chevron-up ${className}`} />
);

export const HeartIcon: React.FC<IconProps> = ({ className = "" }) => (
  <i className={`fas fa-heart ${className}`} />
);

export const SlidersIcon: React.FC<IconProps> = ({ className = "" }) => (
  <i className={`fas fa-sliders-h ${className}`} />
);

export const TimesIcon: React.FC<IconProps> = ({ className = "" }) => (
  <i className={`fas fa-times ${className}`} />
);