export const POLLING_INTERVAL_MS = 3000;
export const TIMELINE_SIZE = 20;
export const LOGS_PAGE_SIZE = 15;
export const DEGRADED_ERROR_RATE = 15;

export const SERVICES = [
  { id: 'inventory', label: 'Inventory', color: '#3b82f6' },
  { id: 'orders', label: 'Orders', color: '#8b5cf6' },
  { id: 'payments', label: 'Payments', color: '#f59e0b' },
];

export const SERVICE_BY_ID = Object.fromEntries(SERVICES.map((service) => [service.id, service]));

export const STATUSES = ['SUCCESS', 'ERROR'];
