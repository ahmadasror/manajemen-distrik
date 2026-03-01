import axiosInstance from './axiosInstance';

export const bulkUploadApi = {
  uploadWilayah: (file) => {
    const formData = new FormData();
    formData.append('file', file);
    return axiosInstance.post('/bulk-uploads/wilayah', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },
  getById: (id) => axiosInstance.get(`/bulk-uploads/${id}`),
  getRows: (id, params) => axiosInstance.get(`/bulk-uploads/${id}/rows`, { params }),
};
