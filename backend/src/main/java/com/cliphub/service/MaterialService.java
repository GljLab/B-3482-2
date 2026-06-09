package com.cliphub.service;

import com.cliphub.dto.MaterialSearchRequest;
import com.cliphub.dto.MaterialUpdateRequest;
import com.cliphub.dto.MaterialUploadMetaRequest;
import com.cliphub.dto.ShareRequest;
import com.cliphub.entity.Material;
import com.cliphub.security.UserPrincipal;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface MaterialService {

    Material upload(UserPrincipal principal, MaterialUploadMetaRequest request, MultipartFile file);

    Material update(UserPrincipal principal, Long materialId, MaterialUpdateRequest request);

    void delete(UserPrincipal principal, Long materialId);

    List<Map<String, Object>> search(UserPrincipal principal, MaterialSearchRequest request);

    Map<String, Object> detail(UserPrincipal principal, Long materialId);

    Map<String, Object> toggleFavorite(UserPrincipal principal, Long materialId);

    Map<String, Object> createShare(UserPrincipal principal, Long materialId, ShareRequest request);

    Map<String, Object> getByShareCode(String code);

    List<Map<String, Object>> listFavorites(UserPrincipal principal);

    String resolveDownloadPath(UserPrincipal principal, Long materialId, String quality, String format);

    String resolvePreviewPath(UserPrincipal principal, Long materialId);
}
