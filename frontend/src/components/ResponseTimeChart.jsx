import { useMemo } from 'react';
import {
  CategoryScale,
  Chart as ChartJS,
  Filler,
  Legend,
  LinearScale,
  LineElement,
  PointElement,
  Tooltip,
} from 'chart.js';
import { Line } from 'react-chartjs-2';
import { SERVICES } from '../constants/services.js';
import { formatTime } from '../utils/format.js';

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Tooltip, Legend, Filler);

const ERROR_COLOR = '#ef4444';
const GRID_COLOR = 'rgba(148, 163, 184, 0.12)';
const TICK_COLOR = '#8b95a7';

/**
 * Aligns every series to the right so that position N always represents the most recent call.
 */
function alignToRight(points, size) {
  const padding = Array.from({ length: Math.max(size - points.length, 0) }, () => null);
  return [...padding, ...points.slice(-size)];
}

export default function ResponseTimeChart({ timeline, size }) {
  const { data, hasData } = useMemo(() => {
    const series = SERVICES.map((service) => ({
      service,
      points: alignToRight(timeline?.[service.id] ?? [], size),
    }));

    return {
      hasData: series.some(({ points }) => points.some(Boolean)),
      data: {
        labels: Array.from({ length: size }, (_, index) => `#${index + 1}`),
        datasets: series.map(({ service, points }) => ({
          label: service.label,
          data: points.map((point) => point?.durationMs ?? null),
          meta: points,
          borderColor: service.color,
          backgroundColor: `${service.color}22`,
          pointBackgroundColor: points.map((point) => (point?.status === 'ERROR' ? ERROR_COLOR : service.color)),
          pointBorderColor: points.map((point) => (point?.status === 'ERROR' ? ERROR_COLOR : service.color)),
          pointRadius: points.map((point) => (point?.status === 'ERROR' ? 5 : 3)),
          pointHoverRadius: 6,
          borderWidth: 2,
          tension: 0.35,
          spanGaps: true,
        })),
      },
    };
  }, [timeline, size]);

  const options = useMemo(
    () => ({
      responsive: true,
      maintainAspectRatio: false,
      animation: { duration: 350 },
      interaction: { mode: 'nearest', intersect: false },
      plugins: {
        legend: {
          position: 'top',
          align: 'end',
          labels: { color: '#cbd5e1', usePointStyle: true, boxWidth: 8, boxHeight: 8, padding: 16 },
        },
        tooltip: {
          backgroundColor: '#0f1623',
          borderColor: '#273246',
          borderWidth: 1,
          padding: 10,
          callbacks: {
            title: (items) => {
              const point = items[0]?.dataset.meta[items[0].dataIndex];
              return point ? `${items[0].dataset.label} · ${formatTime(point.timestamp)}` : '';
            },
            label: (item) => {
              const point = item.dataset.meta[item.dataIndex];
              return point ? ` ${point.operation} — ${point.durationMs} ms (${point.status})` : '';
            },
          },
        },
      },
      scales: {
        x: {
          title: { display: true, text: 'Latest calls (oldest → newest)', color: TICK_COLOR },
          grid: { color: GRID_COLOR },
          ticks: { color: TICK_COLOR },
        },
        y: {
          beginAtZero: true,
          title: { display: true, text: 'Response time (ms)', color: TICK_COLOR },
          grid: { color: GRID_COLOR },
          ticks: { color: TICK_COLOR },
        },
      },
    }),
    [],
  );

  return (
    <section className="panel">
      <header className="panel__header">
        <div>
          <h2 className="panel__title">Response time · last {size} calls per service</h2>
          <p className="panel__subtitle">Red markers indicate failed calls</p>
        </div>
      </header>
      <div className="chart-container">
        {hasData ? (
          <Line data={data} options={options} />
        ) : (
          <div className="empty-state">
            <p className="empty-state__title">No traffic recorded yet</p>
            <p className="empty-state__text">Use “Simulate load” to generate requests across all services.</p>
          </div>
        )}
      </div>
    </section>
  );
}
