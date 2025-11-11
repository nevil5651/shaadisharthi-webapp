// hooks/useIntersectionObserver.ts
import { useEffect, useRef, useState } from 'react';

// Custom hook to observe intersection of an element with viewport
// Used for infinite scrolling and lazy loading
export const useIntersectionObserver = (options: IntersectionObserverInit = {}) => {
  const [isIntersecting, setIsIntersecting] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    // Create IntersectionObserver instance
    const observer = new IntersectionObserver(([entry]) => {
      setIsIntersecting(entry.isIntersecting);
    }, options);

    const element = ref.current;

    // Start observing the element
    if (element) {
      observer.observe(element);
    }

    // Cleanup: stop observing when component unmounts
    return () => {
      if (element) {
        observer.unobserve(element);
      }
    };
  }, [options]);

  return [ref, isIntersecting] as const;
};