import { RatingStars } from '@/app/(@protected)/services/components/RatingStars';
import { Service } from '@/lib/types';
import Image from 'next/image';

interface ServiceHeroProps {
  service: Service;
}

// ServiceHero component displays the main service header with image,
// service name, business name, rating, and review count
export default function ServiceHero({ service }: ServiceHeroProps) {
  return (
    <div className="service-hero rounded-xl p-8 mb-8 bg-gradient-to-r from-pink-50 to-purple-50 dark:from-pink-900/20 dark:to-purple-900/20">
      <div className="flex flex-col md:flex-row items-center">
        {/* Service image with fallback for missing images */}
        {service.imageUrl && service.imageUrl.trim() !== '' ? (
          <Image
            src={service.imageUrl}
            alt={service.serviceName}
            width={160}
            height={160}
            className="rounded-full border-4 border-white dark:border-gray-800 shadow-lg mr-0 md:mr-8 mb-4 md:mb-0"
          />
        ) : (
          // Fallback UI when no image is available
          <div className="w-40 h-40 rounded-full border-4 border-white dark:border-gray-800 shadow-lg mr-0 md:mr-8 mb-4 md:mb-0 bg-gray-200 dark:bg-gray-700 flex items-center justify-center">
            <span className="text-gray-500 dark:text-gray-400">No Image</span>
          </div>
        )}
        <div className="text-center md:text-left">
          {/* Service name */}
          <h1 className="text-3xl md:text-4xl font-heading font-bold text-gray-900 dark:text-white">
            {service.serviceName}
          </h1>
          {/* Business name */}
          <p className="text-gray-600 dark:text-gray-300 mt-2">by {service.businessName}</p>
          <div className="mt-4 flex justify-center md:justify-start items-center">
            {/* Rating stars and review count */}
            <RatingStars rating={Math.round(service.rating)} />
            <span className="ml-2 text-gray-600 dark:text-gray-300">
              ({service.reviewCount} reviews)
            </span>
          </div>
        </div>
      </div>
    </div>
  );
}