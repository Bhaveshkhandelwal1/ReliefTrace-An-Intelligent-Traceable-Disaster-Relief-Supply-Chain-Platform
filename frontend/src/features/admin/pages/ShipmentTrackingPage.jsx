import { useState } from 'react';
import { useAdminOperations } from '../hooks/useAdminOperations';
import { DataTable } from '../../../components/DataTable';
import { StatusBadge } from '../../../components/StatusBadge';
import { ShipmentTimeline } from '../../../components/ShipmentTimeline';
import { Button } from '../../../components/Button';
import { formatDate } from '../../../utils/formatters';

const SAMPLE_TIMELINE_EVENTS = [
  {
    eventType: 'CREATED',
    occurredAt: '2026-10-29T08:00:00Z',
    description: 'Shipment created and scheduled for dispatch from Central Depot.',
  },
  {
    eventType: 'ASSIGNED',
    occurredAt: '2026-10-29T09:15:00Z',
    description: 'Driver Carlos Mendez assigned with Heavy Truck 4x4.',
  },
  {
    eventType: 'PICKED_UP',
    occurredAt: '2026-10-29T10:30:00Z',
    description: 'Cargo verified and loaded. Convoy departed pickup warehouse.',
  },
  {
    eventType: 'IN_TRANSIT',
    occurredAt: '2026-10-29T11:45:00Z',
    description: 'En route via Highway 101 corridor. Approaching Sector 3.',
  },
];

export function ShipmentTrackingPage() {
  const { shipments, isLoadingShipments } = useAdminOperations();
  const [selectedShipment, setSelectedShipment] = useState(null);

  const columns = [
    {
      key: 'id',
      header: 'Shipment Ref',
      render: (row) => (
        <span className="font-mono text-xs font-bold text-slate-800">
          #{String(row.shipmentId || row.id).substring(0, 8)}
        </span>
      ),
    },
    {
      key: 'driver',
      header: 'Driver / Vehicle',
      render: (row) => (
        <div>
          <span className="font-semibold text-slate-800 block text-xs">
            {row.driverName || 'Carlos Mendez'}
          </span>
          <span className="text-[11px] text-slate-500 font-mono">Plate: RLF-782</span>
        </div>
      ),
    },
    {
      key: 'status',
      header: 'Status',
      render: (row) => <StatusBadge status={row.status} type="status" />,
    },
    {
      key: 'date',
      header: 'Scheduled Date',
      render: (row) => (
        <span className="text-xs text-slate-500">{formatDate(row.scheduledPickupAt)}</span>
      ),
    },
    {
      key: 'actions',
      header: 'Action',
      render: (row) => (
        <Button size="sm" variant="outline" onClick={() => setSelectedShipment(row)}>
          Track Timeline →
        </Button>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <div className="border-b border-slate-200 pb-4">
        <h1 className="text-2xl font-bold text-slate-900">
          Shipment Logistics & Timeline Tracking
        </h1>
        <p className="text-sm text-slate-500 mt-1">
          Monitor chain-of-custody tracking events and delivery verification across all active
          convoys.
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Shipments Table */}
        <div className="lg:col-span-2">
          <DataTable
            columns={columns}
            data={shipments}
            isLoading={isLoadingShipments}
            emptyTitle="No active shipments"
            emptyDescription="Dispatched shipments will appear here with live timeline updates."
          />
        </div>

        {/* Timeline Panel */}
        <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm flex flex-col">
          <h2 className="text-base font-bold text-slate-900 mb-1">
            {selectedShipment
              ? `Tracking Shipment #${String(selectedShipment.shipmentId || selectedShipment.id).substring(0, 8)}`
              : 'Select a Shipment'}
          </h2>
          <p className="text-xs text-slate-500 mb-6">
            Chronological event trail and GPS milestones
          </p>

          <div className="flex-1 overflow-y-auto">
            <ShipmentTimeline events={selectedShipment?.events || SAMPLE_TIMELINE_EVENTS} />
          </div>
        </div>
      </div>
    </div>
  );
}
