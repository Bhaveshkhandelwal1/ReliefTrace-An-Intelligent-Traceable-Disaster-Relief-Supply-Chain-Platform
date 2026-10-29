import { Link } from 'react-router-dom';
import { useAuth } from '../../../auth/useAuth';
import { useDriverShipments } from '../hooks/useDriverShipments';
import { DataTable } from '../../../components/DataTable';
import { StatusBadge } from '../../../components/StatusBadge';
import { Button } from '../../../components/Button';
import { formatDate } from '../../../utils/formatters';

export function DriverDashboardPage() {
  const { user } = useAuth();
  const { shipments, isLoading } = useDriverShipments(user?.id);

  const columns = [
    {
      key: 'id',
      header: 'Shipment #',
      render: (row) => (
        <span className="font-mono text-xs font-bold text-slate-800">
          #{String(row.shipmentId || row.id).substring(0, 8)}
        </span>
      ),
    },
    {
      key: 'status',
      header: 'Convoy Status',
      render: (row) => <StatusBadge status={row.status} type="status" />,
    },
    {
      key: 'pickup',
      header: 'Pickup Warehouse',
      render: (row) => (
        <span className="text-xs text-slate-700 font-medium">
          {row.pickupAddress?.street || 'Central Logistics Hub A'}
        </span>
      ),
    },
    {
      key: 'delivery',
      header: 'Dropoff Destination',
      render: (row) => (
        <span className="text-xs text-slate-700 font-medium">
          {row.deliveryAddress?.street || 'Camp Alpha Relief Point'}
        </span>
      ),
    },
    {
      key: 'scheduled',
      header: 'Scheduled',
      render: (row) => (
        <span className="text-xs text-slate-500">{formatDate(row.scheduledPickupAt)}</span>
      ),
    },
    {
      key: 'actions',
      header: 'Action',
      render: (row) => (
        <Link to={`/driver/shipment/${row.shipmentId || row.id}`}>
          <Button size="sm" variant="primary">
            Open Run →
          </Button>
        </Link>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Driver Convoy & Route Hub</h1>
          <p className="text-sm text-slate-500 mt-1">
            Assigned shipments, live status tracking, and road condition alerts.
          </p>
        </div>
      </div>

      {/* Driver status card */}
      <div className="rounded-2xl bg-gradient-to-r from-slate-900 to-indigo-950 p-6 text-white shadow-lg">
        <div className="flex items-center justify-between">
          <div>
            <span className="text-xs uppercase font-extrabold tracking-wider text-indigo-400">
              Active Driver Profile
            </span>
            <h2 className="text-xl font-bold mt-1">{user?.name || 'Convoy Driver'}</h2>
            <p className="text-xs text-slate-300 mt-0.5">
              Assigned Vehicle: Heavy Relief Truck 4x4 (Plate:{' '}
              <span className="font-mono font-bold text-white">RLF-782</span>)
            </p>
          </div>
          <div className="text-right">
            <span className="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-bold bg-emerald-500/20 text-emerald-300 border border-emerald-500/30">
              ● Ready for Dispatch
            </span>
          </div>
        </div>
      </div>

      {/* Shipments Table */}
      <DataTable
        columns={columns}
        data={shipments}
        isLoading={isLoading}
        emptyTitle="No assigned shipments"
        emptyDescription="You have no active or scheduled shipments assigned right now."
      />
    </div>
  );
}
