import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { adminListExams, adminDeleteExam } from '../../api/admin';

const AdminExamsPage: React.FC = () => {
  const navigate = useNavigate();
  const qc = useQueryClient();

  const { data: exams, isLoading } = useQuery({
    queryKey: ['admin-exams'],
    queryFn: adminListExams,
  });

  const deleteMutation = useMutation({
    mutationFn: adminDeleteExam,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin-exams'] }),
  });

  return (
    <div className="max-w-5xl mx-auto px-4 py-8">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Manage Exams</h1>
          <p className="text-gray-500 text-sm mt-1">Create, edit, and manage exam content</p>
        </div>
        <button
          onClick={() => navigate('/admin/exams/new')}
          className="bg-indigo-600 hover:bg-indigo-700 text-white px-4 py-2 rounded-lg text-sm font-medium"
        >
          + New Exam
        </button>
      </div>

      {isLoading && (
        <div className="space-y-3">
          {[1, 2, 3].map((i) => (
            <div key={i} className="bg-white rounded-xl border border-gray-200 h-16 animate-pulse" />
          ))}
        </div>
      )}

      {exams && (
        <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
          <table className="w-full text-sm">
            <thead className="bg-gray-50 border-b border-gray-200">
              <tr>
                <th className="text-left px-4 py-3 text-gray-600 font-medium">Title</th>
                <th className="text-left px-4 py-3 text-gray-600 font-medium">Duration</th>
                <th className="text-left px-4 py-3 text-gray-600 font-medium">Pass %</th>
                <th className="text-left px-4 py-3 text-gray-600 font-medium">Price</th>
                <th className="text-left px-4 py-3 text-gray-600 font-medium">Status</th>
                <th className="text-right px-4 py-3 text-gray-600 font-medium">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {exams.map((exam) => (
                <tr key={exam.examId} className="hover:bg-gray-50">
                  <td className="px-4 py-3 font-medium text-gray-900">{exam.title}</td>
                  <td className="px-4 py-3 text-gray-500">{exam.durationMinutes} min</td>
                  <td className="px-4 py-3 text-gray-500">{exam.passingScore}%</td>
                  <td className="px-4 py-3 text-gray-500">${exam.price?.toFixed(2)}</td>
                  <td className="px-4 py-3">
                    <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${
                      exam.active !== false ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'
                    }`}>
                      {exam.active !== false ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-right">
                    <div className="flex items-center justify-end gap-2">
                      <button
                        onClick={() => navigate(`/admin/exams/${exam.examId}/questions`)}
                        className="text-indigo-600 hover:underline text-xs"
                      >
                        Questions
                      </button>
                      <button
                        onClick={() => navigate(`/admin/exams/${exam.examId}/edit`)}
                        className="text-blue-600 hover:underline text-xs"
                      >
                        Edit
                      </button>
                      <button
                        onClick={() => {
                          if (confirm(`Deactivate "${exam.title}"?`)) {
                            deleteMutation.mutate(exam.examId);
                          }
                        }}
                        className="text-red-500 hover:underline text-xs"
                      >
                        Deactivate
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};

export default AdminExamsPage;
