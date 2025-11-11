import React from 'react';
import { FaGoogle, FaFacebookF, FaApple } from 'react-icons/fa';

const SocialLogin = () => {
  return (
    <>
      <div className="flex items-center my-6">
        <div className="border-t border-gray-300 flex-grow"></div>
        <span className="mx-4 text-gray-500">or sign up with</span>
        <div className="border-t border-gray-300 flex-grow"></div>
      </div>

      <div className="flex justify-center space-x-4 mb-6">
        <button className="w-10 h-10 rounded-full bg-gray-100 flex items-center justify-center text-red-600 hover:bg-red-50 transition-colors">
          <FaGoogle />
        </button>
        <button className="w-10 h-10 rounded-full bg-gray-100 flex items-center justify-center text-blue-600 hover:bg-blue-50 transition-colors">
          <FaFacebookF />
        </button>
        <button className="w-10 h-10 rounded-full bg-gray-100 flex items-center justify-center text-black hover:bg-gray-200 transition-colors">
          <FaApple />
        </button>
      </div>
    </>
  );
};

export default SocialLogin;