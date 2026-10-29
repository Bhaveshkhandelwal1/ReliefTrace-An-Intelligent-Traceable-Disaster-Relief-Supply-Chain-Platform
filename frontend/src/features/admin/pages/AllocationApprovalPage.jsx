import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useAdminOperations } from '../hooks/useAdminOperations';
import { useToast } from '../../../hooks/useToast';
import { allocationRejectionSchema } from '../../../utils/schemas';
import { DataTable } from '../../../components/DataTable';
import { StatusBadge } from '../../../components/StatusBadge';
import { Button } from '../../../components/Button';
import { Modal } from '../../../components/Modal';

export function AllocationApprovalPage() {
  const {
    allocations,
    isLoadingAllocations,
    approveAllocation,
    isApprovingAllocation,
    rejectAllocation,
    isRejectingAllocation,
  } = useAdminOperations();
  const toast = useToast();

  const [rejectingAllocationId, setRejectingAllocationId] = useState(null);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm({
    resolver: zodResolver(allocationRejectionSchema),
    defaultValues: { reason: '' },
  });

  const handleApprove = async (allocationId) => {
    try {
      await approveAllocation({
        allocationId,
        data: { notes: 'Approved by Coordinator Dispatch' },
      });
      toast.success('Allocation approved! Stock reserved and shipment created.');
    } catch {
      toast.error('Failed to approve allocation.');
    }
  };

  const handleRejectSubmit = async (data) => {
    try {
      await rejectAllocation({ allocationId: rejectingAllocationId, data });
      toast.info('Allocation rejected and returned to matching pool.');
      setRejectingAllocationId(null);
      reset();
    } catch {
      toast.error('Failed to reject allocation.');
    }
  };

  const columns = [
    {
      key: 'id',
      header: 'Allocation ID',
      render: (row) => (
        <span className="font-mono text-xs font-bold text-slate-800">
          #{String(row.allocationId || row.id).substring(0, 8)}
        </span>
      ),
    },
    {
      key: 'item',
      header: 'Requested Aid Item',
      render: (row) => (
        <div>
          <span className="font-semibold text-slate-900 block text-sm">
            {row.itemName || 'Emergency Relief Supplies'}
          </span>
          <span className="text-xs text-slate-500">
            Quantity: {row.allocatedQuantity || row.quantity || 100} units
          </span>
        </div>
      ),
    },
    {
      key: 'warehouse',
      header: 'Source Warehouse',
      render: (row) => (
        <span className="text-xs font-medium text-slate-700">
          {row.warehouseName || 'Central Logistics Hub'}
        </span>
      ),
    },
    {
      key: 'urgency',
      header: 'Camp Urgency',
      render: (row) => <StatusBadge status={row.urgency || 'HIGH'} type="urgency" />,
    },
    {
      key: 'status',
      header: 'Status',
      render: (row) => <StatusBadge status={row.status} type="status" />,
    },
    {
      key: 'actions',
      header: 'Coordinator Decision',
      render: (row) =>
        row.status === 'SUGGESTED' || row.status === 'PENDING' ? (
          <div className="flex gap-2">
            <Button
              size="sm"
              variant="success"
              loading={isApprovingAllocation}
              onClick={() => handleApprove(row.allocationId || row.id)}
            >
              Approve
            </Button>
            <Button
              size="sm"
              variant="danger"
              onClick={() => setRejectingAllocationId(row.allocationId || row.id)}
            >
              Reject
            </Button>
          </div>
        ) : (
          <span className="text-xs text-slate-400 font-medium italic">Decided</span>
        ),
    },
  ];

  return (
    <div className="space-y-6">
      <div className="border-b border-slate-200 pb-4">
        <h1 className="text-2xl font-bold text-slate-900">Aid Allocation Approvals</h1>
        <p className="text-sm text-slate-500 mt-1">
          Review automated matching suggestions between warehouse inventory and urgent camp
          requests.
        </p>
      </div>

      <DataTable
        columns={columns}
        data={allocations}
        isLoading={isLoadingAllocations}
        emptyTitle="No pending allocations"
        emptyDescription="All matched supply suggestions have been reviewed and dispatched."
      />

      {/* Reject Modal */}
      <Modal
        isOpen={Boolean(rejectingAllocationId)}
        onClose={() => {
          setRejectingAllocationId(null);
          reset();
        }}
        title="Reject Allocation Suggestion"
        size="sm"
      >
        <form onSubmit={handleSubmit(handleRejectSubmit)} className="space-y-4">
          <div>
            <label
              htmlFor="rejectionReason"
              className="block text-xs font-semibold text-slate-700 uppercase"
            >
              Reason for Rejection
            </label>
            <textarea
              id="rejectionReason"
              rows={3}
              {...register('reason')}
              placeholder="e.g. Route impassable for assigned depot or stock earmarked for hospital priority."
              className="mt-1 block w-full rounded-lg border border-slate-300 py-2 px-3 text-sm focus:ring-indigo-500"
            />
            {errors.reason && <p className="text-xs text-rose-600 mt-1">{errors.reason.message}</p>}
          </div>

          <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
            <Button
              type="button"
              variant="ghost"
              onClick={() => {
                setRejectingAllocationId(null);
                reset();
              }}
            >
              Cancel
            </Button>
            <Button type="submit" variant="danger" loading={isRejectingAllocation}>
              Confirm Rejection
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
