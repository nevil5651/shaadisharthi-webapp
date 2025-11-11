'use client'
import { useState, lazy, Suspense } from 'react';
import Link from 'next/link';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useAuth } from '@/context/useAuth';

// Lazy load icons and components for code splitting and performance optimization
// Reduces initial bundle size by loading icons only when needed
const FaUser = lazy(() => import('react-icons/fa').then(mod => ({ default: mod.FaUser })));
const FaLock = lazy(() => import('react-icons/fa').then(mod => ({ default: mod.FaLock })));
const FaEye = lazy(() => import('react-icons/fa').then(mod => ({ default: mod.FaEye })));
const FaEyeSlash = lazy(() => import('react-icons/fa').then(mod => ({ default: mod.FaEyeSlash })));
const FaFacebookF = lazy(() => import('react-icons/fa').then(mod => ({ default: mod.FaFacebookF })));
const FaGoogle = lazy(() => import('react-icons/fa').then(mod => ({ default: mod.FaGoogle })));
const FaApple = lazy(() => import('react-icons/fa').then(mod => ({ default: mod.FaApple })));
const AuthCard = lazy(() => import('@/components/cards/AuthCard'));

// Zod schema for login form validation
// Validates email format and password minimum length
const loginSchema = z.object({
  email: z.string().min(1, { message: 'Email is required.' }).email({ message: 'Invalid email address.' }),
  password: z.string().min(6, { message: 'Password must be at least 6 characters.' }),
  rememberMe: z.boolean().optional(),
});

// Type inference for type-safe form handling
type LoginFormInputs = z.infer<typeof loginSchema>;

// Simple fallback components for lazy loading
// Provides smooth loading experience without layout shift
const IconFallback = () => <span className="inline-block w-4 h-4 bg-gray-200 dark:bg-gray-600 animate-pulse rounded"></span>;
const AuthCardFallback = () => (
  <div className="bg-white dark:bg-gray-800 rounded-xl shadow-xl p-4 sm:p-6 md:p-8 mx-auto max-w-md w-full">
    <div className="text-center mb-6">
      <div className="flex items-center justify-center mb-4">
        <div className="w-10 h-10 bg-gray-200 dark:bg-gray-600 animate-pulse rounded-full mr-2"></div>
        <div className="h-8 bg-gray-200 dark:bg-gray-600 animate-pulse rounded w-40"></div>
      </div>
      <div className="h-7 bg-gray-200 dark:bg-gray-600 animate-pulse rounded w-48 mx-auto mb-2"></div>
      <div className="h-4 bg-gray-200 dark:bg-gray-600 animate-pulse rounded w-56 mx-auto"></div>
    </div>
    <div className="space-y-4">
      <div className="h-12 bg-gray-200 dark:bg-gray-600 animate-pulse rounded"></div>
      <div className="h-12 bg-gray-200 dark:bg-gray-600 animate-pulse rounded"></div>
      <div className="h-12 bg-gray-200 dark:bg-gray-600 animate-pulse rounded"></div>
      <div className="h-4 bg-gray-200 dark:bg-gray-600 animate-pulse rounded w-32"></div>
    </div>
  </div>
);

// Main login component with authentication flow
export default function Login() {
  // State management for password visibility, loading, and errors
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [apiError, setApiError] = useState<string | null>(null);
  
  // Authentication context hook for login functionality
  const { login } = useAuth();

  // React Hook Form setup with Zod validation
  // Uses onSubmit validation mode for form validation
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<LoginFormInputs>({
    resolver: zodResolver(loginSchema),
    mode: 'onSubmit', // Only validate on submit, not on change
  });

  // Form submission handler
  // Integrates with auth context for login functionality
  const onSubmit = async (data: LoginFormInputs) => {
    setApiError(null);
    setIsLoading(true);

    // Call authentication context login method
    const result = await login({ email: data.email, password: data.password });

    // Handle login result
    if (!result.success) {
      setApiError(result.error || 'An unknown error occurred.');
      setIsLoading(false);
    }
    // On success, the auth context should handle redirect
  };

  return (
    <div className="auth-page min-h-screen flex items-center justify-center bg-gradient-to-br from-pink-50 to-purple-50 dark:from-gray-900 dark:to-gray-800 py-4 sm:py-8 px-2 sm:px-4">
      {/* Suspense boundary for lazy loaded AuthCard */}
      <Suspense fallback={<AuthCardFallback />}>
        <AuthCard
          title="Welcome Back"
          subtitle="Sign in to continue your journey"
        >
          <form className="space-y-4" onSubmit={handleSubmit(onSubmit)}>
            {/* API error display */}
            {apiError && (
              <div className="p-3 mb-4 text-sm text-red-700 bg-red-100 rounded-lg dark:bg-red-900 dark:bg-opacity-20 dark:text-red-300" role="alert">
                {apiError}
              </div>
            )}
            
            {/* Email input field with lazy loaded icon */}
            <div>
              <div className="relative">
                <div className="input-icon text-gray-400 dark:text-pink-400 ">
                  <Suspense fallback={<IconFallback />}>
                    <FaUser />
                  </Suspense>
                </div>
                <input
                  type="email"
                  {...register('email')}
                  placeholder="Enter your email"
                  className="form-input w-full pl-14 pr-3 py-2 rounded-lg border border-gray-300 focus:outline-none focus:ring-2 focus:ring-pink-500 focus:border-transparent dark:bg-gray-700 dark:border-gray-600 dark:text-white dark:placeholder-gray-400 text-sm sm:text-base"
                  disabled={isLoading}
                />
              </div>
              {errors.email && <p className="text-red-500 dark:text-red-400 text-xs sm:text-sm mt-1">{errors.email.message}</p>}
            </div>
            
            {/* Password input field with visibility toggle */}
            <div>
              <div className="relative">
                <div className="input-icon text-gray-400 dark:text-pink-400">
                  <Suspense fallback={<IconFallback />}>
                    <FaLock />
                  </Suspense>
                </div>
                <input
                  type={showPassword ? "text" : "password"}
                  {...register('password')}
                  placeholder="Enter your password"
                  className="form-input w-full pl-14 pr-10 py-2 rounded-lg border border-gray-300 focus:outline-none focus:ring-2 focus:ring-pink-500 focus:border-transparent dark:bg-gray-700 dark:border-gray-600 dark:text-white dark:placeholder-gray-400 text-sm sm:text-base"
                  disabled={isLoading}
                />
                {/* Password visibility toggle button */}
                <button 
                  type="button" 
                  className="password-toggle absolute inset-y-0 right-0 pr-3 flex items-center text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 text-sm sm:text-base"
                  onClick={() => setShowPassword(!showPassword)}
                >
                  <Suspense fallback={<IconFallback />}>
                    {showPassword ? <FaEyeSlash /> : <FaEye />}
                  </Suspense>
                </button>
              </div>
              {errors.password && <p className="text-red-500 dark:text-red-400 text-xs sm:text-sm mt-1">{errors.password.message}</p>}
            </div>

            {/* Remember me and forgot password section */}
            <div className="flex flex-col sm:flex-row sm:justify-between items-center mb-4 sm:mb-6 space-y-3 sm:space-y-0">
              <label className="flex items-center">
                <input 
                  type="checkbox" 
                  {...register('rememberMe')} 
                  className="form-checkbox h-4 w-4 text-pink-600 transition duration-150 ease-in-out dark:bg-gray-700 dark:border-gray-600" 
                  disabled={isLoading} 
                />
                <span className="ml-2 text-gray-700 dark:text-gray-300 text-xs sm:text-sm">Remember me</span>
              </label>
              <Link href="/forgot-password" className="text-pink-500 hover:text-pink-700 dark:text-pink-400 dark:hover:text-pink-300 text-xs sm:text-sm">
                Forgot password?
              </Link>
            </div>

            {/* Submit button with loading state */}
            <button 
              type="submit" 
              className="gradient-btn w-full bg-gradient-to-r from-pink-500 to-purple-600 hover:from-pink-600 hover:to-purple-700 text-white font-bold py-2 px-4 rounded-lg focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-pink-500 transition duration-150 ease-in-out disabled:opacity-50 dark:focus:ring-offset-gray-800 text-sm sm:text-base" 
              disabled={isLoading || isSubmitting}
            >
              {isLoading ? (
                <div className="flex items-center justify-center">
                  <svg className="animate-spin -ml-1 mr-3 h-4 sm:h-5 w-4 sm:w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                  Signing In...
                </div>
              ) : 'Sign In'}
            </button>

            {/* Social login divider */}
            <div className="flex items-center my-4 sm:my-6">
              <div className="border-t border-gray-300 flex-grow dark:border-gray-600"></div>
              <span className="mx-4 text-gray-500 dark:text-gray-400 text-xs sm:text-sm">or continue with</span>
              <div className="border-t border-gray-300 flex-grow dark:border-gray-600"></div>
            </div>

            {/* Social login buttons */}
            <div className="flex justify-center space-x-4 mb-4 sm:mb-6">
              <button type="button" className="w-10 h-10 rounded-full bg-gray-100 flex items-center justify-center text-blue-600 hover:bg-gray-200 transition duration-150 ease-in-out dark:bg-gray-700 dark:hover:bg-gray-600 dark:text-blue-400">
                <Suspense fallback={<IconFallback />}>
                  <FaFacebookF />
                </Suspense>
              </button>
              <button type="button" className="w-10 h-10 rounded-full bg-gray-100 flex items-center justify-center text-red-600 hover:bg-gray-200 transition duration-150 ease-in-out dark:bg-gray-700 dark:hover:bg-gray-600 dark:text-red-400">
                <Suspense fallback={<IconFallback />}>
                  <FaGoogle />
                </Suspense>
              </button>
              <button type="button" className="w-10 h-10 rounded-full bg-gray-100 flex items-center justify-center text-black hover:bg-gray-200 transition duration-150 ease-in-out dark:bg-gray-700 dark:hover:bg-gray-600 dark:text-white">
                <Suspense fallback={<IconFallback />}>
                  <FaApple />
                </Suspense>
              </button>
            </div>

            {/* Registration link */}
            <div className="text-center text-gray-700 dark:text-gray-300 text-xs sm:text-sm">
              Don&apos;t have an account?{' '}
              <Link href="/verify-email" className="text-pink-500 font-medium hover:underline dark:text-pink-400">
                Register now
              </Link>
            </div>
          </form>
        </AuthCard>
      </Suspense>
    </div>
    
  );
}