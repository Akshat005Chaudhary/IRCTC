import { AuthProvider, useAuth } from './context/AuthContext';
import Auth from './views/Auth';
import Dashboard from './views/Dashboard';

function MainApp() {
  const { isAuthenticated } = useAuth();
  return isAuthenticated ? <Dashboard /> : <Auth />;
}

export default function App() {
  return (
    <AuthProvider>
      <MainApp />
    </AuthProvider>
  );
}
