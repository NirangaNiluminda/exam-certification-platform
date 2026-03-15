import { useQuery } from '@tanstack/react-query';
import { listExams } from '../api/exams';
import ExamCard from '../components/ExamCard';

const DashboardPage: React.FC = () => {
  const { data: exams, isLoading, isError } = useQuery({
    queryKey: ['exams'],
    queryFn: listExams,
  });

  return (
    <div className="max-w-6xl mx-auto px-4 py-8">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Available Exams</h1>
        <p className="text-gray-500 text-sm mt-1">
          Enroll in an exam to earn your certification
        </p>
      </div>

      {isLoading && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {[1, 2, 3].map((i) => (
            <div key={i} className="bg-white rounded-xl border border-gray-200 p-6 animate-pulse">
              <div className="h-5 bg-gray-200 rounded mb-3 w-3/4" />
              <div className="h-3 bg-gray-100 rounded mb-2" />
              <div className="h-3 bg-gray-100 rounded w-2/3" />
            </div>
          ))}
        </div>
      )}

      {isError && (
        <div className="bg-red-50 text-red-700 rounded-lg px-4 py-3 text-sm">
          Failed to load exams. Please refresh the page.
        </div>
      )}

      {exams && exams.length === 0 && (
        <div className="text-center py-16 text-gray-400">
          <div className="text-5xl mb-3">📋</div>
          <p>No exams available yet.</p>
        </div>
      )}

      {exams && exams.length > 0 && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {exams.map((exam) => (
            <ExamCard key={exam.examId} exam={exam} />
          ))}
        </div>
      )}
    </div>
  );
};

export default DashboardPage;
