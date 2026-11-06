import { useCallback, useEffect, useRef, useState } from 'react';

/**
 * Runs an async fetcher immediately and then on a fixed interval.
 * In-flight requests are aborted when dependencies change or the component unmounts,
 * and polling pauses while the browser tab is hidden.
 */
export function usePolling(fetcher, { interval = 3000, deps = [] } = {}) {
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);
  const [lastUpdated, setLastUpdated] = useState(null);

  const fetcherRef = useRef(fetcher);
  const controllerRef = useRef(null);

  fetcherRef.current = fetcher;

  const refresh = useCallback(async () => {
    controllerRef.current?.abort();
    const controller = new AbortController();
    controllerRef.current = controller;

    try {
      const result = await fetcherRef.current(controller.signal);
      if (controller.signal.aborted) return;
      setData(result);
      setError(null);
      setLastUpdated(new Date());
    } catch (err) {
      if (err.name !== 'AbortError') setError(err);
    } finally {
      if (!controller.signal.aborted) setLoading(false);
    }
  }, []);

  useEffect(() => {
    setLoading(true);
    refresh();

    const timer = setInterval(() => {
      if (document.visibilityState === 'visible') refresh();
    }, interval);

    return () => {
      clearInterval(timer);
      controllerRef.current?.abort();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [interval, refresh, ...deps]);

  return { data, error, loading, lastUpdated, refresh };
}
