import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { StatusBadge } from '../../src/components/StatusBadge';

describe('StatusBadge', () => {
  it('renders critical urgency badge with correct text', () => {
    render(<StatusBadge status="CRITICAL" type="urgency" />);
    const badge = screen.getByTestId('status-badge-urgency');
    expect(badge).toBeInTheDocument();
    expect(badge).toHaveTextContent(/critical/i);
    expect(badge).toHaveClass('text-rose-800');
  });

  it('renders delivered status badge with green styling', () => {
    render(<StatusBadge status="DELIVERED" type="status" />);
    const badge = screen.getByTestId('status-badge-status');
    expect(badge).toBeInTheDocument();
    expect(badge).toHaveTextContent(/delivered/i);
    expect(badge).toHaveClass('text-emerald-800');
  });

  it('returns null when no status is provided', () => {
    const { container } = render(<StatusBadge status="" />);
    expect(container).toBeEmptyDOMElement();
  });
});
