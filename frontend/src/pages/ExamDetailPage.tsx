import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation } from '@tanstack/react-query';
import { getExam } from '../api/exams';
import { enroll } from '../api/enrollments';
import type { EnrollRequest } from '../types';

const ExamDetailPage: React.FC = () => {
  const { examId } = useParams<{ examId: string }>();
  const navigate = useNavigate();

  const { data: exam, isLoading, isError } = useQuery({
    queryKey: ['exam', examId],
    queryFn: () => getExam(examId!),
    enabled: !!examId,
  });

  const enrollMutation = useMutation({
    mutationFn: (data: EnrollRequest) => enroll(data),
    onSuccess: () => {
      navigate('/enrollments');
    },
  });

  const handleEnroll = () => {
    if (!exam) return;
    enrollMutation.mutate({ examId: exam.examId, examPrice: exam.price });
  };

  if (isLoading) {
    return (
      <div className="max-w-3xl mx-auto px-4 py-12 animate-pulse">
        <div className="h-8 bg-gray-200 rounded w-1/2 mb-4" />
        <div className="h-4 bg-gray-100 rounded mb-2" />
        <div className="h-4 bg-gray-100 rounded w-3/4" />
      </div>
    );
  }

  if (isError || !exam) {
    return (
      <div className="max-w-3xl mx-auto px-4 py-12">
        <div className="bg-red-50 text-red-700 rounded-lg px-4 py-3">Exam not found.</div>
      </div>
    );
  }

  return (
    <div className="max-w-3xl mx-auto px-4 py-8">
      <button onClick={() => navigate('/')} className="text-indigo-600 text-sm hover:underline mb-4 block">
        ← Back to exams
      </button>

      <div className="bg-white rounded-2xl shadow-sm border border-gray-200 p-8">
        <div className="flex items-start justify-between mb-4">
          <h1 className="text-2xl font-bold text-gray-900">{exam.title}</h1>
          <span className="text-2xl font-bold text-indigo-700">${exam.price?.toFixed(2)}</span>
        </div>

        <p className="text-gray-600 mb-6">{exam.description}</p>

        <div className="grid grid-cols-3 gap-4 mb-8">
          <div className="bg-indigo-50 rounded-xl p-4 text-center">
            <div className="text-2xl font-bold text-indigo-700">{exam.durationMinutes}</div>
            <div className="text-xs text-gray-500 mt-1">minutes</div>
          </div>
          <div className="bg-green-50 rounded-xl p-4 text-center">
            <div className="text-2xl font-bold text-green-700">{exam.passingScore}%</div>
            <div className="text-xs text-gray-500 mt-1">passing score</div>
          </div>
          <div className="bg-amber-50 rounded-xl p-4 text-center">
            <div className="text-2xl font-bold text-amber-700">{exam.totalMarks}</div>
            <div className="text-xs text-gray-500 mt-1">total marks</div>
          </div>
        </div>

        {enrollMutation.isError && (
          <p className="text-red-500 text-sm bg-red-50 rounded-lg px-3 py-2 mb-4">
            Enrollment failed. You may already be enrolled in this exam.
          </p>
        )}

        <button
          onClick={handleEnroll}
          disabled={enrollMutation.isPending}
          className="w-full bg-indigo-600 hover:bg-indigo-700 disabled:bg-indigo-300 text-white py-3 rounded-xl font-semibold text-base"
        >
          {enrollMutation.isPending ? 'Processing enrollment…' : `Enroll Now — $${exam.price?.toFixed(2)}`}
        </button>

        <p className="text-xs text-gray-400 text-center mt-3">
          Enrollment triggers payment processing. Your exam slot will be created automatically.
        </p>
      </div>
    </div>
  );
};

export default ExamDetailPage;
