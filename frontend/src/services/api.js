import axios from 'axios';

const API_URL = '/api';

const api = axios.create({
  baseURL: API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Intercepteur pour ajouter le token JWT
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Intercepteur pour gerer les erreurs 401
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// Auth API
export const authAPI = {
  login: (credentials) => api.post('/auth/login', credentials),
  register: (data) => api.post('/auth/register', data),
  getCurrentUser: () => api.get('/auth/me'),
};

// Servers API
export const serversAPI = {
  getAll: (params) => api.get('/servers', { params }),
  getById: (id) => api.get(`/servers/${id}`),
  search: (query, params) => api.get('/servers/search', { params: { q: query, ...params } }),
  create: (data) => api.post('/servers', data),
  update: (id, data) => api.put(`/servers/${id}`, data),
  delete: (id) => api.delete(`/servers/${id}`),
  getEnvironments: () => api.get('/servers/environments'),
  getSites: () => api.get('/servers/sites'),
  getTypes: () => api.get('/servers/types'),
};

// Dashboard API
export const dashboardAPI = {
  getData: () => api.get('/dashboard'),
  getMapData: () => api.get('/dashboard/map'),
};

// Import API
export const importAPI = {
  uploadFile: (file) => {
    const formData = new FormData();
    formData.append('file', file);
    return api.post('/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },
  getHistory: (params) => api.get('/import/history', { params }),
};

// Export API
export const exportAPI = {
  exportCsv: (params) => api.get('/export/csv', { params, responseType: 'blob' }),
  exportExcel: (params) => api.get('/export/excel', { params, responseType: 'blob' }),
};

// Server Software API (logiciels installes sur les serveurs)
export const serverSoftwareAPI = {
  getByServer: (serverId) => api.get(`/server-software/server/${serverId}`),
  getById: (id) => api.get(`/server-software/${id}`),
  create: (data) => api.post('/server-software', data),
  update: (id, data) => api.put(`/server-software/${id}`, data),
  delete: (id) => api.delete(`/server-software/${id}`),
  getNames: () => api.get('/server-software/names'),
  getNeedingUpdate: () => api.get('/server-software/needs-update'),
  countUpdates: (serverId) => api.get(`/server-software/server/${serverId}/updates-count`),
};

// Software Versions API
export const softwareVersionsAPI = {
  getAll: (params) => api.get('/software-versions', { params }),
  getById: (id) => api.get(`/software-versions/${id}`),
  getBySoftware: (softwareName) => api.get(`/software-versions/software/${softwareName}`),
  getSoftwareNames: () => api.get('/software-versions/software-names'),
  getStatuses: () => api.get('/software-versions/statuses'),
  getStats: () => api.get('/software-versions/stats'),
  create: (data) => api.post('/software-versions', data),
  update: (id, data) => api.put(`/software-versions/${id}`, data),
  delete: (id) => api.delete(`/software-versions/${id}`),
  importCsv: (file) => {
    const formData = new FormData();
    formData.append('file', file);
    return api.post('/software-versions/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },
};

// Server Obsolescence API (obsolescence par serveur - PMT DISCOVR)
export const serverObsolescenceAPI = {
  getAll: () => api.get('/server-obsolescence'),
  filter: (params) => api.get('/server-obsolescence/filter', { params }),
  getGrouped: () => api.get('/server-obsolescence/grouped'),
  getStats: () => api.get('/server-obsolescence/stats'),
  getEnvironments: () => api.get('/server-obsolescence/environments'),
  getServers: () => api.get('/server-obsolescence/servers'),
  getSoftwareNames: () => api.get('/server-obsolescence/software-names'),
  getStatuses: () => api.get('/server-obsolescence/statuses'),
  importCsv: (file) => {
    const formData = new FormData();
    formData.append('file', file);
    return api.post('/server-obsolescence/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },
  clearAll: () => api.delete('/server-obsolescence/clear-all'),
};

// Alerts API (alertes de conformite)
export const alertsAPI = {
  getAll: () => api.get('/alerts'),
  getActive: () => api.get('/alerts/active'),
  filter: (params) => api.get('/alerts/filter', { params }),
  getStats: () => api.get('/alerts/stats'),
  getTypes: () => api.get('/alerts/types'),
  getSeverities: () => api.get('/alerts/severities'),
  getStatuses: () => api.get('/alerts/statuses'),
  generateAlerts: () => api.post('/alerts/generate'),
  acknowledge: (id, username) => api.put(`/alerts/${id}/acknowledge`, null, { params: { username } }),
  resolve: (id, username) => api.put(`/alerts/${id}/resolve`, null, { params: { username } }),
  ignore: (id) => api.put(`/alerts/${id}/ignore`),
  clearResolved: () => api.delete('/alerts/clear-resolved'),
  clearAll: () => api.delete('/alerts/clear-all'),
};

// Migrations API (planning des migrations)
export const migrationsAPI = {
  getAll: (params) => api.get('/migrations', { params }),
  getById: (id) => api.get(`/migrations/${id}`),
  getActive: () => api.get('/migrations/active'),
  getByStatus: (status) => api.get(`/migrations/status/${status}`),
  create: (data) => api.post('/migrations', data),
  update: (id, data) => api.put(`/migrations/${id}`, data),
  delete: (id) => api.delete(`/migrations/${id}`),
  getPhases: () => api.get('/migrations/phases'),
  getResponsables: () => api.get('/migrations/responsables'),
  getStatuses: () => api.get('/migrations/statuses'),
  getStats: () => api.get('/migrations/stats'),
  importCsv: (file) => {
    const formData = new FormData();
    formData.append('file', file);
    return api.post('/migrations/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },
};

// Compliance API (rapprochement versions installees / referentiel CSR)
export const complianceAPI = {
  getAll: () => api.get('/compliance'),
  getStats: () => api.get('/compliance/stats'),
};

export default api;
