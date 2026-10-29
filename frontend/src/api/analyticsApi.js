import axiosClient from './axiosClient';

export const analyticsApi = {
  getDashboard: async (params = {}) => {
    const response = await axiosClient.get('/analytics/dashboard', { params });
    return response.data;
  },

  getDonationsSummary: async (params = {}) => {
    const response = await axiosClient.get('/analytics/donations/summary', { params });
    return response.data;
  },

  getRequestsSummary: async (params = {}) => {
    const response = await axiosClient.get('/analytics/requests/summary', { params });
    return response.data;
  },

  getShipmentsSummary: async (params = {}) => {
    const response = await axiosClient.get('/analytics/shipments/summary', { params });
    return response.data;
  },

  getDisastersSummary: async (params = {}) => {
    const response = await axiosClient.get('/analytics/disasters/summary', { params });
    return response.data;
  },
};
