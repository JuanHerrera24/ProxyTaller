import { DEGRADED_ERROR_RATE, SERVICES } from '../constants/services.js';
import { formatDuration, formatNumber, formatPercent, formatTime } from '../utils/format.js';

export default function ServiceCards({ summary, loading }) {
  const metricsById = Object.fromEntries((summary?.services ?? []).map((metrics) => [metrics.serviceId, metrics]));
  const threshold = summary?.degradedThreshold ?? DEGRADED_ERROR_RATE;

  return (
    <section className="service-grid" aria-label="Service health">
      {SERVICES.map((service) => (
        <ServiceCard
          key={service.id}
          service={service}
          metrics={metricsById[service.id]}
          threshold={threshold}
          loading={loading}
        />
      ))}
    </section>
  );
}

function ServiceCard({ service, metrics, threshold, loading }) {
  const degraded = metrics ? metrics.errorRate > threshold : false;
  const successRate = metrics?.totalCalls ? metrics.successRate : 0;

  return (
    <article
      className={`service-card ${degraded ? 'service-card--degraded' : ''} ${loading ? 'is-loading' : ''}`}
      style={{ '--accent': service.color }}
    >
      <header className="service-card__header">
        <div className="service-card__name">
          <span className="service-card__swatch" />
          <h3>{metrics?.displayName ?? `${service.label} Service`}</h3>
        </div>
        <span className={`health-pill ${degraded ? 'health-pill--critical' : 'health-pill--ok'}`}>
          {degraded ? 'Degraded' : 'Healthy'}
        </span>
      </header>

      <dl className="service-card__metrics">
        <div>
          <dt>Total calls</dt>
          <dd>{formatNumber(metrics?.totalCalls)}</dd>
        </div>
        <div>
          <dt>Success rate</dt>
          <dd>{formatPercent(successRate)}</dd>
        </div>
        <div>
          <dt>Avg. response</dt>
          <dd>{formatDuration(metrics?.averageResponseTimeMs ?? 0)}</dd>
        </div>
      </dl>

      <div className="service-card__bar" aria-hidden="true">
        <span style={{ width: `${successRate}%` }} />
      </div>

      <footer className="service-card__footer">
        <span>
          Errors <strong className={degraded ? 'text-critical' : ''}>{formatNumber(metrics?.errorCount)}</strong> (
          {formatPercent(metrics?.errorRate)})
        </span>
        <span>Last call {formatTime(metrics?.lastCallAt)}</span>
      </footer>

      {degraded && (
        <p className="service-card__alert" role="status">
          Error rate above {threshold}% threshold
        </p>
      )}
    </article>
  );
}
