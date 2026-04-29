import axios from 'axios';

const api = axios.create({
  baseURL: '/api',
  timeout: 900000, // 15 minutes
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// Auth
export const login = (data) => api.post('/auth/login', data);
export const register = (data) => api.post('/auth/register', data);

// User
export const getProfile = () => api.get('/user/profile');
export const updateSettings = (data) => api.put('/user/settings', data);

// Resumes
export const getResumes = () => api.get('/resumes');
export const uploadResume = (formData) =>
  api.post('/resumes/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
export const deleteResume = (id) => api.delete(`/resumes/${id}`);
export const downloadResume = (id) =>
  api.get(`/resumes/${id}/download`, { responseType: 'blob' });

// Jobs
export const scanJobs = (data) => api.post('/jobs/scan', data);
export const getAppliedJobs = () => api.get('/jobs/applied');
export const getStats = () => api.get('/jobs/stats');
export const getPortals = () => api.get('/jobs/portals');
export const quickApply = (data) => api.post('/jobs/quick-apply', data);

export default api;
