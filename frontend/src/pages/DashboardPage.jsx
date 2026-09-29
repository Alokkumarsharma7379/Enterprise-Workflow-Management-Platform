import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Plus, ArrowUpRight, Building2 } from 'lucide-react';
import { organizationsApi } from '../api';
import { useAuth } from '../context/AuthContext';
import { useResource } from '../hooks/useResource';
import { Badge, Empty, ErrorMessage, Field, Loading, Modal } from '../components/UI';
export default function DashboardPage() {
  const { user } = useAuth();
  const resource = useResource(organizationsApi.list);
  const [creating, setCreating] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  async function create(event) {
    event.preventDefault(); setBusy(true); setError(null);
    try { await organizationsApi.create(Object.fromEntries(new FormData(event.currentTarget))); setCreating(false); await resource.reload(); }
    catch (failure) { setError(failure); } finally { setBusy(false); }
  }
  return <div className="page"><div className="page-heading"><div><span className="eyebrow">WORKSPACE OVERVIEW</span><h1>Hello, {user.displayName.split(' ')[0]}<span className="heading-dot">.</span></h1><p className="muted">Your teams, your projects, and a little more clarity.</p></div><button className="primary" onClick={() => { setError(null); setCreating(true); }}><Plus size={17}/> New organization</button></div>
    <div className="welcome-panel"><div><span className="eyebrow">A PLACE FOR YOUR TEAM</span><h2>Great work is a team effort.</h2><p>Choose an organization to see its projects,<br/>organize priorities, and keep work moving.</p></div><div className="welcome-art" aria-hidden="true"><span/><span/><span/><Building2 size={38}/></div></div>
    <div className="section-heading"><h2>Your organizations</h2><span className="count">{resource.data?.length || 0}</span></div><ErrorMessage error={resource.error}/>{resource.loading ? <Loading/> : resource.data?.length ? <div className="card-grid">{resource.data.map(org => <Link className="organization-card" key={org.id} to={`/organizations/${org.id}`}><div className="card-top"><span className="org-icon"><Building2 size={23}/></span><ArrowUpRight size={19}/></div><h3>{org.name}</h3><Badge>{org.role}</Badge><div className="card-footer">Open workspace <ArrowUpRight size={15}/></div></Link>)}</div> : <Empty title="Your workspace starts here">Create an organization, then add your first project and invite registered teammates.</Empty>}
    {creating && <Modal title="Create organization" onClose={() => setCreating(false)}><form onSubmit={create}><ErrorMessage error={error}/><Field label="Organization name"><input name="name" placeholder="e.g. Acme Engineering" maxLength={120} required autoFocus/></Field><p className="muted">You’ll become the owner and can add teammates after creating it.</p><button className="primary" disabled={busy}>Create organization</button></form></Modal>}
  </div>;
}
