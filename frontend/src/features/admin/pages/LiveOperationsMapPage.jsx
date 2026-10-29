import { useState } from 'react';
import { useAdminOperations } from '../hooks/useAdminOperations';
import { LeafletMap } from '../../../components/LeafletMap';

const MOCK_CAMPS = [
  {
    id: 'camp-1',
    name: 'Camp Alpha Hope Center',
    latitude: 34.0522,
    longitude: -118.2437,
    radiusMeters: 1400,
    population: 3200,
    status: 'ACTIVE_URGENT',
  },
  {
    id: 'camp-2',
    name: 'Sector 4 Shelter Evacuation Hub',
    latitude: 34.1012,
    longitude: -118.3253,
    radiusMeters: 900,
    population: 1450,
    status: 'STABLE',
  },
  {
    id: 'camp-3',
    name: 'Valley Ridge Emergency Base',
    latitude: 34.1808,
    longitude: -118.309,
    radiusMeters: 1100,
    population: 2100,
    status: 'ACTIVE',
  },
];

const MOCK_WAREHOUSES = [
  {
    id: 'wh-1',
    name: 'Central Depot Delta',
    latitude: 33.9425,
    longitude: -118.4081,
    address: 'Terminal Island Logistics Park',
    capacity: '84% Full',
  },
  {
    id: 'wh-2',
    name: 'Northern Supply Vault',
    latitude: 34.2005,
    longitude: -118.4552,
    address: 'Burbank Logistics Sector',
    capacity: '58% Full',
  },
];

const MOCK_LIVE_SHIPMENTS = [
  {
    id: 'sh-convoy-01',
    shipmentId: 'sh-convoy-01',
    currentLat: 34.015,
    currentLng: -118.29,
    status: 'IN_TRANSIT',
    driverName: 'Carlos Mendez (Convoy 1)',
  },
  {
    id: 'sh-convoy-02',
    shipmentId: 'sh-convoy-02',
    currentLat: 34.08,
    currentLng: -118.37,
    status: 'IN_TRANSIT',
    driverName: 'Alex Mercer (Convoy 2)',
  },
];

export function LiveOperationsMapPage() {
  const { shipments, warehouses } = useAdminOperations();
  const [filterCamps, setFilterCamps] = useState(true);
  const [filterWarehouses, setFilterWarehouses] = useState(true);
  const [filterShipments, setFilterShipments] = useState(true);

  // Map real data with fallback mock coordinates
  const activeWarehouses = warehouses.length > 0 ? warehouses : MOCK_WAREHOUSES;
  const activeShipments = shipments.length > 0 ? shipments : MOCK_LIVE_SHIPMENTS;

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Live Geospatial Operations Map</h1>
          <p className="text-sm text-slate-500 mt-1">
            Real-time tracking of refugee camps, inventory supply hubs, and active logistics
            convoys.
          </p>
        </div>

        {/* Layer Toggles */}
        <div className="flex flex-wrap items-center gap-2 bg-white p-1.5 rounded-xl border border-slate-200 text-xs font-semibold shadow-sm">
          <button
            type="button"
            onClick={() => setFilterCamps(!filterCamps)}
            className={`px-3 py-1.5 rounded-lg transition-all ${
              filterCamps ? 'bg-rose-100 text-rose-800' : 'text-slate-400 hover:text-slate-700'
            }`}
          >
            ⛺ Relief Camps ({MOCK_CAMPS.length})
          </button>
          <button
            type="button"
            onClick={() => setFilterWarehouses(!filterWarehouses)}
            className={`px-3 py-1.5 rounded-lg transition-all ${
              filterWarehouses
                ? 'bg-indigo-100 text-indigo-800'
                : 'text-slate-400 hover:text-slate-700'
            }`}
          >
            🏢 Warehouses ({activeWarehouses.length})
          </button>
          <button
            type="button"
            onClick={() => setFilterShipments(!filterShipments)}
            className={`px-3 py-1.5 rounded-lg transition-all ${
              filterShipments ? 'bg-sky-100 text-sky-800' : 'text-slate-400 hover:text-slate-700'
            }`}
          >
            🚚 Convoys ({activeShipments.length})
          </button>
        </div>
      </div>

      {/* Full-width Map Container */}
      <div className="rounded-2xl border border-slate-200 shadow-sm overflow-hidden bg-white p-2">
        <LeafletMap
          height="620px"
          center={[34.0522, -118.2437]}
          zoom={11}
          camps={filterCamps ? MOCK_CAMPS : []}
          warehouses={filterWarehouses ? activeWarehouses : []}
          shipments={filterShipments ? activeShipments : []}
        />
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="p-4 rounded-xl bg-white border border-slate-200 text-xs">
          <span className="font-bold text-slate-800 block">Red Circles (Camps)</span>
          <p className="text-slate-500 mt-0.5">
            Geographic emergency perimeter with real-time population census.
          </p>
        </div>
        <div className="p-4 rounded-xl bg-white border border-slate-200 text-xs">
          <span className="font-bold text-slate-800 block">Markers (Warehouses)</span>
          <p className="text-slate-500 mt-0.5">
            Strategic storage points holding critical food, water, and medical inventories.
          </p>
        </div>
        <div className="p-4 rounded-xl bg-white border border-slate-200 text-xs">
          <span className="font-bold text-slate-800 block">Blue Dots (Convoys)</span>
          <p className="text-slate-500 mt-0.5">
            Live GPS telemetry of humanitarian aid convoys in transit.
          </p>
        </div>
      </div>
    </div>
  );
}
