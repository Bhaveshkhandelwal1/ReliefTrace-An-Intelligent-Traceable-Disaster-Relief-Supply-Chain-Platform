import axiosClient from './axiosClient';

export const verificationApi = {
  getCases: async (params = {}) => {
    const response = await axiosClient.get('/verification/cases', { params });
    return response.data;
  },

  getCaseById: async (caseId) => {
    const response = await axiosClient.get(`/verification/cases/${caseId}`);
    return response.data;
  },

  approveCase: async (caseId, data = {}) => {
    const response = await axiosClient.patch(`/verification/cases/${caseId}/approve`, data);
    return response.data;
  },

  rejectCase: async (caseId, data) => {
    const response = await axiosClient.patch(`/verification/cases/${caseId}/reject`, data);
    return response.data;
  },

  uploadDocument: async (caseId, data) => {
    const response = await axiosClient.post(`/verification/cases/${caseId}/documents`, data);
    return response.data;
  },

  getCaseByRequestId: async (requestId) => {
    const response = await axiosClient.get(`/verification/cases/by-request/${requestId}`);
    return response.data;
  },
};
