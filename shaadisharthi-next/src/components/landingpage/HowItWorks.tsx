// components/HowItWorks.tsx (CLIENT COMPONENT)
'use client';

import { useRouter } from 'next/navigation';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faArrowRight } from '@fortawesome/free-solid-svg-icons';
import SectionHeader from './SectionHeader';
//import { Step } from '@/lib';

interface Step {
  number: number;
  title: string;
  description: string;
}

const HowItWorks = () => {
  const router = useRouter();
  const steps: Step[] = [
    { number: 1, title: 'Create Your Profile', description: 'Tell us about your wedding vision and preferences to get personalized recommendations.' },
    { number: 2, title: 'Explore Vendors', description: 'Browse our curated selection of top wedding vendors in your area and budget.' },
    { number: 3, title: 'Book & Celebrate', description: 'Connect with vendors, finalize details, and enjoy your perfect wedding day.' },
  ];

  return (
    <section className="py-16 bg-white dark:bg-gray-900">
      <div className="container mx-auto px-4">
        <SectionHeader subtitle="OUR PROCESS" title="Planning Made Simple" />
        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          {steps.map((step) => (
            <div key={step.number} className="text-center dark:bg-gray-800 dark:text-white p-6 bg-gray-50 dark:bg-gray-800 rounded-xl">
              <div className="w-20 h-20 bg-primary dark:bg-pink-600 bg-opacity-10 dark:bg-opacity-20 rounded-full flex items-center justify-center mx-auto mb-4">
                <span className="text-3xl font-bold text-white ">{step.number}</span>
              </div>
              <h3 className="text-xl font-bold mb-2 font-playfair-display text-gray-800 dark:text-white">{step.title}</h3>
              <p className="text-gray-600 dark:text-gray-400">{step.description}</p>
            </div>
          ))}
        </div>
        <div className="mt-16 rounded-xl p-8 text-white" style={{ background: 'linear-gradient(to right, var(--primary), var(--secondary))' }}>
          <div className="flex flex-col md:flex-row items-center">
            <div className="md:w-1/2 mb-6 md:mb-0">
              <h3 className="text-2xl font-bold mb-4 font-playfair-display">Ready to Start Planning?</h3>
              <p>Join thousands of couples who found their perfect vendors through ShaadiSharthi.</p>
            </div>
            <div className="md:w-1/2 flex justify-center md:justify-end">
              <button
                onClick={() => router.push('/verify-email')}
                className="bg-white text-primary hover:bg-gray-100 py-3 px-8 rounded-full text-lg font-medium transition duration-300"
              >
                Get Started <FontAwesomeIcon icon={faArrowRight} className="ml-2 dark:text-primary" />
              </button>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};

export default HowItWorks;