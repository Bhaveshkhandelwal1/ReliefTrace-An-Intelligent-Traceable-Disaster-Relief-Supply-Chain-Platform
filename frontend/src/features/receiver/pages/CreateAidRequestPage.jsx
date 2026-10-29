import { useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useAuth } from '../../../auth/useAuth';
import { useAidRequests } from '../hooks/useAidRequests';
import { useToast } from '../../../hooks/useToast';
import { aidRequestSchema } from '../../../utils/schemas';
import { AidCategory, UrgencyLevel } from '../../../utils/constants';
import { LeafletMap } from '../../../components/LeafletMap';
import { Button } from '../../../components/Button';

export function CreateAidRequestPage() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const { createRequest, isCreating, activeDisasters } = useAidRequests(user?.id);
  const toast = useToast();

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    formState: { errors },
  } = useForm({
    resolver: zodResolver(aidRequestSchema),
    defaultValues: {
      disasterId: activeDisasters?.[0]?.disasterId || '550e8400-e29b-41d4-a716-446655440000',
      category: AidCategory.FOOD,
      itemName: 'Dry Food Rations & Water',
      quantity: 100,
      unit: 'kits',
      urgency: UrgencyLevel.HIGH,
      latitude: 34.0522,
      longitude: -118.2437,
      locationName: 'Camp Alpha Sector 4',
      contactPhone: '+1-555-0199',
      notes: 'Road access currently restricted to high-clearance 4x4 trucks only.',
    },
  });

  const selectedLat = watch('latitude');
  const selectedLng = watch('longitude');

  const handleMapLocationSelect = (coords) => {
    setValue('latitude', coords.latitude, { shouldValidate: true });
    setValue('longitude', coords.longitude, { shouldValidate: true });
    toast.info(`Coordinates pinned: ${coords.latitude}, ${coords.longitude}`);
  };

  const onSubmit = async (data) => {
    try {
      await createRequest(data);
      toast.success('Emergency aid request submitted and routed to verification!');
      navigate('/receiver');
    } catch {
      toast.error('Failed to submit aid request. Please verify inputs.');
    }
  };

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      <div className="border-b border-slate-200 pb-4">
        <h1 className="text-2xl font-bold text-slate-900">Submit Emergency Aid Request</h1>
        <p className="text-sm text-slate-500 mt-1">
          Specify needed aid supplies and drop pin coordinates on the map for delivery routing.
        </p>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 bg-white p-6 sm:p-8 rounded-2xl border border-slate-200 shadow-sm">
          {/* Target Disaster */}
          <div className="md:col-span-2">
            <label htmlFor="disasterId" className="block text-sm font-semibold text-slate-700">
              Relief Disaster Event
            </label>
            <select
              id="disasterId"
              {...register('disasterId')}
              className="mt-1 block w-full rounded-lg border border-slate-300 py-2.5 px-3 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
            >
              {activeDisasters?.map((d) => (
                <option key={d.disasterId} value={d.disasterId}>
                  {d.name} ({d.severityLevel})
                </option>
              )) || (
                <option value="550e8400-e29b-41d4-a716-446655440000">
                  Pacific Coast Flood Relief (Active)
                </option>
              )}
            </select>
          </div>

          {/* Aid Category */}
          <div>
            <label htmlFor="category" className="block text-sm font-semibold text-slate-700">
              Aid Category
            </label>
            <select
              id="category"
              {...register('category')}
              className="mt-1 block w-full rounded-lg border border-slate-300 py-2.5 px-3 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
            >
              {Object.values(AidCategory).map((cat) => (
                <option key={cat} value={cat}>
                  {cat}
                </option>
              ))}
            </select>
          </div>

          {/* Urgency */}
          <div>
            <label htmlFor="urgency" className="block text-sm font-semibold text-slate-700">
              Urgency Level
            </label>
            <select
              id="urgency"
              {...register('urgency')}
              className="mt-1 block w-full rounded-lg border border-slate-300 py-2.5 px-3 text-sm font-semibold text-rose-700 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
            >
              {Object.values(UrgencyLevel).map((lvl) => (
                <option key={lvl} value={lvl}>
                  {lvl}
                </option>
              ))}
            </select>
          </div>

          {/* Specific Item */}
          <div className="md:col-span-2">
            <label htmlFor="itemName" className="block text-sm font-semibold text-slate-700">
              Item Details & Specifications
            </label>
            <input
              id="itemName"
              type="text"
              {...register('itemName')}
              placeholder="e.g. Bottled Potable Water & Water Purification Tablets"
              className="mt-1 block w-full rounded-lg border border-slate-300 py-2 px-3 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
            />
            {errors.itemName && (
              <p className="mt-1 text-xs text-rose-600">{errors.itemName.message}</p>
            )}
          </div>

          {/* Quantity & Unit */}
          <div>
            <label htmlFor="quantity" className="block text-sm font-semibold text-slate-700">
              Quantity Required
            </label>
            <input
              id="quantity"
              type="number"
              {...register('quantity')}
              className="mt-1 block w-full rounded-lg border border-slate-300 py-2 px-3 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
            />
            {errors.quantity && (
              <p className="mt-1 text-xs text-rose-600">{errors.quantity.message}</p>
            )}
          </div>

          <div>
            <label htmlFor="unit" className="block text-sm font-semibold text-slate-700">
              Unit of Measure
            </label>
            <input
              id="unit"
              type="text"
              {...register('unit')}
              placeholder="boxes, pallets, kits"
              className="mt-1 block w-full rounded-lg border border-slate-300 py-2 px-3 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
            />
            {errors.unit && <p className="mt-1 text-xs text-rose-600">{errors.unit.message}</p>}
          </div>

          {/* Location Name & Contact */}
          <div>
            <label htmlFor="locationName" className="block text-sm font-semibold text-slate-700">
              Camp / Distribution Point Name
            </label>
            <input
              id="locationName"
              type="text"
              {...register('locationName')}
              placeholder="e.g. Hope Shelter Camp North"
              className="mt-1 block w-full rounded-lg border border-slate-300 py-2 px-3 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
            />
            {errors.locationName && (
              <p className="mt-1 text-xs text-rose-600">{errors.locationName.message}</p>
            )}
          </div>

          <div>
            <label htmlFor="contactPhone" className="block text-sm font-semibold text-slate-700">
              Camp Coordinator Phone
            </label>
            <input
              id="contactPhone"
              type="tel"
              {...register('contactPhone')}
              placeholder="+1-555-0123"
              className="mt-1 block w-full rounded-lg border border-slate-300 py-2 px-3 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
            />
            {errors.contactPhone && (
              <p className="mt-1 text-xs text-rose-600">{errors.contactPhone.message}</p>
            )}
          </div>

          {/* Interactive Leaflet Map for Geographic Coordinates */}
          <div className="md:col-span-2 space-y-2">
            <div className="flex items-center justify-between">
              <span className="block text-sm font-semibold text-slate-700">
                Drop Delivery Location Pin (Click on Map)
              </span>
              <span className="text-xs font-mono font-semibold text-indigo-600">
                Lat: {selectedLat}, Lng: {selectedLng}
              </span>
            </div>

            <LeafletMap
              height="280px"
              center={[selectedLat || 34.0522, selectedLng || -118.2437]}
              zoom={11}
              selectedLocation={{ latitude: selectedLat, longitude: selectedLng }}
              onLocationSelect={handleMapLocationSelect}
            />
            <p className="text-[11px] text-slate-400">
              Click anywhere on the map to accurately place the delivery destination pin for
              drivers.
            </p>
          </div>

          {/* Notes */}
          <div className="md:col-span-2">
            <label htmlFor="notes" className="block text-sm font-semibold text-slate-700">
              Access & Delivery Notes (Optional)
            </label>
            <textarea
              id="notes"
              rows={2}
              {...register('notes')}
              className="mt-1 block w-full rounded-lg border border-slate-300 py-2 px-3 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
              placeholder="e.g. Gate 3 is blocked by mud, use the southern gravel access road."
            />
          </div>
        </div>

        <div className="flex justify-end gap-3">
          <Button type="button" variant="ghost" onClick={() => navigate('/receiver')}>
            Cancel
          </Button>
          <Button type="submit" variant="primary" loading={isCreating}>
            Submit Aid Request
          </Button>
        </div>
      </form>
    </div>
  );
}
