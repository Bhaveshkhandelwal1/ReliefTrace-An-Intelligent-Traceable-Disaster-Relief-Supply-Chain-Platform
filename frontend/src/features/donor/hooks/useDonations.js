import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { donorApi } from '../../../api/donorApi';
import { disasterApi } from '../../../api/disasterApi';

export function useDonations(donorId) {
  const queryClient = useQueryClient();

  const donationsQuery = useQuery({
    queryKey: ['donations', donorId],
    queryFn: () => donorApi.getDonations(donorId),
    enabled: Boolean(donorId),
  });

  const impactQuery = useQuery({
    queryKey: ['donorImpact', donorId],
    queryFn: () => donorApi.getDonorImpact(donorId),
    enabled: Boolean(donorId),
  });

  const activeDisastersQuery = useQuery({
    queryKey: ['activeDisasters'],
    queryFn: () => disasterApi.getDisasters({ status: 'ACTIVE' }),
  });

  const registerDonationMutation = useMutation({
    mutationFn: (data) => donorApi.createDonation(donorId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['donations', donorId] });
      queryClient.invalidateQueries({ queryKey: ['donorImpact', donorId] });
    },
  });

  const cancelDonationMutation = useMutation({
    mutationFn: (donationId) => donorApi.cancelDonation(donorId, donationId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['donations', donorId] });
    },
  });

  return {
    donations: donationsQuery.data || [],
    isLoadingDonations: donationsQuery.isLoading,
    donationsError: donationsQuery.error,
    impact: impactQuery.data,
    isLoadingImpact: impactQuery.isLoading,
    activeDisasters: activeDisastersQuery.data?.content || activeDisastersQuery.data || [],
    registerDonation: registerDonationMutation.mutateAsync,
    isRegistering: registerDonationMutation.isPending,
    cancelDonation: cancelDonationMutation.mutateAsync,
    isCancelling: cancelDonationMutation.isPending,
  };
}
