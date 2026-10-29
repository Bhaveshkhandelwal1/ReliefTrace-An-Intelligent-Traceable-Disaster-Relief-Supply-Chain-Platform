import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { DataTable } from '../../src/components/DataTable';

describe('DataTable', () => {
  const columns = [
    { key: 'name', header: 'Item Name' },
    { key: 'qty', header: 'Quantity' },
  ];

  it('renders loading state when isLoading is true', () => {
    render(<DataTable columns={columns} data={[]} isLoading={true} />);
    expect(screen.getByRole('status')).toBeInTheDocument();
  });

  it('renders empty state when data array is empty', () => {
    render(
      <DataTable
        columns={columns}
        data={[]}
        isLoading={false}
        emptyTitle="No Aid Records"
        emptyDescription="Please check back later."
      />
    );
    expect(screen.getByText('No Aid Records')).toBeInTheDocument();
    expect(screen.getByText('Please check back later.')).toBeInTheDocument();
  });

  it('renders table rows when data is populated', () => {
    const data = [
      { id: '1', name: 'Water Purifier', qty: 10 },
      { id: '2', name: 'Emergency Blanket', qty: 50 },
    ];
    render(<DataTable columns={columns} data={data} isLoading={false} />);
    expect(screen.getByText('Water Purifier')).toBeInTheDocument();
    expect(screen.getByText('Emergency Blanket')).toBeInTheDocument();
  });
});
