import { useEffect, useRef } from 'react';
import { X, LoaderCircle, AlertCircle, ArrowLeft, ArrowRight } from 'lucide-react';
export function ErrorMessage({ error }) {
  if (!error) return null;
  return <div role="alert" className="error"><AlertCircle size={18}/><div>{error.message}{Object.entries(error.fields || {}).map(([field, message]) => <div key={field}>{field}: {message}</div>)}</div></div>;
}
export function Loading() { return <div className="loading" role="status"><LoaderCircle className="spin" size={20}/> Loading workspace…</div>; }
export function Empty({ title, children }) { return <div className="empty"><div className="empty-mark">◇</div><h3>{title}</h3><p>{children}</p></div>; }
export function Badge({ children, tone = '' }) { return <span className={`badge ${tone.toLowerCase()}`}>{children}</span>; }
export function Pagination({ result, onPage }) {
  if (!result || result.totalPages <= 1) return null;
  return <div className="pagination"><span>Page {result.page + 1} of {result.totalPages} · {result.totalElements} results</span><button className="icon-button" aria-label="Previous page" disabled={result.page === 0} onClick={() => onPage(result.page - 1)}><ArrowLeft size={16}/></button><button className="icon-button" aria-label="Next page" disabled={result.page + 1 >= result.totalPages} onClick={() => onPage(result.page + 1)}><ArrowRight size={16}/></button></div>;
}
export function Modal({ title, onClose, children }) {
  const ref = useRef(null);
  useEffect(() => { const dialog = ref.current; dialog.showModal(); return () => dialog.close(); }, []);
  return <dialog ref={ref} onCancel={onClose} onClick={e => { if (e.target === ref.current) onClose(); }}><div className="modal-title"><h2>{title}</h2><button className="icon-button" aria-label="Close dialog" onClick={onClose}><X size={20}/></button></div>{children}</dialog>;
}
export function Field({ label, children }) { return <label className="field"><span>{label}</span>{children}</label>; }
export function Avatar({ name = '?' }) { return <span className="avatar" title={name}>{name.split(' ').map(s => s[0]).slice(0, 2).join('').toUpperCase()}</span>; }
