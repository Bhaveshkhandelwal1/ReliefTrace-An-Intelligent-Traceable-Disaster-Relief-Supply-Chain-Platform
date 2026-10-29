import axiosClient from './axiosClient';

export const transportApi = {
  createDriver: async (data) => {
    const response = await axiosClient.post('/transport/drivers', data);
    return response.data;
  },

  getDrivers: async (params = {}) => {
    const response = await axiosClient.get('/transport/drivers', { params });
    return response.data;
  },

  getDriverById: async (driverId) => {
    const response = await axiosClient.get(`/transport/drivers/${driverId}`);
    return response.data;
  },

  updateDriver: async (driverId, data) => {
    const response = await axiosClient.put(`/transport/drivers/${driverId}`, data);
    return response.data;
  },

  createVehicle: async (data) => {
    const response = await axiosClient.post('/transport/vehicles', data);
    return response.data;
  },

  getVehicles: async (params = {}) => {
    const response = await axiosClient.get('/transport/vehicles', { params });
    return response.data;
  },

  getShipments: async (params = {}) => {
    const response = await axiosClient.get('/transport/shipments', { params });
    return response.data;
  },

  getShipmentById: async (shipmentId) => {
    const response = await axiosClient.get(`/transport/shipments/${shipmentId}`);
    return response.data;
  },

  createShipment: async (data) => {
    const response = await axiosClient.post('/transport/shipments', data);
    return response.data;
  },

  assignDriver: async (shipmentId, data) => {
    const response = await axiosClient.patch(`/transport/shipments/${shipmentId}/assign`, data);
    return response.data;
  },

  pickupShipment: async (shipmentId) => {
    const response = await axiosClient.patch(`/transport/shipments/${shipmentId}/pickup`);
    return response.data;
  },

  deliverShipment: async (shipmentId, data) => {
    const response = await axiosClient.patch(`/transport/shipments/${shipmentId}/deliver`, data);
    return response.data;
  },

  disputeShipment: async (shipmentId, data) => {
    const response = await axiosClient.patch(`/transport/shipments/${shipmentId}/dispute`, data);
    return response.data;
  },

  getTracking: async (shipmentId) => {
    const response = await axiosClient.get(`/transport/shipments/${shipmentId}/tracking`);
    return response.data;
  },

  reportRoadClosure: async (data) => {
    const response = await axiosClient.post('/transport/road-closures', data);
    return response.data;
  },
};
