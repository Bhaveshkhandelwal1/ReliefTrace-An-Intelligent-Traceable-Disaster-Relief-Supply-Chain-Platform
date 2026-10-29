import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect } from 'vitest';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { KeycloakProvider } from '../../../src/auth/KeycloakProvider';
import { ShipmentRunPage } from '../../../src/features/driver/pages/ShipmentRunPage';

function renderWithProviders(ui) {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
    },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <KeycloakProvider>
        <MemoryRouter initialEntries={['/driver/shipment/sh-3001-mock']}>
          <Routes>
            <Route path="/driver/shipment/:shipmentId" element={ui} />
          </Routes>
        </MemoryRouter>
      </KeycloakProvider>
    </QueryClientProvider>
  );
}

describe('ShipmentRunPage', () => {
  it('renders shipment run details and action buttons', async () => {
    renderWithProviders(<ShipmentRunPage />);

    expect(screen.getByText(/Route Waypoints/i)).toBeInTheDocument();
    expect(screen.getByText(/Pickup Depot/i)).toBeInTheDocument();
    expect(screen.getByText(/Delivery Destination/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Mark PICKED UP/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Confirm DELIVERED/i })).toBeInTheDocument();
  });

  it('opens road closure modal when report button is clicked', async () => {
    const user = userEvent.setup();
    renderWithProviders(<ShipmentRunPage />);

    const reportClosureBtn = screen.getByRole('button', {
      name: /Report Road Closure \/ Delay/i,
    });
    await user.click(reportClosureBtn);

    await waitFor(() => {
      expect(screen.getByText(/Report Road Hazard or Route Closure/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/Incident Cause \/ Hazard/i)).toBeInTheDocument();
    });
  });
});
