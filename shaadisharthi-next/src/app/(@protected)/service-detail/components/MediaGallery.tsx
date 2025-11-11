import Image from 'next/image';
import { Media } from '@/lib/service';
import { useState } from 'react';

interface MediaGalleryProps {
  media: Media[];
}

// MediaGallery component to display images and videos in a grid layout
// Supports lazy loading and loading states for better user experience
export function MediaGallery({ media }: MediaGalleryProps) {
  // Track which images have loaded to show loading states
  const [loadedIndexes, setLoadedIndexes] = useState<Set<number>>(new Set());

  // Mark an image as loaded when it finishes loading
  const handleImageLoad = (index: number) => {
    setLoadedIndexes(prev => new Set(prev).add(index));
  };

  return (
    <section className="bg-white dark:bg-gray-800 rounded-xl shadow-md p-6 mb-8">
      <h2 className="text-2xl font-heading font-bold text-gray-900 dark:text-white mb-4">Media Gallery</h2>
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {media.map((item, index) => (
          <div key={`${item.url}-${index}`} className="relative aspect-video rounded-lg overflow-hidden bg-gray-200 dark:bg-gray-700">
            {item.type === 'Image' ? (
              <>
                {/* Show loading placeholder until image loads */}
                {!loadedIndexes.has(index) && (
                  <div className="absolute inset-0 bg-gray-300 dark:bg-gray-600 animate-pulse"></div>
                )}
                <Image
                  src={item.url}
                  alt={`${item.type} from ${item.url}`}
                  width={400}
                  height={300}
                  className="object-cover w-full h-full"
                  onLoadingComplete={() => handleImageLoad(index)}
                  placeholder="blur"
                  blurDataURL="data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAAYEBQYFBAYGBQYHBwYIChAKCgkJChQODwwQFxQYGBcUFhYaHSUfGhsjHBYWICwgIyYnKSopGR8tMC0oMCUoKSj/2wBDAQcHBwoIChMKChMoGhYaKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCj/wAARCAAIAAoDASIAAhEBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAv/xAAhEAACAQMDBQAAAAAAAAAAAAABAgMABAUGIWGRkqGx0f/EABUBAQEAAAAAAAAAAAAAAAAAAAMF/8QAGhEAAgIDAAAAAAAAAAAAAAAAAAECEgMRkf/aAAwDAQACEQMRAD8AltJagyeH0AthI5xdrLcNM91BF5pX2HaUMk3SgW0Lm6YQd4M/+9"
                />
              </>
            ) : item.type === 'Video' ? (
              // Video element with controls for user interaction
              <video controls className="w-full h-full" preload="metadata">
                <source src={item.url} type="video/mp4" />
                Your browser does not support the video tag.
              </video>
            ) : null}
          </div>
        ))}
      </div>
    </section>
  );
}