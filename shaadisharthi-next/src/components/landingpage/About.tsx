import Image from 'next/image';
import Link from 'next/link';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faCheck, faArrowRight } from '@fortawesome/free-solid-svg-icons';
import SectionHeader from './SectionHeader';

const About = () => {
  return (
    <section id="about" className="py-16 bg-white dark:bg-gray-900">
      <div className="container mx-auto px-4">
        <div className="flex flex-col md:flex-row items-center">
          <div className="md:w-1/2 mb-8 md:mb-0 md:pr-8">
            <Image src="https://images.unsplash.com/photo-1529634806980-85c3dd6d34ac?ixlib=rb-1.2.1&auto=format&fit=crop&w=800&q=80" alt="About ShaadiSharthi" width={800} height={480} className="rounded-xl shadow-lg w-full" />
          </div>
          <div className="md:w-1/2">
            <SectionHeader subtitle="ABOUT US" title="Your Wedding, Our Passion" />
            <p className="text-gray-600 dark:text-gray-300 mb-4">Founded in 2023, ShaadiSharthi was born from a simple idea: wedding planning should be joyful, not stressful. Our team of wedding enthusiasts and tech experts came together to create India&apos;s most trusted wedding vendor marketplace.</p>
            <p className="text-gray-600 dark:text-gray-300 mb-6">We&apos;ve helped over 5,000 couples plan their dream weddings by connecting them with verified, top-rated vendors across the country. Our platform makes it easy to compare options, read reviews, and book everything you need in one place.</p>
            <div className="flex flex-wrap gap-4">
              {['Verified Vendors', 'Transparent Pricing', '24/7 Support'].map((item, index) => (
                <div key={index} className="flex items-center">
                  <div className="w-12 h-12 bg-primary dark:bg-pink-600 bg-opacity-10 dark:bg-opacity-20 rounded-full flex items-center justify-center mr-3">
                    <FontAwesomeIcon icon={faCheck} className="text-white"/>
                  </div>
                  <span className="font-medium text-gray-800 dark:text-white">{item}</span>
                </div>
              ))}
            </div>
            <Link href="/team" className="btn-primary hover:shadow-lg text-white py-3 px-8 rounded-full mt-8 transition duration-300 inline-block">
              Meet Our Team <FontAwesomeIcon icon={faArrowRight} className="ml-2" />
            </Link>
          </div>
        </div>
      </div>
    </section>
  );
};

export default About;