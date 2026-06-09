package com.cliphub.service;

import com.cliphub.dto.*;
import com.cliphub.security.UserPrincipal;

import java.util.List;
import java.util.Map;

public interface CollectionService {

    Map<String, Object> create(UserPrincipal principal, CollectionCreateRequest request);

    Map<String, Object> update(UserPrincipal principal, Long collectionId, CollectionUpdateRequest request);

    void delete(UserPrincipal principal, Long collectionId, boolean removeFavorites);

    List<Map<String, Object>> list(UserPrincipal principal, String keyword, String sortBy, String visibility);

    Map<String, Object> detail(UserPrincipal principal, Long collectionId);

    Map<String, Object> addMaterial(UserPrincipal principal, Long collectionId, Long materialId, String note);

    void removeMaterial(UserPrincipal principal, Long collectionId, Long materialId);

    Map<String, Object> updateMaterialNote(UserPrincipal principal, Long collectionId, Long materialId, String note);

    void sortMaterials(UserPrincipal principal, Long collectionId, MaterialSortRequest request);

    Map<String, Object> setMaterialCollections(UserPrincipal principal, Long materialId, List<Long> collectionIds);

    List<Map<String, Object>> getMaterialCollections(UserPrincipal principal, Long materialId);

    Map<String, Object> createShare(UserPrincipal principal, Long collectionId, CollectionShareRequest request);

    Map<String, Object> getByShareCode(String code, String password);

    void revokeShare(UserPrincipal principal, Long shareId);

    List<Map<String, Object>> listShares(UserPrincipal principal, Long collectionId);

    void sortCollections(UserPrincipal principal, CollectionSortRequest request);

    void ensureDefaultCollection(UserPrincipal principal);

    List<Map<String, Object>> collectionStats(UserPrincipal principal);
}
