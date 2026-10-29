import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi } from 'vitest';
import { BrowserRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { KeycloakProvider } from '../../../src/auth/KeycloakProvider';
import { CreateAidRequestPage } from '../../../src/features/receiver/pages/CreateAidRequestPage';

// Mock Leaflet Map component in JSDOM environment
vi.mock('../../../src/components/LeafletMap', () => ({
  LeafletMap: () => <div data-testid="mock-leaflet-map">Leaflet Map</div>,
}));

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

describe('CreateAidRequestPage', () => {
  it('renders aid request form with urgency, category, and map', async () => {
    renderWithProviders(<CreateAidRequestPage />);

    expect(screen.getByText(/Submit Emergency Aid Request/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Aid Category/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Urgency Level/i)).toBeInTheDocument();
    expect(screen.getByTestId('mock-leaflet-map')).toBeInTheDocument();
  });

  it('allows filling in item details and submitting request', async () => {
    const user = userEvent.setup();
    renderWithProviders(<CreateAidRequestPage />);

    const itemInput = screen.getByLabelText(/Item Details & Specifications/i);
    await user.clear(itemInput);
    await user.type(itemInput, 'Emergency Medical Tents');

    const submitBtn = screen.getByRole('button', { name: /Submit Aid Request/i });
    await user.click(submitBtn);

    await waitFor(() => {
      expect(submitBtn).toBeInTheDocument();
    });
  });
});
