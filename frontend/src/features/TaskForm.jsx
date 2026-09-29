import { useState } from "react";
import { tasksApi } from "../api";
import { ErrorMessage, Field } from "../components/UI";
import { humanize, priorities } from "../utils/permissions";
export default function TaskForm({ projectId, members, onCreated }) {
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);
  async function submit(event) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    const values = Object.fromEntries(new FormData(event.currentTarget));
    values.assigneeId ||= null;
    values.dueDate ||= null;
    try {
      const task = await tasksApi.create(projectId, values);
      onCreated(task);
    } catch (failure) {
      setError(failure);
    } finally {
      setBusy(false);
    }
  }
  return (
    <form onSubmit={submit}>
      <ErrorMessage error={error} />
      <Field label="Task title">
        <input
          name="title"
          placeholder="What needs to happen?"
          maxLength={200}
          required
          autoFocus
        />
      </Field>
      <Field label="Description">
        <textarea
          name="description"
          rows={4}
          maxLength={10000}
          placeholder="Add context, requirements, or a definition of done…"
        />
      </Field>
      <div className="form-grid">
        <Field label="Priority">
          <select name="priority" defaultValue="MEDIUM">
            {priorities.map((priority) => (
              <option key={priority} value={priority}>
                {humanize(priority)}
              </option>
            ))}
          </select>
        </Field>
        <Field label="Assignee">
          <select name="assigneeId">
            <option value="">Unassigned</option>
            {members.map((member) => (
              <option key={member.userId} value={member.userId}>
                {member.displayName}
              </option>
            ))}
          </select>
        </Field>
      </div>
      <Field label="Due date">
        <input name="dueDate" type="date" />
      </Field>
      <div className="form-footer">
        <span className="muted">New tasks start in To do.</span>
        <button className="primary" disabled={busy}>
          {busy ? "Creating…" : "Create task"}
        </button>
      </div>
    </form>
  );
}
