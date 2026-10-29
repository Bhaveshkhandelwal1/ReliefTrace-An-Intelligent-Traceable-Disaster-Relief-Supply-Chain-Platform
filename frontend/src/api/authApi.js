import axiosClient from './axiosClient';

export const authApi = {
  getCurrentProfile: async () => {
    const response = await axiosClient.get('/auth/me');
    return response.data;
  },
};
