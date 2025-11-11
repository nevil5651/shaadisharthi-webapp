// components/ServiceCard.tsx
import Image from 'next/image';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { IconDefinition } from '@fortawesome/fontawesome-svg-core';

interface ServiceCardProps {
  image: string;
  alt: string;
  icon: IconDefinition;
  title: string;
  description: string;
}

const ServiceCard: React.FC<ServiceCardProps> = ({ image, alt, icon, title, description }) => {
  return (
    <div className="service-card bg-white dark:bg-gray-800 rounded-xl overflow-hidden shadow-lg transition duration-500">
      <div className="h-48 overflow-hidden">
        <Image src={image} alt={alt} width={800} height={480} className="w-full h-full object-cover" priority={false} />
      </div>
      <div className="p-6">
        <div className="w-16 h-16 bg-primary dark:bg-pink-600 bg-opacity-10 dark:bg-opacity-20 rounded-full flex items-center justify-center mx-auto mb-4 -mt-12">
          <FontAwesomeIcon icon={icon} className="text-white  text-2xl" />
        </div>
        <h3 className="text-xl font-bold text-center mb-2 font-playfair-display text-gray-800 dark:text-white">{title}</h3>
        <p className="text-gray-600 dark:text-gray-400 text-center mb-4">{description}</p>
      </div>
    </div>
  );
};

export default ServiceCard;