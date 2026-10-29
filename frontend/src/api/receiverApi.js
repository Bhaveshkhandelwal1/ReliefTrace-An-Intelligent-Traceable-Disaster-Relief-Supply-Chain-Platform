import axiosClient from './axiosClient';

export const receiverApi = {
  registerReceiver: async (data) => {
    const response = await axiosClient.post('/receivers', data);
    return response.data;
  },

  getReceiverById: async (receiverId) => {
    const response = await axiosClient.get(`/receivers/${receiverId}`);
    return response.data;
  },

  updateReceiver: async (receiverId, data) => {
    const response = await axiosClient.put(`/receivers/${receiverId}`, data);
    return response.data;
  },

  listReceivers: async (params = {}) => {
    const response = await axiosClient.get('/receivers', { params });
    return response.data;
  },

  createAidRequest: async (receiverId, data) => {
    const response = await axiosClient.post(`/receivers/${receiverId}/requests`, data);
    return response.data;
  },

  getAidRequests: async (receiverId, params = {}) => {
    const response = await axiosClient.get(`/receivers/${receiverId}/requests`, { params });
    return response.data;
  },

  getAidRequestById: async (receiverId, requestId) => {
    const response = await axiosClient.get(`/receivers/${receiverId}/requests/${requestId}`);
    return response.data;
  },

  cancelAidRequest: async (receiverId, requestId) => {
    const response = await axiosClient.patch(
      `/receivers/${receiverId}/requests/${requestId}/cancel`
    );
    return response.data;
  },

  getAllRequests: async (params = {}) => {
    const response = await axiosClient.get('/requests', { params });
    return response.data;
  },

  confirmDelivery: async (receiverId, requestId, data) => {
    const response = await axiosClient.patch(
      `/receivers/${receiverId}/requests/${requestId}/confirm-delivery`,
      data
    );
    return response.data;
  },
};
