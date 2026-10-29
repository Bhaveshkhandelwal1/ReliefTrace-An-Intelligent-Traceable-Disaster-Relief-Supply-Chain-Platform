import { useState } from 'react';
import { useAuth } from '../../../auth/useAuth';
import { useDonations } from '../hooks/useDonations';
import { useToast } from '../../../hooks/useToast';
import { DataTable } from '../../../components/DataTable';
import { StatusBadge } from '../../../components/StatusBadge';
import { Button } from '../../../components/Button';
import { Modal } from '../../../components/Modal';
import { formatCurrency, formatDate } from '../../../utils/formatters';

export function DonationHistoryPage() {
  const { user } = useAuth();
  const { donations, isLoadingDonations, cancelDonation, isCancelling } = useDonations(user?.id);
  const toast = useToast();
  const [selectedDonation, setSelectedDonation] = useState(null);

  const handleCancel = async (donationId) => {
    try {
      await cancelDonation(donationId);
      toast.info('Donation registration has been cancelled.');
    } catch {
      toast.error('Failed to cancel donation.');
    }
  };

  const columns = [
    {
      key: 'id',
      header: 'Reference ID',
      render: (row) => (
        <span className="font-mono text-xs font-semibold text-slate-800">
          #{String(row.donationId || row.id).substring(0, 8)}
        </span>
      ),
    },
    {
      key: 'type',
      header: 'Type',
      render: (row) => <span className="text-xs font-semibold text-slate-700">{row.type}</span>,
    },
    {
      key: 'amount',
      header: 'Contributed',
      render: (row) =>
        row.type === 'MONETARY' ? (
          <span className="font-bold text-emerald-600">
            {formatCurrency(row.amount, row.currency)}
          </span>
        ) : (
          <span className="text-xs text-slate-700">{row.items?.length || 1} Items Line</span>
        ),
    },
    {
      key: 'status',
      header: 'Status',
      render: (row) => <StatusBadge status={row.status} type="donation" />,
    },
    {
      key: 'createdAt',
      header: 'Date Logged',
      render: (row) => <span className="text-xs text-slate-500">{formatDate(row.createdAt)}</span>,
    },
    {
      key: 'actions',
      header: 'Actions',
      render: (row) => (
        <div className="flex gap-2">
          <Button size="sm" variant="outline" onClick={() => setSelectedDonation(row)}>
            Receipt
          </Button>
          {(row.status === 'REGISTERED' || row.status === 'PLEDGED') && (
            <Button
              size="sm"
              variant="ghost"
              className="text-rose-600 hover:text-rose-800"
              disabled={isCancelling}
              onClick={() => handleCancel(row.donationId || row.id)}
            >
              Cancel
            </Button>
          )}
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Donation History & Receipts</h1>
          <p className="text-sm text-slate-500 mt-1">
            Complete audit trail of your contributions and downloadable tax-deductible receipts.
          </p>
        </div>
      </div>

      <DataTable
        columns={columns}
        data={donations}
        isLoading={isLoadingDonations}
        emptyTitle="No contribution history"
        emptyDescription="All completed and active donations will appear here with instant receipts."
      />

      {/* Official Tax-Deductible Receipt Modal */}
      {selectedDonation && (
        <Modal
          isOpen={Boolean(selectedDonation)}
          onClose={() => setSelectedDonation(null)}
          title="Official Relief Contribution Receipt"
          size="md"
        >
          <div className="space-y-6 text-slate-800">
            <div className="border-b border-slate-200 pb-4 text-center">
              <span className="text-xs uppercase font-extrabold tracking-widest text-indigo-600">
                DisasterReliefNet Foundation
              </span>
              <h3 className="text-xl font-black mt-1">Donation Acknowledgement</h3>
              <p className="text-xs text-slate-500 mt-1">Tax Exemption 501(c)(3) Verified</p>
            </div>

            <div className="grid grid-cols-2 gap-4 text-xs">
              <div>
                <span className="text-slate-400 block font-medium">Receipt No:</span>
                <span className="font-mono font-bold">
                  REC-{String(selectedDonation.donationId || selectedDonation.id).substring(0, 12)}
                </span>
              </div>
              <div>
                <span className="text-slate-400 block font-medium">Date Issued:</span>
                <span className="font-semibold">{formatDate(selectedDonation.createdAt)}</span>
              </div>
              <div>
                <span className="text-slate-400 block font-medium">Donor Name:</span>
                <span className="font-semibold">{user?.name || 'Authorized Donor'}</span>
              </div>
              <div>
                <span className="text-slate-400 block font-medium">Contribution Type:</span>
                <span className="font-semibold">{selectedDonation.type}</span>
              </div>
            </div>

            <div className="bg-slate-50 rounded-xl p-4 border border-slate-200">
              <div className="flex items-baseline justify-between">
                <span className="text-sm font-semibold text-slate-700">Total Validated Value</span>
                <span className="text-2xl font-black text-indigo-700">
                  {selectedDonation.type === 'MONETARY'
                    ? formatCurrency(selectedDonation.amount, selectedDonation.currency)
                    : `${selectedDonation.items?.length || 1} Physical Goods Lines`}
                </span>
              </div>
            </div>

            <p className="text-[11px] text-slate-400 text-center leading-relaxed">
              No goods or services were provided in exchange for this contribution other than
              intangible religious or charitable benefits. Thank you for your humanitarian support.
            </p>

            <div className="flex justify-end pt-2">
              <Button variant="primary" onClick={() => setSelectedDonation(null)}>
                Close Receipt
              </Button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
}
