import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const Navbar: React.FC = () => {
  const { isAuthenticated, isAdmin, user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <nav className="bg-indigo-700 text-white shadow-md">
      <div className="max-w-6xl mx-auto px-4 py-3 flex items-center justify-between">
        <Link to="/" className="text-xl font-bold tracking-tight hover:text-indigo-200">
          🎓 ExamPlatform
        </Link>

        {isAuthenticated ? (
          <div className="flex items-center gap-6 text-sm">
            <Link to="/" className="hover:text-indigo-200">Exams</Link>
            <Link to="/enrollments" className="hover:text-indigo-200">My Enrollments</Link>
            {isAdmin && (
              <Link to="/admin/exams" className="hover:text-indigo-200 font-semibold">
                Admin
              </Link>
            )}
            <span className="text-indigo-300 text-xs">{user?.email}</span>
            <button
              onClick={handleLogout}
              className="bg-indigo-900 hover:bg-indigo-800 px-3 py-1 rounded text-sm"
            >
              Logout
            </button>
          </div>
        ) : (
          <div className="flex gap-4 text-sm">
            <Link to="/login" className="hover:text-indigo-200">Login</Link>
            <Link to="/register" className="bg-white text-indigo-700 px-3 py-1 rounded hover:bg-indigo-100 font-medium">
              Register
            </Link>
          </div>
        )}
      </div>
    </nav>
  );
};

export default Navbar;
