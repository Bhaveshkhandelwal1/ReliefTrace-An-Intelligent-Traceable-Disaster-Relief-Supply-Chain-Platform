import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { analyticsApi } from '../../../api/analyticsApi';
import { matchingApi } from '../../../api/matchingApi';
import { verificationApi } from '../../../api/verificationApi';
import { transportApi } from '../../../api/transportApi';
import { inventoryApi } from '../../../api/inventoryApi';
import { disasterApi } from '../../../api/disasterApi';

export function useAdminOperations() {
  const queryClient = useQueryClient();

  const dashboardQuery = useQuery({
    queryKey: ['adminDashboard'],
    queryFn: () => analyticsApi.getDashboard(),
  });

  const allocationsQuery = useQuery({
    queryKey: ['allocations'],
    queryFn: () => matchingApi.getAllocations(),
  });

  const verificationCasesQuery = useQuery({
    queryKey: ['verificationCases'],
    queryFn: () => verificationApi.getCases(),
  });

  const shipmentsQuery = useQuery({
    queryKey: ['adminShipments'],
    queryFn: () => transportApi.getShipments(),
  });

  const warehousesQuery = useQuery({
    queryKey: ['warehouses'],
    queryFn: () => inventoryApi.getWarehouses(),
  });

  const disastersQuery = useQuery({
    queryKey: ['disasters'],
    queryFn: () => disasterApi.getDisasters(),
  });

  const approveAllocationMutation = useMutation({
    mutationFn: ({ allocationId, data }) => matchingApi.approveAllocation(allocationId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['allocations'] });
      queryClient.invalidateQueries({ queryKey: ['adminDashboard'] });
    },
  });

  const rejectAllocationMutation = useMutation({
    mutationFn: ({ allocationId, data }) => matchingApi.rejectAllocation(allocationId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['allocations'] });
    },
  });

  const approveCaseMutation = useMutation({
    mutationFn: ({ caseId, data }) => verificationApi.approveCase(caseId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['verificationCases'] });
    },
  });

  const rejectCaseMutation = useMutation({
    mutationFn: ({ caseId, data }) => verificationApi.rejectCase(caseId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['verificationCases'] });
    },
  });

  return {
    dashboard: dashboardQuery.data,
    isLoadingDashboard: dashboardQuery.isLoading,
    allocations: allocationsQuery.data?.content || allocationsQuery.data || [],
    isLoadingAllocations: allocationsQuery.isLoading,
    verificationCases: verificationCasesQuery.data?.content || verificationCasesQuery.data || [],
    isLoadingVerification: verificationCasesQuery.isLoading,
    shipments: shipmentsQuery.data?.content || shipmentsQuery.data || [],
    isLoadingShipments: shipmentsQuery.isLoading,
    warehouses: warehousesQuery.data?.content || warehousesQuery.data || [],
    disasters: disastersQuery.data?.content || disastersQuery.data || [],
    approveAllocation: approveAllocationMutation.mutateAsync,
    isApprovingAllocation: approveAllocationMutation.isPending,
    rejectAllocation: rejectAllocationMutation.mutateAsync,
    isRejectingAllocation: rejectAllocationMutation.isPending,
    approveCase: approveCaseMutation.mutateAsync,
    isApprovingCase: approveCaseMutation.isPending,
    rejectCase: rejectCaseMutation.mutateAsync,
    isRejectingCase: rejectCaseMutation.isPending,
  };
}
