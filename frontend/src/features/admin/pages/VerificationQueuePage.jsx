import { useState } from 'react';
import { useAdminOperations } from '../hooks/useAdminOperations';
import { useToast } from '../../../hooks/useToast';
import { DataTable } from '../../../components/DataTable';
import { StatusBadge } from '../../../components/StatusBadge';
import { Button } from '../../../components/Button';
import { Modal } from '../../../components/Modal';
import { formatDate } from '../../../utils/formatters';

export function VerificationQueuePage() {
  const {
    verificationCases,
    isLoadingVerification,
    approveCase,
    isApprovingCase,
    rejectCase,
    isRejectingCase,
  } = useAdminOperations();
  const toast = useToast();

  const [selectedCase, setSelectedCase] = useState(null);

  const handleApprove = async (caseId) => {
    try {
      await approveCase({ caseId, data: { reviewNotes: 'Identity and credentials verified' } });
      toast.success('Receiver case verified and approved!');
      setSelectedCase(null);
    } catch {
      toast.error('Failed to approve verification case.');
    }
  };

  const handleReject = async (caseId) => {
    try {
      await rejectCase({
        caseId,
        data: { reviewNotes: 'Documentation invalid or incomplete' },
      });
      toast.info('Verification case rejected.');
      setSelectedCase(null);
    } catch {
      toast.error('Failed to reject verification case.');
    }
  };

  const columns = [
    {
      key: 'id',
      header: 'Case #',
      render: (row) => (
        <span className="font-mono text-xs font-bold text-slate-800">
          #{String(row.caseId || row.id).substring(0, 8)}
        </span>
      ),
    },
    {
      key: 'receiver',
      header: 'Receiver / Organization',
      render: (row) => (
        <div>
          <span className="font-semibold text-slate-900 block text-sm">
            {row.receiverName || 'Camp Coordination Unit'}
          </span>
          <span className="text-xs text-slate-500">
            ID: {String(row.receiverId).substring(0, 8)}
          </span>
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
      header: 'Opened At',
      render: (row) => (
        <span className="text-xs text-slate-500">{formatDate(row.openedAt || row.createdAt)}</span>
      ),
    },
    {
      key: 'actions',
      header: 'Action',
      render: (row) => (
        <Button size="sm" variant="outline" onClick={() => setSelectedCase(row)}>
          Review Case →
        </Button>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <div className="border-b border-slate-200 pb-4">
        <h1 className="text-2xl font-bold text-slate-900">Verification Operations Queue</h1>
        <p className="text-sm text-slate-500 mt-1">
          Validate authenticity of requesting relief organizations and disaster-affected
          communities.
        </p>
      </div>

      <DataTable
        columns={columns}
        data={verificationCases}
        isLoading={isLoadingVerification}
        emptyTitle="Verification queue empty"
        emptyDescription="All incoming receivers and camps have been authenticated."
      />

      {/* Case Review Modal */}
      {selectedCase && (
        <Modal
          isOpen={Boolean(selectedCase)}
          onClose={() => setSelectedCase(null)}
          title={`Review Case #${String(selectedCase.caseId || selectedCase.id).substring(0, 8)}`}
          size="md"
        >
          <div className="space-y-5">
            <div className="p-4 rounded-xl bg-slate-50 border border-slate-200 text-xs space-y-2">
              <div className="flex justify-between">
                <span className="font-medium text-slate-500">Status:</span>
                <StatusBadge status={selectedCase.status} type="status" />
              </div>
              <div className="flex justify-between">
                <span className="font-medium text-slate-500">Receiver Entity ID:</span>
                <span className="font-mono font-bold">{selectedCase.receiverId}</span>
              </div>
              <div className="flex justify-between">
                <span className="font-medium text-slate-500">Associated Aid Request:</span>
                <span className="font-mono font-bold">
                  {selectedCase.requestId ? String(selectedCase.requestId).substring(0, 8) : 'N/A'}
                </span>
              </div>
            </div>

            <div>
              <h3 className="text-xs font-bold uppercase tracking-wider text-slate-600 mb-2">
                Attached Verification Proof Documents
              </h3>
              <div className="border border-slate-200 rounded-xl p-3 bg-white space-y-2">
                <div className="flex items-center justify-between text-xs">
                  <span className="font-semibold text-slate-800">
                    📄 Government Emergency Registration Certificate.pdf
                  </span>
                  <span className="text-[10px] uppercase font-bold text-emerald-600 bg-emerald-50 px-2 py-0.5 rounded">
                    AUTHENTIC
                  </span>
                </div>
                <div className="flex items-center justify-between text-xs">
                  <span className="font-semibold text-slate-800">
                    📄 Camp Commander Photo ID.jpg
                  </span>
                  <span className="text-[10px] uppercase font-bold text-indigo-600 bg-indigo-50 px-2 py-0.5 rounded">
                    MATCHED
                  </span>
                </div>
              </div>
            </div>

            <div className="flex justify-end gap-2 pt-3 border-t border-slate-100">
              <Button variant="ghost" onClick={() => setSelectedCase(null)}>
                Close
              </Button>
              <Button
                variant="danger"
                loading={isRejectingCase}
                onClick={() => handleReject(selectedCase.caseId || selectedCase.id)}
              >
                Reject Case
              </Button>
              <Button
                variant="success"
                loading={isApprovingCase}
                onClick={() => handleApprove(selectedCase.caseId || selectedCase.id)}
              >
                Approve Verification
              </Button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
}
