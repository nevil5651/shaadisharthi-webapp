'use client';

import { useRouter } from 'next/navigation';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faRing, faTimes, faUser, faUserTie } from '@fortawesome/free-solid-svg-icons';

interface UserTypeModalProps {
  action: 'login' | 'register' | null;
  onClose: () => void;
}

const UserTypeModal: React.FC<UserTypeModalProps> = ({ action, onClose }) => {
  const router = useRouter();

  if (!action) return null;

  const handleSelect = (type: 'customer' | 'provider') => {
    router.push(`/${type}/${action}`); // Client-side navigation
    onClose();
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 z-50 flex items-center justify-center p-4">
      <div className="bg-white dark:bg-gray-800 rounded-xl w-full max-w-md mx-4 relative overflow-hidden">
        <button onClick={onClose} className="absolute top-4 right-4 text-gray-500 dark:text-gray-400 hover:text-primary dark:hover:text-pink-400 transition duration-300">
          <FontAwesomeIcon icon={faTimes} className="text-xl" />
        </button>
        <div className="bg-gradient-to-r from-primary to-secondary p-6 text-black text-center">
          <FontAwesomeIcon icon={faRing} className="text-4xl mb-4" />
          <h3 className="text-2xl font-bold font-playfair-display">Welcome to ShaadiSharthi</h3>
          <p className="text-opacity-80">Choose your account type to continue</p>
        </div>
        <div className="p-8 text-center">
          <div className="flex flex-col space-y-4">
            <button onClick={() => handleSelect('customer')} className="btn-primary hover:shadow-lg text-white py-3 px-8 rounded-full text-lg transition duration-300">
              <FontAwesomeIcon icon={faUser} className="mr-2" /> I&apos;m a Customer
            </button>
            <button onClick={() => handleSelect('provider')} className="bg-transparent hover:bg-gray-100 dark:hover:bg-gray-700 text-primary dark:text-pink-400 py-3 px-8 border-2 border-primary dark:border-pink-400 rounded-full text-lg transition duration-300">
              <FontAwesomeIcon icon={faUserTie} className="mr-2" /> I&apos;m a Service Provider
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default UserTypeModal;