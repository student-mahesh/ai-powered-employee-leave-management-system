import {useEffect, useState} from 'react';
import api from '../services/api';

const initialDraft = {email: '', fullName: '', password: '', role: 'EMPLOYEE', departmentId: '', managerId: ''};

export default function AdminEmployees() {
  const [rows, setRows] = useState({content: []});
  const [deps, setDeps] = useState([]);
  const [managers, setManagers] = useState([]);
  const [draft, setDraft] = useState(initialDraft);
  const [message, setMessage] = useState('');
  const [edit, setEdit] = useState(null);
  const [saving, setSaving] = useState(false);
  const load = () => Promise.all([api.get('/employees'), api.get('/departments'), api.get('/employees', {params: {role: 'MANAGER', size: 100}})])
    .then(([employees, departments, managerPage]) => { setRows(employees.data); setDeps(departments.data); setManagers(managerPage.data.content || []); })
    .catch(() => setMessage('Unable to load employee data.'));
  useEffect(() => { load(); }, []);
  const select = value => Number(value) || null;
  const create = async event => {
    event.preventDefault(); setSaving(true); setMessage('');
    try {
      await api.post('/employees', {...draft, departmentId: select(draft.departmentId), managerId: select(draft.managerId)});
      setDraft(initialDraft); setMessage('Employee created.'); load();
    } catch (error) { setMessage(error.response?.data?.message || 'Employee could not be created.'); } finally { setSaving(false); }
  };
  const update = async event => {
    event.preventDefault(); setSaving(true); setMessage('');
    try {
      await api.put(`/employees/${edit.id}`, {fullName: edit.fullName, departmentId: edit.departmentId || null, managerId: edit.managerId || null});
      setEdit(null); setMessage('Employee updated.'); load();
    } catch (error) { setMessage(error.response?.data?.message || 'Employee update failed.'); } finally { setSaving(false); }
  };
  const managerOptions = currentId => managers.filter(manager => manager.id !== currentId).map(manager => <option key={manager.id} value={manager.id}>{manager.fullName}</option>);
  const departmentName = id => deps.find(department => department.id === id)?.name || 'Not assigned';
  const managerName = id => managers.find(manager => manager.id === id)?.fullName || 'Not assigned';
  return <>
    <h1 className="h3 mb-4">Employees</h1>
    {message && <div className="alert alert-info">{message}</div>}
    <div className="card mb-4"><div className="card-header bg-white">Add employee</div><form className="card-body row g-3" onSubmit={create}>
      <div className="col-md-4"><label className="form-label">Work email</label><input type="email" className="form-control" value={draft.email} onChange={e => setDraft({...draft, email: e.target.value})} required disabled={saving} /></div>
      <div className="col-md-4"><label className="form-label">Full name</label><input className="form-control" value={draft.fullName} onChange={e => setDraft({...draft, fullName: e.target.value})} maxLength="160" required disabled={saving} /></div>
      <div className="col-md-4"><label className="form-label">Temporary password</label><input type="password" className="form-control" value={draft.password} onChange={e => setDraft({...draft, password: e.target.value})} minLength="8" maxLength="100" required autoComplete="new-password" disabled={saving} /></div>
      <div className="col-md-4"><label className="form-label">Role</label><select className="form-select" value={draft.role} onChange={e => setDraft({...draft, role: e.target.value})} disabled={saving}><option value="EMPLOYEE">Employee</option><option value="MANAGER">Manager</option><option value="ADMIN">Admin</option></select></div>
      <div className="col-md-4"><label className="form-label">Department</label><select className="form-select" value={draft.departmentId} onChange={e => setDraft({...draft, departmentId: e.target.value})} disabled={saving}><option value="">Not assigned</option>{deps.map(department => <option key={department.id} value={department.id}>{department.name}</option>)}</select></div>
      <div className="col-md-4"><label className="form-label">Manager</label><select className="form-select" value={draft.managerId} onChange={e => setDraft({...draft, managerId: e.target.value})} disabled={saving}><option value="">Not assigned</option>{managerOptions()}</select></div>
      <div><button className="btn btn-primary" disabled={saving}>{saving ? 'Saving…' : 'Create employee'}</button></div>
    </form></div>
    {edit && <div className="card border-primary mb-4"><div className="card-header bg-white">Edit employee</div><form className="card-body row g-3" onSubmit={update}>
      <div className="col-md-4"><input className="form-control" value={edit.fullName} onChange={e => setEdit({...edit, fullName: e.target.value})} required disabled={saving} /></div>
      <div className="col-md-4"><select className="form-select" value={edit.departmentId || ''} onChange={e => setEdit({...edit, departmentId: select(e.target.value)})} disabled={saving}><option value="">Department</option>{deps.map(department => <option key={department.id} value={department.id}>{department.name}</option>)}</select></div>
      <div className="col-md-4"><select className="form-select" value={edit.managerId || ''} onChange={e => setEdit({...edit, managerId: select(e.target.value)})} disabled={saving}><option value="">Manager</option>{managerOptions(edit.id)}</select></div>
      <div><button className="btn btn-primary me-2" disabled={saving}>Save changes</button><button type="button" className="btn btn-outline-secondary" onClick={() => setEdit(null)} disabled={saving}>Cancel</button></div>
    </form></div>}
    <div className="card"><div className="card-header bg-white">Employees</div><div className="table-responsive"><table className="table mb-0"><thead><tr><th>Name</th><th>Email</th><th>Role</th><th>Department</th><th>Manager</th><th /></tr></thead><tbody>
      {(rows.content || []).map(employee => <tr key={employee.id}><td>{employee.fullName}</td><td>{employee.email}</td><td>{employee.role}</td><td>{departmentName(employee.departmentId)}</td><td>{managerName(employee.managerId)}</td><td><button type="button" className="btn btn-sm btn-outline-primary" onClick={() => setEdit({...employee})}>Edit</button></td></tr>)}
      {!(rows.content || []).length && <tr><td colSpan="6" className="text-center text-muted py-3">No employees found.</td></tr>}
    </tbody></table></div></div>
  </>;
}
