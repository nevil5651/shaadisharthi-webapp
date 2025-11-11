import React, { useState } from 'react';
import { FaEye, FaEyeSlash, FaLock } from 'react-icons/fa';

interface PasswordFieldProps {
  placeholder?: string;
  name: string;
  value: string;
  onChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
  required?: boolean;
}

const PasswordField: React.FC<PasswordFieldProps> = ({
  placeholder,
  name,
  value,
  onChange,
  required = false,
}) => {
  const [showPassword, setShowPassword] = useState(false);

  return (
    <div className="mb-5">
      <label htmlFor={name} className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
        {placeholder}
      </label>
      <div className="relative">
        <div className="input-icon">
          <FaLock />
        </div>
        <input
          type={showPassword ? "text" : "password"}
          id={name}
          name={name}
          placeholder={placeholder}
          required={required}
          className="form-input w-full pl-10 pr-10 py-2 rounded-lg border border-gray-300 focus:outline-none focus:ring-2 focus:ring-pink-500 focus:border-transparent dark:bg-gray-700 dark:border-gray-600 dark:text-white dark:placeholder-gray-400"
          value={value}
          onChange={onChange}
        />
        <button 
          type="button" 
          className="password-toggle absolute inset-y-0 right-0 pr-3 flex items-center text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
          onClick={() => setShowPassword(!showPassword)}
        >
          {showPassword ? <FaEyeSlash /> : <FaEye />}
        </button>
      </div>
    </div>
  );
};

export default PasswordField;