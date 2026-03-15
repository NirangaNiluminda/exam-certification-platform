import { useQuery } from '@tanstack/react-query';
import { motion } from 'framer-motion';
import { useNavigate } from 'react-router-dom';
import { adminListExams } from '../../api/admin';
import StatCard from '../../components/StatCard';

const AdminDashboardPage: React.FC = () => {
  const navigate = useNavigate();

  const { data: exams, isLoading } = useQuery({
    queryKey: ['admin-exams'],
    queryFn: adminListExams,
  });

  const totalExams = exams?.length ?? 0;
  const activeCount = exams?.filter((e) => e.active !== false).length ?? 0;
  const inactiveCount = exams?.filter((e) => e.active === false).length ?? 0;
  const recentExams = exams?.slice(0, 5) ?? [];

  return (
    <motion.div
      className="max-w-5xl mx-auto px-4 py-8"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      transition={{ duration: 0.4 }}
    >
      {/* Header */}
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900">Admin Dashboard</h1>
        <p className="text-gray-500 mt-1">Manage your exam platform</p>
      </div>

      {/* Stat Cards */}
      {isLoading ? (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-8">
          {[1, 2, 3].map((i) => (
            <div key={i} className="bg-gray-100 rounded-2xl p-6 h-32 animate-pulse" />
          ))}
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-8">
          <StatCard
            icon="🗂"
            label="Total Exams"
            value={totalExams}
            color="bg-indigo-50"
            textColor="text-indigo-700"
            delay={0}
          />
          <StatCard
            icon="✅"
            label="Active Exams"
            value={activeCount}
            color="bg-green-50"
            textColor="text-green-700"
            delay={0.1}
          />
          <StatCard
            icon="⏸"
            label="Inactive Exams"
            value={inactiveCount}
            color="bg-gray-100"
            textColor="text-gray-600"
            delay={0.2}
          />
        </div>
      )}

      {/* Quick Actions */}
      <h2 className="text-lg font-semibold text-gray-800 mb-3">Quick Actions</h2>
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-8">
        <motion.button
          className="bg-indigo-600 text-white rounded-2xl p-6 text-left hover:bg-indigo-700 transition-colors w-full"
          onClick={() => navigate('/admin/exams/new')}
          initial={{ opacity: 0, x: -10 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.35, delay: 0.3 }}
          whileHover={{ scale: 1.02 }}
          whileTap={{ scale: 0.98 }}
        >
          <div className="text-3xl mb-3">➕</div>
          <div className="text-lg font-semibold">Create New Exam</div>
          <div className="text-indigo-200 text-sm mt-1">Add a new exam to the platform</div>
        </motion.button>

        <motion.button
          className="bg-white border-2 border-indigo-100 text-indigo-700 rounded-2xl p-6 text-left hover:bg-indigo-50 transition-colors w-full"
          onClick={() => navigate('/admin/exams')}
          initial={{ opacity: 0, x: 10 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.35, delay: 0.4 }}
          whileHover={{ scale: 1.02 }}
          whileTap={{ scale: 0.98 }}
        >
          <div className="text-3xl mb-3">📋</div>
          <div className="text-lg font-semibold">Manage All Exams</div>
          <div className="text-indigo-400 text-sm mt-1">View, edit, and deactivate exams</div>
        </motion.button>
      </div>

      {/* Recent Exams */}
      <div className="flex items-center justify-between mb-3">
        <h2 className="text-lg font-semibold text-gray-800">Recent Exams</h2>
        <button
          onClick={() => navigate('/admin/exams')}
          className="text-indigo-600 hover:underline text-sm"
        >
          View all →
        </button>
      </div>

      {isLoading && (
        <div className="space-y-3">
          {[1, 2, 3].map((i) => (
            <div
              key={i}
              className="bg-white rounded-xl border border-gray-200 h-12 animate-pulse"
            />
          ))}
        </div>
      )}

      {!isLoading && recentExams.length > 0 && (
        <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
          <table className="w-full text-sm">
            <thead className="bg-gray-50 border-b border-gray-200">
              <tr>
                <th className="text-left px-4 py-3 text-gray-600 font-medium">Title</th>
                <th className="text-left px-4 py-3 text-gray-600 font-medium">Duration</th>
                <th className="text-left px-4 py-3 text-gray-600 font-medium">Status</th>
                <th className="text-right px-4 py-3 text-gray-600 font-medium">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {recentExams.map((exam, index) => (
                <motion.tr
                  key={exam.examId}
                  className="hover:bg-gray-50"
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  transition={{ duration: 0.2, delay: 0.5 + index * 0.05 }}
                >
                  <td className="px-4 py-3 font-medium text-gray-900">{exam.title}</td>
                  <td className="px-4 py-3 text-gray-500">{exam.durationMinutes} min</td>
                  <td className="px-4 py-3">
                    <span
                      className={`text-xs px-2 py-0.5 rounded-full font-medium ${
                        exam.active !== false
                          ? 'bg-green-100 text-green-700'
                          : 'bg-gray-100 text-gray-500'
                      }`}
                    >
                      {exam.active !== false ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-right">
                    <button
                      onClick={() => navigate(`/admin/exams/${exam.examId}/edit`)}
                      className="text-indigo-600 hover:underline text-xs"
                    >
                      Edit
                    </button>
                  </td>
                </motion.tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {!isLoading && recentExams.length === 0 && (
        <div className="text-center py-12 text-gray-400">
          <div className="text-4xl mb-2">📋</div>
          <p className="text-sm">No exams yet. Create your first exam!</p>
        </div>
      )}
    </motion.div>
  );
};

export default AdminDashboardPage;
