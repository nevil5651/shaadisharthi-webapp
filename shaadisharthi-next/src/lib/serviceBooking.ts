export interface BookingFormData {
  service_id: number;
  service_name: string;
  service_price: number;
  customer_name: string;
  phone: string;
  email: string;
  event_address: string;
  event_start_date: string;
  event_time: string;
  event_end_date?: string;
  notes?: string;
}