import { useState } from 'react';
import { commentsApi } from '../api';
import { useAuth } from '../context/AuthContext';
import { useResource } from '../hooks/useResource';
import { canAdmin, dateTime } from '../utils/permissions';
import { Avatar, Empty, ErrorMessage, Field, Loading, Pagination } from '../components/UI';
export default function Comments({ taskId, role, writable, onChange }) {
  const { user } = useAuth();
  const [page, setPage] = useState(0);
  const resource = useResource(() => commentsApi.list(taskId, page), [taskId, page]);
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);
  const [editing, setEditing] = useState(null);
  async function perform(action, done) {
    setBusy(true); setError(null);
    try { await action(); done?.(); await resource.reload(); onChange(); }
    catch (failure) { setError(failure); } finally { setBusy(false); }
  }
  return <section className="panel"><div className="section-heading"><h2>Discussion</h2><span className="count">{resource.data?.totalElements || 0}</span></div><ErrorMessage error={error || resource.error}/>
    {writable && <form className="comment-form" onSubmit={e => { e.preventDefault(); const form = e.currentTarget; const body = new FormData(form).get('body'); perform(() => commentsApi.create(taskId,body),() => form.reset()); }}><Field label="Add a comment"><textarea name="body" placeholder="Share an update or ask a question…" maxLength={10000} rows={3} required/></Field><button className="primary" disabled={busy}>Post comment</button></form>}
    {resource.loading ? <Loading/> : resource.data?.content.length ? resource.data.content.map(comment => <article className="comment" key={comment.id}><Avatar name={comment.author.displayName}/><div className="comment-content"><div className="comment-meta"><strong>{comment.author.displayName}</strong><time>{dateTime(comment.createdAt)}</time></div>{editing === comment.id ? <form onSubmit={e => { e.preventDefault(); const body = new FormData(e.currentTarget).get('body'); perform(() => commentsApi.update(comment.id,{ body, version: comment.version }),() => setEditing(null)); }}><textarea aria-label="Edit comment" name="body" defaultValue={comment.body} maxLength={10000} required/><div className="actions"><button disabled={busy}>Save comment</button><button type="button" onClick={() => setEditing(null)}>Cancel</button></div></form> : <p className="preserve-lines">{comment.body}</p>}<div className="actions">{writable && comment.author.id === user.id && <button className="text-button" onClick={() => setEditing(comment.id)}>Edit</button>}{writable && (comment.author.id === user.id || canAdmin(role)) && <button className="text-button danger" disabled={busy} onClick={() => { if (window.confirm('Delete this comment?')) perform(() => commentsApi.remove(comment.id)); }}>Delete</button>}</div></div></article>) : <Empty title="Start the conversation">Keep decisions and updates close to the work.</Empty>}
    <Pagination result={resource.data} onPage={setPage}/>
  </section>;
}
