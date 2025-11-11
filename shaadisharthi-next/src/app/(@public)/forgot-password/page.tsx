'use client'
import { useState, useCallback } from 'react';
import { useForm, UseFormRegister } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import axios from 'axios';
import AuthCard from '@/components/cards/AuthCard';
import { FaEnvelope } from 'react-icons/fa';
import Link from 'next/link';

// Validation schema for forgot password form using Zod
// Ensures email is required and valid
const forgotPasswordSchema = z.object({
  email: z.string().min(1, { message: 'Email is required.' }).email({ message: 'Invalid email address.' }),
});

// Type inference from Zod schema for type-safe form handling
type ForgotPasswordFormInputs = z.infer<typeof forgotPasswordSchema>;

// Props interface for the reusable email input component
interface EmailInputProps {
  register: UseFormRegister<ForgotPasswordFormInputs>;
  error: { message?: string } | undefined;
  disabled: boolean;
}

// Memoized input component for email field
// Uses react-hook-form's register for form state management
// Includes error display and disabled state for loading
const EmailInput = ({ register, error, disabled }: EmailInputProps) => (
  <div>
    <div className="relative">
      <div className="input-icon"><FaEnvelope /></div>
      <input 
        type="email" 
        {...register('email')} 
        placeholder="Email Address" 
        className="form-input w-full pl-10 pr-3 py-2 rounded-lg border border-gray-300 focus:outline-none focus:ring-2 focus:ring-pink-500 focus:border-transparent dark:bg-gray-700 dark:border-gray-600 dark:text-white dark:placeholder-gray-400" 
        disabled={disabled} 
      />
    </div>
    {error && <p className="text-red-500 dark:text-red-400 text-sm mt-1">{error.message}</p>}
  </div>
);

// Loading spinner component for async operations
// Reusable component that can display custom text
const LoadingSpinner = ({ text = "Sending..." }: { text?: string }) => (
  <div className="flex items-center justify-center">
    <svg className="animate-spin -ml-1 mr-3 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
    </svg>
    {text}
  </div>
);

// Main forgot password component
// Handles password reset request flow
export default function ForgotPassword() {
  // State management for loading, API messages, and errors
  const [isLoading, setIsLoading] = useState(false);
  const [apiMessage, setApiMessage] = useState<string | null>(null);
  const [apiError, setApiError] = useState<string | null>(null);

  // React Hook Form setup with Zod validation
  // Uses onChange validation mode for better user experience
  const { register, handleSubmit, formState: { errors } } = useForm<ForgotPasswordFormInputs>({
    resolver: zodResolver(forgotPasswordSchema),
    mode: 'onChange' // Validate on change for better UX
  });

  // Form submission handler
  // Makes API call to initiate password reset process
  // Implements security best practice by not revealing if email exists
  const onSubmit = useCallback(async (data: ForgotPasswordFormInputs) => {
    setIsLoading(true);
    setApiError(null);
    setApiMessage(null);
    
    try {
      const apiUrl = process.env.NEXT_PUBLIC_API_URL;
      if (!apiUrl) {
        throw new Error("API URL is not configured.");
      }

      // API call to backend password reset endpoint
      const response = await axios.post(`${apiUrl}/Customer/cstmr-forgot-password`, data);
      
      // Security: Always show success message even if email doesn't exist
      // Prevents email enumeration attacks
      setApiMessage(response.data.message || 'If the email exists, a reset link has been sent.');
    } catch (err) {
      // Error handling with Axios-specific error checking
      if (axios.isAxiosError(err) && err.response) {
        // Security: Show same message for both success and error cases
        setApiMessage(err.response.data.message || 'If the email exists, a reset link has been sent.');
      } else {
        setApiError('An unexpected error occurred.');
      }
    } finally {
      setIsLoading(false);
    }
  }, []);

  return (
    <div className="auth-page min-h-screen flex items-center justify-center bg-gradient-to-br from-pink-50 to-purple-50 dark:from-gray-900 dark:to-gray-800 py-8 px-4">
      <AuthCard title="Forgot Password" subtitle="Enter your email to reset password">
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          {/* API error display */}
          {apiError && (
            <div className="p-3 mb-4 text-sm text-red-700 bg-red-100 rounded-lg dark:bg-red-900 dark:bg-opacity-20 dark:text-red-300" role="alert">
              {apiError}
            </div>
          )}
          
          {/* API success message display */}
          {apiMessage && (
            <div className="p-3 mb-4 text-sm text-green-700 bg-green-100 rounded-lg dark:bg-green-900 dark:bg-opacity-20 dark:text-green-300" role="alert">
              {apiMessage}
            </div>
          )}

          {/* Email input field with validation */}
          <EmailInput 
            register={register} 
            error={errors.email} 
            disabled={isLoading} 
          />

          {/* Submit button with loading state */}
          <button type="submit" className="gradient-btn w-full bg-gradient-to-r from-pink-500 to-purple-600 hover:from-pink-600 hover:to-purple-700 text-white font-bold py-2 px-4 rounded-lg focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-pink-500 transition duration-150 ease-in-out disabled:opacity-50 dark:focus:ring-offset-gray-800" disabled={isLoading}>
            {isLoading ? <LoadingSpinner /> : 'Send Reset Link'}
          </button>

          {/* Navigation link back to login */}
          <div className="text-center text-gray-700 dark:text-gray-300">
            <p>
              Remember your password?{' '}
              <Link href="/login" className="text-pink-600 dark:text-pink-400 font-medium hover:underline">
                Sign In
              </Link>
            </p>
          </div>
        </form>
      </AuthCard>
    </div>
  );
}