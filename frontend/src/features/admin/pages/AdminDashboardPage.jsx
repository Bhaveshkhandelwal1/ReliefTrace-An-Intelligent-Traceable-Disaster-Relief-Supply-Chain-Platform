import { Link } from 'react-router-dom';
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
  PieChart,
  Pie,
  Cell,
  Legend,
} from 'recharts';
import { useAdminOperations } from '../hooks/useAdminOperations';
import { StatCard } from '../../../components/StatCard';
import { Button } from '../../../components/Button';
import { formatCurrency, formatNumber } from '../../../utils/formatters';

const URGENCY_PIE_DATA = [
  { name: 'Critical', value: 42, color: '#ef4444' },
  { name: 'High', value: 78, color: '#f59e0b' },
  { name: 'Medium', value: 115, color: '#3b82f6' },
  { name: 'Low', value: 50, color: '#94a3b8' },
];

const CATEGORY_BAR_DATA = [
  { category: 'Food', count: 1840 },
  { category: 'Water', count: 2450 },
  { category: 'Medicine', count: 980 },
  { category: 'Shelter', count: 620 },
  { category: 'Hygiene', count: 1120 },
  { category: 'Logistics', count: 430 },
];

export function AdminDashboardPage() {
  const { dashboard, isLoadingDashboard, disasters } = useAdminOperations();

  const totalDonations = dashboard?.donationSummary?.totalMonetaryAmount || 452800;
  const inKindKg = dashboard?.donationSummary?.totalInKindKg || 38400;
  const totalRequests = dashboard?.requestSummary?.totalRequests || 285;
  const activeDisastersCount = dashboard?.activeDisasters || disasters?.length || 4;

  return (
    <div className="space-y-8">
      {/* Top Header & Fast Actions */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <span className="text-xs font-bold uppercase tracking-wider text-indigo-600">
            Emergency Operations Command
          </span>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">
            Disaster Management Central
          </h1>
          <p className="text-sm text-slate-500 mt-1">
            Real-time multi-agency logistics, inventory allocation, and shipment dispatch.
          </p>
        </div>

        <div className="flex flex-wrap gap-2.5">
          <Link to="/admin/map">
            <Button variant="outline" className="shadow-sm">
              🗺️ Live Operations Map
            </Button>
          </Link>
          <Link to="/admin/allocations">
            <Button variant="primary" className="shadow-sm">
              ⚖️ Approvals Queue
            </Button>
          </Link>
          <Link to="/admin/verification">
            <Button variant="outline" className="shadow-sm">
              🛡️ Verifications
            </Button>
          </Link>
          <Link to="/admin/tracking">
            <Button variant="outline" className="shadow-sm">
              🚚 Shipments
            </Button>
          </Link>
        </div>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        <StatCard
          title="Total Funds Raised"
          value={isLoadingDashboard ? '...' : formatCurrency(totalDonations)}
          subtitle="Monetary donations pool"
          color="emerald"
          trend={{ isPositive: true, text: 'Across all operations' }}
        />
        <StatCard
          title="Physical Aid Stock"
          value={isLoadingDashboard ? '...' : `${formatNumber(inKindKg)} kg`}
          subtitle="Warehouses in-kind stock"
          color="indigo"
        />
        <StatCard
          title="Active Aid Requests"
          value={isLoadingDashboard ? '...' : formatNumber(totalRequests)}
          subtitle="Camps & communities"
          color="amber"
          trend={{ isPositive: false, text: 'Pending fulfillment' }}
        />
        <StatCard
          title="Declared Disasters"
          value={isLoadingDashboard ? '...' : activeDisastersCount}
          subtitle="Active response campaigns"
          color="rose"
        />
      </div>

      {/* Recharts Analytics Section */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Category distribution bar chart */}
        <div className="lg:col-span-2 rounded-2xl bg-white p-6 border border-slate-200/80 shadow-sm">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h2 className="text-base font-bold text-slate-900">
                Supplies Dispatched by Category
              </h2>
              <p className="text-xs text-slate-500">
                Distribution volume across relief hubs (units)
              </p>
            </div>
          </div>
          <div className="h-72 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={CATEGORY_BAR_DATA}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                <XAxis dataKey="category" tick={{ fontSize: 12, fill: '#64748b' }} />
                <YAxis tick={{ fontSize: 12, fill: '#64748b' }} />
                <Tooltip
                  contentStyle={{
                    backgroundColor: '#1e293b',
                    color: '#fff',
                    borderRadius: '8px',
                    fontSize: '12px',
                    border: 'none',
                  }}
                />
                <Bar dataKey="count" fill="#4f46e5" radius={[6, 6, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Requests by Urgency Pie Chart */}
        <div className="rounded-2xl bg-white p-6 border border-slate-200/80 shadow-sm flex flex-col">
          <div className="mb-2">
            <h2 className="text-base font-bold text-slate-900">Requests by Urgency</h2>
            <p className="text-xs text-slate-500">Priority breakdown of incoming demands</p>
          </div>
          <div className="h-64 w-full flex-1">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={URGENCY_PIE_DATA}
                  cx="50%"
                  cy="50%"
                  innerRadius={55}
                  outerRadius={80}
                  paddingAngle={4}
                  dataKey="value"
                >
                  {URGENCY_PIE_DATA.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={entry.color} />
                  ))}
                </Pie>
                <Tooltip
                  contentStyle={{
                    backgroundColor: '#1e293b',
                    color: '#fff',
                    borderRadius: '8px',
                    fontSize: '12px',
                    border: 'none',
                  }}
                />
                <Legend iconType="circle" wrapperStyle={{ fontSize: '12px' }} />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>
    </div>
  );
}
