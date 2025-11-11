import Nav from '@/components/landingpage/Nav';
import Hero from '@/components/landingpage/Hero';
import Stats from '@/components/landingpage/Stats';
import Services from '@/components/landingpage/Services';
import HowItWorks from '@/components/landingpage/HowItWorks';
import Testimonials from '@/components/landingpage/Testimonials';
import Gallery from '@/components/landingpage/Gallery';
import About from '@/components/landingpage/About';
import Contact from '@/components/landingpage/Contact';
import Footer from '@/components/landingpage/Footer';


export default function Home() {
  return (
    <>
      <Nav />
      <main>
        <Hero />
        <Stats />
        <Services />
        <HowItWorks />
        <Testimonials />
        <Gallery />
        <About />
        <Contact />
      </main>
      <Footer />
    </>
  );
}

export const revalidate = 3600; // ISR revalidate every hour