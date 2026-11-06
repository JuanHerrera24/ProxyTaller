const numberFormatter = new Intl.NumberFormat('en-US');

const timeFormatter = new Intl.DateTimeFormat('en-US', {
  hour: '2-digit',
  minute: '2-digit',
  second: '2-digit',
  hour12: false,
});

const dateTimeFormatter = new Intl.DateTimeFormat('en-US', {
  month: 'short',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
  second: '2-digit',
  hour12: false,
});

export const formatNumber = (value) => numberFormatter.format(value ?? 0);

export const formatPercent = (value) => `${(value ?? 0).toFixed(1)}%`;

export const formatDuration = (ms) => {
  if (ms === null || ms === undefined) return '—';
  return ms >= 1000 ? `${(ms / 1000).toFixed(2)} s` : `${Math.round(ms)} ms`;
};

export const formatTime = (iso) => (iso ? timeFormatter.format(new Date(iso)) : '—');

export const formatDateTime = (iso) => (iso ? dateTimeFormatter.format(new Date(iso)) : '—');

export const shortId = (id) => (id ? id.slice(0, 8) : '');

/**
 * Converts the value of a datetime-local input (browser local time) into an ISO-8601 UTC instant.
 */
export const localInputToIso = (value) => (value ? new Date(value).toISOString() : undefined);

export const toPrettyJson = (value) => JSON.stringify(value ?? null, null, 2);
