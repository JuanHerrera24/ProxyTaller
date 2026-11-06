import { SERVICES, STATUSES } from '../constants/services.js';

export default function LogFilters({ filters, onChange, onReset }) {
  const update = (field) => (event) => onChange({ ...filters, [field]: event.target.value });
  const hasActiveFilters = Object.values(filters).some(Boolean);
  const invalidRange = filters.from && filters.to && new Date(filters.from) > new Date(filters.to);

  return (
    <div className="filters">
      <label className="field">
        <span className="field__label">Service</span>
        <select value={filters.service} onChange={update('service')}>
          <option value="">All services</option>
          {SERVICES.map((service) => (
            <option key={service.id} value={service.id}>
              {service.label}
            </option>
          ))}
        </select>
      </label>

      <label className="field">
        <span className="field__label">Status</span>
        <select value={filters.status} onChange={update('status')}>
          <option value="">All statuses</option>
          {STATUSES.map((status) => (
            <option key={status} value={status}>
              {status}
            </option>
          ))}
        </select>
      </label>

      <label className="field">
        <span className="field__label">From</span>
        <input type="datetime-local" step="1" value={filters.from} max={filters.to || undefined} onChange={update('from')} />
      </label>

      <label className="field">
        <span className="field__label">To</span>
        <input type="datetime-local" step="1" value={filters.to} min={filters.from || undefined} onChange={update('to')} />
      </label>

      <button type="button" className="button button--ghost" onClick={onReset} disabled={!hasActiveFilters}>
        Clear filters
      </button>

      {invalidRange && <p className="filters__warning">The start date must be earlier than the end date.</p>}
    </div>
  );
}
