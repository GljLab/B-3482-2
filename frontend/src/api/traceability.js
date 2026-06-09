import http from './http';

const traceApi = {
  getMaterialTimeline(materialId) {
    return http.get(`/traceability/materials/${materialId}/timeline`);
  },
  getMaterialRefStats(materialId) {
    return http.get(`/traceability/materials/${materialId}/ref-stats`);
  },
  getMaterialProjects(materialId) {
    return http.get(`/traceability/materials/${materialId}/projects`);
  },
  getMaterialCollections(materialId) {
    return http.get(`/traceability/materials/${materialId}/collections`);
  },
  getMaterialModHistory(materialId) {
    return http.get(`/traceability/materials/${materialId}/mod-history`);
  },
  getMaterialTransferHistory(materialId) {
    return http.get(`/traceability/materials/${materialId}/transfer-history`);
  },
  setMaterialSource(materialId, data) {
    return http.put(`/traceability/materials/${materialId}/source-info`, data);
  },
  getDeleteImpact(materialId) {
    return http.get(`/traceability/materials/${materialId}/delete-impact`);
  },
  deleteWithStrategy(materialId, data) {
    return http.delete(`/traceability/materials/${materialId}/with-strategy`, { data });
  },
  listRecycleBin() {
    return http.get('/traceability/recycle-bin');
  },
  restoreFromRecycle(materialId) {
    return http.post(`/traceability/recycle-bin/${materialId}/restore`);
  },
  permanentDelete(materialId) {
    return http.delete(`/traceability/recycle-bin/${materialId}/permanent`);
  },
  getProjectInventory(projectId) {
    return http.get(`/traceability/projects/${projectId}/material-inventory`);
  },
  getProjectContributors(projectId) {
    return http.get(`/traceability/projects/${projectId}/contributors`);
  },
  getProjectAvailability(projectId) {
    return http.get(`/traceability/projects/${projectId}/availability-check`);
  },
  exportInventory(projectId, format) {
    return http.get(`/traceability/projects/${projectId}/inventory-export`, {
      params: { format }
    });
  },
  checkVersionMaterials(projectId, versionId) {
    return http.get(`/traceability/projects/${projectId}/versions/${versionId}/material-check`);
  },
  recommendMaterials(projectId, limit = 10) {
    return http.get(`/traceability/projects/${projectId}/recommend-materials`, {
      params: { limit }
    });
  },
  copyProject(sourceProjectId, data) {
    return http.post(`/traceability/projects/${sourceProjectId}/copy`, data);
  },
  getHotMaterials(dimension, limit = 10) {
    return http.get('/traceability/rankings/hot-materials', {
      params: { dimension, limit }
    });
  },
  getHighReuseMaterials(threshold = 2) {
    return http.get('/traceability/rankings/high-reuse', {
      params: { threshold }
    });
  },
  getUnusedMaterials() {
    return http.get('/traceability/materials/unused');
  },
  getTeamContribution() {
    return http.get('/traceability/team/contribution-stats');
  },
  transferOwnership(data) {
    return http.post('/traceability/materials/transfer-ownership', data);
  },
  adminCleanup(data) {
    return http.post('/traceability/admin/cleanup-unused', data);
  },
  exportProjectInventoryUrl(projectId, format = 'csv') {
    const base = import.meta.env.VITE_API_BASE_URL || '/api';
    const token = localStorage.getItem('clip_hub_token') || '';
    return `${base}/traceability/projects/${projectId}/inventory-export?format=${format}${token ? '&token=' + encodeURIComponent(token) : ''}`;
  }
};

export default traceApi;
