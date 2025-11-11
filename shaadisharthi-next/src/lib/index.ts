import { IconDefinition } from '@fortawesome/fontawesome-svg-core';

export interface Service {
  title: string;
  description: string;
  image: string;
  alt: string;
  icon: IconDefinition;
}

export interface Testimonial {
  images: string[];
  name: string;
  location: string;
  quote: string;
  rating: number;
}

export interface Stat {
  number: string;
  label: string;
  color: string;
}

export interface Step {
  number: number;
  title: string;
  description: string;
}