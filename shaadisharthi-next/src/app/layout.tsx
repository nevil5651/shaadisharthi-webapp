import type { Metadata, Viewport } from "next";
import localFont from "next/font/local";
import { ThemeProvider } from "next-themes";
import "./globals.css";

const playfair = localFont({
  src: "../fonts/PlayfairDisplay-Regular.ttf",
  variable: "--font-playfair-display",
});

const poppins = localFont({
  src: [
    { path: "../fonts/Poppins-Light.ttf", weight: "300" },
    { path: "../fonts/Poppins-Regular.ttf", weight: "400" },
    { path: "../fonts/Poppins-Medium.ttf", weight: "500" },
    { path: "../fonts/Poppins-SemiBold.ttf", weight: "600" },
  ],
  variable: "--font-poppins",
});

export const metadata: Metadata = {
  title: "ShaadiSharthi - Your Wedding Planning Partner",
  description:
    "Discover India's finest wedding vendors and plan your perfect day with ease",
};

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1.0,
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en" suppressHydrationWarning>
      <body className={`${playfair.variable} ${poppins.variable}`}>
        <ThemeProvider
          attribute="class"
          defaultTheme="system"
          enableSystem
          disableTransitionOnChange
        >
          {children}
        </ThemeProvider>
      </body>
    </html>
  );
}
