'use client';
import { useState, useEffect, useCallback, useMemo, Suspense } from 'react';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import axios from 'axios';
import { toast } from 'react-toastify';
import AuthCard from '@/components/cards/AuthCard';
import { FaUser, FaEnvelope, FaPhone, FaPhoneAlt, FaLock, FaEye, FaEyeSlash } from 'react-icons/fa';
import dynamic from 'next/dynamic';

// Loading component for Suspense fallback
// Shows loading state during token verification
function Loading() {
  return (
    <div className="auth-page min-h-screen flex items-center justify-center bg-gradient-to-br from-pink-50 to-purple-50 dark:from-gray-900 dark:to-gray-800 py-8 px-4">
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

// Dynamically import SocialLogin to reduce initial bundle size
// Code splitting for better performance
const SocialLogin = dynamic(() => import('@/components/ui/SocialLogin'), {
  ssr: false,
  loading: () => <div className="h-16 flex items-center justify-center text-gray-600 dark:text-gray-400">Loading social login options...</div>
});

// Comprehensive validation schema for registration form
// Includes password confirmation matching and terms acceptance
// Token is optional in Zod but required by backend for verification
const registerSchema = z.object({
  name: z.string().min(1, { message: 'Full name is required.' }),
  email: z.string().min(1, { message: 'Email is required.' }).email({ message: 'Invalid email address.' }),
  phone: z.string().length(10, { message: 'Phone number must be 10 digits.' }).regex(/^\d+$/, { message: 'Invalid phone number.' }),
  altPhone: z.string().length(10, { message: 'Alternate phone must be 10 digits.' }).regex(/^\d+$/, { message: 'Invalid phone number.' }).optional().or(z.literal('')),
  password: z.string().min(6, { message: 'Password must be at least 6 characters.' }),
  confirmPassword: z.string().min(6, { message: 'Please confirm your password.' }),
  termsAccepted: z.boolean().refine(val => val === true, { message: 'You must accept the Terms & Conditions.' }),
  token: z.string().min(1, { message: 'Verification token is required.' }).optional(), // Make required in Zod if strictly enforcing
}).refine(data => data.password === data.confirmPassword, {
  message: "Passwords don't match",
  path: ['confirmPassword'],
});

// Type inference for type-safe form handling
export type RegisterFormInputs = z.infer<typeof registerSchema>;

// Props interface for reusable form input component
interface FormInputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  icon?: React.ElementType;
  type: string;
  placeholder?: string;
  disabled?: boolean;
  error?: string;
  showPasswordToggle?: boolean;
  isPasswordVisible?: boolean;
  onTogglePassword?: () => void;
}

// Memoized form inputs to prevent unnecessary re-renders
// Optimized component for better performance in forms
const FormInput: React.FC<FormInputProps> = ({
  icon,
  type,
  placeholder,
  disabled,
  error,
  showPasswordToggle,
  isPasswordVisible,
  onTogglePassword,
  ...registerProps
}) => {
  // Memoize icon component to prevent unnecessary re-renders
  const InputIcon = useMemo(() => icon, [icon]);
  
  return (
    <div>
      <div className="relative">
        <div className="input-icon">{InputIcon && <InputIcon />}</div>
        <input 
          type={type} 
          placeholder={placeholder} 
          className={`form-input w-full pl-10 pr-3 py-2 rounded-lg border border-gray-300 focus:outline-none focus:ring-2 focus:ring-pink-500 focus:border-transparent dark:bg-gray-700 dark:border-gray-600 dark:text-white dark:placeholder-gray-400 ${disabled ? 'bg-gray-100 dark:bg-gray-600 cursor-not-allowed' : ''}`} 
          disabled={disabled} 
          {...registerProps} 
        />
        {/* Password visibility toggle for password fields */}
        {showPasswordToggle && (
          <button 
            type="button" 
            className="password-toggle absolute inset-y-0 right-0 pr-3 flex items-center text-gray-400 hover:text-gray-600 dark:hover:text-gray-300" 
            onClick={onTogglePassword}
            disabled={disabled}
          >
            {isPasswordVisible ? <FaEyeSlash /> : <FaEye />}
          </button>
        )}
      </div>
      {error && <p className="text-red-500 dark:text-red-400 text-sm mt-1">{error}</p>}
    </div>
  );
};

// Main registration form component with token verification flow
function RegisterForm() {
  // State management for password visibility, loading, and errors
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [apiError, setApiError] = useState<string | null>(null);
  
  // Router and search params for token handling
  const router = useRouter();
  const searchParams = useSearchParams();
  const token = searchParams.get('token'); // Get verification token from URL
  const [isVerified, setIsVerified] = useState(false); // Track email verification status

  // React Hook Form setup with Zod validation
  // Uses onChange validation mode for real-time feedback
  const { register, handleSubmit, formState: { errors, isSubmitting }, setValue } = useForm<RegisterFormInputs>({
    resolver: zodResolver(registerSchema),
    mode: 'onChange' // Validate on change for better UX
  });

  // Token verification function
  // Verifies the email verification token from URL parameters
  // Memoized to prevent unnecessary re-renders
  const verifyToken = useCallback(async () => {
    if (!token) {
      // Redirect to email verification if no token present
      router.push('/verify-email');
      return;
    }

    setIsLoading(true);
    setApiError(null);
    try {
      const apiUrl = process.env.NEXT_PUBLIC_API_URL;
      // API call to verify token and get associated email
      const response = await axios.get(`${apiUrl}/Customer/cstmr-email-verification?token=${token}`);
      if (response.data.email) {
        // Pre-fill email field and set token if verification successful
        setValue('email', response.data.email);
        setValue('token', token);
        setIsVerified(true);
      } else {
        throw new Error('Invalid response');
      }
    } catch {
      // Handle invalid or expired tokens
      setApiError('Invalid or expired verification token. Redirecting...');
      setTimeout(() => router.push('/verify-email'), 3000);
    } finally {
      setIsLoading(false);
    }
  }, [token, router, setValue]);

  // Effect to verify token on component mount
  useEffect(() => {
    verifyToken();
  }, [verifyToken]);

  // Form submission handler
  // Creates user account after successful email verification
  // Memoized to prevent unnecessary re-renders
  const onSubmit = useCallback(async (data: RegisterFormInputs) => {
    setIsLoading(true);
    setApiError(null);
    const toastId = toast.loading("Creating your account...");

    try {
      const apiUrl = process.env.NEXT_PUBLIC_API_URL;
      if (!apiUrl) {
        throw new Error("API URL is not configured.");
      }
      
      // Prepare payload - remove confirmPassword and termsAccepted as they're not needed by API
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      const { confirmPassword, termsAccepted, ...payload } = data;

      // API call to register user
      await axios.post(`${apiUrl}/Customer/cstmr-rgt`, payload);

      // Success notification
      toast.update(toastId, {
        render: 'Registration successful! Redirecting to login...',
        type: 'success',
        isLoading: false,
        autoClose: 2000,
      });

      // Redirect to login after successful registration
      setTimeout(() => {
        router.push('/login');
      }, 2000);

    } catch (err) {
      console.error('Registration failed:', err);
      let errorMessage = 'An unexpected error occurred.';
      
      // Handle different error types
      if (axios.isAxiosError(err) && err.response) {
        errorMessage = err.response.data.error || 'Registration failed. Please try again.';
      } else if (err instanceof Error) {
        errorMessage = err.message;
      }
      
      setApiError(errorMessage);
      toast.update(toastId, { render: errorMessage, type: 'error', isLoading: false, autoClose: 5000 });
      // Only re-enable the form on failure
      setIsLoading(false);
    }
  }, [router]);

  // Password visibility toggle handlers
  // Memoized to prevent unnecessary re-renders
  const togglePassword = useCallback(() => {
    setShowPassword(prev => !prev);
  }, []);

  const toggleConfirmPassword = useCallback(() => {
    setShowConfirmPassword(prev => !prev);
  }, []);

  // Show loading state during token verification
  if (!isVerified && token) {
    return (
      <div className="auth-page min-h-screen flex items-center justify-center bg-gradient-to-br from-pink-50 to-purple-50 dark:from-gray-900 dark:to-gray-800 py-8 px-4">
        <AuthCard title="Verifying..." subtitle="Please wait while we verify your email">
          {apiError && <p className="text-red-500 dark:text-red-400 text-center">{apiError}</p>}
        </AuthCard>
      </div>
    );
  }

  return (
    <div className="auth-page min-h-screen flex items-center justify-center bg-gradient-to-br from-pink-50 to-purple-50 dark:from-gray-900 dark:to-gray-800 py-8 px-4">
      <AuthCard title="Create Your Account" subtitle="Join us to begin your journey">
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          {/* API error display */}
          {apiError && (
            <div className="p-3 mb-4 text-sm text-red-700 bg-red-100 rounded-lg dark:bg-red-900 dark:bg-opacity-20 dark:text-red-300" role="alert">
              {apiError}
            </div>
          )}

          {/* Hidden Token Field - populated during token verification */}
          <input type="hidden" {...register('token')} />

          {/* Name Field */}
          <FormInput
            icon={FaUser}
            type="text"
            placeholder="Full Name"
            disabled={isLoading}
            error={errors.name?.message}
            {...register('name')}
          />

          {/* Email Field (prefilled and disabled if verified) */}
          {/* Prevents email modification after verification */}
          <FormInput
            icon={FaEnvelope}
            type="email"
            placeholder="Email Address"
            disabled={isVerified || isLoading}
            error={errors.email?.message}
            {...register('email')}
          />

          {/* Phone Field */}
          <FormInput
            icon={FaPhone}
            type="tel"
            placeholder="Phone Number"
            disabled={isLoading}
            error={errors.phone?.message}
            {...register('phone')}
          />

          {/* Alternate Phone Field (Optional) */}
          <FormInput
            icon={FaPhoneAlt}
            type="tel"
            placeholder="Alternate Phone Number (Optional)"
            disabled={isLoading}
            error={errors.altPhone?.message}
            {...register('altPhone')}
          />

          {/* Password Field with visibility toggle */}
          <FormInput
            icon={FaLock}
            type={showPassword ? "text" : "password"}
            placeholder="Password"
            disabled={isLoading}
            error={errors.password?.message}
            showPasswordToggle={true}
            isPasswordVisible={showPassword}
            onTogglePassword={togglePassword}
            {...register('password')}
          />

          {/* Confirm Password Field with visibility toggle */}
          <FormInput
            icon={FaLock}
            type={showConfirmPassword ? "text" : "password"}
            placeholder="Confirm Password"
            disabled={isLoading}
            error={errors.confirmPassword?.message}
            showPasswordToggle={true}
            isPasswordVisible={showConfirmPassword}
            onTogglePassword={toggleConfirmPassword}
            {...register('confirmPassword')}
          />

          {/* Terms and Conditions acceptance */}
          <div className="flex items-start mb-6">
            <div className="flex items-center h-5">
              <input
                id="terms"
                type="checkbox"
                {...register('termsAccepted')}
                className="w-4 h-4 text-pink-600 border-gray-300 rounded focus:ring-pink-500 dark:bg-gray-700 dark:border-gray-600"
                disabled={isLoading}
              />
            </div>
            <div className="ml-3 text-sm">
              <label htmlFor="terms" className="text-gray-700 dark:text-gray-300">
                I agree to the{' '}
                <Link href="/terms" className="text-pink-600 dark:text-pink-400 hover:underline">
                  Terms & Conditions
                </Link>{' '}
                and{' '}
                <Link href="/privacy" className="text-pink-600 dark:text-pink-400 hover:underline">
                  Privacy Policy
                </Link>
              </label>
            </div>
          </div>
          {errors.termsAccepted && <p className="text-red-500 dark:text-red-400 text-sm -mt-4 mb-4">{errors.termsAccepted.message}</p>}

          {/* Submit Button with loading state */}
          <button type="submit" className="gradient-btn w-full bg-gradient-to-r from-pink-500 to-purple-600 hover:from-pink-600 hover:to-purple-700 text-white font-bold py-2 px-4 rounded-lg focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-pink-500 transition duration-150 ease-in-out disabled:opacity-50 dark:focus:ring-offset-gray-800" disabled={isLoading || isSubmitting}>
            {isLoading ? (
              <div className="flex items-center justify-center">
                <svg className="animate-spin -ml-1 mr-3 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                Registering...
              </div>
            ) : 'Register Now'}
          </button>

          {/* Social login options */}
          <SocialLogin />
          
          {/* Login redirect */}
          <div className="text-center text-gray-700 dark:text-gray-300">
            <p>
              Already have an account?{' '}
              <Link href="/login" className="text-pink-600 dark:text-pink-400 font-medium hover:underline">
                Sign In
              </Link>
            </p>
          </div>
        </form>
      </AuthCard>
    </div>
  );
};

// Main page component with Suspense boundary
// Wraps the form component for proper loading states
export default function RegisterPage() {
  return (
    <Suspense fallback={<Loading />}>
      <RegisterForm />
    </Suspense>
  );
}