import { useQuery } from '@tanstack/react-query';
import { motion } from 'framer-motion';
import { useNavigate } from 'react-router-dom';
import { listExams } from '../api/exams';
import { getMyEnrollments } from '../api/enrollments';
import { useAuth } from '../context/AuthContext';
import StatCard from '../components/StatCard';
import AnimatedExamCard from '../components/AnimatedExamCard';

const StudentDashboardPage: React.FC = () => {
  const { user } = useAuth();
  const navigate = useNavigate();

  const { data: exams, isLoading: examsLoading, isError: examsError } = useQuery({
    queryKey: ['exams'],
    queryFn: listExams,
  });

  const { data: enrollments, isLoading: enrollmentsLoading } = useQuery({
    queryKey: ['enrollments-me'],
    queryFn: getMyEnrollments,
  });

  const isLoading = examsLoading || enrollmentsLoading;
  const availableCount = exams?.length ?? 0;
  const enrolledCount = enrollments?.length ?? 0;
  const confirmedCount = enrollments?.filter((e) => e.status === 'CONFIRMED').length ?? 0;

  return (
    <motion.div
      className="max-w-6xl mx-auto px-4 py-8"
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4 }}
    >
      {/* Welcome Banner */}
      <div className="bg-gradient-to-r from-indigo-600 to-indigo-800 rounded-2xl p-8 text-white mb-8">
        <h1 className="text-3xl font-bold mb-1">Welcome back!</h1>
        <p className="text-indigo-200 text-sm mb-2">{user?.email}</p>
        <p className="text-indigo-100">
          Track your progress and explore new certification exams below.
        </p>
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
            icon="📋"
            label="Available Exams"
            value={availableCount}
            color="bg-indigo-50"
            textColor="text-indigo-700"
            delay={0}
          />
          <StatCard
            icon="📚"
            label="Total Enrolled"
            value={enrolledCount}
            color="bg-amber-50"
            textColor="text-amber-700"
            delay={0.1}
          />
          <StatCard
            icon="✅"
            label="Confirmed Enrollments"
            value={confirmedCount}
            color="bg-green-50"
            textColor="text-green-700"
            delay={0.2}
          />
        </div>
      )}

      {/* Section Header */}
      <div className="flex items-center justify-between mb-5">
        <h2 className="text-xl font-bold text-gray-900">Available Exams</h2>
        <button
          onClick={() => navigate('/enrollments')}
          className="text-indigo-600 hover:underline text-sm font-medium"
        >
          My Enrollments →
        </button>
      </div>

      {/* Error State */}
      {examsError && (
        <div className="bg-red-50 text-red-700 rounded-lg px-4 py-3 text-sm">
          Failed to load exams. Please refresh the page.
        </div>
      )}

      {/* Empty State */}
      {!examsLoading && !examsError && exams?.length === 0 && (
        <div className="text-center py-16 text-gray-400">
          <div className="text-5xl mb-3">📋</div>
          <p>No exams available yet.</p>
        </div>
      )}

      {/* Animated Exam Grid */}
      {!examsLoading && exams && exams.length > 0 && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {exams.map((exam, index) => (
            <AnimatedExamCard key={exam.examId} exam={exam} index={index} />
          ))}
        </div>
      )}

      {/* Loading Skeleton for Exam Grid */}
      {examsLoading && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {[1, 2, 3].map((i) => (
            <div
              key={i}
              className="bg-white rounded-xl border border-gray-200 p-6 animate-pulse"
            >
              <div className="h-5 bg-gray-200 rounded mb-3 w-3/4" />
              <div className="h-3 bg-gray-100 rounded mb-2" />
              <div className="h-3 bg-gray-100 rounded w-2/3" />
            </div>
          ))}
        </div>
      )}
    </motion.div>
  );
};

export default StudentDashboardPage;
