import {
  Link,
  NavLink,
  Outlet,
  useNavigate,
  useParams,
} from "react-router-dom";
import {
  Layers2,
  LayoutDashboard,
  Building2,
  LogOut,
  ChevronRight,
  CircleHelp,
} from "lucide-react";
import { useState } from "react";
import { useAuth } from "../context/AuthContext";
import { authApi, organizationsApi } from "../api";
import { useResource } from "../hooks/useResource";
import { Avatar, ErrorMessage } from "./UI";
export default function AppLayout() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const { id } = useParams();
  const orgs = useResource(organizationsApi.list, [id]);
  const [error, setError] = useState(null);
  async function logout() {
    try {
      await authApi.logout();
      navigate("/login");
    } catch (failure) {
      setError(failure);
    }
  }
  return (
    <div className="app-shell">
      <aside className="sidebar">
        <Link className="brand" to="/dashboard">
          <span className="brand-icon">
            <Layers2 size={21} />
          </span>{" "}
          workflow<span className="brand-dot">.</span>
        </Link>
        <div className="workspace-label">YOUR WORKSPACE</div>
        <label className="sr-only" htmlFor="org-switch">
          Switch organization
        </label>
        <select
          id="org-switch"
          className="org-switch"
          value={orgs.data?.some((org) => org.id === id) ? id : ""}
          onChange={(event) =>
            event.target.value &&
            navigate(`/organizations/${event.target.value}`)
          }
        >
          <option value="">Select organization</option>
          {orgs.data?.map((org) => (
            <option key={org.id} value={org.id}>
              {org.name}
            </option>
          ))}
        </select>
        <nav>
          <NavLink to="/dashboard">
            <LayoutDashboard size={18} /> Overview
          </NavLink>
          <div className="nav-section">ORGANIZATIONS</div>
          {orgs.data?.map((org) => (
            <NavLink key={org.id} to={`/organizations/${org.id}`}>
              <Building2 size={17} />
              <span>{org.name}</span>
              <ChevronRight size={14} />
            </NavLink>
          ))}
        </nav>
        <ErrorMessage error={orgs.error} />
        <div className="sidebar-bottom">
          <div className="sidebar-note">
            <CircleHelp size={17} />
            <span>
              A little structure.
              <br />A lot more clarity.
            </span>
          </div>
          <div className="profile">
            <Avatar name={user.displayName} />
            <div>
              <strong>{user.displayName}</strong>
              <span>{user.email}</span>
            </div>
            <button
              className="icon-button"
              title="Log out"
              aria-label="Log out"
              onClick={logout}
            >
              <LogOut size={17} />
            </button>
          </div>
        </div>
      </aside>
      <main>
        <div className="topbar">
          <span>
            Workspace <ChevronRight size={13} /> Projects & collaboration
          </span>
          <span className="topbar-note">Make room for meaningful work</span>
        </div>
        <ErrorMessage error={error} />
        <Outlet />
      </main>
    </div>
  );
}
