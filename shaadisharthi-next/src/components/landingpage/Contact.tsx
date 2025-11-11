// components/Contact.tsx
'use client';

import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';
import api from '../../lib/axios';
import toast from 'react-hot-toast';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faPaperPlane, faMapMarkerAlt, faPhone, faEnvelope, faClock } from '@fortawesome/free-solid-svg-icons';
import { faFacebookF, faInstagram, faPinterestP, faYoutube } from '@fortawesome/free-brands-svg-icons';
import SectionHeader from './SectionHeader';
import { Toaster } from 'react-hot-toast';

const schema = z.object({
  name: z.string().min(1, 'Full Name is required'),
  email: z.string().email('Invalid email address'),
  subject: z.string().min(1, 'Subject is required').max(40, 'Subject cannot exceed 40 characters'),
  message: z.string().min(1, 'Message is required').max(400, 'Message cannot exceed 400 characters'),
});

type FormData = z.infer<typeof schema>;

const Contact = () => {
  const { register, handleSubmit, formState: { errors, isSubmitting }, reset } = useForm<FormData>({
    resolver: zodResolver(schema),
  });

  const onSubmit = async (data: FormData) => {
    try {
      await api.post('/Customer/AddGuestQuery', data, { timeout: 5000 });
      toast.success('Message sent successfully! You will be Notified via email soon.');
      reset();
    } catch  {
      toast.error('Failed to send message. Please try again.');
    }
  };

  return (
    <section id="contact" className="py-16 bg-gray-50 dark:bg-gray-900">
      <Toaster position="top-right" />
      <div className="container mx-auto px-4">
        <SectionHeader subtitle="GET IN TOUCH" title="We'd Love to Hear From You" />
        <div className="flex flex-col md:flex-row gap-8">
          <div className="md:w-1/2 bg-white dark:bg-gray-800 p-8 rounded-xl shadow-md">
            <h3 className="text-2xl font-bold mb-6 font-playfair-display text-gray-800 dark:text-white">Send us a Message</h3>
            <form onSubmit={handleSubmit(onSubmit)}>
              <div className="mb-4">
                <label htmlFor="name" className="block text-gray-700 dark:text-gray-300 mb-2 font-medium">Full Name</label>
                <input id="name" {...register('name')} className="w-full px-4 py-3 border border-gray-300 dark:border-gray-600 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary dark:focus:ring-pink-400 focus:border-transparent bg-white dark:bg-gray-700 text-gray-800 dark:text-white" />
                {errors.name && <p className="text-red-500 dark:text-red-400 text-sm mt-1">{errors.name.message}</p>}
              </div>
              <div className="mb-4">
                <label htmlFor="email" className="block text-gray-700 dark:text-gray-300 mb-2 font-medium">Email Address</label>
                <input id="email" type="email" {...register('email')} className="w-full px-4 py-3 border border-gray-300 dark:border-gray-600 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary dark:focus:ring-pink-400 focus:border-transparent bg-white dark:bg-gray-700 text-gray-800 dark:text-white" />
                {errors.email && <p className="text-red-500 dark:text-red-400 text-sm mt-1">{errors.email.message}</p>}
              </div>
              <div className="mb-4">
                <label htmlFor="subject" className="block text-gray-700 dark:text-gray-300 mb-2 font-medium">Subject</label>
                <input id="subject" {...register('subject')} maxLength={100} className="w-full px-4 py-3 border border-gray-300 dark:border-gray-600 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary dark:focus:ring-pink-400 focus:border-transparent bg-white dark:bg-gray-700 text-gray-800 dark:text-white" />
                {errors.subject && <p className="text-red-500 dark:text-red-400 text-sm mt-1">{errors.subject.message}</p>}
              </div>
              <div className="mb-4">
                <label htmlFor="message" className="block text-gray-700 dark:text-gray-300 mb-2 font-medium">Message</label>
                <textarea id="message" rows={5} {...register('message')} maxLength={1000} className="w-full px-4 py-3 border border-gray-300 dark:border-gray-600 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary dark:focus:ring-pink-400 focus:border-transparent bg-white dark:bg-gray-700 text-gray-800 dark:text-white" />
                {errors.message && <p className="text-red-500 dark:text-red-400 text-sm mt-1">{errors.message.message}</p>}
              </div>
              <button type="submit" disabled={isSubmitting} className="w-full bg-gradient-to-r from-pink-600 to-orange-500 dark:from-pink-500 dark:to-orange-400 text-white py-3 rounded-lg transition duration-300 hover:from-pink-700 hover:to-orange-600 dark:hover:from-pink-600 dark:hover:to-orange-500 disabled:opacity-50">
                {isSubmitting ? 'Sending...' : 'Send Message'} <FontAwesomeIcon icon={faPaperPlane} className="ml-2" />
              </button>
            </form>
          </div>
          <div className="md:w-1/2 bg-white dark:bg-gray-800 p-8 rounded-xl shadow-md">
            <h3 className="text-2xl font-bold mb-6 font-playfair-display text-gray-800 dark:text-white">Contact Information</h3>
            <div className="space-y-6">
              <div className="flex items-start">
                <div className="bg-primary dark:bg-pink-600 bg-opacity-10 dark:bg-opacity-20 p-3 rounded-full mr-4">
                  <FontAwesomeIcon icon={faMapMarkerAlt} className="text-white" />
                </div>
                <div>
                  <h4 className="font-bold text-gray-800 dark:text-white">Our Location</h4>
                  <p className="text-gray-600 dark:text-gray-400">ShaadiSharthi Headquarters, 123 Wedding Street, Mumbai 400001, India</p>
                </div>
              </div>
              <div className="flex items-start">
                <div className="bg-primary dark:bg-pink-600 bg-opacity-10 dark:bg-opacity-20 p-3 rounded-full mr-4">
                  <FontAwesomeIcon icon={faPhone} className="text-white" />
                </div>
                <div>
                  <h4 className="font-bold text-gray-800 dark:text-white">Phone Number</h4>
                  <p className="text-gray-600 dark:text-gray-400">+91 98765 43210 (10AM - 7PM)</p>
                  <p className="text-gray-600 dark:text-gray-400">+91 87654 32109 (Emergency)</p>
                </div>
              </div>
              <div className="flex items-start">
                <div className="bg-primary dark:bg-pink-600 bg-opacity-10 dark:bg-opacity-20 p-3 rounded-full mr-4">
                  <FontAwesomeIcon icon={faEnvelope} className="text-white" />
                </div>
                <div>
                  <h4 className="font-bold text-gray-800 dark:text-white">Email Address</h4>
                  <p className="text-gray-600 dark:text-gray-400">info@shaadisharthi.com</p>
                  <p className="text-gray-600 dark:text-gray-400">support@shaadisharthi.com</p>
                </div>
              </div>
              <div className="flex items-start">
                <div className="bg-primary dark:bg-pink-600 bg-opacity-10 dark:bg-opacity-20 p-3 rounded-full mr-4">
                  <FontAwesomeIcon icon={faClock} className="text-white" />
                </div>
                <div>
                  <h4 className="font-bold text-gray-800 dark:text-white">Working Hours</h4>
                  <p className="text-gray-600 dark:text-gray-400">Monday - Saturday: 9:00 AM - 7:00 PM</p>
                  <p className="text-gray-600 dark:text-gray-400">Sunday: Emergency Support Only</p>
                </div>
              </div>
            </div>
            <div className="mt-8">
              <h4 className="font-bold mb-4 font-playfair-display text-gray-800 dark:text-white">Follow Us</h4>
              <div className="flex space-x-4">
                <a href="#" className="bg-primary dark:bg-pink-600 bg-opacity-10 dark:bg-opacity-20 hover:bg-primary dark:hover:bg-pink-600 p-3 rounded-full transition duration-300 group">
                  <FontAwesomeIcon icon={faFacebookF} className="text-white" />
                </a>
                <a href="#" className="bg-primary dark:bg-pink-600 bg-opacity-10 dark:bg-opacity-20 hover:bg-primary dark:hover:bg-pink-600 p-3 rounded-full transition duration-300 group">
                  <FontAwesomeIcon icon={faInstagram} className="text-white" />
                </a>
                <a href="#" className="bg-primary dark:bg-pink-600 bg-opacity-10 dark:bg-opacity-20 hover:bg-primary dark:hover:bg-pink-600 p-3 rounded-full transition duration-300 group">
                  <FontAwesomeIcon icon={faPinterestP} className="text-white" />
                </a>
                <a href="#" className="bg-primary dark:bg-pink-600 bg-opacity-10 dark:bg-opacity-20 hover:bg-primary dark:hover:bg-pink-600 p-3 rounded-full transition duration-300 group">
                  <FontAwesomeIcon icon={faYoutube} className="text-white" />
                </a>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};

export default Contact;