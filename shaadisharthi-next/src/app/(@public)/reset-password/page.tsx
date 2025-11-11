'use client';
import { useState, useEffect, Suspense } from 'react';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import axios from 'axios';
import AuthCard from '@/components/cards/AuthCard';
import { FaLock, FaEye, FaEyeSlash } from 'react-icons/fa';
import { ToastContainer, toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';

// Loading component for Suspense fallback
function Loading() {
  return (
    <div className="auth-page">
      <AuthCard title="Loading..." subtitle="Please wait">
        <div className="flex items-center justify-center p-8">
          <svg className="animate-spin h-8 w-8 text-pink-500" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
          </svg>
        </div>
      </AuthCard>
    </div>
  );
}

// Comprehensive password validation schema
// Enforces strong password requirements including special characters
const resetPasswordSchema = z.object({
  password: z.string().min(8, { message: 'Password must be at least 8 characters.' })
    .regex(/[A-Z]/, { message: 'Password must contain at least one uppercase letter.' })
    .regex(/[a-z]/, { message: 'Password must contain at least one lowercase letter.' })
    .regex(/[0-9]/, { message: 'Password must contain at least one number.' })
    .regex(/[^A-Za-z0-9]/, { message: 'Password must contain at least one special character.' }),
  confirmPassword: z.string().min(8, { message: 'Please confirm your password.' }),
  token: z.string().min(1, { message: 'Verification token is required.' }).optional(),
}).refine(data => data.password === data.confirmPassword, {
  message: "Passwords don't match",
  path: ['confirmPassword'],
});

// Type inference for type-safe form handling
type ResetPasswordFormInputs = z.infer<typeof resetPasswordSchema>;

// Main reset password form component
// Handles password reset with token validation
function ResetPasswordForm() {
  // State management for password visibility, loading, and errors
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [apiError, setApiError] = useState<string | null>(null);
  const [isValidToken, setIsValidToken] = useState(false); // Track token validation status
  
  // Router and search params for token handling
  const router = useRouter();
  const searchParams = useSearchParams();
  const token = searchParams.get('token'); // Get reset token from URL parameters

  // React Hook Form setup with Zod validation
  const { register, handleSubmit, formState: { errors, isSubmitting }, setValue } = useForm<ResetPasswordFormInputs>({
    resolver: zodResolver(resetPasswordSchema),
  });

  // Effect to validate token on component mount
  useEffect(() => {
    if (!token) {
      // Redirect to forgot password if no token present
      router.push('/forgot-password');
      return;
    }

    // Set hidden token field for form submission
    setValue('token', token);
    setIsValidToken(true); // Assume token is valid; backend will validate on submit

    // Optional: Verify token here if needed, but since submit validates it, skip for simplicity
  }, [token, router, setValue]);

  // Form submission handler
  // Processes password reset with new password
  const onSubmit = async (data: ResetPasswordFormInputs) => {
    setIsLoading(true);
    setApiError(null);
    try {
      const apiUrl = process.env.NEXT_PUBLIC_API_URL;
      if (!apiUrl) {
        throw new Error("API URL is not configured.");
      }

      // Prepare payload - remove confirmPassword as it's not needed by API
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      const { confirmPassword, ...payload } = data;
      
      // API call to reset password
      await axios.post(`${apiUrl}/Customer/cstmr-reset-password`, payload);
      
      // Success notification
      toast.success('Password reset successful! Redirecting to login...', {
        position: "top-right",
        autoClose: 3000,
        hideProgressBar: false,
        closeOnClick: true,
        pauseOnHover: true,
        draggable: true,
      });
      
      // Redirect to login after successful password reset
      setTimeout(() => router.push('/login'), 3000);
    } catch (err) {
      // Error handling for API failures
      if (axios.isAxiosError(err) && err.response) {
        setApiError(err.response.data.error || 'Failed to reset password. Please try again.');
      } else {
        setApiError('An unexpected error occurred.');
      }
    } finally {
      setIsLoading(false);
    }
  };

  // Show verification state during token validation
  if (!isValidToken && token) {
    return (
      <div className="auth-page">
        <AuthCard title="Verifying..." subtitle="Please wait while we verify your token">
          {apiError && <p className="text-red-500 text-center">{apiError}</p>}
        </AuthCard>
      </div>
    );
  }

  return (
    <div className="auth-page">
      {/* Toast container for notifications */}
      <ToastContainer />
      <AuthCard title="Reset Password" subtitle="Enter your new password">
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          {/* API error display */}
          {apiError && (
            <div className="p-3 mb-4 text-sm text-red-700 bg-red-100 rounded-lg" role="alert">
              {apiError}
            </div>
          )}

          {/* Hidden Token Field - populated from URL parameters */}
          <input type="hidden" {...register('token')} />

          {/* New Password Field with visibility toggle */}
          <div>
            <div className="relative">
              <div className="input-icon"><FaLock /></div>
              <input type={showPassword ? "text" : "password"} {...register('password')} placeholder="New Password" className="form-input" disabled={isLoading} />
              {/* Password visibility toggle button */}
              <button type="button" className="password-toggle" onClick={() => setShowPassword(!showPassword)}>
                {showPassword ? <FaEyeSlash /> : <FaEye />}
              </button>
            </div>
            {/* Password validation error messages */}
            {errors.password && <p className="text-red-500 text-sm mt-1">{errors.password.message}</p>}
          </div>

          {/* Confirm New Password Field with visibility toggle */}
          <div>
            <div className="relative">
              <div className="input-icon"><FaLock /></div>
              <input type={showConfirmPassword ? "text" : "password"} {...register('confirmPassword')} placeholder="Confirm New Password" className="form-input" disabled={isLoading} />
              {/* Password visibility toggle button */}
              <button type="button" className="password-toggle" onClick={() => setShowConfirmPassword(!showConfirmPassword)}>
                {showConfirmPassword ? <FaEyeSlash /> : <FaEye />}
              </button>
            </div>
            {/* Password confirmation error message */}
            {errors.confirmPassword && <p className="text-red-500 text-sm mt-1">{errors.confirmPassword.message}</p>}
          </div>

          {/* Submit Button with loading state */}
          <button type="submit" className="gradient-btn" disabled={isLoading || isSubmitting}>
            {isLoading ? (
              <div className="flex items-center justify-center">
                <svg className="animate-spin -ml-1 mr-3 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                Resetting...
              </div>
            ) : 'Reset Password'}
          </button>

          {/* Login redirect */}
          <div className="text-center text-gray-700">
            <p>
              Remember your password?{' '}
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

// Main page component with Suspense boundary
// Handles loading state during token validation
export default function ResetPasswordPage() {
  return (
    <Suspense fallback={<Loading />}>
      <ResetPasswordForm />
    </Suspense>
  );
}