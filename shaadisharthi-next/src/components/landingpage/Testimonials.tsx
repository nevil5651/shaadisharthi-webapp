
import Image from 'next/image';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faQuoteLeft, faStar, faStarHalfAlt } from '@fortawesome/free-solid-svg-icons';
import SectionHeader from './SectionHeader';
import { Testimonial } from '@/lib/index';

const Testimonials = () => {
  const testimonials: Testimonial[] = [
    {
      images: ['https://randomuser.me/api/portraits/women/32.jpg'],
      name: 'Priya & Rohit',
      location: 'Delhi | December 2022',
      quote: '"ShaadiSharthi made our wedding planning so easy! We found the perfect photographer and caterer who understood exactly what we wanted. The platform saved us so much time and stress!"',
      rating: 5,
    },
    {
      images: ['https://randomuser.me/api/portraits/men/45.jpg', 'https://randomuser.me/api/portraits/women/45.jpg'],
      name: 'Aditya & Sneha',
      location: 'Mumbai | February 2023',
      quote: '"We were overwhelmed with wedding planning until we found ShaadiSharthi. The venue recommendations were spot-on, and we booked our dream location within days!"',
      rating: 4.5,
    },
    {
      images: ['https://randomuser.me/api/portraits/men/22.jpg'],
      name: 'Raj Photography',
      location: 'Vendor | Bangalore',
      quote: '"As a wedding photographer, ShaadiSharthi has connected me with wonderful couples who appreciate my artistic style. My bookings have increased by 40% since joining!"',
      rating: 5,
    },
  ];

  return (
    <section id="testimonials" className="py-16 bg-gray-50 dark:bg-gray-900">
      <div className="container mx-auto px-4">
        <SectionHeader subtitle="HAPPY COUPLES" title="What Couples Say About Us" />
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
          {testimonials.map((testimonial, index) => (
            <div key={index} className="bg-white dark:bg-gray-800 p-8 rounded-xl shadow-md relative">
              <div className="absolute -top-4 -left-4 text-6xl text-primary dark:text-pink-400 opacity-10">
                <FontAwesomeIcon icon={faQuoteLeft} className="text-white" />
              </div>
              <div className="flex items-center mb-6">
                {testimonial.images.map((img, i) => (
                  <Image key={i} src={img} alt="Client" width={64} height={64} className={`w-16 h-16 rounded-full object-cover border-4 border-primary dark:border-pink-400 border-opacity-20 ${i > 0 ? '-ml-4' : 'mr-4'}`} />
                ))}
                <div className={testimonial.images.length > 1 ? 'ml-4' : ''}>
                  <h4 className="font-bold font-playfair-display text-gray-800 dark:text-white">{testimonial.name}</h4>
                  <p className="text-gray-500 dark:text-gray-400 text-sm">{testimonial.location}</p>
                </div>
              </div>
              <p className="text-gray-600 dark:text-gray-300 italic relative z-10">{testimonial.quote}</p>
              <div className="mt-6 text-yellow-400">
                {Array.from({ length: Math.floor(testimonial.rating) }, (_, i) => (
                  <FontAwesomeIcon key={i} icon={faStar} className="text-white"/>
                ))}
                {testimonial.rating % 1 !== 0 && <FontAwesomeIcon icon={faStarHalfAlt} className="text-white"/>}
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
};

export default Testimonials;