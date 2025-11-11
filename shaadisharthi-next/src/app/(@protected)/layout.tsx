import { getSession } from '@/lib/session';
import "../globals.css";
import '@fortawesome/fontawesome-free/css/all.min.css';
import { AuthProvider } from '@/context/AuthContext';
import Header from '@/components/layout/Header';
import Footer from '@/components/layout/Footer';
import { redirect } from 'next/navigation';
import { Metadata } from 'next';
import ToastProvider from '@/components/ToastProvider';
import AuthHandler from '@/components/auth/AuthHandler';
import Providers from './services/components/providers';

interface LayoutProps {
  children: React.ReactNode;
}

// This metadata appears in browser tab and search results
export const metadata: Metadata = {
  title: 'ShaadiSharthi - Dashboard',
  description: 'Your wedding management dashboard',
};

// This layout wraps all protected pages (pages that require login)
export default async function ProtectedLayout({ children }: LayoutProps) {
  // Check if user has a valid session (server-side check)
  const session = await getSession();

  // If no session exists, redirect to login page immediately
  // This is our SECOND layer of protection (middleware is first layer)
  if (!session) {
    redirect('/login?error=session_required');
  }

  // If we get here, user is properly authenticated
  return (
    // AuthProvider gives all child components access to user information and auth methods
    <AuthProvider>
      {/* AuthHandler runs in background to monitor session health */}
      <AuthHandler />
      <div className="flex flex-col min-h-screen">
        <Header />
        <main className="flex-grow">
          <div className="container mx-auto px-4 py-8">
            {/* ToastProvider shows success/error messages to user */}
            <ToastProvider />
            {/* Providers might be for other contexts like theme, etc. */}
            <Providers>{children}</Providers>
          </div>
        </main>
        <Footer />
      </div>
    </AuthProvider>
  );
}