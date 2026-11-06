const API_BASE_URL = (import.meta.env.VITE_API_URL ?? '').replace(/\/+$/, '');

export class ApiError extends Error {
  constructor(message, status, payload) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.payload = payload;
  }
}

async function request(path, { method = 'GET', query, body, signal } = {}) {
  const url = new URL(`${API_BASE_URL}${path}`, window.location.origin);

  Object.entries(query ?? {}).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      url.searchParams.set(key, value);
    }
  });

  let response;
  try {
    response = await fetch(url, {
      method,
      signal,
      headers: body ? { 'Content-Type': 'application/json' } : undefined,
      body: body ? JSON.stringify(body) : undefined,
    });
  } catch (error) {
    if (error.name === 'AbortError') throw error;
    throw new ApiError('Unable to reach the monitoring API', 0);
  }

  const payload = await response.json().catch(() => null);

  if (!response.ok) {
    throw new ApiError(payload?.message ?? `Request failed with status ${response.status}`, response.status, payload);
  }
  return payload;
}

export const monitoringApi = {
  getSummary: (signal) => request('/api/metrics/summary', { signal }),

  getTimeline: (limit, signal) => request('/api/metrics/timeline', { query: { limit }, signal }),

  getLogs: ({ service, status, from, to, page, size }, signal) =>
    request('/api/metrics/logs', { query: { service, status, from, to, page, size }, signal }),

  simulateLoad: (calls) => request('/api/metrics/simulate-load', { method: 'POST', query: { calls } }),
};
