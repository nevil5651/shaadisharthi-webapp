
export interface Booking {
  id: string;
  serviceName: string;
  serviceImage: string;
  providerName: string;
  date: string;
  time: string;
  amount: number;
  status: 'pending' | 'confirmed' | 'cancelled' | 'completed';
  paymentStatus: 'paid' | 'unpaid';
}
