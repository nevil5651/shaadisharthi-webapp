'use client';

import { useState, useEffect, useCallback } from 'react';
import { RatingStars } from '@/app/(@protected)/services/components/RatingStars';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useForm } from 'react-hook-form';
import { Review } from '@/lib/service';
import { toast } from 'react-toastify';
import api from '@/lib/axios';
import { isAxiosError } from 'axios';

// Review form validation schema using Zod
const reviewSchema = z.object({
    rating: z.number().min(1, "Please select a rating").max(5, "Rating cannot exceed 5"),
    reviewText: z.string().min(10, "Review must be at least 10 characters").max(500, "Review cannot exceed 500 characters"),
});

type ReviewFormData = z.infer<typeof reviewSchema>;

interface ReviewFormProps {
    serviceId: number;
    onSubmitSuccess?: (newReview: Review | null) => void; 
    onServerUpdate?: (serverReviews: Review[]) => void;
}

interface ServerReview {
    reviewId: number | string;
    customerName: string;
    rating: number;
    reviewText: string;
    createdAt: string;
}

// ReviewForm component handles review submission with validation,
// optimistic updates, and error handling
export function ReviewForm({ serviceId, onSubmitSuccess, onServerUpdate }: ReviewFormProps) {
   const {
        register,
        handleSubmit,
        setValue,
        watch,
        reset,
        formState: { errors, isSubmitting },
    } = useForm<ReviewFormData>({
        resolver: zodResolver(reviewSchema),
        defaultValues: { rating: 0, reviewText: '' },
    });

    // Disable form if service ID is invalid
    const [isFormDisabled, setIsFormDisabled] = useState(!serviceId);

    useEffect(() => {
        if (!serviceId) {
            toast.error('Invalid service ID. Cannot submit review.');
            setIsFormDisabled(true);
        } else {
            setIsFormDisabled(false);
        }
    }, [serviceId]);

    // Watch form values for real-time validation feedback
    const reviewText = watch('reviewText');
    const currentRating = watch('rating');
    const maxCharacters = 500;
    const remainingCharacters = maxCharacters - (reviewText?.length || 0);

    // Handle form submission with optimistic updates
    const onSubmit = useCallback(async (data: ReviewFormData) => {
        // Create optimistic review for immediate UI update
        const optimisticReview: Review = {
            reviewId: `temp-${Date.now()}`,
            customerName: 'You', // Replace with actual user name from auth context
            rating: data.rating,
            text: data.reviewText,
            createdAt: new Date().toISOString(),
        };

        // Update UI optimistically (optional)
        onSubmitSuccess?.(optimisticReview);

        try {
            // Submit review to server
            const res = await api.post(`/Customer/reviews/${serviceId}`, {
                reviewText: data.reviewText,
                rating: data.rating
            });

            // Server returns updated page-1 array of reviews
            const serverData: ServerReview[] = res.data;
            const serverReviews: Review[] = serverData.map((r) => ({
                reviewId: String(r.reviewId),
                customerName: r.customerName,
                rating: r.rating,
                text: r.reviewText,
                createdAt: new Date(r.createdAt).toISOString(),
            }));

            // Notify parent with server authoritative list to replace optimistic data
            onServerUpdate?.(serverReviews);

            toast.success('Review submitted successfully!');
            reset(); // Reset form after successful submission
        } catch (error) {
            let errorMessage = 'Failed to submit review. Please try again.';
            if (isAxiosError(error)) {
                errorMessage = error.response?.data?.error || error.message;
            } else if (error instanceof Error) {
                errorMessage = error.message;
            }
            toast.error(errorMessage);
            console.error('Error submitting review:', error);
            // Remove optimistic item on error
            onSubmitSuccess?.(null);
        }
    }, [serviceId, reset, onSubmitSuccess, onServerUpdate]);

    return (
        <div className="bg-white dark:bg-gray-800 rounded-xl shadow-md p-6">
            <h3 className="text-xl font-heading font-bold text-gray-900 dark:text-white mb-4">Write a Review</h3>
            <form onSubmit={handleSubmit(onSubmit)}>
                <input type="hidden" {...register('rating')} />
                <div className="mb-4">
                    <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">Your Rating</label>
                    <RatingStars 
                        rating={currentRating} 
                        interactive 
                        setRating={(rating) => setValue('rating', rating, { shouldValidate: true })} 
                    />
                    {errors.rating && (
                        <p className="mt-1 text-sm text-red-600 dark:text-red-400">{errors.rating.message}</p>
                    )}
                </div>
                <div className="mb-4">
                    <label htmlFor="reviewText" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                        Your Review
                    </label>
                    <textarea
                        id="reviewText"
                        {...register('reviewText')}
                        rows={4}
                        maxLength={maxCharacters}
                        className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-primary focus:border-primary transition-all bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                        placeholder="Share your experience..."
                        disabled={isFormDisabled}
                    />
                    <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
                        {remainingCharacters} characters remaining
                    </p>
                    {errors.reviewText && (
                        <p className="mt-1 text-sm text-red-600 dark:text-red-400">{errors.reviewText.message}</p>
                    )}
                </div>
                <button
                    type="submit"
                    disabled={isSubmitting || isFormDisabled}
                    className={`gradient-btn ${isFormDisabled ? 'opacity-50 cursor-not-allowed' : ''}`}
                >
                    {isSubmitting ? 'Submitting...' : 'Submit Review'}
                </button>
            </form>
        </div>
    );
}