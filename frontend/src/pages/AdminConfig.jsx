import {useEffect, useState} from 'react';
import {ConfirmDialog} from '../components/common/Ui';
import api from '../services/api';

export default function AdminConfig() {
  const [deps, setDeps] = useState([]);
  const [types, setTypes] = useState([]);
  const [holidays, setHolidays] = useState({content: []});
  const [message, setMessage] = useState('');
  const [messageVariant, setMessageVariant] = useState('warning');
  const [selectedType, setSelectedType] = useState(null);
  const [updatingType, setUpdatingType] = useState(false);

  const showMessage = (text, variant = 'warning') => {
    setMessage(text);
    setMessageVariant(variant);
  };
  const load = () => Promise.all([api.get('/departments'), api.get('/leave-types'), api.get('/holidays')])
    .then(([d, t, h]) => { setDeps(d.data); setTypes(t.data); setHolidays(h.data); })
    .catch(() => showMessage('Unable to load configuration.'));
  useEffect(() => { load(); }, []);

  const add = (path, body, form, fallback) => api.post(path, body)
    .then(() => { form.reset(); setMessage(''); load(); })
    .catch(error => showMessage(error.response?.data?.message || fallback));
  const updateLeaveType = async () => {
    if (!selectedType) return;
    setUpdatingType(true);
    const nextActive = !selectedType.active;
    try {
      const {data} = await api.put(`/leave-types/${selectedType.id}/active`, null, {params: {value: nextActive}});
      setTypes(current => current.map(type => type.id === data.id ? data : type));
      showMessage(`${data.name} is now ${data.active ? 'active' : 'inactive'}.`, 'success');
      setSelectedType(null);
    } catch (error) {
      showMessage(error.response?.data?.message || `Unable to ${nextActive ? 'activate' : 'deactivate'} ${selectedType.name}.`, 'danger');
    } finally {
      setUpdatingType(false);
    }
  };

  return <>
    <h1 className="h3 mb-4">Leave Configuration</h1>
    {message && <div className={`alert alert-${messageVariant}`}>{message}</div>}
    <div className="row g-3">
      <div className="col-lg-6"><div className="card"><div className="card-header bg-white">Departments</div><div className="card-body">
        <form className="row g-2 mb-3" onSubmit={event => { event.preventDefault(); const form = new FormData(event.currentTarget); add('/departments', {name: form.get('name'), code: form.get('code')}, event.currentTarget, 'Department could not be created.'); }}>
          <div className="col"><input className="form-control" name="name" placeholder="Department name" required /></div><div className="col"><input className="form-control" name="code" placeholder="Code" /></div><div className="col-auto"><button className="btn btn-primary">Add</button></div>
        </form>
        {deps.map(department => <div className="border-top py-2" key={department.id}>{department.name} <small className="text-muted">{department.code}</small></div>)}
      </div></div></div>
      <div className="col-lg-6"><div className="card"><div className="card-header bg-white">Leave types</div><div className="card-body">
        <form className="row g-2 mb-3" onSubmit={event => { event.preventDefault(); const form = new FormData(event.currentTarget); add('/leave-types', {name: form.get('name')}, event.currentTarget, 'Leave type could not be created.'); }}>
          <div className="col"><input className="form-control" name="name" placeholder="Leave type name" maxLength="80" required /></div><div className="col-auto"><button className="btn btn-primary">Add</button></div>
        </form>
        {types.map(type => <div className="d-flex justify-content-between align-items-center border-top py-2" key={type.id}><span>{type.name} <small className="text-muted">{type.active ? 'Active' : 'Inactive'}</small></span><button type="button" className="btn btn-sm btn-outline-secondary" onClick={() => setSelectedType(type)} disabled={updatingType}>{type.active ? 'Deactivate' : 'Activate'}</button></div>)}
      </div></div></div>
      <div className="col-12"><div className="card"><div className="card-header bg-white">Holidays</div><div className="card-body">
        <form className="row g-2 mb-3" onSubmit={event => { event.preventDefault(); const form = new FormData(event.currentTarget); add('/holidays', {date: form.get('date'), name: form.get('name')}, event.currentTarget, 'Holiday could not be created.'); }}>
          <div className="col-md-3"><input type="date" className="form-control" name="date" required /></div><div className="col"><input className="form-control" name="name" placeholder="Holiday name" required /></div><div className="col-auto"><button className="btn btn-primary">Add holiday</button></div>
        </form>
        {(holidays.content || []).map(holiday => <div className="border-top py-2" key={holiday.id}>{holiday.date} — {holiday.name}</div>)}
      </div></div></div>
    </div>
    <ConfirmDialog open={Boolean(selectedType)} title={`${selectedType?.active ? 'Deactivate' : 'Activate'} leave type?`} confirmLabel={selectedType?.active ? 'Deactivate' : 'Activate'} danger={selectedType?.active} busy={updatingType} onClose={() => setSelectedType(null)} onConfirm={updateLeaveType}>
      {selectedType?.active ? <>Deactivate <strong>{selectedType.name}</strong>? Employees will no longer be able to select it for new leave requests.</> : <>Activate <strong>{selectedType?.name}</strong>? It will be available for new leave requests.</>}
    </ConfirmDialog>
  </>;
}
