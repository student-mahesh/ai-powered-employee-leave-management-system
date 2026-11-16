import React, {useEffect, useRef} from 'react';

export function PageHeader({eyebrow, title, description, action}) {
  return <div className="page-header d-flex flex-column flex-lg-row justify-content-between align-items-lg-end gap-3">
    <div>{eyebrow && <div className="eyebrow">{eyebrow}</div>}<h1>{title}</h1>{description && <p>{description}</p>}</div>
    {action && <div className="page-header-action">{action}</div>}
  </div>;
}

export function LoadingState({label = 'Loading data…'}) { return <div className="state-box"><span className="spinner-border spinner-border-sm" aria-hidden="true"/> {label}</div>; }
export function EmptyState({title = 'No records found', detail = 'No information is available.'}) { return <div className="empty-state"><i className="bi bi-inbox"/><h2>{title}</h2><p>{detail}</p></div>; }
export function ErrorAlert({message, onRetry}) { return message ? <div className="alert alert-danger app-alert" role="alert"><i className="bi bi-exclamation-circle"/>{message}{onRetry && <button className="btn btn-sm btn-outline-danger ms-auto" onClick={onRetry}>Retry</button>}</div> : null; }
export function SuccessAlert({message}) { return message ? <div className="alert alert-success app-alert" role="status"><i className="bi bi-check-circle"/>{message}</div> : null; }

export function StatusBadge({status}) {
  const normalized = String(status || '').toUpperCase();
  const styles = {APPROVED: 'success', PENDING: 'warning', REJECTED: 'danger', CANCELLED: 'secondary', WAITING: 'warning', SUBMITTED: 'success'};
  return <span className={`status-badge status-${styles[normalized] || 'secondary'}`}>{normalized.replace('_', ' ') || 'UNKNOWN'}</span>;
}

export function Pagination({page, onChange}) {
  if (!page || page.totalPages <= 1) return null;
  return <nav className="table-pagination" aria-label="Pagination"><span>Page {page.number + 1} of {page.totalPages} · {page.totalElements} total</span><div><button className="btn btn-sm btn-outline-secondary" disabled={page.first} onClick={() => onChange(page.number - 1)}>Previous</button><button className="btn btn-sm btn-outline-secondary ms-2" disabled={page.last} onClick={() => onChange(page.number + 1)}>Next</button></div></nav>;
}

export function ConfirmDialog({open, title, children, confirmLabel = 'Confirm', busy, danger, onConfirm, onClose}) {
  const closeRef = useRef();
  useEffect(() => { if (open) closeRef.current?.focus(); }, [open]);
  if (!open) return null;
  return <div className="confirm-backdrop" role="presentation"><section className="confirm-dialog" role="dialog" aria-modal="true" aria-labelledby="confirm-title"><div className="confirm-icon"><i className={`bi ${danger ? 'bi-exclamation-triangle' : 'bi-question-circle'}`}/></div><h2 id="confirm-title">{title}</h2><div className="confirm-content">{children}</div><div className="d-flex justify-content-end gap-2 mt-4"><button ref={closeRef} className="btn btn-light" disabled={busy} onClick={onClose}>Cancel</button><button className={`btn ${danger ? 'btn-danger' : 'btn-primary'}`} disabled={busy} onClick={onConfirm}>{busy ? <><span className="spinner-border spinner-border-sm me-2"/>Working…</> : confirmLabel}</button></div></section></div>;
}

export function formatDate(value) { return value ? new Intl.DateTimeFormat(undefined, {year: 'numeric', month: 'short', day: 'numeric'}).format(new Date(`${value}T00:00:00`)) : '—'; }
export function formatDateTime(value) { return value ? new Intl.DateTimeFormat(undefined, {dateStyle: 'medium', timeStyle: 'short'}).format(new Date(value)) : '—'; }
