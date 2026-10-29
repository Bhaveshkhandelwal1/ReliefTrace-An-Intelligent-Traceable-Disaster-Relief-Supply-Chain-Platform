import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect } from 'vitest';
import { BrowserRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { KeycloakProvider } from '../../../src/auth/KeycloakProvider';
import { AllocationApprovalPage } from '../../../src/features/admin/pages/AllocationApprovalPage';

function renderWithProviders(ui) {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
    },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <KeycloakProvider>
        <BrowserRouter>{ui}</BrowserRouter>
      </KeycloakProvider>
    </QueryClientProvider>
  );
}

describe('AllocationApprovalPage', () => {
  it('renders allocation approval screen with suggested allocations', async () => {
    renderWithProviders(<AllocationApprovalPage />);

    expect(screen.getByText(/Aid Allocation Approvals/i)).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText('Emergency Medical Trauma Kits')).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /Approve/i })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /Reject/i })).toBeInTheDocument();
    });
  });

  it('opens reject modal when reject button is clicked', async () => {
    const user = userEvent.setup();
    renderWithProviders(<AllocationApprovalPage />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Reject/i })).toBeInTheDocument();
    });

    const rejectBtn = screen.getByRole('button', { name: /Reject/i });
    await user.click(rejectBtn);

    await waitFor(() => {
      expect(screen.getByText(/Reject Allocation Suggestion/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/Reason for Rejection/i)).toBeInTheDocument();
    });
  });
});
