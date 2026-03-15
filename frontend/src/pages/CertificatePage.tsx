import { useParams, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { getCertificate } from '../api/certificates';

const CertificatePage: React.FC = () => {
  const { certificateId } = useParams<{ certificateId: string }>();
  const navigate = useNavigate();

  const { data: cert, isLoading, isError } = useQuery({
    queryKey: ['certificate', certificateId],
    queryFn: () => getCertificate(certificateId!),
    enabled: !!certificateId,
  });

  if (isLoading) {
    return (
      <div className="max-w-2xl mx-auto px-4 py-12 animate-pulse">
        <div className="bg-white rounded-2xl p-8">
          <div className="h-6 bg-gray-200 rounded mb-4 w-1/2" />
          <div className="h-4 bg-gray-100 rounded mb-2 w-3/4" />
        </div>
      </div>
    );
  }

  if (isError || !cert) {
    return (
      <div className="max-w-2xl mx-auto px-4 py-12">
        <div className="bg-red-50 text-red-700 rounded-lg px-4 py-3">Certificate not found.</div>
      </div>
    );
  }

  const passed = cert.scorePercentage >= 50; // visual indicator

  return (
    <div className="max-w-2xl mx-auto px-4 py-8">
      <button onClick={() => navigate('/enrollments')} className="text-indigo-600 text-sm hover:underline mb-4 block">
        ← Back to enrollments
      </button>

      {/* Certificate card */}
      <div className="bg-white rounded-2xl shadow-lg border-2 border-indigo-200 p-8 text-center">
        <div className="text-5xl mb-3">🏅</div>
        <h1 className="text-2xl font-bold text-gray-900 mb-1">Certificate of Achievement</h1>
        <p className="text-gray-500 text-sm mb-6">This certifies that</p>

        <p className="text-xl font-semibold text-indigo-700 mb-1">{cert.studentName}</p>
        <p className="text-gray-500 text-sm mb-4">has successfully completed</p>

        <h2 className="text-lg font-bold text-gray-900 mb-6">{cert.examTitle}</h2>

        <div className="flex justify-center gap-8 mb-6">
          <div className="text-center">
            <div className={`text-3xl font-bold ${passed ? 'text-green-600' : 'text-red-600'}`}>
              {cert.scorePercentage.toFixed(1)}%
            </div>
            <div className="text-xs text-gray-400">Score</div>
          </div>
          <div className="text-center">
            <div className="text-3xl font-bold text-gray-700">
              {new Date(cert.issuedAt).toLocaleDateString()}
            </div>
            <div className="text-xs text-gray-400">Issued</div>
          </div>
        </div>

        <div className="bg-gray-50 rounded-lg px-4 py-2 mb-6 inline-block">
          <span className="text-xs text-gray-500">Verification Code: </span>
          <span className="text-xs font-mono font-bold text-gray-800">{cert.verifyCode}</span>
        </div>

        <div className="flex gap-3 justify-center">
          {cert.downloadUrl && (
            <a
              href={cert.downloadUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="bg-indigo-600 hover:bg-indigo-700 text-white px-5 py-2 rounded-lg text-sm font-medium"
            >
              Download PDF
            </a>
          )}
          <button
            onClick={() => navigate(`/verify/${cert.verifyCode}`)}
            className="border border-gray-300 hover:border-gray-400 text-gray-700 px-5 py-2 rounded-lg text-sm"
          >
            Verify Certificate
          </button>
        </div>
      </div>
    </div>
  );
};

export default CertificatePage;
