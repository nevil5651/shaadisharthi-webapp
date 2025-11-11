import type { Metadata } from "next";
import "../globals.css";
import { AuthProvider } from "@/context/AuthContext";
import ToastProvider from '@/components/ToastProvider'

export const metadata: Metadata = {
  title: 'ShaadiSharthi',
  description: 'Your one-stop platform for crafting unforgettable weddings with ease.',
}

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (  
   <AuthProvider>
    <ToastProvider />
    {children}
    </AuthProvider>
      
  );
}
