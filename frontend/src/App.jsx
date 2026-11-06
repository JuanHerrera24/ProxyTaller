import { useCallback, useState } from 'react';
import { monitoringApi } from './api/client.js';
import { usePolling } from './hooks/usePolling.js';
import { LOGS_PAGE_SIZE, POLLING_INTERVAL_MS, TIMELINE_SIZE } from './constants/services.js';
import { localInputToIso } from './utils/format.js';
import Header from './components/Header.jsx';
import OverviewStrip from './components/OverviewStrip.jsx';
import ServiceCards from './components/ServiceCards.jsx';
import ResponseTimeChart from './components/ResponseTimeChart.jsx';
import LogFilters from './components/LogFilters.jsx';
import LogsTable from './components/LogsTable.jsx';
import Toast from './components/Toast.jsx';

const EMPTY_FILTERS = { service: '', status: '', from: '', to: '' };

export default function App() {
  const [filters, setFilters] = useState(EMPTY_FILTERS);
  const [page, setPage] = useState(0);
  const [simulating, setSimulating] = useState(false);
  const [toast, setToast] = useState(null);
  const closeToast = useCallback(() => setToast(null), []);

  const fetchDashboard = useCallback(async (signal) => {
    const [summary, timeline] = await Promise.all([
      monitoringApi.getSummary(signal),
      monitoringApi.getTimeline(TIMELINE_SIZE, signal),
    ]);
    return { summary, timeline };
  }, []);

  const fetchLogs = useCallback(
    (signal) => {
      const validRange = !filters.from || !filters.to || new Date(filters.from) <= new Date(filters.to);
      return monitoringApi.getLogs(
        {
          service: filters.service,
          status: filters.status,
          from: validRange ? localInputToIso(filters.from) : undefined,
          to: validRange ? localInputToIso(filters.to) : undefined,
          page,
          size: LOGS_PAGE_SIZE,
        },
        signal,
      );
    },
    [filters, page],
  );

  const dashboard = usePolling(fetchDashboard, { interval: POLLING_INTERVAL_MS });
  const logs = usePolling(fetchLogs, { interval: POLLING_INTERVAL_MS, deps: [fetchLogs] });

  const handleFiltersChange = (nextFilters) => {
    setFilters(nextFilters);
    setPage(0);
  };

  const handleSimulateLoad = async () => {
    setSimulating(true);
    try {
      const result = await monitoringApi.simulateLoad();
      setToast({
        tone: 'success',
        title: 'Load simulation completed',
        message: `${result.requestedCalls} calls in ${result.elapsedMs} ms · ${result.successCount} succeeded · ${result.errorCount} failed`,
      });
      await Promise.all([dashboard.refresh(), logs.refresh()]);
    } catch (error) {
      setToast({ tone: 'error', title: 'Load simulation failed', message: error.message });
    } finally {
      setSimulating(false);
    }
  };

  const connectionError = dashboard.error ?? logs.error;

  return (
    <div className="app-shell">
      <Header
        lastUpdated={dashboard.lastUpdated}
        online={!connectionError}
        simulating={simulating}
        onSimulateLoad={handleSimulateLoad}
      />

      <main className="content">
        {connectionError && (
          <div className="banner banner--error" role="alert">
            <strong>Connection issue.</strong> {connectionError.message}. Retrying every {POLLING_INTERVAL_MS / 1000} seconds.
          </div>
        )}

        <OverviewStrip summary={dashboard.data?.summary} />

        <ServiceCards summary={dashboard.data?.summary} loading={dashboard.loading && !dashboard.data} />

        <ResponseTimeChart timeline={dashboard.data?.timeline} size={TIMELINE_SIZE} />

        <section className="panel">
          <header className="panel__header">
            <div>
              <h2 className="panel__title">Request log</h2>
              <p className="panel__subtitle">Every call intercepted by the logging proxy · refreshed every 3 seconds</p>
            </div>
          </header>
          <LogFilters filters={filters} onChange={handleFiltersChange} onReset={() => handleFiltersChange(EMPTY_FILTERS)} />
          <LogsTable pageData={logs.data} loading={logs.loading && !logs.data} onPageChange={setPage} />
        </section>
      </main>

      {toast && <Toast {...toast} onClose={closeToast} />}
    </div>
  );
}
