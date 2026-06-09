package com.cliphub.service;

import com.cliphub.dto.CleanupUnusedRequest;
import com.cliphub.dto.CopyProjectRequest;
import com.cliphub.dto.DeleteImpactRequest;
import com.cliphub.dto.MaterialSourceRequest;
import com.cliphub.dto.MaterialTransferRequest;
import com.cliphub.security.UserPrincipal;

import java.util.List;
import java.util.Map;

public interface TraceabilityService {

    Map<String, Object> getMaterialUsageTimeline(UserPrincipal principal, Long materialId);

    Map<String, Object> getMaterialReferenceStats(UserPrincipal principal, Long materialId);

    List<Map<String, Object>> getMaterialProjects(UserPrincipal principal, Long materialId);

    List<Map<String, Object>> getMaterialCollections(UserPrincipal principal, Long materialId);

    List<Map<String, Object>> getMaterialModificationHistory(UserPrincipal principal, Long materialId);

    Map<String, Object> getProjectMaterialInventory(UserPrincipal principal, Long projectId);

    List<Map<String, Object>> getProjectMaterialContributors(UserPrincipal principal, Long projectId);

    List<Map<String, Object>> checkProjectMaterialAvailability(UserPrincipal principal, Long projectId);

    Map<String, Object> exportProjectMaterialInventory(UserPrincipal principal, Long projectId, String format);

    List<Map<String, Object>> getHotMaterialsRanking(UserPrincipal principal, String dimension, int limit);

    List<Map<String, Object>> getUnusedMaterials(UserPrincipal principal);

    List<Map<String, Object>> getHighReuseMaterials(UserPrincipal principal, int threshold);

    List<Map<String, Object>> getTeamContributionStats(UserPrincipal principal);

    List<Map<String, Object>> recommendMaterialsForProject(UserPrincipal principal, Long projectId, int limit);

    Map<String, Object> assessDeleteImpact(UserPrincipal principal, Long materialId);

    Map<String, Object> deleteMaterialWithStrategy(UserPrincipal principal, Long materialId, DeleteImpactRequest request);

    List<Map<String, Object>> listRecycleBin(UserPrincipal principal);

    Map<String, Object> restoreFromRecycleBin(UserPrincipal principal, Long materialId);

    Map<String, Object> permanentlyDeleteFromRecycleBin(UserPrincipal principal, Long materialId);

    Map<String, Object> setMaterialSourceInfo(UserPrincipal principal, Long materialId, MaterialSourceRequest request);

    Map<String, Object> transferMaterialsOwnership(UserPrincipal principal, MaterialTransferRequest request);

    List<Map<String, Object>> getTransferHistory(UserPrincipal principal, Long materialId);

    void saveVersionMaterialSnapshot(Long versionId, Long projectId);

    Map<String, Object> checkVersionMaterialsAvailability(UserPrincipal principal, Long projectId, Long versionId);

    Map<String, Object> copyProject(UserPrincipal principal, Long sourceProjectId, CopyProjectRequest request);

    Map<String, Object> adminCleanupUnusedMaterials(UserPrincipal principal, CleanupUnusedRequest request);
}
