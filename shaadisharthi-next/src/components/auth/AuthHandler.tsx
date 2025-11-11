'use client'; // This runs only in browser

import { useEffect, useRef } from 'react';
import { useAuth } from '@/context/AuthContext';
import { usePathname } from 'next/navigation';

// How often to check if session is still valid (1 minute)
const SESSION_CHECK_INTERVAL = 60000;
// How long user can be inactive before auto-logout (30 minutes)
const INACTIVITY_TIMEOUT = 30 * 60 * 1000;

// This component runs in background on all protected pages
// It monitors session health and user activity
export default function AuthHandler() {
  const { isAuthenticated, logout, fetchProfile } = useAuth();
  const pathname = usePathname();
  // Refs to store timers so we can clear them later
  const inactivityTimer = useRef<NodeJS.Timeout | undefined>(undefined);
  const sessionCheckInterval = useRef<NodeJS.Timeout | undefined>(undefined);

  // Resets the inactivity timer when user does something
  const resetInactivityTimer = () => {
    // Clear existing timer
    if (inactivityTimer.current) {
      clearTimeout(inactivityTimer.current);
    }
    
    // Start new timer - if it expires, user gets logged out
    inactivityTimer.current = setTimeout(() => {
      console.log('Auto logout due to inactivity');
      logout();
    }, INACTIVITY_TIMEOUT);
  };

  // Checks with backend to see if session is still valid
  const checkSessionValidity = async () => {
    // Only check if we think we're logged in
    if (!isAuthenticated) return;

    try {
      // Force refresh to get current session status from backend
      await fetchProfile(true);
    } catch (error) {
      // If fetch fails, session is probably invalid - log user out
      console.error('Session validation failed, logging out:', error);
      logout();
    }
  };

  // Set up activity listeners to detect when user is active
  useEffect(() => {
    // Only monitor activity if user is logged in
    if (!isAuthenticated) return;

    // These events indicate user activity
    const events = ['mousedown', 'mousemove', 'keypress', 'scroll', 'touchstart', 'click'];
    
    const handleActivity = () => {
      resetInactivityTimer();
    };

    // Listen for all these activity events
    events.forEach(event => {
      document.addEventListener(event, handleActivity);
    });

    // Start the initial inactivity timer
    resetInactivityTimer();

    // Cleanup: remove event listeners when component unmounts or auth state changes
    return () => {
      events.forEach(event => {
        document.removeEventListener(event, handleActivity);
      });
      if (inactivityTimer.current) {
        clearTimeout(inactivityTimer.current);
      }
    };
  }, [isAuthenticated, logout]);

  // Set up periodic session checks (every minute)
  useEffect(() => {
    if (!isAuthenticated) return;

    // Check session validity every minute
    sessionCheckInterval.current = setInterval(checkSessionValidity, SESSION_CHECK_INTERVAL);

    // Cleanup: clear interval when component unmounts or auth state changes
    return () => {
      if (sessionCheckInterval.current) {
        clearInterval(sessionCheckInterval.current);
      }
    };
  }, [isAuthenticated]);

  // Check session whenever user navigates to a new page
  useEffect(() => {
    if (isAuthenticated) {
      checkSessionValidity();
    }
  }, [pathname]);

  // This component doesn't render anything visible
  return null;
}