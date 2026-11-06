import { formatTime } from '../utils/format.js';

export default function Header({ lastUpdated, online, simulating, onSimulateLoad }) {
  return (
    <header className="topbar">
      <div className="topbar__brand">
        <div className="topbar__logo" aria-hidden="true">
          <svg viewBox="0 0 24 24" width="20" height="20">
            <path
              d="M3 13h4l2.5-6 3.5 11 2.5-7 1.5 2H21"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
        </div>
        <div>
          <h1 className="topbar__title">Observability Console</h1>
          <p className="topbar__subtitle">Inventory · Orders · Payments — audited through the logging proxy</p>
        </div>
      </div>

      <div className="topbar__actions">
        <div className={`live-indicator ${online ? 'live-indicator--online' : 'live-indicator--offline'}`}>
          <span className="live-indicator__dot" />
          <span>{online ? 'Live' : 'Offline'}</span>
          <span className="live-indicator__time">Updated {formatTime(lastUpdated?.toISOString())}</span>
        </div>

        <button type="button" className="button button--primary" onClick={onSimulateLoad} disabled={simulating}>
          {simulating ? (
            <>
              <span className="spinner" aria-hidden="true" /> Simulating…
            </>
          ) : (
            <>
              <svg viewBox="0 0 24 24" width="16" height="16" aria-hidden="true">
                <path d="M13 2 4 14h7l-1 8 9-12h-7l1-8Z" fill="currentColor" />
              </svg>
              Simulate load
            </>
          )}
        </button>
      </div>
    </header>
  );
}
