import { Navigate, Outlet, Route, Routes, useLocation } from "react-router-dom";
import { useAuth } from "./context/AuthContext";
import { ErrorMessage, Loading } from "./components/UI";
import AppLayout from "./components/AppLayout";
import AuthPage from "./pages/AuthPage";
import DashboardPage from "./pages/DashboardPage";
import OrganizationPage from "./pages/OrganizationPage";
import ProjectPage from "./pages/ProjectPage";
import TaskPage from "./pages/TaskPage";
function ProtectedRoute() {
  const { user, loading, error, restore } = useAuth();
  if (loading) return <Loading />;
  if (error)
    return (
      <div className="page">
        <ErrorMessage error={error} />
        <button onClick={restore}>Retry connection</button>
      </div>
    );
  return user ? <Outlet /> : <Navigate to="/login" replace />;
}
function PublicAuth({ register }) {
  const { user, loading } = useAuth();
  if (loading) return <Loading />;
  return user ? (
    <Navigate to="/dashboard" replace />
  ) : (
    <AuthPage key={register ? "register" : "login"} register={register} />
  );
}
function ScopedPage({ children }) {
  const location = useLocation();
  return <div key={location.pathname}>{children}</div>;
}
export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<PublicAuth />} />
      <Route path="/register" element={<PublicAuth register />} />
      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route
            path="/organizations/:id"
            element={
              <ScopedPage>
                <OrganizationPage />
              </ScopedPage>
            }
          />
          <Route
            path="/projects/:id"
            element={
              <ScopedPage>
                <ProjectPage />
              </ScopedPage>
            }
          />
          <Route
            path="/tasks/:id"
            element={
              <ScopedPage>
                <TaskPage />
              </ScopedPage>
            }
          />
        </Route>
      </Route>
      <Route path="/" element={<Navigate to="/dashboard" replace />} />
      <Route
        path="*"
        element={
          <div className="empty">
            <h1>Page not found</h1>
            <a href="/dashboard">Back to your workspace</a>
          </div>
        }
      />
    </Routes>
  );
}
