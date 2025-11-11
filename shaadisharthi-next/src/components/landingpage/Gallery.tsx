// components/Gallery.tsx
import Image from 'next/image';

const Gallery = () => {
  const images = [
    'https://images.unsplash.com/photo-1515934751635-c81c6bc9a2d8?ixlib=rb-1.2.1&auto=format&fit=crop&w=800&q=80',
    'https://images.unsplash.com/photo-1519225421980-715cb0215aed?ixlib=rb-1.2.1&auto=format&fit=crop&w=800&q=80',
    'https://images.unsplash.com/photo-1513151233558-d860c5398176?ixlib=rb-1.2.1&auto=format&fit=crop&w=800&q=80',
    'https://images.unsplash.com/photo-1523438885200-e635ba2c371e?ixlib=rb-1.2.1&auto=format&fit=crop&w=800&q=80',
  ];

  return (
    <section className="py-0 bg-white dark:bg-gray-900">
      <div className="grid grid-cols-2 md:grid-cols-4">
        {images.map((src, index) => (
          <div key={index} className="h-48 md:h-64 overflow-hidden">
            <Image src={src} alt="Wedding" width={800} height={480} className="w-full h-full object-cover hover:scale-105 transition duration-500" loading="lazy" />
          </div>
        ))}
      </div>
    </section>
  );
};

export default Gallery;