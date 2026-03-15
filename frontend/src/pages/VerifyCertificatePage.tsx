import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { verifyCertificate } from '../api/certificates';

const VerifyCertificatePage: React.FC = () => {
  const { verifyCode: routeCode } = useParams<{ verifyCode?: string }>();
  const navigate = useNavigate();
  const [inputCode, setInputCode] = useState(routeCode ?? '');
  const [searchCode, setSearchCode] = useState(routeCode ?? '');

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['verify', searchCode],
    queryFn: () => verifyCertificate(searchCode),
    enabled: !!searchCode,
  });

  const handleSearch = () => {
    setSearchCode(inputCode.trim());
    if (inputCode.trim()) refetch();
  };

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center px-4">
      <div className="w-full max-w-md">
        <div className="text-center mb-6">
          <div className="text-5xl mb-2">🔍</div>
          <h1 className="text-2xl font-bold text-gray-900">Verify Certificate</h1>
          <p className="text-gray-500 text-sm mt-1">
            Enter a verification code to validate a certificate
          </p>
        </div>

        <div className="bg-white rounded-2xl shadow-sm border border-gray-200 p-6">
          <div className="flex gap-2 mb-4">
            <input
              type="text"
              value={inputCode}
              onChange={(e) => setInputCode(e.target.value)}
              placeholder="e.g. CERT-XXXXX"
              className="flex-1 border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 font-mono"
              onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
            />
            <button
              onClick={handleSearch}
              disabled={isLoading}
              className="bg-indigo-600 hover:bg-indigo-700 text-white px-4 py-2 rounded-lg text-sm font-medium"
            >
              Verify
            </button>
          </div>

          {isLoading && (
            <div className="text-center text-gray-400 text-sm py-4">Checking…</div>
          )}

          {isError && (
            <div className="bg-red-50 rounded-xl p-4 text-center">
              <div className="text-3xl mb-2">❌</div>
              <p className="text-red-700 font-medium">Invalid verification code</p>
              <p className="text-red-500 text-sm mt-1">No certificate found for this code.</p>
            </div>
          )}

          {data && (
            <div className={`rounded-xl p-4 ${data.valid ? 'bg-green-50' : 'bg-red-50'}`}>
              <div className="text-center mb-3">
                <div className="text-3xl mb-1">{data.valid ? '✅' : '❌'}</div>
                <p className={`font-bold ${data.valid ? 'text-green-700' : 'text-red-700'}`}>
                  {data.valid ? 'Certificate is Valid' : 'Certificate is Invalid'}
                </p>
              </div>

              {data.valid && (
                <div className="space-y-2 text-sm">
                  <div className="flex justify-between">
                    <span className="text-gray-500">Student</span>
                    <span className="font-medium text-gray-800">{data.studentName}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-500">Exam</span>
                    <span className="font-medium text-gray-800">{data.examTitle}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-500">Score</span>
                    <span className="font-medium text-green-700">{data.scorePercentage.toFixed(1)}%</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-500">Issued</span>
                    <span className="font-medium text-gray-800">
                      {new Date(data.issuedAt).toLocaleDateString()}
                    </span>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>

        <div className="text-center mt-4">
          <button onClick={() => navigate('/')} className="text-indigo-600 text-sm hover:underline">
            ← Back to platform
          </button>
        </div>
      </div>
    </div>
  );
};

export default VerifyCertificatePage;
