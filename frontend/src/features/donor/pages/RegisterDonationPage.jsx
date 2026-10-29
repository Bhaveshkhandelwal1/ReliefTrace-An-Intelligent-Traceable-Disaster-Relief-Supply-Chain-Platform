import { useNavigate } from 'react-router-dom';
import { useForm, useFieldArray } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useAuth } from '../../../auth/useAuth';
import { useDonations } from '../hooks/useDonations';
import { useToast } from '../../../hooks/useToast';
import { donationSchema } from '../../../utils/schemas';
import { DonationType, AidCategory } from '../../../utils/constants';
import { Button } from '../../../components/Button';

export function RegisterDonationPage() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const { registerDonation, isRegistering, activeDisasters } = useDonations(user?.id);
  const toast = useToast();

  const {
    register,
    control,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm({
    resolver: zodResolver(donationSchema),
    defaultValues: {
      type: DonationType.MONETARY,
      disasterId: activeDisasters?.[0]?.disasterId || '550e8400-e29b-41d4-a716-446655440000',
      amount: 100,
      currency: 'USD',
      items: [
        {
          category: AidCategory.FOOD,
          itemName: 'Emergency Ready Rations MRE',
          quantity: 50,
          unit: 'boxes',
          weightKg: 250,
        },
      ],
    },
  });

  const selectedType = watch('type');

  const { fields, append, remove } = useFieldArray({
    control,
    name: 'items',
  });

  const onSubmit = async (data) => {
    try {
      await registerDonation(data);
      toast.success('Thank you! Your donation was registered successfully.');
      navigate('/donor/history');
    } catch {
      toast.error('Failed to register donation. Please check details and try again.');
    }
  };

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      <div className="border-b border-slate-200 pb-4">
        <h1 className="text-2xl font-bold text-slate-900">Register Aid Contribution</h1>
        <p className="text-sm text-slate-500 mt-1">
          Direct your financial or in-kind resources to verified emergency relief operations.
        </p>
      </div>

      <form
        onSubmit={handleSubmit(onSubmit)}
        className="space-y-6 bg-white p-6 sm:p-8 rounded-2xl border border-slate-200 shadow-sm"
      >
        {/* Disaster Event Selection */}
        <div>
          <label htmlFor="disasterId" className="block text-sm font-semibold text-slate-700">
            Target Relief Operation / Disaster
          </label>
          <select
            id="disasterId"
            {...register('disasterId')}
            className="mt-1.5 block w-full rounded-lg border border-slate-300 py-2.5 px-3 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
          >
            {activeDisasters && activeDisasters.length > 0 ? (
              activeDisasters.map((disaster) => (
                <option key={disaster.disasterId} value={disaster.disasterId}>
                  {disaster.name} ({disaster.type} - {disaster.severityLevel})
                </option>
              ))
            ) : (
              <option value="550e8400-e29b-41d4-a716-446655440000">
                Pacific Coast Flood Relief 2026 (Active)
              </option>
            )}
          </select>
          {errors.disasterId && (
            <p className="mt-1 text-xs text-rose-600">{errors.disasterId.message}</p>
          )}
        </div>

        {/* Contribution Type Toggle */}
        <div>
          <span className="block text-sm font-semibold text-slate-700 mb-2">Contribution Type</span>
          <div className="grid grid-cols-2 gap-4">
            <label
              className={`flex items-center justify-center gap-2 p-3 rounded-xl border text-sm font-semibold cursor-pointer transition-all ${
                selectedType === DonationType.MONETARY
                  ? 'border-indigo-600 bg-indigo-50/50 text-indigo-700 ring-1 ring-indigo-600'
                  : 'border-slate-200 text-slate-600 hover:bg-slate-50'
              }`}
            >
              <input
                type="radio"
                value={DonationType.MONETARY}
                {...register('type')}
                className="sr-only"
              />
              <span>💵 Financial Donation</span>
            </label>
            <label
              className={`flex items-center justify-center gap-2 p-3 rounded-xl border text-sm font-semibold cursor-pointer transition-all ${
                selectedType === DonationType.IN_KIND
                  ? 'border-indigo-600 bg-indigo-50/50 text-indigo-700 ring-1 ring-indigo-600'
                  : 'border-slate-200 text-slate-600 hover:bg-slate-50'
              }`}
            >
              <input
                type="radio"
                value={DonationType.IN_KIND}
                {...register('type')}
                className="sr-only"
              />
              <span>📦 In-Kind Physical Goods</span>
            </label>
          </div>
        </div>

        {/* Monetary Inputs */}
        {selectedType === DonationType.MONETARY && (
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 p-4 rounded-xl bg-slate-50 border border-slate-200">
            <div className="sm:col-span-2">
              <label
                htmlFor="amount"
                className="block text-xs font-semibold text-slate-700 uppercase"
              >
                Donation Amount
              </label>
              <div className="mt-1 relative rounded-lg shadow-sm">
                <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3">
                  <span className="text-slate-500 sm:text-sm">$</span>
                </div>
                <input
                  id="amount"
                  type="number"
                  step="0.01"
                  {...register('amount')}
                  className="block w-full rounded-lg border border-slate-300 pl-7 pr-12 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                  placeholder="250.00"
                />
              </div>
              {errors.amount && (
                <p className="mt-1 text-xs text-rose-600">{errors.amount.message}</p>
              )}
            </div>

            <div>
              <label
                htmlFor="currency"
                className="block text-xs font-semibold text-slate-700 uppercase"
              >
                Currency
              </label>
              <select
                id="currency"
                {...register('currency')}
                className="mt-1 block w-full rounded-lg border border-slate-300 py-2 px-3 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
              >
                <option value="USD">USD ($)</option>
                <option value="EUR">EUR (€)</option>
                <option value="GBP">GBP (£)</option>
              </select>
            </div>
          </div>
        )}

        {/* In-Kind Item Lines */}
        {selectedType === DonationType.IN_KIND && (
          <div className="space-y-4 p-4 rounded-xl bg-slate-50 border border-slate-200">
            <div className="flex items-center justify-between">
              <h3 className="text-sm font-bold text-slate-800">In-Kind Items Line Registry</h3>
              <button
                type="button"
                onClick={() =>
                  append({
                    category: AidCategory.WATER,
                    itemName: '',
                    quantity: 1,
                    unit: 'packs',
                    weightKg: 10,
                  })
                }
                className="text-xs font-semibold text-indigo-600 hover:text-indigo-800"
              >
                + Add Item Line
              </button>
            </div>

            {fields.map((field, index) => (
              <div
                key={field.id}
                className="grid grid-cols-1 sm:grid-cols-12 gap-3 p-3 bg-white rounded-lg border border-slate-200 items-end"
              >
                <div className="sm:col-span-3">
                  <label
                    htmlFor={`items-${index}-category`}
                    className="block text-[11px] font-semibold text-slate-600"
                  >
                    Category
                  </label>
                  <select
                    id={`items-${index}-category`}
                    {...register(`items.${index}.category`)}
                    className="mt-1 block w-full rounded-md border border-slate-300 py-1.5 px-2 text-xs focus:ring-indigo-500"
                  >
                    {Object.values(AidCategory).map((cat) => (
                      <option key={cat} value={cat}>
                        {cat}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="sm:col-span-4">
                  <label
                    htmlFor={`items-${index}-itemName`}
                    className="block text-[11px] font-semibold text-slate-600"
                  >
                    Item Description
                  </label>
                  <input
                    id={`items-${index}-itemName`}
                    type="text"
                    {...register(`items.${index}.itemName`)}
                    placeholder="e.g. Purified Drinking Water (1L)"
                    className="mt-1 block w-full rounded-md border border-slate-300 py-1.5 px-2 text-xs focus:ring-indigo-500"
                  />
                  {errors.items?.[index]?.itemName && (
                    <p className="text-[10px] text-rose-600">Required</p>
                  )}
                </div>

                <div className="sm:col-span-2">
                  <label
                    htmlFor={`items-${index}-quantity`}
                    className="block text-[11px] font-semibold text-slate-600"
                  >
                    Quantity
                  </label>
                  <input
                    id={`items-${index}-quantity`}
                    type="number"
                    {...register(`items.${index}.quantity`)}
                    className="mt-1 block w-full rounded-md border border-slate-300 py-1.5 px-2 text-xs focus:ring-indigo-500"
                  />
                </div>

                <div className="sm:col-span-2">
                  <label
                    htmlFor={`items-${index}-unit`}
                    className="block text-[11px] font-semibold text-slate-600"
                  >
                    Unit
                  </label>
                  <input
                    id={`items-${index}-unit`}
                    type="text"
                    {...register(`items.${index}.unit`)}
                    placeholder="cases"
                    className="mt-1 block w-full rounded-md border border-slate-300 py-1.5 px-2 text-xs focus:ring-indigo-500"
                  />
                </div>

                <div className="sm:col-span-1 flex justify-end">
                  <button
                    type="button"
                    disabled={fields.length === 1}
                    onClick={() => remove(index)}
                    aria-label="Remove item line"
                    className="p-1.5 text-slate-400 hover:text-rose-600 disabled:opacity-30"
                  >
                    ✕
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}

        <div className="flex justify-end gap-3 pt-4 border-t border-slate-100">
          <Button type="button" variant="ghost" onClick={() => navigate('/donor')}>
            Cancel
          </Button>
          <Button type="submit" variant="primary" loading={isRegistering}>
            Submit Contribution
          </Button>
        </div>
      </form>
    </div>
  );
}
