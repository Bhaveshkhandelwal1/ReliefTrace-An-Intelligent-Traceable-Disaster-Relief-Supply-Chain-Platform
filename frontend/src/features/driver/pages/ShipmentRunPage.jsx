import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useAuth } from '../../../auth/useAuth';
import { useDriverShipments } from '../hooks/useDriverShipments';
import { useToast } from '../../../hooks/useToast';
import { reportClosureSchema } from '../../../utils/schemas';
import { StatusBadge } from '../../../components/StatusBadge';
import { Button } from '../../../components/Button';
import { Modal } from '../../../components/Modal';

export function ShipmentRunPage() {
  const { shipmentId } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const {
    shipments,
    pickupShipment,
    isPickingUp,
    deliverShipment,
    isDelivering,
    disputeShipment,
    isDisputing,
    reportRoadClosure,
    isReportingClosure,
  } = useDriverShipments(user?.id);
  const toast = useToast();

  const [isClosureModalOpen, setIsClosureModalOpen] = useState(false);

  // Find active shipment from cache or fallback default
  const shipment = shipments.find((s) => (s.shipmentId || s.id) === shipmentId) || {
    shipmentId: shipmentId || 'sh-7789-abc',
    status: 'ASSIGNED',
    pickupAddress: { street: 'Main Supply Warehouse Delta', city: 'Sector 1' },
    deliveryAddress: { street: 'Camp Hope Distribution Depot', city: 'Sector 5' },
    receiverName: 'Coordinator Maria Gomez',
    receiverPhone: '+1-555-0199',
    itemsSummary: '500x MRE Food Rations, 200x Clean Water Jugs',
  };

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm({
    resolver: zodResolver(reportClosureSchema),
    defaultValues: {
      reason: 'Bridge washed out due to flash flood waters',
      location: 'Route 101 North at Mile Marker 42',
      estimatedDelayMinutes: 90,
      alternativeRouteNotes: 'Rerouting convoy via eastern ridge access gravel road.',
    },
  });

  const handlePickup = async () => {
    try {
      await pickupShipment(shipmentId);
      toast.success('Shipment marked as PICKED UP! Inventory locked for transit.');
    } catch {
      toast.error('Failed to update status to picked up.');
    }
  };

  const handleDeliver = async () => {
    try {
      await deliverShipment({
        shipmentId,
        data: {
          proofOfDeliveryUrl: 'https://images.unsplash.com/photo-1593113598332-cd288d649433',
          recipientSignature: 'Driver Verified Handover',
          deliveredAt: new Date().toISOString(),
        },
      });
      toast.success('Shipment successfully DELIVERED to relief camp!');
      navigate('/driver');
    } catch {
      toast.error('Failed to record delivery.');
    }
  };

  const handleDispute = async () => {
    try {
      await disputeShipment({
        shipmentId,
        data: {
          reason: 'Severe weather / road impassable',
          evidenceUrl: 'https://images.unsplash.com/photo-1593113598332-cd288d649433',
        },
      });
      toast.error('Shipment reported as FAILED / DISPUTED.');
    } catch {
      toast.error('Failed to report issue.');
    }
  };

  const onReportClosureSubmit = async (data) => {
    try {
      await reportRoadClosure(data);
      toast.info('Road blockage reported to Central Operations Dispatch.');
      setIsClosureModalOpen(false);
      reset();
    } catch {
      toast.error('Failed to submit road closure report.');
    }
  };

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      <div className="flex items-center justify-between border-b border-slate-200 pb-4">
        <div>
          <button
            type="button"
            onClick={() => navigate('/driver')}
            className="text-xs font-semibold text-slate-500 hover:text-slate-800 mb-1"
          >
            ← Back to Convoy Hub
          </button>
          <h1 className="text-2xl font-bold text-slate-900">
            Shipment Run #{String(shipment.shipmentId).substring(0, 8)}
          </h1>
        </div>
        <StatusBadge status={shipment.status} type="status" />
      </div>

      {/* Route Addresses */}
      <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm space-y-4">
        <h2 className="text-sm font-bold uppercase tracking-wider text-slate-500">
          Route Waypoints
        </h2>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div className="p-4 rounded-xl bg-slate-50 border border-slate-200">
            <span className="text-[11px] font-bold uppercase text-indigo-600 block">
              1. Pickup Depot
            </span>
            <p className="font-bold text-slate-900 mt-1">{shipment.pickupAddress?.street}</p>
            <p className="text-xs text-slate-500">
              {shipment.pickupAddress?.city || 'Warehouse District'}
            </p>
          </div>

          <div className="p-4 rounded-xl bg-slate-50 border border-slate-200">
            <span className="text-[11px] font-bold uppercase text-emerald-600 block">
              2. Delivery Destination
            </span>
            <p className="font-bold text-slate-900 mt-1">{shipment.deliveryAddress?.street}</p>
            <p className="text-xs text-slate-500">
              {shipment.receiverName || 'Camp Coordinator'} (
              {shipment.receiverPhone || '+1-555-0199'})
            </p>
          </div>
        </div>

        <div className="pt-2 border-t border-slate-100">
          <span className="text-xs font-semibold text-slate-600 block">Cargo Manifest:</span>
          <p className="text-xs text-slate-500 mt-0.5">{shipment.itemsSummary}</p>
        </div>
      </div>

      {/* Driver Actions */}
      <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm space-y-4">
        <h2 className="text-sm font-bold uppercase tracking-wider text-slate-500">
          Mission Status Actions
        </h2>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <Button
            variant="primary"
            loading={isPickingUp}
            disabled={shipment.status !== 'ASSIGNED' && shipment.status !== 'CREATED'}
            onClick={handlePickup}
            className="w-full"
          >
            Mark PICKED UP
          </Button>

          <Button
            variant="success"
            loading={isDelivering}
            disabled={shipment.status === 'DELIVERED'}
            onClick={handleDeliver}
            className="w-full"
          >
            Confirm DELIVERED
          </Button>

          <Button
            variant="outline"
            onClick={() => setIsClosureModalOpen(true)}
            className="w-full text-amber-700 border-amber-300 hover:bg-amber-50"
          >
            ⚠️ Report Road Closure / Delay
          </Button>

          <Button
            variant="danger"
            loading={isDisputing}
            disabled={shipment.status === 'DELIVERED' || shipment.status === 'FAILED'}
            onClick={handleDispute}
            className="w-full"
          >
            Report Route Failure / Breakdown
          </Button>
        </div>
      </div>

      {/* Road Closure Report Modal */}
      <Modal
        isOpen={isClosureModalOpen}
        onClose={() => setIsClosureModalOpen(false)}
        title="Report Road Hazard or Route Closure"
        size="md"
      >
        <form onSubmit={handleSubmit(onReportClosureSubmit)} className="space-y-4">
          <div>
            <label
              htmlFor="closureReason"
              className="block text-xs font-semibold text-slate-700 uppercase"
            >
              Incident Cause / Hazard
            </label>
            <input
              id="closureReason"
              type="text"
              {...register('reason')}
              placeholder="e.g. Flooded bridge, landslide, security checkpoint"
              className="mt-1 block w-full rounded-lg border border-slate-300 py-2 px-3 text-sm focus:ring-indigo-500"
            />
            {errors.reason && <p className="text-xs text-rose-600 mt-1">{errors.reason.message}</p>}
          </div>

          <div>
            <label
              htmlFor="closureLocation"
              className="block text-xs font-semibold text-slate-700 uppercase"
            >
              Road Location / Milepost
            </label>
            <input
              id="closureLocation"
              type="text"
              {...register('location')}
              placeholder="e.g. Route 7 km 42"
              className="mt-1 block w-full rounded-lg border border-slate-300 py-2 px-3 text-sm focus:ring-indigo-500"
            />
            {errors.location && (
              <p className="text-xs text-rose-600 mt-1">{errors.location.message}</p>
            )}
          </div>

          <div>
            <label
              htmlFor="closureDelay"
              className="block text-xs font-semibold text-slate-700 uppercase"
            >
              Estimated Delay (Minutes)
            </label>
            <input
              id="closureDelay"
              type="number"
              {...register('estimatedDelayMinutes')}
              className="mt-1 block w-full rounded-lg border border-slate-300 py-2 px-3 text-sm focus:ring-indigo-500"
            />
          </div>

          <div>
            <label
              htmlFor="closureNotes"
              className="block text-xs font-semibold text-slate-700 uppercase"
            >
              Detour or Alternative Route Notes
            </label>
            <textarea
              id="closureNotes"
              rows={2}
              {...register('alternativeRouteNotes')}
              className="mt-1 block w-full rounded-lg border border-slate-300 py-2 px-3 text-sm focus:ring-indigo-500"
            />
          </div>

          <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
            <Button type="button" variant="ghost" onClick={() => setIsClosureModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" variant="danger" loading={isReportingClosure}>
              Broadcast Closure Alert
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
