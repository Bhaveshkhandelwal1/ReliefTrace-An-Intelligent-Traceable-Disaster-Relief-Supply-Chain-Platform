import PropTypes from 'prop-types';
import { MapContainer, TileLayer, Marker, Popup, Circle, useMapEvents } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';

// Fix Leaflet default icon paths in bundler environments
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
  iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
});

// Helper component for coordinate picker clicks
function LocationPickerHandler({ onSelect }) {
  useMapEvents({
    click(e) {
      if (onSelect) {
        onSelect({
          latitude: Number(e.latlng.lat.toFixed(6)),
          longitude: Number(e.latlng.lng.toFixed(6)),
        });
      }
    },
  });
  return null;
}

LocationPickerHandler.propTypes = {
  onSelect: PropTypes.func,
};

LocationPickerHandler.defaultProps = {
  onSelect: null,
};

export function LeafletMap({
  center,
  zoom,
  height,
  camps,
  warehouses,
  shipments,
  selectedLocation,
  onLocationSelect,
}) {
  return (
    <div
      style={{ height }}
      className="w-full rounded-2xl overflow-hidden shadow-inner relative border border-slate-200"
    >
      <MapContainer
        center={center}
        zoom={zoom}
        scrollWheelZoom={false}
        style={{ height: '100%', width: '100%' }}
      >
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />

        {onLocationSelect && <LocationPickerHandler onSelect={onLocationSelect} />}

        {/* Selected location marker */}
        {selectedLocation && selectedLocation.latitude && selectedLocation.longitude && (
          <Marker position={[selectedLocation.latitude, selectedLocation.longitude]}>
            <Popup>
              <div className="text-xs font-semibold text-slate-800">Selected Relief Location</div>
              <div className="text-[11px] text-slate-500">
                {selectedLocation.latitude}, {selectedLocation.longitude}
              </div>
            </Popup>
          </Marker>
        )}

        {/* Relief Camps */}
        {camps.map((camp) => (
          <Circle
            key={camp.id || camp.name}
            center={[camp.latitude, camp.longitude]}
            radius={camp.radiusMeters || 1200}
            pathOptions={{ color: '#ef4444', fillColor: '#ef4444', fillOpacity: 0.3 }}
          >
            <Popup>
              <div className="p-1">
                <span className="inline-block rounded bg-rose-100 px-1.5 py-0.5 text-[10px] font-bold text-rose-700 uppercase">
                  Relief Camp
                </span>
                <h4 className="mt-1 font-bold text-slate-900 text-sm">{camp.name}</h4>
                <p className="text-xs text-slate-600 mt-1">
                  Population: <span className="font-semibold">{camp.population || 'N/A'}</span>
                </p>
                <p className="text-xs text-slate-500">Status: {camp.status || 'Active'}</p>
              </div>
            </Popup>
          </Circle>
        ))}

        {/* Warehouses */}
        {warehouses.map((wh) => (
          <Marker key={wh.id || wh.name} position={[wh.latitude, wh.longitude]}>
            <Popup>
              <div className="p-1">
                <span className="inline-block rounded bg-indigo-100 px-1.5 py-0.5 text-[10px] font-bold text-indigo-700 uppercase">
                  Supply Warehouse
                </span>
                <h4 className="mt-1 font-bold text-slate-900 text-sm">{wh.name}</h4>
                <p className="text-xs text-slate-600 mt-1">{wh.address || 'Central Storage'}</p>
                <p className="text-xs text-slate-500">Capacity: {wh.capacity || 'Optimal'}</p>
              </div>
            </Popup>
          </Marker>
        ))}

        {/* Live Shipments */}
        {shipments.map((shipment) => (
          <Circle
            key={shipment.id || shipment.shipmentId}
            center={[shipment.currentLat, shipment.currentLng]}
            radius={600}
            pathOptions={{ color: '#0284c7', fillColor: '#38bdf8', fillOpacity: 0.7 }}
          >
            <Popup>
              <div className="p-1">
                <span className="inline-block rounded bg-sky-100 px-1.5 py-0.5 text-[10px] font-bold text-sky-800 uppercase">
                  Live Convoy
                </span>
                <h4 className="mt-1 font-bold text-slate-900 text-sm">
                  Shipment #{String(shipment.id || shipment.shipmentId).substring(0, 8)}
                </h4>
                <p className="text-xs text-slate-600 mt-1">Status: {shipment.status}</p>
                <p className="text-xs text-slate-500">
                  Driver: {shipment.driverName || 'En route'}
                </p>
              </div>
            </Popup>
          </Circle>
        ))}
      </MapContainer>
    </div>
  );
}

LeafletMap.propTypes = {
  center: PropTypes.arrayOf(PropTypes.number),
  zoom: PropTypes.number,
  height: PropTypes.string,
  camps: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.string,
      name: PropTypes.string.isRequired,
      latitude: PropTypes.number.isRequired,
      longitude: PropTypes.number.isRequired,
      radiusMeters: PropTypes.number,
      population: PropTypes.number,
      status: PropTypes.string,
    })
  ),
  warehouses: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.string,
      name: PropTypes.string.isRequired,
      latitude: PropTypes.number.isRequired,
      longitude: PropTypes.number.isRequired,
      address: PropTypes.string,
      capacity: PropTypes.string,
    })
  ),
  shipments: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.string,
      shipmentId: PropTypes.string,
      currentLat: PropTypes.number.isRequired,
      currentLng: PropTypes.number.isRequired,
      status: PropTypes.string.isRequired,
      driverName: PropTypes.string,
    })
  ),
  selectedLocation: PropTypes.shape({
    latitude: PropTypes.number,
    longitude: PropTypes.number,
  }),
  onLocationSelect: PropTypes.func,
};

LeafletMap.defaultProps = {
  center: [34.0522, -118.2437], // Default geographic center
  zoom: 11,
  height: '400px',
  camps: [],
  warehouses: [],
  shipments: [],
  selectedLocation: null,
  onLocationSelect: null,
};
