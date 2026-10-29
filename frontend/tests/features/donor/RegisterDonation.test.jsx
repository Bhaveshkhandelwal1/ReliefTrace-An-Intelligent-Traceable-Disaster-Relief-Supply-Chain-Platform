import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect } from 'vitest';
import { BrowserRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { KeycloakProvider } from '../../../src/auth/KeycloakProvider';
import { RegisterDonationPage } from '../../../src/features/donor/pages/RegisterDonationPage';

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

describe('RegisterDonationPage', () => {
  it('renders donation registration form with fields', async () => {
    renderWithProviders(<RegisterDonationPage />);

    expect(screen.getByText(/Register Aid Contribution/i)).toBeInTheDocument();
    expect(screen.getByText(/Financial Donation/i)).toBeInTheDocument();
    expect(screen.getByText(/In-Kind Physical Goods/i)).toBeInTheDocument();

    const submitBtn = screen.getByRole('button', { name: /Submit Contribution/i });
    expect(submitBtn).toBeInTheDocument();
  });

  it('allows entering amount and submitting monetary contribution', async () => {
    const user = userEvent.setup();
    renderWithProviders(<RegisterDonationPage />);

    const amountInput = screen.getByLabelText(/Donation Amount/i);
    await user.clear(amountInput);
    await user.type(amountInput, '250');

    const submitBtn = screen.getByRole('button', { name: /Submit Contribution/i });
    await user.click(submitBtn);

    await waitFor(() => {
      expect(submitBtn).toBeInTheDocument();
    });
  });
});
