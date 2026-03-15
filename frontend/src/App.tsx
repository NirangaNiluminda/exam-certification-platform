import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from './context/AuthContext';
import Navbar from './components/Navbar';
import ProtectedRoute from './components/ProtectedRoute';
import AdminRoute from './components/AdminRoute';

// Pages
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import StudentDashboardPage from './pages/StudentDashboardPage';
import AdminDashboardPage from './pages/admin/AdminDashboardPage';
import ExamDetailPage from './pages/ExamDetailPage';
import MyEnrollmentsPage from './pages/MyEnrollmentsPage';
import TakeExamPage from './pages/TakeExamPage';
import CertificatePage from './pages/CertificatePage';
import VerifyCertificatePage from './pages/VerifyCertificatePage';
import AdminExamsPage from './pages/admin/AdminExamsPage';
import AdminExamFormPage from './pages/admin/AdminExamFormPage';
import AdminQuestionsPage from './pages/admin/AdminQuestionsPage';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      staleTime: 30_000,
    },
  },
});

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <BrowserRouter>
          <div className="min-h-screen bg-gray-50">
            <Navbar />
            <Routes>
              {/* Public routes */}
              <Route path="/login" element={<LoginPage />} />
              <Route path="/register" element={<RegisterPage />} />
              <Route path="/verify/:verifyCode?" element={<VerifyCertificatePage />} />

              {/* Protected student routes */}
              <Route path="/" element={<ProtectedRoute><StudentDashboardPage /></ProtectedRoute>} />
              <Route path="/exams/:examId" element={<ProtectedRoute><ExamDetailPage /></ProtectedRoute>} />
              <Route path="/enrollments" element={<ProtectedRoute><MyEnrollmentsPage /></ProtectedRoute>} />
              <Route path="/exam/:slotId" element={<ProtectedRoute><TakeExamPage /></ProtectedRoute>} />
              <Route path="/certificates/:certificateId" element={<ProtectedRoute><CertificatePage /></ProtectedRoute>} />

              {/* Admin-only routes */}
              <Route path="/admin" element={<AdminRoute><AdminDashboardPage /></AdminRoute>} />
              <Route path="/admin/exams" element={<AdminRoute><AdminExamsPage /></AdminRoute>} />
              <Route path="/admin/exams/new" element={<AdminRoute><AdminExamFormPage /></AdminRoute>} />
              <Route path="/admin/exams/:examId/edit" element={<AdminRoute><AdminExamFormPage /></AdminRoute>} />
              <Route path="/admin/exams/:examId/questions" element={<AdminRoute><AdminQuestionsPage /></AdminRoute>} />

              {/* Fallback */}
              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </div>
        </BrowserRouter>
      </AuthProvider>
    </QueryClientProvider>
  );
}

export default App;
