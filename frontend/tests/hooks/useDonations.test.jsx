import PropTypes from 'prop-types';
import { renderHook, waitFor } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useDonations } from '../../src/features/donor/hooks/useDonations';

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
    },
  });

  const Wrapper = ({ children }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );

  Wrapper.propTypes = {
    children: PropTypes.node.isRequired,
  };

  return Wrapper;
}

describe('useDonations hook', () => {
  it('fetches donations and impact successfully via MSW', async () => {
    const { result } = renderHook(() => useDonations('donor-123-uuid'), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isLoadingDonations).toBe(false);
    });

    expect(result.current.donations).toHaveLength(1);
    expect(result.current.donations[0].donationId).toBe('don-1001-mock');
    expect(result.current.impact?.totalMonetary).toBe(15400);
  });
});
