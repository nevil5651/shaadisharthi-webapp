import React from 'react';

interface AuthCardProps {
  title: string;
  subtitle: string;
  children: React.ReactNode;
}

// Custom SVG wedding ring icon
// Premium detailed wedding ring icon
const RingIcon = () => (
  <svg 
    className="text-pink-500 dark:text-pink-400 w-10 h-10 mr-2" 
    viewBox="0 0 32 32" 
    fill="none"
    stroke="currentColor"
    strokeWidth="1"
    aria-hidden="true"
  >
    {/* Outer ring band */}
    <ellipse cx="16" cy="16" rx="7" ry="7" strokeWidth="1.5" />
    
    {/* Inner ring band */}
    <ellipse cx="16" cy="16" rx="5" ry="5" strokeWidth="1.2" />
    
    {/* Main diamond */}
    <path d="M16,10 L20,16 L16,22 L12,16 Z" fill="currentColor" />
    
    {/* Diamond facets for realism */}
    <path d="M16,10 L18,13 L16,16 Z" fill="white" fillOpacity="0.4" />
    <path d="M16,16 L18,13 L20,16 Z" fill="white" fillOpacity="0.2" />
    <path d="M16,16 L20,16 L16,22 Z" fill="white" fillOpacity="0.1" />
    <path d="M16,16 L12,16 L16,22 Z" fill="white" fillOpacity="0.3" />
    
    {/* Smaller side stones */}
    <path d="M22,16 L24,14 L26,16 L24,18 Z" fill="currentColor" fillOpacity="0.7" />
    <path d="M10,16 L8,14 L6,16 L8,18 Z" fill="currentColor" fillOpacity="0.7" />
    <path d="M16,22 L14,24 L16,26 L18,24 Z" fill="currentColor" fillOpacity="0.7" />
    <path d="M16,10 L14,8 L16,6 L18,8 Z" fill="currentColor" fillOpacity="0.7" />
    
    {/* Decorative elements around the band */}
    <circle cx="23.5" cy="16" r="0.8" fill="currentColor" />
    <circle cx="8.5" cy="16" r="0.8" fill="currentColor" />
    <circle cx="16" cy="23.5" r="0.8" fill="currentColor" />
    <circle cx="16" cy="8.5" r="0.8" fill="currentColor" />
  </svg>
);

export default function AuthCard({ title, subtitle, children }: AuthCardProps) {
  return (
    <div className="bg-white dark:bg-gray-800 rounded-xl shadow-xl p-6 sm:p-8 m-4 sm:m-7 max-w-md w-full">
      <div className="text-center mb-6">
        <div className="flex items-center justify-center mb-4">
          <RingIcon />
          <span className="text-2xl sm:text-3xl font-bold text-gray-800 dark:text-white font-serif">ShaadiSharthi</span>
        </div>
        <h1 className="text-xl sm:text-2xl font-bold text-gray-800 dark:text-white font-serif">{title}</h1>
        <p className="text-gray-600 dark:text-gray-400 mt-2 text-sm sm:text-base">{subtitle}</p>
      </div>
      {children}
    </div>
  );
}