import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { transportApi } from '../../../api/transportApi';

export function useDriverShipments(driverId) {
  const queryClient = useQueryClient();

  const shipmentsQuery = useQuery({
    queryKey: ['driverShipments', driverId],
    queryFn: () => transportApi.getShipments({ driverId }),
    enabled: Boolean(driverId),
  });

  const pickupMutation = useMutation({
    mutationFn: (shipmentId) => transportApi.pickupShipment(shipmentId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['driverShipments', driverId] });
    },
  });

  const deliverMutation = useMutation({
    mutationFn: ({ shipmentId, data }) => transportApi.deliverShipment(shipmentId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['driverShipments', driverId] });
    },
  });

  const disputeMutation = useMutation({
    mutationFn: ({ shipmentId, data }) => transportApi.disputeShipment(shipmentId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['driverShipments', driverId] });
    },
  });

  const roadClosureMutation = useMutation({
    mutationFn: (data) => transportApi.reportRoadClosure(data),
  });

  return {
    shipments: shipmentsQuery.data?.content || shipmentsQuery.data || [],
    isLoading: shipmentsQuery.isLoading,
    error: shipmentsQuery.error,
    pickupShipment: pickupMutation.mutateAsync,
    isPickingUp: pickupMutation.isPending,
    deliverShipment: deliverMutation.mutateAsync,
    isDelivering: deliverMutation.isPending,
    disputeShipment: disputeMutation.mutateAsync,
    isDisputing: disputeMutation.isPending,
    reportRoadClosure: roadClosureMutation.mutateAsync,
    isReportingClosure: roadClosureMutation.isPending,
  };
}
