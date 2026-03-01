import axiosInstance from './axiosInstance';

export const wilayahApi = {
  // Province
  getProvinces: (params) => axiosInstance.get('/wilayah/provinces', { params }),
  getProvince: (id) => axiosInstance.get(`/wilayah/provinces/${id}`),
  createProvince: (data) => axiosInstance.post('/wilayah/provinces', data),
  updateProvince: (id, data) => axiosInstance.put(`/wilayah/provinces/${id}`, data),
  deleteProvince: (id) => axiosInstance.delete(`/wilayah/provinces/${id}`),

  // State (Kab/Kota)
  getStates: (params) => axiosInstance.get('/wilayah/states', { params }),
  getState: (id) => axiosInstance.get(`/wilayah/states/${id}`),
  createState: (data) => axiosInstance.post('/wilayah/states', data),
  updateState: (id, data) => axiosInstance.put(`/wilayah/states/${id}`, data),
  deleteState: (id) => axiosInstance.delete(`/wilayah/states/${id}`),

  // District (Kecamatan)
  getDistricts: (params) => axiosInstance.get('/wilayah/districts', { params }),
  getDistrict: (id) => axiosInstance.get(`/wilayah/districts/${id}`),
  createDistrict: (data) => axiosInstance.post('/wilayah/districts', data),
  updateDistrict: (id, data) => axiosInstance.put(`/wilayah/districts/${id}`, data),
  deleteDistrict: (id) => axiosInstance.delete(`/wilayah/districts/${id}`),

  // SubDistrict (Kel/Desa)
  getSubDistricts: (params) => axiosInstance.get('/wilayah/subdistricts', { params }),
  getSubDistrict: (id) => axiosInstance.get(`/wilayah/subdistricts/${id}`),
  createSubDistrict: (data) => axiosInstance.post('/wilayah/subdistricts', data),
  updateSubDistrict: (id, data) => axiosInstance.put(`/wilayah/subdistricts/${id}`, data),
  deleteSubDistrict: (id) => axiosInstance.delete(`/wilayah/subdistricts/${id}`),

  // Inquiry
  inquiry: (params) => axiosInstance.get('/wilayah/inquiry', { params }),

  // Validasi (Nominatim OSM)
  validate: (params) => axiosInstance.get('/wilayah/validate', { params }),
};
