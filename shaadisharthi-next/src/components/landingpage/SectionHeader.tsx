interface SectionHeaderProps {
  subtitle: string;
  title: string;
}

const SectionHeader: React.FC<SectionHeaderProps> = ({ subtitle, title }) => {
  return (
    <div className="text-center mb-12">
      <span className="text-primary dark:text-pink-400 font-semibold">{subtitle}</span>
      <h2 className="text-3xl md:text-4xl font-bold font-playfair-display mt-2 text-gray-800 dark:text-white">{title}</h2>
      <div className="w-20 h-1 bg-primary dark:bg-pink-400 mx-auto mt-4"></div>
    </div>
  );
};

export default SectionHeader;