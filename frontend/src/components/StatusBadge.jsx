export default function StatusBadge({ status }) {
  const tone = status === 'SUCCESS' ? 'success' : 'error';
  return (
    <span className={`status-badge status-badge--${tone}`}>
      <span className="status-badge__dot" />
      {status}
    </span>
  );
}
