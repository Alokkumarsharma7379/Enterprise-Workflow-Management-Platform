import { useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import {
  Plus,
  ArrowUpRight,
  FolderKanban,
  Settings2,
  Users,
  Trash2,
} from "lucide-react";
import { organizationsApi, projectsApi } from "../api";
import { useResource } from "../hooks/useResource";
import { canAdmin, humanize } from "../utils/permissions";
import {
  Avatar,
  Badge,
  Empty,
  ErrorMessage,
  Field,
  Loading,
  Modal,
} from "../components/UI";
import Activity from "../features/Activity";
export default function OrganizationPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const resource = useResource(async () => {
    const [org, projects, members] = await Promise.all([
      organizationsApi.get(id),
      projectsApi.list(id),
      organizationsApi.members(id),
    ]);
    return { org, projects, members };
  }, [id]);
  const [tab, setTab] = useState("projects");
  const [modal, setModal] = useState(null);
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);
  const open = (name) => {
    setError(null);
    setModal(name);
  };
  async function perform(action, close = true) {
    setBusy(true);
    setError(null);
    try {
      await action();
      if (close) setModal(null);
      await resource.reload();
    } catch (failure) {
      setError(failure);
    } finally {
      setBusy(false);
    }
  }
  if (!resource.data)
    return (
      <div className="page">
        <ErrorMessage error={resource.error} />
        {resource.loading && <Loading />}
      </div>
    );
  const { org, projects, members } = resource.data;
  const admin = canAdmin(org.role);
  return (
    <div className="page">
      <div className="breadcrumbs">
        <Link to="/dashboard">Overview</Link>
        <span>/</span>
        <span>{org.name}</span>
      </div>
      <div className="page-heading">
        <div>
          <span className="eyebrow">ORGANIZATION</span>
          <h1>{org.name}</h1>
          <p className="muted">A shared space for your team’s next chapter.</p>
        </div>
        <div className="actions">
          {org.role === "OWNER" && (
            <button onClick={() => open("settings")}>
              <Settings2 size={16} /> Settings
            </button>
          )}
          {admin && (
            <button className="primary" onClick={() => open("project")}>
              <Plus size={17} /> New project
            </button>
          )}
        </div>
      </div>
      <div className="summary-strip">
        <div>
          <FolderKanban size={20} />
          <strong>{projects.length}</strong>
          <span>Projects</span>
        </div>
        <div>
          <Users size={20} />
          <strong>{members.length}</strong>
          <span>Active members</span>
        </div>
        <div>
          <Badge>{org.role}</Badge>
          <span>Your role</span>
        </div>
      </div>
      <div className="tabs">
        {["projects", "members", "activity"].map((name) => (
          <button
            className={tab === name ? "active" : ""}
            key={name}
            onClick={() => setTab(name)}
          >
            {humanize(name)}
          </button>
        ))}
      </div>
      <ErrorMessage error={resource.error} />
      {!modal && <ErrorMessage error={error} />}
      {tab === "projects" &&
        (projects.length ? (
          <div className="card-grid">
            {projects.map((project) => (
              <Link
                key={project.id}
                className="project-card"
                to={`/projects/${project.id}`}
              >
                <div className="card-top">
                  <span className="project-key">{project.key}</span>
                  <ArrowUpRight size={18} />
                </div>
                <h3>{project.name}</h3>
                <p>
                  {project.description ||
                    "A new space to plan, collaborate, and move work forward."}
                </p>
                <div className="card-footer">
                  <Badge tone={project.status}>
                    {humanize(project.status)}
                  </Badge>
                  Open project
                </div>
              </Link>
            ))}
          </div>
        ) : (
          <Empty title="Make space for your first project">
            {admin
              ? "Create a project to start planning tasks with your team."
              : "An owner or admin can create the first project."}
          </Empty>
        ))}
      {tab === "members" && (
        <section className="panel">
          <div className="section-heading">
            <h2>People in this workspace</h2>
            {admin && (
              <button onClick={() => open("member")}>
                <Plus size={16} /> Add member
              </button>
            )}
          </div>
          <p className="muted">
            Members can access all projects in this organization.
          </p>
          <div className="member-list">
            {members.map((member) => {
              const manageable =
                member.role !== "OWNER" &&
                (org.role === "OWNER" ||
                  (org.role === "ADMIN" && member.role !== "ADMIN"));
              return (
                <div className="member-row" key={member.userId}>
                  <Avatar name={member.displayName} />
                  <strong>{member.displayName}</strong>
                  {manageable ? (
                    <>
                      <select
                        aria-label={`Role for ${member.displayName}`}
                        value={member.role}
                        disabled={busy}
                        onChange={(e) =>
                          perform(
                            () =>
                              organizationsApi.changeMember(
                                id,
                                member.userId,
                                e.target.value,
                              ),
                            false,
                          )
                        }
                      >
                        {(org.role === "OWNER"
                          ? ["ADMIN", "MANAGER", "MEMBER"]
                          : ["MANAGER", "MEMBER"]
                        ).map((role) => (
                          <option key={role}>{role}</option>
                        ))}
                      </select>
                      <button
                        className="icon-button danger"
                        aria-label={`Remove ${member.displayName}`}
                        disabled={busy}
                        onClick={() => {
                          if (
                            window.confirm(
                              `Remove ${member.displayName}? Their assigned tasks will be unassigned.`,
                            )
                          )
                            perform(
                              () =>
                                organizationsApi.removeMember(
                                  id,
                                  member.userId,
                                ),
                              false,
                            );
                        }}
                      >
                        <Trash2 size={16} />
                      </button>
                    </>
                  ) : (
                    <Badge>{member.role}</Badge>
                  )}
                </div>
              );
            })}
          </div>
        </section>
      )}
      {tab === "activity" && <Activity scope="organizations" id={id} />}
      {modal === "project" && (
        <Modal title="Create project" onClose={() => setModal(null)}>
          <form
            onSubmit={(e) => {
              e.preventDefault();
              const values = Object.fromEntries(new FormData(e.currentTarget));
              perform(() => projectsApi.create(id, values));
            }}
          >
            <ErrorMessage error={error} />
            <Field label="Project name">
              <input
                name="name"
                maxLength={120}
                placeholder="e.g. Platform engineering"
                required
                autoFocus
              />
            </Field>
            <Field label="Project key">
              <input
                name="key"
                pattern="[A-Za-z][A-Za-z0-9]{1,9}"
                maxLength={10}
                placeholder="PLAT"
                required
              />
            </Field>
            <small className="muted">
              2–10 letters or digits, starting with a letter. This key cannot be
              changed.
            </small>
            <Field label="Description">
              <textarea name="description" maxLength={10000} />
            </Field>
            <button className="primary" disabled={busy}>
              Create project
            </button>
          </form>
        </Modal>
      )}
      {modal === "member" && (
        <Modal title="Add a teammate" onClose={() => setModal(null)}>
          <form
            onSubmit={(e) => {
              e.preventDefault();
              const values = Object.fromEntries(new FormData(e.currentTarget));
              perform(() => organizationsApi.addMember(id, values));
            }}
          >
            <ErrorMessage error={error} />
            <p className="muted">
              Your teammate needs to register an account first.
            </p>
            <Field label="Teammate’s email">
              <input name="email" type="email" required autoFocus />
            </Field>
            <Field label="Role">
              <select name="role" defaultValue="MEMBER">
                {(org.role === "OWNER"
                  ? ["MEMBER", "MANAGER", "ADMIN"]
                  : ["MEMBER", "MANAGER"]
                ).map((role) => (
                  <option key={role}>{role}</option>
                ))}
              </select>
            </Field>
            <button className="primary" disabled={busy}>
              Add member
            </button>
          </form>
        </Modal>
      )}
      {modal === "settings" && (
        <Modal title="Organization settings" onClose={() => setModal(null)}>
          <form
            onSubmit={(e) => {
              e.preventDefault();
              const values = Object.fromEntries(new FormData(e.currentTarget));
              perform(() =>
                organizationsApi.update(id, {
                  ...values,
                  version: org.version,
                }),
              );
            }}
          >
            <ErrorMessage error={error} />
            <Field label="Organization name">
              <input
                name="name"
                defaultValue={org.name}
                maxLength={120}
                required
              />
            </Field>
            <button className="primary" disabled={busy}>
              Save changes
            </button>
          </form>
          <div className="danger-zone">
            <p>
              Deleting this organization permanently removes its projects,
              tasks, and history.
            </p>
            <button
              className="danger"
              disabled={busy}
              onClick={async () => {
                if (
                  window.confirm(
                    `Permanently delete ${org.name} and all of its data?`,
                  )
                ) {
                  setBusy(true);
                  try {
                    await organizationsApi.remove(id);
                    navigate("/dashboard");
                  } catch (failure) {
                    setError(failure);
                  } finally {
                    setBusy(false);
                  }
                }
              }}
            >
              Delete organization
            </button>
          </div>
        </Modal>
      )}
    </div>
  );
}
