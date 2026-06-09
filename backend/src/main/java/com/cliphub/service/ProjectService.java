package com.cliphub.service;

import com.cliphub.dto.CollaboratorRequest;
import com.cliphub.dto.ProjectCreateRequest;
import com.cliphub.dto.SaveVersionRequest;
import com.cliphub.security.UserPrincipal;

import java.util.List;
import java.util.Map;

public interface ProjectService {

    Map<String, Object> createProject(UserPrincipal principal, ProjectCreateRequest request);

    List<Map<String, Object>> listProjects(UserPrincipal principal);

    Map<String, Object> saveVersion(UserPrincipal principal, Long projectId, SaveVersionRequest request);

    Map<String, Object> rollback(UserPrincipal principal, Long projectId, Long versionId);

    Map<String, Object> exportProject(UserPrincipal principal, Long projectId, String format);

    Map<String, Object> addCollaborator(UserPrincipal principal, Long projectId, CollaboratorRequest request);

    void removeCollaborator(UserPrincipal principal, Long projectId, Long userId);

    List<Map<String, Object>> listVersions(UserPrincipal principal, Long projectId);

    Map<String, Object> bindMaterial(UserPrincipal principal, Long projectId, Long materialId);

    Map<String, Object> importFromCollection(UserPrincipal principal, Long projectId, Long collectionId);
}
