import PropTypes from 'prop-types';

export function LoadingSpinner({ size, text }) {
  const sizeClasses = {
    sm: 'h-4 w-4 border-2',
    md: 'h-8 w-8 border-3',
    lg: 'h-12 w-12 border-4',
  };

  return (
    <div className="flex flex-col items-center justify-center p-8 text-center" role="status">
      <div
        className={`animate-spin rounded-full border-indigo-600 border-t-transparent ${
          sizeClasses[size] || sizeClasses.md
        }`}
      />
      {text && <span className="mt-3 text-sm font-medium text-slate-600">{text}</span>}
      <span className="sr-only">Loading...</span>
    </div>
  );
}

LoadingSpinner.propTypes = {
  size: PropTypes.oneOf(['sm', 'md', 'lg']),
  text: PropTypes.string,
};

LoadingSpinner.defaultProps = {
  size: 'md',
  text: 'Loading...',
};
