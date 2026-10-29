import PropTypes from 'prop-types';

export function StatCard({ title, value, subtitle, icon, trend, color }) {
  const colorSchemes = {
    indigo: 'bg-indigo-50 text-indigo-600',
    emerald: 'bg-emerald-50 text-emerald-600',
    amber: 'bg-amber-50 text-amber-600',
    rose: 'bg-rose-50 text-rose-600',
    blue: 'bg-blue-50 text-blue-600',
  };

  return (
    <div className="rounded-2xl border border-slate-100 bg-white p-5 shadow-sm transition-all hover:shadow-md">
      <div className="flex items-center justify-between">
        <span className="text-sm font-medium text-slate-500">{title}</span>
        {icon && (
          <div
            className={`flex h-10 w-10 items-center justify-center rounded-xl ${
              colorSchemes[color] || colorSchemes.indigo
            }`}
          >
            {icon}
          </div>
        )}
      </div>
      <div className="mt-3 flex items-baseline gap-2">
        <span className="text-2xl font-bold tracking-tight text-slate-900">{value}</span>
        {trend && (
          <span
            className={`text-xs font-semibold ${
              trend.isPositive ? 'text-emerald-600' : 'text-rose-600'
            }`}
          >
            {trend.isPositive ? '↑' : '↓'} {trend.text}
          </span>
        )}
      </div>
      {subtitle && <p className="mt-1 text-xs text-slate-400">{subtitle}</p>}
    </div>
  );
}

StatCard.propTypes = {
  title: PropTypes.string.isRequired,
  value: PropTypes.oneOfType([PropTypes.string, PropTypes.number]).isRequired,
  subtitle: PropTypes.string,
  icon: PropTypes.node,
  trend: PropTypes.shape({
    isPositive: PropTypes.bool.isRequired,
    text: PropTypes.string.isRequired,
  }),
  color: PropTypes.oneOf(['indigo', 'emerald', 'amber', 'rose', 'blue']),
};

StatCard.defaultProps = {
  subtitle: null,
  icon: null,
  trend: null,
  color: 'indigo',
};
