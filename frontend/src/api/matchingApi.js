import axiosClient from './axiosClient';

export const matchingApi = {
  getSuggestions: async (requestId) => {
    const response = await axiosClient.get('/matching/suggestions', {
      params: { requestId },
    });
    return response.data;
  },

  createAllocation: async (data) => {
    const response = await axiosClient.post('/matching/allocations', data);
    return response.data;
  },

  getAllocations: async (params = {}) => {
    const response = await axiosClient.get('/matching/allocations', { params });
    return response.data;
  },

  getAllocationById: async (allocationId) => {
    const response = await axiosClient.get(`/matching/allocations/${allocationId}`);
    return response.data;
  },

  approveAllocation: async (allocationId, data = {}) => {
    const response = await axiosClient.patch(`/matching/allocations/${allocationId}/approve`, data);
    return response.data;
  },

  rejectAllocation: async (allocationId, data = {}) => {
    const response = await axiosClient.patch(`/matching/allocations/${allocationId}/reject`, data);
    return response.data;
  },
};
