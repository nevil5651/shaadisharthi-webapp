'use client' 

import React, { createContext, useState, useEffect, useRef, useCallback, useContext } from 'react'
import { toast } from 'react-toastify'
import { useRouter } from 'next/navigation'
import { usePathname } from "next/navigation";
import { publicRoutes } from "@/lib/auth-routes";

// Define what user information looks like
interface User {
  id: string
  name: string
  email: string
  phone_no: string
  address?: string
  created_at: string
  avatar?: string
}

// Login credentials can be email/password or other fields
interface Credentials {
  [key: string]: string;
}

// This defines all the methods and data our auth system provides
interface AuthContextType {
  user: User | null
  isAuthenticated: boolean
  isLoading: boolean
  isLoggingOut: boolean
  login: (credentials: Credentials) => Promise<{ success: boolean; error?: string }>
  logout: () => Promise<void>
  updateUser: (updatedData: Partial<User>) => Promise<void>
  fetchProfile: (forceRefresh?: boolean) => Promise<void>
  updateAccountDetails: (updatedData: Partial<User>) => Promise<{ success: boolean; error?: string }>
}

// Create the context that components can use to access auth information
export const AuthContext = createContext<AuthContextType | undefined>(undefined)

// Keys for storing user data in browser's local storage (for caching)
const USER_CACHE_KEY = 'user_cache'
// Cache user data for 5 minutes to avoid unnecessary API calls
const CACHE_MAX_AGE = 5 * 60 * 1000

// The main provider that wraps our app and manages authentication state
export const AuthProvider = ({ children }: { children: React.ReactNode }) => {
  const [user, setUser] = useState<User | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [isLoggingOut, setIsLoggingOut] = useState(false)
  const router = useRouter()
  const pathname = usePathname();
  // This prevents multiple profile fetches happening at same time
  const fetchInProgress = useRef(false)

  // Helper function to get user data from local storage cache
  const getCachedUser = (): { user: User, timestamp: number } | null => {
    if (typeof window === 'undefined') return null
    try {
      const cached = localStorage.getItem(USER_CACHE_KEY)
      return cached ? JSON.parse(cached) : null
    } catch {
      return null
    }
  }

  // Helper function to save user data to local storage cache
  const setCachedUser = (user: User) => {
    if (typeof window === 'undefined') return
    localStorage.setItem(USER_CACHE_KEY, JSON.stringify({
      user,
      timestamp: Date.now()
    }))
  }

  // Helper function to clear user data from cache (on logout)
  const clearCachedUser = () => {
    if (typeof window === 'undefined') return
    localStorage.removeItem(USER_CACHE_KEY)
  }

  // Logout function - clears session on both frontend and backend
  const logout = useCallback(async () => {
    // Prevent multiple logout clicks from causing issues
    if (isLoggingOut) return

    setIsLoggingOut(true)
    // const toastId = toast.loading('Logging out...')

    try {
      // Call our logout API to clear the session cookie on backend
      const logoutPromise = fetch('/customer/api/auth/logout', {
        method: 'POST',
        credentials: 'include'
      });

      // Set a timeout so we don't wait forever if backend is slow
      const timeoutPromise = new Promise((_, reject) => 
        setTimeout(() => reject(new Error('Logout timeout')), 3000)
      );

      // Wait for either logout to complete or timeout to happen
      await Promise.race([logoutPromise, timeoutPromise]);
      
      // Clear user data from frontend state regardless of backend response
      setUser(null)
      clearCachedUser()
      
      // Set a flag so login page knows to show "logged out successfully" message
      if (typeof window !== 'undefined') {
        sessionStorage.setItem('logout_status', 'success')
      }
      
      // Redirect to login page - using window.location for complete page reload
      router.push('/login')
      toast.success('Logged out successfully')
      
    } catch (error) {
      console.error('Logout error:', error)
      // Even if backend call fails, we still clear frontend state and redirect
      setUser(null)
      clearCachedUser()
      router.push('/login')
      //toast.error('Logout out successful')
      toast.success('Logged out successfully.');
    } finally {
      setIsLoggingOut(false)
    }
  }, [isLoggingOut, router]);

  // Fetches user profile from backend - used when app loads and when we need fresh data
  const fetchProfile = useCallback(async (forceRefresh = false) => {
    // Prevent multiple simultaneous profile fetches
    if (fetchInProgress.current) return
    
    // Don't fetch profile if we're on a public route (like login page)
    if (publicRoutes.includes(pathname)) {
      setUser(null)
      setIsLoading(false)
      return
    }

    fetchInProgress.current = true
    setIsLoading(true)

    try {
      // First check if we have cached user data that's still fresh
      if (!forceRefresh) {
        const cached = getCachedUser()
        if (cached && Date.now() - cached.timestamp < CACHE_MAX_AGE) {
          setUser(cached.user)
          setIsLoading(false)
          fetchInProgress.current = false
          return
        }
      }

      // If no fresh cache, fetch from backend
      const res = await fetch('/customer/api/auth/me', {
        credentials: 'include', // Important: sends cookies with request
        cache: forceRefresh ? 'no-cache' : 'default',
        headers: {
          'Cache-Control': 'no-cache' // Ensure we get fresh data
        }
      })

      // Special case: if we're on a token-secured route (like password reset),
      // we don't want to logout on 401 error
      const isTokenSecuredRoute = typeof window !== 'undefined' && 
        (window.location.pathname === '/register' || window.location.pathname === '/reset-password') && 
        new URLSearchParams(window.location.search).has('token')

      // If backend says we're unauthorized (401) and we're not on a token route, logout
      if (res.status === 401 && !isTokenSecuredRoute) {
        await logout()
        return
      }

      // If response isn't successful, throw error
      if (!res.ok) {
        throw new Error(`Failed to fetch profile: ${res.status}`)
      }

      // Parse and use the user data from backend
      const userData = await res.json()
      setUser(userData)
      setCachedUser(userData) // Cache for next time
    } catch (error) {
      console.error('Profile fetch error:', error)
      
      // Again, don't throw errors for token-secured routes
      const isTokenSecuredRoute = typeof window !== 'undefined' && 
        (window.location.pathname === '/register' || window.location.pathname === '/reset-password') && 
        new URLSearchParams(window.location.search).has('token')

      if (!isTokenSecuredRoute) {
        // Only throw error for non-token routes
        throw error
      }
    } finally {
      setIsLoading(false)
      fetchInProgress.current = false
    }
  }, [pathname, logout])

  // Login function - handles the entire login process
  const login = useCallback(async (credentials: Credentials) => {
    setIsLoading(true)
    try {
      // Get backend API URL from environment variables
      const apiUrl = process.env.NEXT_PUBLIC_API_URL?.trim()
      if (!apiUrl) {
        throw new Error("API URL is not configured")
      }

      // Step 1: Send credentials to Java backend for validation
      const backendRes = await fetch(`${apiUrl}/Customer/signin`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(credentials),
      })

      // If backend rejects credentials, show error
      if (!backendRes.ok) {
        const errorData = await backendRes.json()
        return { success: false, error: errorData.message || 'Authentication failed' }
      }

      // Backend returns a JWT token on successful login
      const { token } = await backendRes.json()

      // Step 2: Send token to our Next.js API to create session cookie
      const sessionRes = await fetch('api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token }),
      })

      if (!sessionRes.ok) {
        return { success: false, error: 'Failed to create session' }
      }

      // Step 3: Fetch user profile data now that we're logged in
      await fetchProfile(true)
      
      // Step 4: Redirect user to where they wanted to go (or dashboard)
      const urlParams = new URLSearchParams(window.location.search)
      const returnUrl = urlParams.get('returnUrl') || '/dashboard'
      router.push(returnUrl)
      
      // Show success message to user
      toast.success('Logged in successfully')
      return { success: true }
    } catch (error) {
      console.error('Login error:', error)
      return { 
        success: false, 
        error: error instanceof Error ? error.message : 'Login failed' 
      }
    } finally {
      setIsLoading(false)
    }
  }, [fetchProfile, router])

  // Updates user data in frontend state (without saving to backend)
  const updateUser = useCallback(async (updatedData: Partial<User>) => {
    setUser(prev => {
      if (!prev) return null
      const newUser = {
        ...prev,
        ...updatedData,
        name: updatedData.name || prev.name,
        email: prev.email // Never change email from frontend
      }
      setCachedUser(newUser) // Update cache with new data
      return newUser
    })
  }, [])

  // Saves user profile changes to backend
  const updateAccountDetails = useCallback(async (updatedData: Partial<User>) => {
    try {
      setIsLoading(true)
      
      // Remove email from data we send to backend (email shouldn't be editable)
      const { email, ...updatePayload } = updatedData
      
      // Send update to backend
      const response = await fetch('api/auth/update', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(updatePayload)
      })

      if (!response.ok) {
        const errorData = await response.json()
        const message = errorData.message || errorData.error || 'Failed to update account';
        throw new Error(message);   
         }

      // Get updated user data from backend response
      const responseData = await response.json()
      const updatedUser = responseData.data || responseData
      
      // Update frontend state with new user data
      setUser(updatedUser)
      setCachedUser(updatedUser)
      
      return { success: true }
    } catch (error) {
      console.error('Update account error:', error)
      return { 
        success: false, 
        error: error instanceof Error ? error.message : 'Update failed' 
      }
    } finally {
      setIsLoading(false)
    }
  }, [])

  // When component first loads, fetch user profile to check if we're logged in
  useEffect(() => {
    fetchProfile().catch(() => {
      // Errors are already handled in fetchProfile, so we can ignore here
    })
  }, [fetchProfile])

  // This object contains all the auth data and methods that child components can use
  const value: AuthContextType = {
    user,
    isAuthenticated: !!user, // Convert to boolean: true if user exists, false if null
    isLoading,
    isLoggingOut,
    login,
    logout,
    updateUser,
    updateAccountDetails,
    fetchProfile,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

// Custom hook to easily use auth context in components
export const useAuth = () => {
  const context = useContext(AuthContext)
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}