import PropTypes from 'prop-types';

export function StatusBadge({ status, type }) {
  if (!status) return null;

  const normalized = String(status).toUpperCase();

  const getStyle = () => {
    switch (normalized) {
      // Urgency
      case 'CRITICAL':
        return 'bg-rose-100 text-rose-800 border-rose-200';
      case 'HIGH':
        return 'bg-amber-100 text-amber-800 border-amber-200';
      case 'MEDIUM':
        return 'bg-blue-100 text-blue-800 border-blue-200';
      case 'LOW':
        return 'bg-slate-100 text-slate-800 border-slate-200';

      // General Positive
      case 'DELIVERED':
      case 'APPROVED':
      case 'FULFILLED':
      case 'RECEIVED':
      case 'ACTIVE':
        return 'bg-emerald-100 text-emerald-800 border-emerald-200';

      // In progress
      case 'IN_TRANSIT':
      case 'PICKED_UP':
      case 'ASSIGNED':
      case 'UNDER_REVIEW':
      case 'MONITORING':
        return 'bg-indigo-100 text-indigo-800 border-indigo-200';

      // Pending
      case 'SUBMITTED':
      case 'SUGGESTED':
      case 'REGISTERED':
      case 'PLEDGED':
      case 'OPEN':
      case 'CREATED':
        return 'bg-amber-100 text-amber-800 border-amber-200';

      // Negative
      case 'CANCELLED':
      case 'REJECTED':
      case 'FAILED':
      case 'DISPUTED':
      case 'CLOSED':
        return 'bg-rose-100 text-rose-800 border-rose-200';

      default:
        return 'bg-slate-100 text-slate-700 border-slate-200';
    }
  };

  const displayText = normalized.replace(/_/g, ' ');

  return (
    <span
      className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold border ${getStyle()}`}
      data-testid={`status-badge-${type || 'status'}`}
    >
      <span className="mr-1.5 h-1.5 w-1.5 rounded-full bg-current opacity-75" />
      {displayText}
    </span>
  );
}

StatusBadge.propTypes = {
  status: PropTypes.string,
  type: PropTypes.string,
};

StatusBadge.defaultProps = {
  status: '',
  type: 'status',
};
