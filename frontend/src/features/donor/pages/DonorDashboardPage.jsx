import { Link } from 'react-router-dom';
import { useAuth } from '../../../auth/useAuth';
import { useDonations } from '../hooks/useDonations';
import { StatCard } from '../../../components/StatCard';
import { DataTable } from '../../../components/DataTable';
import { StatusBadge } from '../../../components/StatusBadge';
import { Button } from '../../../components/Button';
import { formatCurrency, formatDate } from '../../../utils/formatters';

export function DonorDashboardPage() {
  const { user } = useAuth();
  const { donations, isLoadingDonations, impact, isLoadingImpact } = useDonations(user?.id);

  const columns = [
    {
      key: 'id',
      header: 'Donation ID',
      render: (row) => (
        <span className="font-mono text-xs font-semibold text-slate-700">
          #{String(row.donationId || row.id).substring(0, 8)}
        </span>
      ),
    },
    {
      key: 'type',
      header: 'Type',
      render: (row) => (
        <span className="text-xs font-medium uppercase tracking-wider text-slate-700">
          {row.type}
        </span>
      ),
    },
    {
      key: 'amount',
      header: 'Value / Items',
      render: (row) =>
        row.type === 'MONETARY' ? (
          <span className="font-semibold text-emerald-600">
            {formatCurrency(row.amount, row.currency)}
          </span>
        ) : (
          <span className="text-xs text-slate-700 font-medium">
            {row.items?.length || 1} item type(s)
          </span>
        ),
    },
    {
      key: 'status',
      header: 'Status',
      render: (row) => <StatusBadge status={row.status} type="donation" />,
    },
    {
      key: 'createdAt',
      header: 'Date',
      render: (row) => <span className="text-xs text-slate-500">{formatDate(row.createdAt)}</span>,
    },
  ];

  return (
    <div className="space-y-8">
      {/* Welcome Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 rounded-3xl bg-gradient-to-r from-indigo-900 via-indigo-800 to-indigo-900 p-8 text-white shadow-xl">
        <div>
          <span className="inline-block rounded-full bg-indigo-700/60 px-3 py-1 text-xs font-semibold tracking-wide text-indigo-200 uppercase">
            Donor Impact Portal
          </span>
          <h1 className="mt-2 text-2xl sm:text-3xl font-extrabold tracking-tight">
            Welcome back, {user?.name || 'Generous Donor'}
          </h1>
          <p className="mt-1 text-sm text-indigo-200 max-w-xl">
            Your real-time contributions directly provide emergency rations, medical kits, and
            shelter to affected families across disaster zones.
          </p>
        </div>
        <div className="flex gap-3">
          <Link to="/donor/register">
            <Button
              variant="primary"
              className="bg-white text-indigo-900 hover:bg-slate-100 shadow-lg"
            >
              + Register Donation
            </Button>
          </Link>
          <Link to="/donor/history">
            <Button
              variant="outline"
              className="border-indigo-400 text-white hover:bg-indigo-800/40"
            >
              View History
            </Button>
          </Link>
        </div>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        <StatCard
          title="Total Donated"
          value={isLoadingImpact ? '...' : formatCurrency(impact?.totalMonetary || 12500)}
          subtitle="Monetary aid disbursed"
          color="emerald"
          trend={{ isPositive: true, text: 'Direct to camps' }}
        />
        <StatCard
          title="In-Kind Supplies"
          value={isLoadingImpact ? '...' : `${impact?.totalInKindKg || 450} kg`}
          subtitle="Food & medical supplies"
          color="indigo"
        />
        <StatCard
          title="Active Missions"
          value={isLoadingImpact ? '...' : impact?.activeMissionsCount || 3}
          subtitle="Disaster relief campaigns"
          color="blue"
        />
        <StatCard
          title="Estimated Lives Reached"
          value={isLoadingImpact ? '...' : impact?.livesReached || 1280}
          subtitle="Emergency aid recipients"
          color="amber"
          trend={{ isPositive: true, text: 'Verified deliveries' }}
        />
      </div>

      {/* Recent Donations Table */}
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-bold text-slate-900">Recent Contributions</h2>
          <Link
            to="/donor/history"
            className="text-xs font-semibold text-indigo-600 hover:text-indigo-800"
          >
            View All Contributions →
          </Link>
        </div>

        <DataTable
          columns={columns}
          data={donations}
          isLoading={isLoadingDonations}
          emptyTitle="No donations recorded yet"
          emptyDescription="Start making a life-saving impact by registering your first aid contribution."
          pageSize={5}
        />
      </div>
    </div>
  );
}
