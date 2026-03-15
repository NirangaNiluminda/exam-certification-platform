import { useNavigate } from 'react-router-dom';
import type { Exam } from '../types';

interface Props {
  exam: Exam;
}

const ExamCard: React.FC<Props> = ({ exam }) => {
  const navigate = useNavigate();

  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 flex flex-col gap-3 hover:shadow-md transition-shadow">
      <div className="flex items-start justify-between">
        <h3 className="text-lg font-semibold text-gray-900">{exam.title}</h3>
        <span className="text-indigo-700 font-bold text-sm whitespace-nowrap ml-2">
          ${exam.price?.toFixed(2)}
        </span>
      </div>

      <p className="text-gray-500 text-sm line-clamp-2">{exam.description}</p>

      <div className="flex gap-4 text-xs text-gray-400">
        <span>⏱ {exam.durationMinutes} min</span>
        <span>📊 Pass: {exam.passingScore}%</span>
        <span>🏆 {exam.totalMarks} marks</span>
      </div>

      <button
        onClick={() => navigate(`/exams/${exam.examId}`)}
        className="mt-auto bg-indigo-600 hover:bg-indigo-700 text-white py-2 px-4 rounded-lg text-sm font-medium"
      >
        View & Enroll
      </button>
    </div>
  );
};

export default ExamCard;
