import { Stat } from '@/lib/index';

const Stats = () => {
  const stats: Stat[] = [
    { number: '5,000+', label: 'Weddings Planned', color: 'text-primary dark:text-pink-400' },
    { number: '1,200+', label: 'Verified Vendors', color: 'text-secondary dark:text-orange-400' },
    { number: '50+', label: 'Cities Covered', color: 'text-accent dark:text-blue-400' },
    { number: '98%', label: 'Happy Couples', color: 'text-primary dark:text-pink-400' },
  ];

  return (
    <div className="container mx-auto px-4 -mt-16 relative z-10">
      <div className="bg-white dark:bg-gray-800 rounded-xl shadow-xl p-6 grid grid-cols-2 md:grid-cols-4 gap-4">
        {stats.map((stat, index) => (
          <div key={index} className="text-center">
            <div className={`text-3xl font-bold ${stat.color} mb-2`}>{stat.number}</div>
            <div className="text-gray-600 dark:text-gray-300">{stat.label}</div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default Stats;
