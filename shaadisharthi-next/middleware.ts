// middleware.ts
import { NextRequest, NextResponse } from 'next/server';
import { validateSession } from '@/lib/session';

// Security headers to protect against common web vulnerabilities
const securityHeaders = {
  'X-Frame-Options': 'DENY',           // Prevents our site from being embedded in iframes (clickjacking protection)
  'X-Content-Type-Options': 'nosniff', // Stops browser from guessing file types (MIME sniffing protection)
  'Referrer-Policy': 'strict-origin-when-cross-origin', // Controls how much referrer info is shared
  'X-XSS-Protection': '1; mode=block', // Enables cross-site scripting protection in older browsers
};

// Routes that require user to be logged in (will redirect to login if not authenticated)
const protectedRoutes = ['/dashboard', '/service-detail', '/services', '/bookings', '/account', '/service-booking'];

// Routes that are publicly accessible (even without login)
const publicRoutes = ['/login', '/signup', '/verify-email', '/register', '/cstmr-verify-email', '/forgot-password', '/reset-password'];

export default async function middleware(req: NextRequest) {
  const path = req.nextUrl.pathname;
  const searchParams = req.nextUrl.searchParams;

  // Apply security headers to EVERY response that goes through middleware
  const response = NextResponse.next();
  Object.entries(securityHeaders).forEach(([key, value]) => {
    response.headers.set(key, value);
  });

  // Check if this is a special route that uses tokens instead of sessions
  // These are routes like password reset or registration that have temporary tokens in URL
  const isRegisterWithToken = path === '/register' && searchParams.has('token');
  const isResetPasswordWithToken = path === '/reset-password' && searchParams.has('token');
  const isTokenSecuredRoute = isRegisterWithToken || isResetPasswordWithToken;

  // If it's a token-secured route, we allow access without checking sessions
  // This is because these routes have their own security via the token in URL
  if (isTokenSecuredRoute) {
    return response;
  }

  // For all other routes, check if user has a valid session
  const { isValid: hasValidSession } = await validateSession();
  
  // Check if current path is a protected route (requires login)
  const isProtectedRoute = protectedRoutes.some(prefix => path.startsWith(prefix));
  // Check if current path is a public route (doesn't require login)
  const isPublicRoute = publicRoutes.includes(path);

  // If user tries to access protected route without being logged in, redirect to login
  if (isProtectedRoute && !hasValidSession) {
    const loginUrl = new URL('/login', req.nextUrl);
    // Remember where user was trying to go, so we can send them back after login
    loginUrl.searchParams.set('returnUrl', path);
    return NextResponse.redirect(loginUrl);
  }

  // If user is already logged in but tries to access public route like login page, 
  // redirect them to dashboard (no need to see login page when already logged in)
  if (isPublicRoute && hasValidSession) {
    return NextResponse.redirect(new URL('/dashboard', req.nextUrl));
  }

  // If none of the above conditions match, allow the request to proceed
  return response;
}

// This tells Next.js which routes should go through this middleware
// We exclude API routes, static files, and images for performance
export const config = {
  matcher: [
    '/((?!api|_next/static|_next/image|.*\\.(?:png|jpg|jpeg|gif|webp|svg|ico)$).*)',
  ],
};