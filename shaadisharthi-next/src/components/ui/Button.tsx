import React from 'react';

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'outline';
  fullWidth?: boolean;
  isLoading?: boolean;
  icon?: React.ReactNode;
}

const Button: React.FC<ButtonProps> = ({
  children,
  variant = 'primary',
  fullWidth = false,
  isLoading = false,
  icon,
  ...props
}) => {
  const baseClasses = "inline-flex items-center justify-center rounded-lg font-medium py-2.5 px-5 transition-colors focus:outline-none";
  
  const variants = {
    primary: "bg-primary hover:bg-primary-dark text-white shadow-md hover:shadow-lg",
    secondary: "bg-secondary hover:bg-secondary-dark text-white",
    outline: "bg-transparent border border-primary text-primary hover:bg-primary hover:text-white",
  };

  return (
    <button
      className={`${baseClasses} ${variants[variant]} ${fullWidth ? 'w-full' : ''} ${props.disabled ? 'opacity-50 cursor-not-allowed' : ''}`}
      disabled={isLoading || props.disabled}
      {...props}
    >
      {isLoading ? (
        <svg className="animate-spin -ml-1 mr-2 h-4 w-4 text-current" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
          <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
          <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
        </svg>
      ) : icon ? (
        <span className="mr-2">{icon}</span>
      ) : null}
      {children}
    </button>
  );
};

export default Button;