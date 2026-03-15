import { useEffect, useState } from 'react';
import { motion } from 'framer-motion';

interface StatCardProps {
  icon: string;
  label: string;
  value: number;
  color: string;
  textColor: string;
  delay?: number;
}

const StatCard: React.FC<StatCardProps> = ({ icon, label, value, color, textColor, delay = 0 }) => {
  const [displayValue, setDisplayValue] = useState(0);

  useEffect(() => {
    if (value === 0) {
      setDisplayValue(0);
      return;
    }
    let current = 0;
    const step = Math.max(1, Math.ceil(value / 30));
    const id = setInterval(() => {
      current += step;
      if (current >= value) {
        setDisplayValue(value);
        clearInterval(id);
      } else {
        setDisplayValue(current);
      }
    }, 20);
    return () => clearInterval(id);
  }, [value]);

  return (
    <motion.div
      className={`${color} rounded-2xl p-6 flex flex-col gap-2`}
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4, delay }}
      whileHover={{ scale: 1.03, transition: { duration: 0.15 } }}
    >
      <span className="text-3xl">{icon}</span>
      <div className={`text-4xl font-bold ${textColor}`}>{displayValue}</div>
      <div className="text-sm text-gray-600">{label}</div>
    </motion.div>
  );
};

export default StatCard;
