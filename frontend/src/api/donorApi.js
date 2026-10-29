import axiosClient from './axiosClient';

export const donorApi = {
  registerDonor: async (data) => {
    const response = await axiosClient.post('/donors', data);
    return response.data;
  },

  getDonorById: async (donorId) => {
    const response = await axiosClient.get(`/donors/${donorId}`);
    return response.data;
  },

  updateDonor: async (donorId, data) => {
    const response = await axiosClient.put(`/donors/${donorId}`, data);
    return response.data;
  },

  listDonors: async (params = {}) => {
    const response = await axiosClient.get('/donors', { params });
    return response.data;
  },

  createDonation: async (donorId, data) => {
    const response = await axiosClient.post(`/donors/${donorId}/donations`, data);
    return response.data;
  },

  getDonations: async (donorId, params = {}) => {
    const response = await axiosClient.get(`/donors/${donorId}/donations`, { params });
    return response.data;
  },

  getDonationById: async (donorId, donationId) => {
    const response = await axiosClient.get(`/donors/${donorId}/donations/${donationId}`);
    return response.data;
  },

  cancelDonation: async (donorId, donationId) => {
    const response = await axiosClient.patch(`/donors/${donorId}/donations/${donationId}/cancel`);
    return response.data;
  },

  getDonorImpact: async (donorId) => {
    const response = await axiosClient.get(`/donors/${donorId}/impact`);
    return response.data;
  },
};
