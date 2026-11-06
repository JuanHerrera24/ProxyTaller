import { formatDuration, formatNumber, formatPercent } from '../utils/format.js';

export default function OverviewStrip({ summary }) {
  const degradedCount = summary?.services.filter((service) => service.degraded).length ?? 0;

  const items = [
    { label: 'Total requests', value: formatNumber(summary?.totalCalls) },
    { label: 'Global error rate', value: formatPercent(summary?.errorRate) },
    { label: 'Avg. response time', value: formatDuration(summary?.averageResponseTimeMs ?? 0) },
    {
      label: 'Service health',
      value: degradedCount === 0 ? 'All operational' : `${degradedCount} degraded`,
      tone: degradedCount === 0 ? 'ok' : 'critical',
    },
  ];

  return (
    <section className="overview" aria-label="Global indicators">
      {items.map((item) => (
        <div key={item.label} className="overview__item">
          <span className="overview__label">{item.label}</span>
          <span className={`overview__value ${item.tone ? `overview__value--${item.tone}` : ''}`}>{item.value}</span>
        </div>
      ))}
    </section>
  );
}
