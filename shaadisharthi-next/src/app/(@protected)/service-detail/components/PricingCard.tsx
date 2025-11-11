import { Service } from '@/lib/service';
import { useRouter } from 'next/navigation';

interface PricingCardProps {
  service: Service;
}

// PricingCard component displays service pricing, contact information,
// and handles navigation to the booking page
export function PricingCard({ service }: PricingCardProps) {
  const router = useRouter();
  
  // Format price with Indian Rupee symbol and locale formatting
  const displayPrice = service.price || 0;
  const formattedPrice = `₹${displayPrice.toLocaleString('en-IN', { minimumFractionDigits: 2 })} + taxes`;
  
  // Display contact information with fallbacks
  const phoneNumber = service.phone ? `📞 +91 ${service.phone}` : '📞 Not available';
  const email = service.email || '✉️ Not available';

  // Handle booking navigation - passes service ID to booking page
  const handleBookNow = () => {
    // Pass only the service ID to the booking page.
    // The booking page will be responsible for fetching the service details.
    router.push(`/service-booking?serviceId=${service.serviceId}`);
  };

  return (
    <div className="bg-white dark:bg-gray-800 rounded-xl shadow-md p-6">
      <h3 className="text-xl font-heading font-bold text-gray-900 dark:text-white mb-4">Pricing</h3>
      <p className="text-2xl font-bold text-primary mb-4">{formattedPrice}</p>
      <p className="text-gray-600 dark:text-gray-300 mb-2">{phoneNumber}</p>
      <p className="text-gray-600 dark:text-gray-300 mb-4">{email}</p>
      <button onClick={handleBookNow} className="gradient-btn">
        Book Now
      </button>
    </div>
  );
}