import 'server-only';
import { decodeJwt } from 'jose';
import { cookies } from 'next/headers';

// This defines what information is stored in our session token
interface SessionPayload {
  sub: string;    // User ID (subject)
  iat: number;    // When the token was issued (timestamp in seconds)
  exp: number;    // When the token expires (timestamp in seconds)
}

// Simple cache to avoid decoding the same token multiple times in short period
// This improves performance but cache clears when server restarts
const sessionCache = new Map();
// Cache lives for 30 seconds - balance between performance and security
const CACHE_TTL = 30000;

// Main function to check if user's session is valid
export async function validateSession(): Promise<{ isValid: boolean; payload: SessionPayload | null }> {
  // Get the session cookie from browser
  const cookieStore = await cookies();
  const sessionCookie = cookieStore.get('session');
  
  // If no session cookie exists, user is definitely not logged in
  if (!sessionCookie?.value) {
    return { isValid: false, payload: null };
  }

  // Check if we recently validated this same token (performance optimization)
  const cached = sessionCache.get(sessionCookie.value);
  if (cached && Date.now() - cached.timestamp < CACHE_TTL) {
    return { isValid: true, payload: cached.payload };
  }

  try {
    // Decode the JWT token to get the information inside (without verifying signature)
    const payload = decodeJwt<SessionPayload>(sessionCookie.value);
    const nowInSeconds = Math.floor(Date.now() / 1000);

    // Check if token has expired (current time is after expiration time)
    if (!payload.exp || payload.exp < nowInSeconds) {
      // If expired, clean up the session immediately
      await deleteSession();
      return { isValid: false, payload: null };
    }

    // If we got here, token is valid - cache it for future requests
    sessionCache.set(sessionCookie.value, {
      payload,
      timestamp: Date.now()
    });

    return { isValid: true, payload };
  } catch (error) {
    // If token is corrupted or invalid format, log error and clear session
    console.error('Session validation error:', error);
    await deleteSession();
    return { isValid: false, payload: null };
  }
}

// Creates a new session when user logs in successfully
export async function createSession(token: string): Promise<boolean> {
  try {
    // Decode the token to check it's valid before setting cookie
    const payload = decodeJwt<SessionPayload>(token);
    const nowInSeconds = Math.floor(Date.now() / 1000);

    // Make sure token isn't already expired
    if (!payload.exp || payload.exp < nowInSeconds) {
      console.error('Token expired or invalid expiration');
      return false;
    }

    // Calculate when the cookie should expire (same as token expiration)
    const expiresAt = new Date(payload.exp * 1000);
    const cookieStore = await cookies();

    cookieStore.set('session', token, {
  httpOnly: true,
  secure: process.env.NODE_ENV === 'production',  // stays fine
  expires: expiresAt,
  sameSite: process.env.NODE_ENV === 'production' ? 'none' : 'lax',
  path: '/',
});


    // Cache the new session for better performance
    sessionCache.set(token, {
      payload,
      timestamp: Date.now()
    });

    return true;
  } catch (error) {
    console.error('Session creation error:', error);
    return false;
  }
}

// Clears the session when user logs out
export async function deleteSession(): Promise<void> {
  try {
    const cookieStore = await cookies();
    const sessionCookie = cookieStore.get('session');
    
    // Remove from cache if it exists
    if (sessionCookie?.value) {
      sessionCache.delete(sessionCookie.value);
    }
    
    // Delete the cookie from browser
    cookieStore.delete('session');
  } catch (error) {
    console.error('Session deletion error:', error);
  }
}

// Simple function to get current session info
export async function getSession() {
  const { isValid, payload } = await validateSession();
  
  // If no valid session, return null
  if (!isValid || !payload) {
    return null;
  }

  // Return simplified session information for the rest of the app
  return {
    customerId: payload.sub,
    expiresAt: payload.exp,
  };
}