import PropTypes from 'prop-types';
import { formatDate } from '../utils/formatters';

export function ShipmentTimeline({ events }) {
  if (!events || events.length === 0) {
    return <div className="text-xs text-slate-500 py-3">No tracking events recorded yet.</div>;
  }

  return (
    <div className="relative pl-6 space-y-6 before:absolute before:left-2 before:top-2 before:bottom-2 before:w-0.5 before:bg-slate-200">
      {events.map((evt, idx) => (
        <div key={idx} className="relative group">
          {/* Timeline node */}
          <div className="absolute -left-6 top-1 flex h-4 w-4 items-center justify-center rounded-full bg-indigo-600 ring-4 ring-white" />
          <div>
            <span className="text-xs font-semibold uppercase tracking-wider text-indigo-600">
              {evt.eventType || 'STATUS UPDATE'}
            </span>
            <p className="text-sm font-medium text-slate-800 mt-0.5">{evt.description}</p>
            <time className="text-[11px] text-slate-400 mt-1 block">
              {formatDate(evt.occurredAt)}
            </time>
          </div>
        </div>
      ))}
    </div>
  );
}

ShipmentTimeline.propTypes = {
  events: PropTypes.arrayOf(
    PropTypes.shape({
      eventType: PropTypes.string,
      occurredAt: PropTypes.string.isRequired,
      description: PropTypes.string.isRequired,
    })
  ),
};

ShipmentTimeline.defaultProps = {
  events: [],
};
