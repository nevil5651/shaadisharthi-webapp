export interface User {
  id: number;
  name: string;
  email: string;
  phone: string;
  createdAt: string;
}

export interface Service {
  providerId: string;
  serviceId: number;
  serviceName: string;
  description: string;
  price: number;
  rating: number;
  reviewCount: number;
  location: string;
  imageUrl: string;
  category: string;
  businessName: string;
  email: string | null;
  phone: string | null;
}

export interface Booking {
  id: number;
  serviceId: number;
  serviceName: string;
  providerName: string;
  eventDate: string;
  eventTime: string;
  totalAmount: number;
  status: 'Requested' | 'Accepted' | 'Completed' | 'Cancelled';
}

export interface Notification {
  id: number;
  message: string;
  createdAt: string;
  read: boolean;
  type: string;
}