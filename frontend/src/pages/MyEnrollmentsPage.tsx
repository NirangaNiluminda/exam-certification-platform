import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { getMyEnrollments } from '../api/enrollments';
import { getCertificateByEnrollment } from '../api/certificates';
const statusColor: Record<string, string> = {
  PENDING: 'bg-yellow-100 text-yellow-700',
  PAYMENT_COMPLETED: 'bg-blue-100 text-blue-700',
  EXAM_SCHEDULED: 'bg-green-100 text-green-700',
  EXAM_SUBMITTED: 'bg-indigo-100 text-indigo-700',
  GRADED: 'bg-purple-100 text-purple-700',
  CERTIFIED: 'bg-emerald-100 text-emerald-700',
  FAILED: 'bg-red-100 text-red-700',
  CANCELLED: 'bg-gray-100 text-gray-500',
};

const CertificateButton: React.FC<{ enrollmentId: string }> = ({ enrollmentId }) => {
  const navigate = useNavigate();
  const { data: cert } = useQuery({
    queryKey: ['cert-by-enrollment', enrollmentId],
    queryFn: () => getCertificateByEnrollment(enrollmentId),
    retry: false,
  });
  if (!cert) return null;
  return (
    <button
      onClick={() => navigate(`/certificates/${cert.certificateId}`)}
      className="bg-emerald-600 hover:bg-emerald-700 text-white text-sm px-4 py-1.5 rounded-lg"
    >
      View Certificate
    </button>
  );
};

const MyEnrollmentsPage: React.FC = () => {
  const navigate = useNavigate();
  const { data: enrollments, isLoading, isError } = useQuery({
    queryKey: ['enrollments-me'],
    queryFn: getMyEnrollments,
    refetchInterval: 5000, // Poll every 5s — status updates via SAGA can take a moment
  });

  return (
    <div className="max-w-4xl mx-auto px-4 py-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-2">My Enrollments</h1>
      <p className="text-gray-500 text-sm mb-6">
        Enrollment status updates automatically as the payment and slot creation process completes.
      </p>

      {isLoading && (
        <div className="space-y-3">
          {[1, 2].map((i) => (
            <div key={i} className="bg-white rounded-xl border border-gray-200 p-5 animate-pulse">
              <div className="h-4 bg-gray-200 rounded w-1/3 mb-2" />
              <div className="h-3 bg-gray-100 rounded w-1/4" />
            </div>
          ))}
        </div>
      )}

      {isError && (
        <div className="bg-red-50 text-red-700 rounded-lg px-4 py-3 text-sm">
          Failed to load enrollments.
        </div>
      )}

      {enrollments && enrollments.length === 0 && (
        <div className="text-center py-16 text-gray-400">
          <div className="text-5xl mb-3">📚</div>
          <p className="mb-4">No enrollments yet.</p>
          <button onClick={() => navigate('/')} className="text-indigo-600 hover:underline text-sm">
            Browse available exams →
          </button>
        </div>
      )}

      {enrollments && enrollments.length > 0 && (
        <div className="space-y-4">
          {enrollments.map((enrollment) => (
            <div key={enrollment.id} className="bg-white rounded-xl border border-gray-200 p-5 flex items-center justify-between">
              <div>
                <div className="text-sm font-mono text-gray-400 mb-1">
                  Exam ID: {enrollment.examId}
                </div>
                <div className="text-xs text-gray-400">
                  Enrolled: {new Date(enrollment.enrolledAt).toLocaleString()}
                </div>
                {enrollment.slotId && (
                  <div className="text-xs text-indigo-600 mt-1">
                    Slot ready ✓
                  </div>
                )}
              </div>

              <div className="flex items-center gap-3">
                <span className={`text-xs font-semibold px-3 py-1 rounded-full ${statusColor[enrollment.status]}`}>
                  {enrollment.status}
                </span>

                {enrollment.status === 'EXAM_SCHEDULED' && enrollment.slotId && (
                  <button
                    onClick={() => navigate(`/exam/${enrollment.slotId}`)}
                    className="bg-indigo-600 hover:bg-indigo-700 text-white text-sm px-4 py-1.5 rounded-lg"
                  >
                    Take Exam
                  </button>
                )}
                {enrollment.status === 'CERTIFIED' && (
                  <CertificateButton enrollmentId={enrollment.id} />
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default MyEnrollmentsPage;
