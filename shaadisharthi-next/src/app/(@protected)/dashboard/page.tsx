'use client'

import { useState, useEffect, useCallback, Suspense } from 'react';
import { FaCalendarCheck, FaHeart, FaComments, FaArrowRight } from 'react-icons/fa';
import ServiceCard from './components/ServiceCard';
import BookingCard from './components/BookingCard';
import api from '@/lib/axios';
import { isAxiosError } from 'axios';
import { BookingCardSkeleton } from './components/BookingCardSkeleton';
import ServiceCardSkeleton from './components/ServiceCardSkeleton';
import { useRecommendedServices } from '@/hooks/useRecommendedServices';
import { useAuth } from '@/context/useAuth'
import Link from 'next/link';

// Interface defining the structure of a booking object
interface Booking {
  id: number;
  serviceName: string;
  date: string;
  time: string;
  providerName: string;
  status: string;
}

// StatsCard Component: Displays statistical information with icon and gradient background
// Memoized to prevent unnecessary re-renders
const StatsCard = ({ title, value, icon, color }: {
  title: string;
  value: number;
  icon: React.ReactNode;
  color: string;
}) => (
  <div className={`bg-gradient-to-r ${color} rounded-xl p-6 text-white`}>
    <div className="flex items-center">
      {/* Icon container with semi-transparent background */}
      <div className="bg-white text-gray-500 dark:text-gray-500 bg-opacity-20 rounded-full p-3 mr-4">
        {icon}
      </div>
      <div>
        {/* Stat title */}
        <p className="text-sm opacity-80">{title}</p>
        {/* Stat value */}
        <h3 className="text-2xl font-bold">{value}</h3>
      </div>
    </div>
  </div>
);

// Main Dashboard Component: Primary dashboard page for wedding planning
export default function Dashboard() {

  const { user } = useAuth()

  // State for dashboard statistics cards
  const [stats, setStats] = useState([
    { title: 'Upcoming', value: 0, icon: <FaCalendarCheck />, color: 'from-teal-500 to-cyan-600' },
    { title: 'Saved', value: 0, icon: <FaHeart />, color: 'from-purple-500 to-indigo-600' },
    { title: 'Messages', value: 0, icon: <FaComments />, color: 'from-orange-500 to-pink-500' }
  ]);

  // Custom hook for fetching recommended services with loading, error, and retry functionality
  const { services: recommendedServices, isLoading: servicesLoading, error: servicesError, retryFetch } = useRecommendedServices();

  // State for upcoming bookings
  const [upcomingBookings, setUpcomingBookings] = useState<Booking[]>([]);
  const [loading, setLoading] = useState(true); // Loading state for bookings
  const [error, setError] = useState<string | null>(null); // Error state for bookings

  // Fetch upcoming bookings from API with error handling
  const fetchUpcomingBookings = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      // API call to get customer's upcoming bookings
      const response = await api.get('Customer/cstmr-upcoming-bookings');
      setUpcomingBookings(response.data.bookings || []);
      // Update the 'Upcoming' stat with totalBookings from the response
      setStats((prevStats) =>
        prevStats.map((stat) =>
          stat.title === 'Upcoming' ? { ...stat, value: response.data.totalBookings || 0 } : stat
        )
      );
    } catch (err) {
      console.error('Failed to fetch upcoming bookings:', err);
      // Handle different types of errors
      if (isAxiosError(err)) {
        setError(err.response?.data?.message || err.message || 'An unexpected error occurred.');
      } else {
        setError('An unexpected error occurred.');
      }
    } finally {
      setLoading(false);
    }
  }, []);

  // Fetch bookings when component mounts
  useEffect(() => {
    fetchUpcomingBookings();
  }, [fetchUpcomingBookings]);

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      {/* Hero Banner: Promotional section with call-to-action */}
      <section className="bg-gradient-to-r from-pink-100 to-purple-100 dark:from-pink-900 dark:to-purple-900 py-16 md:py-24">
        <div className="container mx-auto px-4 text-center">
          <h1 className="text-4xl md:text-5xl font-serif font-bold text-gray-800 dark:text-white mb-4">
            Plan Your Dream Wedding
          </h1>
          <p className="text-lg text-gray-600 dark:text-gray-300 max-w-2xl mx-auto mb-8">
            Discover the best vendors, manage your bookings, and create unforgettable memories
          </p>
          <Link
            href="/services"
            className="inline-flex items-center bg-primary hover:bg-pink-700 text-white font-medium py-3 px-8 rounded-full transition-colors"
          >
            Explore Services <FaArrowRight className="ml-2" />
          </Link>
        </div>
      </section>

      <main className="container mx-auto px-4 py-12">
        {/* Welcome Section: User greeting and statistics overview */}
        <section className="mb-12">
          <div className="bg-white dark:bg-gray-800 rounded-xl shadow-sm p-6 md:p-8">
            <h2 className="text-2xl font-serif font-bold text-gray-800 dark:text-white mb-4">Welcome {user?.name || 'Guest'}!</h2>
            <p className="text-gray-600 dark:text-gray-300 mb-6">
              You have {stats[0].value} upcoming booking{upcomingBookings.length !== 1 ? 's' : ''}. Let&apos;s make your wedding planning journey smooth and memorable.
            </p>

            {/* Statistics Grid: Upcoming, Saved, Messages counters */}
            <div className="grid grid-cols-1  md:grid-cols-3 gap-6">
              {stats.map((stat, index) => (
                <StatsCard
                  key={index}
                  title={stat.title}
                  value={stat.value}
                  icon={stat.icon}
                  color={stat.color}
                />
              ))}
            </div>
          </div>
        </section>

        {/* Recommended Services Section: Personalized service recommendations */}
        <section className="mb-12">
          <div className="flex items-center justify-between mb-8">
            <h2 className="text-2xl font-serif font-bold text-gray-800 dark:text-white">Recommended For You</h2>
            <Link href="/services" className="text-primary hover:underline flex items-center">
              View All <FaArrowRight className="ml-1" />
            </Link>
          </div>

          {/* Services Grid with loading, error, and empty states */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
            {servicesLoading ? (
              // Loading state: Show skeleton cards
              Array.from({ length: 4 }).map((_, index) => (
                <ServiceCardSkeleton key={index} />
              ))
            ) : servicesError ? (
              // Error state: Show error message and retry button
              <div className="col-span-full text-center py-8">
                <p className="text-red-600 dark:text-red-400 mb-4">{servicesError}</p>
                <button
                  onClick={retryFetch}
                  className="px-4 py-2 bg-primary text-white rounded hover:bg-primary-dark"
                >
                  Retry
                </button>
              </div>
            ) : recommendedServices.length === 0 ? (
              // Empty state: No services available
              <div className="col-span-full text-center py-8 text-gray-500 dark:text-gray-400">
                No recommended services available.
              </div>
            ) : (
              // Success state: Display recommended services
              <Suspense fallback={
                Array.from({ length: 4 }).map((_, index) => (
                  <ServiceCardSkeleton key={index} />
                ))
              }>
                {recommendedServices.map(service => (
                  <ServiceCard key={service.serviceId} service={service} />
                ))}
              </Suspense>
            )}
          </div>
        </section>

        {/* Upcoming Bookings Section: User's upcoming appointments */}
        <section className="mb-12">
          <div className="flex items-center justify-between mb-6">
            <h2 className="text-2xl font-serif font-bold text-gray-800 dark:text-white">Upcoming Bookings</h2>
            <Link href="/bookings" className="text-primary hover:underline flex items-center">
              View All <FaArrowRight className="ml-1" />
            </Link>
          </div>

          {/* Bookings List with loading, error, and empty states */}
          <div className="space-y-4">
            {loading ? (
              // Loading state: Show skeleton cards
              Array.from({ length: 3 }).map((_, index) => (
                <BookingCardSkeleton key={index} />
              ))
            ) : error ? (
              // Error state: Show error message and retry option
              <div className="bg-red-50 dark:bg-red-900/20 border-l-4 border-red-500 p-6 rounded-lg text-center">
                <p className="text-red-800 dark:text-red-200 font-semibold mb-2">Failed to load bookings</p>
                <p className="text-red-700 dark:text-red-300 text-sm mb-4">{error}</p>
                <button
                  onClick={fetchUpcomingBookings}
                  className="bg-red-600 hover:bg-red-700 text-white font-medium py-2 px-4 rounded-lg transition-colors"
                >
                  Try Again
                </button>
              </div>) : upcomingBookings.length > 0 ? (
                // Success state: Display upcoming bookings (limited to 3)
                upcomingBookings.slice(0, 3).map(booking => (
                  <BookingCard key={booking.id} booking={booking} />
                ))
              ) : (
              // Empty state: No bookings found
              <div className="bg-white dark:bg-gray-800 rounded-xl shadow-sm p-8 text-center">
                <div className="mx-auto h-16 w-16 text-gray-400 mb-4">
                  <FaCalendarCheck className="w-full h-full" />
                </div>
                <h3 className="text-lg font-medium text-gray-900 dark:text-white font-serif">No upcoming bookings</h3>
                <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">Book your first vendor to get started</p>
              </div>
            )}
          </div>
        </section>
      </main>
    </div>
  );
}

