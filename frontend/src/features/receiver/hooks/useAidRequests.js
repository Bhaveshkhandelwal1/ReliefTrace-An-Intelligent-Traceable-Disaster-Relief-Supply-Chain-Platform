import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { receiverApi } from '../../../api/receiverApi';
import { disasterApi } from '../../../api/disasterApi';

export function useAidRequests(receiverId) {
  const queryClient = useQueryClient();

  const requestsQuery = useQuery({
    queryKey: ['aidRequests', receiverId],
    queryFn: () => receiverApi.getAidRequests(receiverId),
    enabled: Boolean(receiverId),
  });

  const activeDisastersQuery = useQuery({
    queryKey: ['activeDisasters'],
    queryFn: () => disasterApi.getDisasters({ status: 'ACTIVE' }),
  });

  const createRequestMutation = useMutation({
    mutationFn: (data) => receiverApi.createAidRequest(receiverId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['aidRequests', receiverId] });
    },
  });

  const confirmDeliveryMutation = useMutation({
    mutationFn: ({ requestId, data }) => receiverApi.confirmDelivery(receiverId, requestId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['aidRequests', receiverId] });
    },
  });

  const cancelRequestMutation = useMutation({
    mutationFn: (requestId) => receiverApi.cancelAidRequest(receiverId, requestId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['aidRequests', receiverId] });
    },
  });

  return {
    requests: requestsQuery.data?.content || requestsQuery.data || [],
    isLoadingRequests: requestsQuery.isLoading,
    requestsError: requestsQuery.error,
    activeDisasters: activeDisastersQuery.data?.content || activeDisastersQuery.data || [],
    createRequest: createRequestMutation.mutateAsync,
    isCreating: createRequestMutation.isPending,
    confirmDelivery: confirmDeliveryMutation.mutateAsync,
    isConfirming: confirmDeliveryMutation.isPending,
    cancelRequest: cancelRequestMutation.mutateAsync,
    isCancelling: cancelRequestMutation.isPending,
  };
}
