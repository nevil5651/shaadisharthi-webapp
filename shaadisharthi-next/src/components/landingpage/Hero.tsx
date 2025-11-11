
'use client';

import { useRouter } from 'next/navigation';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faSearch, faUserTie } from '@fortawesome/free-solid-svg-icons';

const Hero: React.FC = () => {
  const router = useRouter();

  return (
    <section id="home" className="pt-20">
      <div className="hero-section flex items-center justify-center">
        <div className="text-center text-white px-4">
          <h1 className="text-4xl md:text-6xl font-bold mb-6 font-playfair-display">Your Dream Wedding Starts Here</h1>
          <p className="text-xl md:text-2xl mb-8 max-w-2xl mx-auto">Discover India&apos;s finest wedding vendors and plan your perfect day with ease</p>
          <div className="flex flex-col md:flex-row space-y-4 md:space-y-0 md:space-x-4 justify-center">
            <button
              onClick={() => router.push('/login')}
              className="btn-primary hover:shadow-lg text-white py-3 px-8 rounded-full text-lg transition duration-300"
            >
              Find Services <FontAwesomeIcon icon={faSearch} className="ml-2" />
            </button>
            <a
  href="https://shaadisharthi.theworkpc.com/provider"
  target="_blank"
  rel="noopener noreferrer"
  className="bg-transparent hover:bg-white text-white hover:text-black py-3 px-8 border-2 border-white rounded-full text-lg transition duration-300"
>
  Become a Vendor <FontAwesomeIcon icon={faUserTie} className="ml-2 hover:text-black" />
</a>
          </div>
        </div>
      </div>
    </section>
  );
};

export default Hero;
