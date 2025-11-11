import React from 'react';

interface InputFieldProps {
  icon?: React.ReactNode;
  type?: string;
  placeholder?: string;
  name: string;
  required?: boolean;
  label?: string; // Added label prop
  value: string; // Added value prop
  onChange: (e: React.ChangeEvent<HTMLInputElement>) => void; // Added onChange prop
}

export default function InputField({ icon, type = 'text', placeholder, name, required = false, label, value, onChange }: InputFieldProps) {
  return (
    <div className="mb-5">
      {label && (
        <label htmlFor={name} className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
          {label}
        </label>
      )}
      <div className="relative">
        {icon && (
          <div className="input-icon">
            {icon}
          </div>
        )}
        <input
          type={type}
          name={name}
          placeholder={placeholder}
          required={required}
          value={value}
          onChange={onChange}
          className="form-input w-full pl-10 pr-3 py-2 rounded-lg border border-gray-300 focus:outline-none focus:ring-2 focus:ring-pink-500 focus:border-transparent dark:bg-gray-700 dark:border-gray-600 dark:text-white dark:placeholder-gray-400"
        />
      </div>
    </div>
  );
}