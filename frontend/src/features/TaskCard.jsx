import { Link } from 'react-router-dom';
import { CalendarDays } from 'lucide-react';
import { Avatar, Badge } from '../components/UI';
import { humanize } from '../utils/permissions';
export default function TaskCard({ task }) {
  return <Link className="task-card" to={`/tasks/${task.id}`}><div className="task-card-top"><span className="task-id">{task.identifier}</span><Badge tone={task.priority}>{humanize(task.priority)}</Badge></div><h3>{task.title}</h3>{task.labels.length > 0 && <div className="labels">{task.labels.map(label => <span className="label-chip" key={label.id}><i style={{ background: label.color }}/>{label.name}</span>)}</div>}<div className="task-card-footer">{task.dueDate ? <span><CalendarDays size={13}/>{task.dueDate}</span> : <span>No due date</span>}{task.assignee ? <Avatar name={task.assignee.displayName}/> : <span className="unassigned">Unassigned</span>}</div></Link>;
}
