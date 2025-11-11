'use client';

import { useAuth } from '@/context/useAuth';
import { useState, useCallback, memo, useEffect, useRef } from 'react';
import {
  FaUser,
  FaRing,
  FaBell,
  FaTimes
} from 'react-icons/fa';
import Link from 'next/link';

/**
 * Notification Dropdown Component
 * Renders the dropdown panel with real-time notifications.
 * Shows messages, supports "Clear All", and "View All" link.
 * Purely in-memory — no read/unread persistence.
 */
const NotificationDropdown = memo(({ 
  notifOpen, 
  messages, 
  onClear 
}: { 
  notifOpen: boolean; 
  messages: string[]; 
  onClear: () => void;
}) => {
  if (!notifOpen) return null;

  return (
    <div
      className={`
        /* Default (Laptop/Tablet) */
        absolute right-0 mt-2 w-80 bg-white dark:bg-gray-800 
        rounded-md shadow-lg py-1 z-50 border border-gray-200 dark:border-gray-700

        /* Mobile override */
        sm:absolute sm:right-0 sm:w-80
        md:w-80
        max-sm:fixed max-sm:left-0 max-sm:right-0 max-sm:top-16 max-sm:w-[calc(100%-1.75rem)] 
        max-sm:mx-auto max-sm:px-1.5 
        max-sm:rounded-md max-sm:border max-sm:border-gray-300
        max-sm:shadow-none max-sm:bg-white dark:max-sm:bg-gray-800
      `}
    >
      {/* Header */}
      <div className="px-4 py-2 border-b border-gray-100 dark:border-gray-700 flex items-center justify-between">
        <h6 className="font-semibold text-gray-800 dark:text-white">Notifications</h6>
        {messages.length > 0 && (
          <button
            onClick={onClear}
            className="text-xs text-pink-500 hover:text-pink-600 font-medium flex items-center gap-1 transition-colors"
          >
            <FaTimes className="text-xs" /> Clear
          </button>
        )}
      </div>

      {/* Scrollable Notification List */}
      <div className="max-h-96 overflow-y-auto" id="notificationContainer">
        {messages.length === 0 ? (
          <div className="px-4 py-3 text-center text-gray-500 dark:text-gray-400 italic">
            No new notifications
          </div>
        ) : (
          messages.map((msg, idx) => (
            <div
              key={idx}
              className="px-4 py-3 hover:bg-gray-50 dark:hover:bg-gray-700 border-b border-gray-100 dark:border-gray-700 last:border-0 transition-colors"
            >
              <div className="flex items-start">
                {/* Bell Icon */}
                <div className="flex-shrink-0 mr-3">
                  <FaBell className="text-pink-500 mt-0.5" />
                </div>
                {/* Message Content */}
                <div className="flex-1 min-w-0">
                  <p className="text-sm text-gray-900 dark:text-gray-100 break-words">
                    {msg}
                  </p>
                  <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">
                    Just now
                  </p>
                </div>
              </div>
            </div>
          ))
        )}
      </div>

      {/* Footer: View All Link (only if >10 messages) */}
      {messages.length > 10 && (
        <div className="px-4 py-2 border-t border-gray-100 dark:border-gray-700 text-center">
          <Link 
            href="/notifications"
            className="text-sm text-pink-500 hover:underline font-medium"
            onClick={(e) => e.stopPropagation()} // Prevent header click-outside from closing
          >
            View all notifications
          </Link>
        </div>
      )}
    </div>
  );
});


NotificationDropdown.displayName = 'NotificationDropdown';

/**
 * User Account Dropdown
 * Contains profile links and logout button.
 * Closes when any link is clicked.
 */
const UserDropdown = memo(({ 
  userMenuOpen, 
  logout, 
  isLoggingOut,
  onClose
}: { 
  userMenuOpen: boolean; 
  logout: () => void;
  isLoggingOut: boolean;
  onClose: () => void;
}) => {
  if (!userMenuOpen) return null;
  
  return (
    <div className="absolute right-0 mt-2 w-48 bg-white dark:bg-gray-700 dark:text-white rounded-md shadow-lg py-2 z-50 border border-gray-200 dark:border-gray-700">
      <Link 
        href="/account" 
        onClick={onClose} 
        className="block px-4 py-2 text-gray-700 dark:text-white dark:hover:bg-gray-600 hover:bg-gray-100 transition-colors"
      >
        My Profile
      </Link>
      <Link 
        href="/dashboard" 
        onClick={onClose} 
        className="block px-4 py-2 text-gray-700 dark:text-white dark:hover:bg-gray-600 hover:bg-gray-100 transition-colors"
      >
        Dashboard
      </Link>
      <Link 
        href="/services" 
        onClick={onClose} 
        className="block px-4 py-2 text-gray-700 dark:text-white dark:hover:bg-gray-600 hover:bg-gray-100 transition-colors"
      >
        Vendors
      </Link>
      <Link 
        href="/bookings" 
        onClick={onClose} 
        className="block px-4 py-2 text-gray-700 dark:text-white dark:hover:bg-gray-600 hover:bg-gray-100 transition-colors"
      >
        My Bookings
      </Link>
      
      <div className="border-t border-gray-200 dark:border-gray-600 my-1"></div>
      
      <button
        onClick={() => {
          logout();
          onClose();
        }}
        disabled={isLoggingOut}
        className="block w-full text-left px-4 py-2 text-gray-700 dark:text-white dark:hover:bg-gray-600 hover:bg-gray-100 transition-colors disabled:opacity-50"
      >
        {isLoggingOut ? 'Logging out...' : 'Logout'}
      </button>
    </div>
  );
});

UserDropdown.displayName = 'UserDropdown';

/**
 * Main Header Component
 * Sticky top navigation with:
 * - Logo & Branding
 * - Desktop Navigation
 * - Real-time Notifications (via WebSocket)
 * - User Account Dropdown
 */
function Header() {
  // === State Management ===
  const [userMenuOpen, setUserMenuOpen] = useState(false);     // User dropdown visibility
  const [notifOpen, setNotifOpen] = useState(false);          // Notification dropdown visibility
  const [messages, setMessages] = useState<string[]>([]);     // List of incoming notifications
  const [unreadCount, setUnreadCount] = useState(0);          // Badge count (unread messages)
  const socketRef = useRef<WebSocket | null>(null);           // Persistent WebSocket reference
  const { logout, isLoggingOut } = useAuth();                 // Auth context for logout

  // === Event Handlers (Memoized for performance) ===
  const toggleUserMenu = useCallback(() => {
    setUserMenuOpen(prev => !prev);
    setNotifOpen(false); // Close notification when opening user menu
  }, []);

  const toggleNotif = useCallback(() => {
    setNotifOpen(prev => !prev);
    setUserMenuOpen(false); // Close user menu when opening notifications
  }, []);

  const closeMenus = useCallback(() => {
    setUserMenuOpen(false);
    setNotifOpen(false);
  }, []);

  const clearNotifications = useCallback(() => {
    setMessages([]);
    setUnreadCount(0);
  }, []);

  // === Click Outside Handler ===
  // Closes both dropdowns when clicking outside their containers
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      const target = e.target as HTMLElement;
      const isNotifContainer = target.closest('.notification-container');
      const isUserContainer = target.closest('.user-container');
      
      if (!isNotifContainer && !isUserContainer) {
        closeMenus();
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [closeMenus]);

  // === WebSocket Connection & Real-time Updates ===
  // Establishes persistent connection to backend WebSocket endpoint
  useEffect(() => {
    const BASE_URL = process.env.NEXT_PUBLIC_WEBSOCKET_API_URL;

    const socketUrl = BASE_URL || 'ws://localhost:6190/ShaadiSharthi/CustomerSocket';
    const socket = new WebSocket(socketUrl);
    socketRef.current = socket;

    // Connection established
    socket.onopen = () => {
      console.log("WebSocket connected: Ready for real-time notifications");
    };

    // Handle incoming message
    socket.onmessage = (event) => {
      const msg = event.data;
      console.log("Notification received:", msg);

      // Append new message and increment unread count
      setMessages(prev => [...prev, msg]);
      setUnreadCount(prev => prev + 1);
    };

    // Log errors (do not break UI)
    socket.onerror = (error) => {
      console.error("WebSocket error:", error);
    };

    // Connection closed
    socket.onclose = () => {
      console.log("WebSocket closed. Will not reconnect automatically.");
    };

    // Cleanup: Close socket on component unmount
    return () => {
      if (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING) {
        socket.close();
        console.log("WebSocket cleanup: Connection closed");
      }
    };
  }, []); // Run once on mount

  return (
    <header className="dashboard-header sticky top-0 z-50 bg-white shadow-sm dark:bg-gray-900 dark:text-white transition-colors">
      <div className="container mx-auto px-4 py-3">
        <div className="flex items-center justify-between">

          {/* === Logo & Branding === */}
          <div className="flex items-center">
            <Link href="/" className="flex items-center hover:opacity-90 transition-opacity">
              <FaRing className="text-pink-500 text-3xl mr-2" />
              <span className="text-2xl font-bold text-gray-800 dark:text-white font-serif">
                ShaadiSharthi
              </span>
            </Link>
          </div>

          {/* === Desktop Navigation === */}
          <nav className="hidden md:flex items-center space-x-8">
            <Link href="/dashboard" className="text-gray-700 dark:text-gray-300 hover:text-pink-500 font-medium transition-colors">
              Home
            </Link>
            <Link href="/services" className="text-gray-700 dark:text-gray-300 hover:text-pink-500 font-medium transition-colors">
              Vendors
            </Link>
            <Link href="/bookings" className="text-gray-700 dark:text-gray-300 hover:text-pink-500 font-medium transition-colors">
              My Bookings
            </Link>
          </nav>

          {/* === Right-side Icons & Dropdowns === */}
          <div className="flex items-center space-x-6">

            {/* === Notification Bell with Badge === */}
            <div className="relative notification-container">
              <button 
                className="text-gray-600 dark:text-gray-300 hover:text-pink-500 relative transition-colors"
                onClick={toggleNotif}
                aria-label="Toggle notifications"
              >
                <FaBell className="text-xl" />
                {/* Unread Badge */}
                {unreadCount > 0 && (
                  <span className="absolute -top-2 -right-2 bg-pink-500 text-white text-xs rounded-full w-5 h-5 flex items-center justify-center animate-pulse">
                    {unreadCount > 99 ? '99+' : unreadCount}
                  </span>
                )}
              </button>

              {/* Notification Dropdown */}
              <NotificationDropdown 
                notifOpen={notifOpen} 
                messages={messages} 
                onClear={clearNotifications}
              />
            </div>

            {/* === User Account Menu === */}
            <div className="relative user-container">
              <button 
                className="user-avatar cursor-pointer flex items-center space-x-2 hover:opacity-90 transition-opacity"
                onClick={toggleUserMenu}
                aria-label="Toggle user menu"
              >
                <div className="w-8 h-8 rounded-full bg-pink-500 flex items-center justify-center text-white">
                  <FaUser />
                </div>
                <span className="hidden md:inline text-gray-700 dark:text-gray-300 font-medium">
                  My Account
                </span>
              </button>

              <UserDropdown 
                userMenuOpen={userMenuOpen} 
                logout={logout} 
                isLoggingOut={isLoggingOut}
                onClose={closeMenus}
              />
            </div>
          </div>
        </div>
      </div>
    </header>
  );
}

export default memo(Header);