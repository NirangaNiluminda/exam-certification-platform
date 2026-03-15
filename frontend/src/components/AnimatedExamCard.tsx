import { motion } from 'framer-motion';
import ExamCard from './ExamCard';
import type { Exam } from '../types';

interface Props {
  exam: Exam;
  index: number;
}

const AnimatedExamCard: React.FC<Props> = ({ exam, index }) => {
  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.95 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ duration: 0.3, delay: index * 0.08 }}
      whileHover={{ y: -4, transition: { duration: 0.15 } }}
      whileTap={{ scale: 0.98 }}
    >
      <ExamCard exam={exam} />
    </motion.div>
  );
};

export default AnimatedExamCard;
