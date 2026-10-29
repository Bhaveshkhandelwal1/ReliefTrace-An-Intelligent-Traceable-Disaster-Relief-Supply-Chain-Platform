import { Link } from 'react-router-dom';
import { useAuth } from '../../../auth/useAuth';
import { useAidRequests } from '../hooks/useAidRequests';
import { DataTable } from '../../../components/DataTable';
import { StatusBadge } from '../../../components/StatusBadge';
import { Button } from '../../../components/Button';
import { formatDate } from '../../../utils/formatters';

export function ReceiverDashboardPage() {
  const { user } = useAuth();
  const { requests, isLoadingRequests } = useAidRequests(user?.id);

  const columns = [
    {
      key: 'id',
      header: 'Request Ref',
      render: (row) => (
        <span className="font-mono text-xs font-semibold text-slate-800">
          #{String(row.requestId || row.id).substring(0, 8)}
        </span>
      ),
    },
    {
      key: 'category',
      header: 'Category / Item',
      render: (row) => (
        <div>
          <span className="font-semibold text-slate-900 block text-sm">
            {row.itemName || row.category}
          </span>
          <span className="text-xs text-slate-500">
            {row.quantity} {row.unit || 'units'}
          </span>
        </div>
      ),
    },
    {
      key: 'urgency',
      header: 'Urgency',
      render: (row) => <StatusBadge status={row.urgency} type="urgency" />,
    },
    {
      key: 'status',
      header: 'Relief Status',
      render: (row) => <StatusBadge status={row.status} type="status" />,
    },
    {
      key: 'createdAt',
      header: 'Requested At',
      render: (row) => <span className="text-xs text-slate-500">{formatDate(row.createdAt)}</span>,
    },
    {
      key: 'actions',
      header: 'Actions',
      render: (row) => (
        <div className="flex gap-2">
          {row.status === 'IN_TRANSIT' && (
            <Link to={`/receiver/confirm-delivery/${row.requestId || row.id}`}>
              <Button size="sm" variant="success">
                Confirm Receipt
              </Button>
            </Link>
          )}
          {row.status === 'SUBMITTED' && (
            <span className="text-xs text-amber-600 font-medium py-1">In Verification Queue</span>
          )}
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Emergency Aid Requests</h1>
          <p className="text-sm text-slate-500 mt-1">
            Request essential provisions for your community, camp, or emergency facility.
          </p>
        </div>
        <div>
          <Link to="/receiver/request">
            <Button variant="primary" className="shadow-md">
              + Create Aid Request
            </Button>
          </Link>
        </div>
      </div>

      <DataTable
        columns={columns}
        data={requests}
        isLoading={isLoadingRequests}
        emptyTitle="No aid requests submitted"
        emptyDescription="If your relief camp or community is in need of emergency supplies, submit a request."
      />
    </div>
  );
}
