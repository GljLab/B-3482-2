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
  getRecycleBin() {
    return this.listRecycleBin();
  },
  restoreFromRecycle(materialId) {
    return http.post(`/traceability/recycle-bin/${materialId}/restore`);
  },
  restoreFromRecycleBin(materialId) {
    return this.restoreFromRecycle(materialId);
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
  exportProjectInventoryUrl(projectId, format = 'csv') {
    const base = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, '');
    const token = localStorage.getItem('clip_hub_token') || '';
    return `${base}/traceability/projects/${projectId}/inventory-export?format=${format}${token ? '&token=' + encodeURIComponent(token) : ''}`;
  },
  exportProjectInventory(projectId, format) {
    return this.exportProjectInventoryUrl(projectId, format);
  },

  checkVersionMaterials(projectId, versionId) {
    return http.get(`/traceability/projects/${projectId}/versions/${versionId}/material-check`);
  },
  rollbackCheck(projectId, versionId) {
    if (typeof projectId === 'object') {
      const opts = projectId;
      return this.checkVersionMaterials(opts.projectId || opts.id, opts.versionId);
    }
    return this.checkVersionMaterials(projectId, versionId || 0);
  },

  recommendMaterials(projectId, limit) {
    let actualLimit = 10;
    if (typeof limit === 'object' && limit !== null) {
      actualLimit = limit.limit || 10;
    } else if (typeof limit === 'number') {
      actualLimit = limit;
    }
    return http.get(`/traceability/projects/${projectId}/recommend-materials`, {
      params: { limit: actualLimit }
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
  cleanupUnused(materialIds) {
    const payload = {};
    if (Array.isArray(materialIds)) {
      payload.materialIds = materialIds;
    } else if (materialIds && typeof materialIds === 'object') {
      Object.assign(payload, materialIds);
    }
    return this.adminCleanup({ ...payload, dryRun: false });
  },
  simulateCleanupUnused(materialIds) {
    const payload = {};
    if (Array.isArray(materialIds)) {
      payload.materialIds = materialIds;
    } else if (materialIds && typeof materialIds === 'object') {
      Object.assign(payload, materialIds);
    }
    return this.adminCleanup({ ...payload, dryRun: true });
  }
};

export default traceApi;
