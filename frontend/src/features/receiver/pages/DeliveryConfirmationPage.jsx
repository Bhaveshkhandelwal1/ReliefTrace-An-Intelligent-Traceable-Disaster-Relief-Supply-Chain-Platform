import { useParams, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useAuth } from '../../../auth/useAuth';
import { useAidRequests } from '../hooks/useAidRequests';
import { useToast } from '../../../hooks/useToast';
import { deliveryConfirmationSchema } from '../../../utils/schemas';
import { Button } from '../../../components/Button';

export function DeliveryConfirmationPage() {
  const { requestId } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const { confirmDelivery, isConfirming } = useAidRequests(user?.id);
  const toast = useToast();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm({
    resolver: zodResolver(deliveryConfirmationSchema),
    defaultValues: {
      shortageQuantity: 0,
      damageNotes: '',
      proofOfDeliveryUrl:
        'https://images.unsplash.com/photo-1593113598332-cd288d649433?auto=format&fit=crop&w=600&q=80',
      recipientSignature: user?.name || 'Camp Coordinator',
    },
  });

  const onSubmit = async (data) => {
    try {
      await confirmDelivery({ requestId, data });
      toast.success('Delivery receipt confirmed and closed in platform ledger!');
      navigate('/receiver');
    } catch {
      toast.error('Failed to confirm delivery. Please retry.');
    }
  };

  return (
    <div className="max-w-xl mx-auto space-y-6">
      <div className="border-b border-slate-200 pb-4">
        <h1 className="text-2xl font-bold text-slate-900">Confirm Delivery Receipt</h1>
        <p className="text-sm text-slate-500 mt-1">
          Verify physical handover of aid supplies for Request #{requestId?.substring(0, 8)}.
        </p>
      </div>

      <form
        onSubmit={handleSubmit(onSubmit)}
        className="space-y-6 bg-white p-6 sm:p-8 rounded-2xl border border-slate-200 shadow-sm"
      >
        <div>
          <label htmlFor="shortageQuantity" className="block text-sm font-semibold text-slate-700">
            Shortage Quantity (Units Missing, if any)
          </label>
          <input
            id="shortageQuantity"
            type="number"
            {...register('shortageQuantity')}
            className="mt-1 block w-full rounded-lg border border-slate-300 py-2 px-3 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
          />
          {errors.shortageQuantity && (
            <p className="mt-1 text-xs text-rose-600">{errors.shortageQuantity.message}</p>
          )}
        </div>

        <div>
          <label htmlFor="damageNotes" className="block text-sm font-semibold text-slate-700">
            Damage or Discrepancy Notes
          </label>
          <textarea
            id="damageNotes"
            rows={3}
            {...register('damageNotes')}
            placeholder="e.g. 2 water cartons were damaged in transit due to rain."
            className="mt-1 block w-full rounded-lg border border-slate-300 py-2 px-3 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
          />
        </div>

        <div>
          <label
            htmlFor="proofOfDeliveryUrl"
            className="block text-sm font-semibold text-slate-700"
          >
            Proof of Delivery Photo URL
          </label>
          <input
            id="proofOfDeliveryUrl"
            type="text"
            {...register('proofOfDeliveryUrl')}
            className="mt-1 block w-full rounded-lg border border-slate-300 py-2 px-3 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
          />
          {errors.proofOfDeliveryUrl && (
            <p className="mt-1 text-xs text-rose-600">{errors.proofOfDeliveryUrl.message}</p>
          )}
        </div>

        <div>
          <label
            htmlFor="recipientSignature"
            className="block text-sm font-semibold text-slate-700"
          >
            Authorized Receiver Signature / Name
          </label>
          <input
            id="recipientSignature"
            type="text"
            {...register('recipientSignature')}
            className="mt-1 block w-full rounded-lg border border-slate-300 py-2 px-3 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
          />
          {errors.recipientSignature && (
            <p className="mt-1 text-xs text-rose-600">{errors.recipientSignature.message}</p>
          )}
        </div>

        <div className="flex justify-end gap-3 pt-4 border-t border-slate-100">
          <Button type="button" variant="ghost" onClick={() => navigate('/receiver')}>
            Back
          </Button>
          <Button type="submit" variant="success" loading={isConfirming}>
            Confirm & Complete Receipt
          </Button>
        </div>
      </form>
    </div>
  );
}
