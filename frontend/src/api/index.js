import { request, query, setSession, refreshSession } from './client';
const post = (path, body) => request(path, { method: 'POST', body });
const patch = (path, body) => request(path, { method: 'PATCH', body });
const remove = path => request(path, { method: 'DELETE' });

export const authApi = {
  async login(values) { const session = await post('/auth/login', values); setSession(session); return session; },
  async register(values) { const session = await post('/auth/register', values); setSession(session); return session; },
  async logout() { await post('/auth/logout'); setSession(null); },
  refresh: refreshSession,
};
export const organizationsApi = {
  list: () => request('/organizations'), get: id => request(`/organizations/${id}`),
  create: values => post('/organizations', values), update: (id, values) => patch(`/organizations/${id}`, values),
  remove: id => remove(`/organizations/${id}`), members: id => request(`/organizations/${id}/members`),
  addMember: (id, values) => post(`/organizations/${id}/members`, values),
  changeMember: (id, user, role) => patch(`/organizations/${id}/members/${user}`, { role }),
  removeMember: (id, user) => remove(`/organizations/${id}/members/${user}`),
};
export const projectsApi = {
  list: org => request(`/organizations/${org}/projects`), get: id => request(`/projects/${id}`),
  create: (org, values) => post(`/organizations/${org}/projects`, values),
  update: (id, values) => patch(`/projects/${id}`, values), remove: id => remove(`/projects/${id}`),
  statistics: id => request(`/projects/${id}/statistics`),
};
export const tasksApi = {
  list: (project, filters) => request(`/projects/${project}/tasks${query(filters)}`), get: id => request(`/tasks/${id}`),
  create: (project, values) => post(`/projects/${project}/tasks`, values),
  update: (id, values) => patch(`/tasks/${id}`, values), remove: id => remove(`/tasks/${id}`),
};
export const commentsApi = {
  list: (id, page = 0) => request(`/tasks/${id}/comments${query({ page })}`),
  create: (id, body) => post(`/tasks/${id}/comments`, { body }),
  update: (id, values) => patch(`/comments/${id}`, values), remove: id => remove(`/comments/${id}`),
};
export const labelsApi = {
  list: id => request(`/projects/${id}/labels`), create: (id, values) => post(`/projects/${id}/labels`, values),
  update: (id, values) => patch(`/labels/${id}`, values), remove: id => remove(`/labels/${id}`),
  attach: (task, label) => request(`/tasks/${task}/labels/${label}`, { method: 'PUT' }),
  detach: (task, label) => remove(`/tasks/${task}/labels/${label}`),
};
export const activityApi = { list: (scope, id, page = 0) => request(`/${scope}/${id}/activity${query({ page })}`) };
