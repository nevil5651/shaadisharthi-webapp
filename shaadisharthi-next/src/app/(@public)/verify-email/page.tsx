'use client'
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import axios from 'axios';
import AuthCard from '@/components/cards/AuthCard';
import { FaEnvelope } from 'react-icons/fa';
import Link from 'next/link';

// Validation schema for email verification form
// Simple email validation for initial registration step
const verifyEmailSchema = z.object({
  email: z.string().min(1, { message: 'Email is required.' }).email({ message: 'Invalid email address.' }),
});

// Type inference for type-safe form handling
type VerifyEmailFormInputs = z.infer<typeof verifyEmailSchema>;

// Email verification component - initial step in registration flow
// Sends verification email to user before allowing account creation
export default function VerifyEmail() {
  // State management for loading, errors, and success state
  const [isLoading, setIsLoading] = useState(false);
  const [apiError, setApiError] = useState<string | null>(null);
  const [isSent, setIsSent] = useState(false); // Track if verification email was sent

  // React Hook Form setup with Zod validation
  const { register, handleSubmit, formState: { errors } } = useForm<VerifyEmailFormInputs>({
    resolver: zodResolver(verifyEmailSchema),
  });

  // Form submission handler
  // Initiates email verification process
  const onSubmit = async (data: VerifyEmailFormInputs) => {
    setIsLoading(true);
    setApiError(null);
    try {
      const apiUrl = process.env.NEXT_PUBLIC_API_URL;
      if (!apiUrl) {
        throw new Error("API URL is not configured.");
      }

      // API call to send verification email
      await axios.post(`${apiUrl}/Customer/cstmr-verify-email`, data);
      setIsSent(true); // Mark as sent to show success state
    } catch (err) {
      // Comprehensive error handling for different API response scenarios
      if (axios.isAxiosError(err) && err.response) {
        if (err.response.status === 409) {
          // Handle email already registered scenario
          setApiError('Email already in use. Please log in or use a different email.');
        } else if (err.response.status === 429) {
          // Handle rate limiting
          setApiError('Too many requests. Please try again later.');
        } else {
          // Generic API error
          setApiError(err.response.data.error || 'Failed to send verification email. Please try again.');
        }
      } else {
        // Network or unexpected errors
        setApiError('An unexpected error occurred.');
      }
    } finally {
      setIsLoading(false);
    }
  };

  // Success state - shown after verification email is sent
  if (isSent) {
    return (
      <div className="auth-page">
        <AuthCard title="Verification Sent" subtitle="Check your inbox">
          {/* Success message with instructions */}
          <p className="text-center text-gray-700 mb-4">
            A verification link has been sent to your email. Please click the link to complete registration.
          </p>
          {/* Login redirect for existing users */}
          <p className="text-center text-gray-700">
            Already have an account?{' '}
            <Link href="/login" className="text-pink-600 font-medium hover:underline">
              Sign In
            </Link>
          </p>
        </AuthCard>
      </div>
    );
  }

  // Main form state - email input for verification
  return (
    <div className="auth-page">
      <AuthCard title="Verify Your Email" subtitle="Enter your email to get started">
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          {/* API error display */}
          {apiError && (
            <div className="p-3 mb-4 text-sm text-red-700 bg-red-100 rounded-lg" role="alert">
              {apiError}
            </div>
          )}

          {/* Email Field */}
          <div>
            <div className="relative">
              <div className="input-icon"><FaEnvelope /></div>
              <input type="email" {...register('email')} placeholder="Email Address" className="form-input" disabled={isLoading} />
            </div>
            {/* Email validation errors */}
            {errors.email && <p className="text-red-500 text-sm mt-1">{errors.email.message}</p>}
          </div>

          {/* Submit Button with loading state */}
          <button type="submit" className="gradient-btn" disabled={isLoading}>
            {isLoading ? (
              <div className="flex items-center justify-center">
                <svg className="animate-spin -ml-1 mr-3 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                Sending...
              </div>
            ) : 'Send Verification Link'}
          </button>

          {/* Login redirect for existing users */}
          <div className="text-center text-gray-700">
            <p>
              Already have an account?{' '}
              <Link href="/login" className="text-pink-600 font-medium hover:underline">
                Sign In
              </Link>
            </p>
          </div>
        </form>
      </AuthCard>
    </div>
  );
}