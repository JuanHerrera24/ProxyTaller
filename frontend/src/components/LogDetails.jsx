import { useState } from 'react';
import { formatDateTime, formatDuration, toPrettyJson } from '../utils/format.js';

export default function LogDetails({ log }) {
  const failed = log.status === 'ERROR';

  return (
    <div className="log-details">
      <dl className="log-details__meta">
        <div>
          <dt>Request ID</dt>
          <dd className="mono">{log.requestId}</dd>
        </div>
        <div>
          <dt>Endpoint</dt>
          <dd className="mono">
            POST /api/services/{log.serviceId}/{log.operation}
          </dd>
        </div>
        <div>
          <dt>Started at</dt>
          <dd>{formatDateTime(log.timestamp)}</dd>
        </div>
        <div>
          <dt>Duration</dt>
          <dd>{formatDuration(log.durationMs)}</dd>
        </div>
      </dl>

      <div className="log-details__grid">
        <JsonBlock title="Input parameters" value={log.parameters} />

        {failed ? (
          <div className="code-block code-block--error">
            <div className="code-block__header">
              <span>Error · {log.error?.type}</span>
            </div>
            <p className="code-block__message">{log.error?.message}</p>
            <pre>{(log.error?.stackTrace ?? []).join('\n')}</pre>
          </div>
        ) : (
          <JsonBlock title="Response" value={log.response} />
        )}
      </div>
    </div>
  );
}

function JsonBlock({ title, value }) {
  const [copied, setCopied] = useState(false);
  const json = toPrettyJson(value);

  const copy = async (event) => {
    event.stopPropagation();
    try {
      await navigator.clipboard.writeText(json);
      setCopied(true);
      setTimeout(() => setCopied(false), 1500);
    } catch {
      setCopied(false);
    }
  };

  return (
    <div className="code-block">
      <div className="code-block__header">
        <span>{title}</span>
        <button type="button" className="code-block__copy" onClick={copy}>
          {copied ? 'Copied' : 'Copy'}
        </button>
      </div>
      <pre>{json}</pre>
    </div>
  );
}
