import axiosClient from './axiosClient';

export const inventoryApi = {
  createWarehouse: async (data) => {
    const response = await axiosClient.post('/inventory/warehouses', data);
    return response.data;
  },

  getWarehouses: async (params = {}) => {
    const response = await axiosClient.get('/inventory/warehouses', { params });
    return response.data;
  },

  getWarehouseById: async (warehouseId) => {
    const response = await axiosClient.get(`/inventory/warehouses/${warehouseId}`);
    return response.data;
  },

  updateWarehouse: async (warehouseId, data) => {
    const response = await axiosClient.put(`/inventory/warehouses/${warehouseId}`, data);
    return response.data;
  },

  createItem: async (data) => {
    const response = await axiosClient.post('/inventory/items', data);
    return response.data;
  },

  getItems: async (params = {}) => {
    const response = await axiosClient.get('/inventory/items', { params });
    return response.data;
  },

  getItemById: async (itemId) => {
    const response = await axiosClient.get(`/inventory/items/${itemId}`);
    return response.data;
  },

  adjustStock: async (itemId, data) => {
    const response = await axiosClient.post(`/inventory/items/${itemId}/stock`, data);
    return response.data;
  },

  getReservations: async (params = {}) => {
    const response = await axiosClient.get('/inventory/reservations', { params });
    return response.data;
  },

  deleteReservation: async (reservationId) => {
    const response = await axiosClient.delete(`/inventory/reservations/${reservationId}`);
    return response.data;
  },
};
