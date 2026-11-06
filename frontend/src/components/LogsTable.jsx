import { Fragment, useState } from 'react';
import { SERVICE_BY_ID } from '../constants/services.js';
import { formatDateTime, formatDuration, formatNumber, shortId } from '../utils/format.js';
import StatusBadge from './StatusBadge.jsx';
import LogDetails from './LogDetails.jsx';

const COLUMN_COUNT = 7;

export default function LogsTable({ pageData, loading, onPageChange }) {
  const [expandedId, setExpandedId] = useState(null);
  const rows = pageData?.content ?? [];

  const toggle = (requestId) => setExpandedId((current) => (current === requestId ? null : requestId));

  return (
    <>
      <div className="table-wrapper">
        <table className="logs-table">
          <thead>
            <tr>
              <th aria-label="Expand" />
              <th>Request ID</th>
              <th>Service</th>
              <th>Operation</th>
              <th className="align-right">Duration</th>
              <th>Status</th>
              <th>Timestamp</th>
            </tr>
          </thead>
          <tbody>
            {loading && (
              <tr>
                <td colSpan={COLUMN_COUNT} className="logs-table__placeholder">
                  Loading logs…
                </td>
              </tr>
            )}

            {!loading && rows.length === 0 && (
              <tr>
                <td colSpan={COLUMN_COUNT} className="logs-table__placeholder">
                  No log entries match the current filters.
                </td>
              </tr>
            )}

            {rows.map((log) => {
              const expanded = expandedId === log.requestId;
              const service = SERVICE_BY_ID[log.serviceId];

              return (
                <Fragment key={log.requestId}>
                  <tr
                    className={`logs-table__row ${expanded ? 'is-expanded' : ''} ${log.status === 'ERROR' ? 'is-error' : ''}`}
                    onClick={() => toggle(log.requestId)}
                    onKeyDown={(event) => {
                      if (event.key === 'Enter' || event.key === ' ') {
                        event.preventDefault();
                        toggle(log.requestId);
                      }
                    }}
                    tabIndex={0}
                    aria-expanded={expanded}
                  >
                    <td className="logs-table__chevron" aria-hidden="true">
                      <svg viewBox="0 0 24 24" width="14" height="14">
                        <path d="m9 6 6 6-6 6" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                      </svg>
                    </td>
                    <td className="mono" title={log.requestId}>
                      {shortId(log.requestId)}
                    </td>
                    <td>
                      <span className="service-tag" style={{ '--accent': service?.color }}>
                        {service?.label ?? log.serviceId}
                      </span>
                    </td>
                    <td className="mono">{log.operation}</td>
                    <td className="mono align-right">{formatDuration(log.durationMs)}</td>
                    <td>
                      <StatusBadge status={log.status} />
                    </td>
                    <td className="text-muted">{formatDateTime(log.timestamp)}</td>
                  </tr>

                  {expanded && (
                    <tr className="logs-table__details">
                      <td colSpan={COLUMN_COUNT}>
                        <LogDetails log={log} />
                      </td>
                    </tr>
                  )}
                </Fragment>
              );
            })}
          </tbody>
        </table>
      </div>

      {pageData && pageData.totalElements > 0 && (
        <Pagination
          page={pageData.page}
          totalPages={pageData.totalPages}
          totalElements={pageData.totalElements}
          size={pageData.size}
          onPageChange={onPageChange}
        />
      )}
    </>
  );
}

function Pagination({ page, totalPages, totalElements, size, onPageChange }) {
  const firstItem = page * size + 1;
  const lastItem = Math.min((page + 1) * size, totalElements);

  return (
    <nav className="pagination" aria-label="Log pagination">
      <span className="pagination__summary">
        Showing <strong>{formatNumber(firstItem)}</strong>–<strong>{formatNumber(lastItem)}</strong> of{' '}
        <strong>{formatNumber(totalElements)}</strong> entries
      </span>
      <div className="pagination__controls">
        <button type="button" className="button button--ghost" onClick={() => onPageChange(0)} disabled={page === 0}>
          First
        </button>
        <button type="button" className="button button--ghost" onClick={() => onPageChange(page - 1)} disabled={page === 0}>
          Previous
        </button>
        <span className="pagination__page">
          Page {page + 1} of {Math.max(totalPages, 1)}
        </span>
        <button
          type="button"
          className="button button--ghost"
          onClick={() => onPageChange(page + 1)}
          disabled={page >= totalPages - 1}
        >
          Next
        </button>
        <button
          type="button"
          className="button button--ghost"
          onClick={() => onPageChange(totalPages - 1)}
          disabled={page >= totalPages - 1}
        >
          Last
        </button>
      </div>
    </nav>
  );
}
