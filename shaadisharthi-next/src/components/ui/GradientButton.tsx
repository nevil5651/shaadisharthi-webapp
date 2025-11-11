import React from 'react';

interface GradientButtonProps {
  children: React.ReactNode;
  onClick?: () => void;
  type?: 'button' | 'submit' | 'reset';
}

const GradientButton: React.FC<GradientButtonProps> = ({ children, onClick, type = 'button' }) => {
  return (
    <button 
      type={type}
      className="gradient-btn"
      onClick={onClick}
    >
      {children}
    </button>
  );
};

export default GradientButton;