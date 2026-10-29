import axiosClient from './axiosClient';

export const disasterApi = {
  createDisaster: async (data) => {
    const response = await axiosClient.post('/disasters', data);
    return response.data;
  },

  getDisasters: async (params = {}) => {
    const response = await axiosClient.get('/disasters', { params });
    return response.data;
  },

  getDisasterById: async (disasterId) => {
    const response = await axiosClient.get(`/disasters/${disasterId}`);
    return response.data;
  },

  updateDisaster: async (disasterId, data) => {
    const response = await axiosClient.put(`/disasters/${disasterId}`, data);
    return response.data;
  },

  closeDisaster: async (disasterId, data) => {
    const response = await axiosClient.patch(`/disasters/${disasterId}/close`, data);
    return response.data;
  },

  createDisasterUpdate: async (disasterId, data) => {
    const response = await axiosClient.post(`/disasters/${disasterId}/updates`, data);
    return response.data;
  },

  getDisasterUpdates: async (disasterId, params = {}) => {
    const response = await axiosClient.get(`/disasters/${disasterId}/updates`, { params });
    return response.data;
  },
};
