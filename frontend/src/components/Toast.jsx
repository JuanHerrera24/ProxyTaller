import { useEffect } from 'react';

const AUTO_DISMISS_MS = 5000;

export default function Toast({ tone, title, message, onClose }) {
  useEffect(() => {
    const timer = setTimeout(onClose, AUTO_DISMISS_MS);
    return () => clearTimeout(timer);
  }, [title, message, onClose]);

  return (
    <div className={`toast toast--${tone}`} role="status" aria-live="polite">
      <div>
        <p className="toast__title">{title}</p>
        <p className="toast__message">{message}</p>
      </div>
      <button type="button" className="toast__close" onClick={onClose} aria-label="Dismiss notification">
        ×
      </button>
    </div>
  );
}
