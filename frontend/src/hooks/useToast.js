import toast from 'react-hot-toast';

export function useToast() {
  return {
    success: (message, options) =>
      toast.success(message, {
        duration: 4000,
        position: 'top-right',
        style: {
          background: '#064e3b',
          color: '#ecfdf5',
          borderRadius: '0.5rem',
          fontWeight: 500,
        },
        ...options,
      }),
    error: (message, options) =>
      toast.error(message, {
        duration: 5000,
        position: 'top-right',
        style: {
          background: '#7f1d1d',
          color: '#fef2f2',
          borderRadius: '0.5rem',
          fontWeight: 500,
        },
        ...options,
      }),
    info: (message, options) =>
      toast(message, {
        duration: 4000,
        position: 'top-right',
        icon: 'ℹ️',
        style: {
          background: '#1e293b',
          color: '#f8fafc',
          borderRadius: '0.5rem',
          fontWeight: 500,
        },
        ...options,
      }),
  };
}
