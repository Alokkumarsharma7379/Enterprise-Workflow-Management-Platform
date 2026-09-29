import { useState } from 'react';
import { activityApi } from '../api';
import { useResource } from '../hooks/useResource';
import { Avatar, Empty, ErrorMessage, Loading, Pagination } from '../components/UI';
import { dateTime, humanize } from '../utils/permissions';
export default function Activity({ scope, id, revision = 0 }) {
  const [page, setPage] = useState(0);
  const resource = useResource(() => activityApi.list(scope, id, page), [scope, id, page, revision]);
  return <section className="panel"><h2>Activity</h2><p className="muted">The changes and decisions that move work forward.</p><ErrorMessage error={resource.error}/>{resource.loading ? <Loading/> : resource.data?.content.length ? <div className="timeline">{resource.data.content.map(event => <div className="timeline-event" key={event.id}><Avatar name={event.actor.displayName}/><div><p><strong>{event.actor.displayName}</strong> · {humanize(event.actionType)}</p>{(event.oldValue || event.newValue) && <p className="activity-values">{event.oldValue && <span>{event.oldValue}</span>}{event.oldValue && event.newValue && ' → '}{event.newValue && <span>{event.newValue}</span>}</p>}<time>{dateTime(event.createdAt)}</time></div></div>)}</div> : <Empty title="No activity yet">Changes will appear here as your team works.</Empty>}<Pagination result={resource.data} onPage={setPage}/></section>;
}
