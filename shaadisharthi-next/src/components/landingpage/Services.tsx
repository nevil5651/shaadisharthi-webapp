
'use client';

import { useState } from 'react';
import ServiceCard from './ServiceCard';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faChevronDown, faChevronUp } from '@fortawesome/free-solid-svg-icons';
import { faCamera, faBuilding, faUtensils, faPalette, faTshirt, faGift, faGem, faClipboardList, faMagic, faVideo, faUserTie, faPaintBrush, faBirthdayCake, faEnvelope, faMusic, faMicrophoneAlt } from '@fortawesome/free-solid-svg-icons';
import SectionHeader from './SectionHeader';

const Services = () => {
  const [showMore, setShowMore] = useState(false);

  const services = [
    { title: 'Photographers', description: 'Capture your precious moments with our award-winning photographers.', image: 'https://images.unsplash.com/photo-1523438885200-e635ba2c371e?ixlib=rb-1.2.1&auto=format&fit=crop&w=800&q=80', alt: 'Photography', icon: faCamera },
    { title: 'Venues', description: 'Find the perfect setting for your ceremony and reception.', image: 'https://images.unsplash.com/photo-1517245386807-bb43f82c33c4?ixlib=rb-1.2.1&auto=format&fit=crop&w=800&q=80', alt: 'Venues', icon: faBuilding },
    { title: 'Caterers', description: 'Exquisite cuisine that will delight your guests\' palates.', image: 'https://images.unsplash.com/photo-1515934751635-c81c6bc9a2d8?ixlib=rb-1.2.1&auto=format&fit=crop&w=800&q=80', alt: 'Caterers', icon: faUtensils },
    { title: 'Decorators', description: 'Transform your venue into a magical space.', image: 'https://images.unsplash.com/photo-1513151233558-d860c5398176?ixlib=rb-1.2.1&auto=format&fit=crop&w=800&q=80', alt: 'Decorators', icon: faPalette },
    { title: 'Bridal Wear', description: 'Stunning outfits for the bride on her special day.', image: 'https://images.unsplash.com/photo-1529634806980-85c3dd6d34ac?ixlib=rb-1.2.1&auto=format&fit=crop&w=800&q=80', alt: 'Bridal Wear', icon: faTshirt },
    { title: 'Favors', description: 'Beautiful keepsakes for your wedding guests.', image: 'https://images.unsplash.com/photo-1519225421980-715cb0215aed?ixlib=rb-1.2.1&auto=format&fit=crop&w=800&q=80', alt: 'Favors', icon: faGift },
    { title: 'Jewellery', description: 'Exquisite pieces to complement your bridal look.', image: 'https://images.unsplash.com/photo-1515934751635-c81c6bc9a2d8?ixlib=rb-1.2.1&auto=format&fit=crop&w=800&q=80', alt: 'Jewellery', icon: faGem },
    { title: 'Planners', description: 'Professional planners to handle every detail.', image: 'https://images.unsplash.com/photo-1513151233558-d860c5398176?ixlib=rb-1.2.1&auto=format&fit=crop&w=800&q=80', alt: 'Planners', icon: faClipboardList },
    { title: 'Bridal Makeup', description: 'Professional artists to enhance your natural beauty.', image: 'https://images.unsplash.com/photo-1523438885200-e635ba2c371e?ixlib=rb-1.2.1&auto=format&fit=crop&w=800&q=80', alt: 'Makeup', icon: faMagic },
    { title: 'Videographers', description: 'Capture your special moments in motion.', image: 'https://images.unsplash.com/photo-1515934751635-c81c6bc9a2d8?ixlib=rb-1.2.1&auto=format&fit=crop&w=800&q=80', alt: 'Videographers', icon: faVideo },
    { title: 'Groom Wear', description: 'Elegant outfits for the groom.', image: 'https://images.unsplash.com/photo-1513151233558-d860c5398176?ixlib=rb-1.2.1&auto=format&fit=crop&w=800&q=80', alt: 'Groom Wear', icon: faUserTie },
    { title: 'Mehendi Artists', description: 'Beautiful henna designs for your hands and feet.', image: 'https://images.unsplash.com/photo-1523438885200-e635ba2c371e?ixlib=rb-1.2.1&auto=format&fit=crop&w=800&q=80', alt: 'Mehendi Artists', icon: faPaintBrush },
    { title: 'Cakes', description: 'Delicious and beautiful wedding cakes.', image: 'https://images.unsplash.com/photo-1515934751635-c81c6bc9a2d8?ixlib=rb-1.2.1&auto=format&fit=crop&w=800&q=80', alt: 'Cakes', icon: faBirthdayCake },
    { title: 'Invitation Cards', description: 'Beautiful designs for your wedding invitations.', image: 'https://images.unsplash.com/photo-1513151233558-d860c5398176?ixlib=rb-1.2.1&auto=format&fit=crop&w=800&q=80', alt: 'Cards', icon: faEnvelope },
    { title: 'Choreographers', description: 'Create magical dance performances for your wedding.', image: 'https://images.unsplash.com/photo-1523438885200-e635ba2c371e?ixlib=rb-1.2.1&auto=format&fit=crop&w=800&q=80', alt: 'Choreographers', icon: faMusic },
    { title: 'Entertainment', description: 'Live music, DJs, and performers for your wedding.', image: 'https://images.unsplash.com/photo-1515934751635-c81c6bc9a2d8?ixlib=rb-1.2.1&auto=format&fit=crop&w=800&q=80', alt: 'Entertainment', icon: faMicrophoneAlt },
  ];

  return (
    <section id="services" className="py-16 bg-gray-50 dark:bg-gray-900">
      <div className="container mx-auto px-4">
        <SectionHeader subtitle="OUR SERVICES" title="Everything For Your Perfect Wedding" />
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8" id="mainServices">
          {services.slice(0, 3).map((service) => (
            <ServiceCard key={service.title} {...service} />
          ))}
        </div>
        <div className={`grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8 ${showMore ? 'block' : 'hidden'}`} id="moreServices">
          {services.slice(3).map((service) => (
            <ServiceCard key={service.title} {...service} />
          ))}
        </div>
        <div className="text-center mt-12">
          <button
            onClick={() => setShowMore(!showMore)}
            className="btn-secondary hover:shadow-lg text-white py-3 px-8 rounded-full text-lg transition duration-300"
          >
            {showMore ? 'View Less Services' : 'View All Services'} <FontAwesomeIcon icon={showMore ? faChevronUp : faChevronDown} className="ml-2" />
          </button>
        </div>
      </div>
    </section>
  );
};

export default Services;