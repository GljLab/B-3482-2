package com.cliphub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cliphub.common.BusinessException;
import com.cliphub.dto.MaterialSearchRequest;
import com.cliphub.dto.MaterialUpdateRequest;
import com.cliphub.dto.MaterialUploadMetaRequest;
import com.cliphub.dto.ShareRequest;
import com.cliphub.entity.*;
import com.cliphub.entity.Collection;
import com.cliphub.mapper.*;
import com.cliphub.security.UserPrincipal;
import com.cliphub.service.AuditLogService;
import com.cliphub.service.MaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MaterialServiceImpl implements MaterialService {

    private final MaterialMapper materialMapper;
    private final MaterialTagRelMapper materialTagRelMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final FavoriteMapper favoriteMapper;
    private final ShareLinkMapper shareLinkMapper;
    private final UserMapper userMapper;
    private final AuditLogService auditLogService;
    private final CollectionMaterialRelMapper collectionMaterialRelMapper;
    private final CollectionMapper collectionMapper;
    @Lazy
    private final TraceabilityServiceImpl traceabilitySvc;

    @Value("${app.storage.root}")
    private String storageRoot;

    @Override
    @Transactional
    public Material upload(UserPrincipal principal, MaterialUploadMetaRequest request, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }

        Path root = Paths.get(storageRoot, "uploads");
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "创建存储目录失败");
        }

        String extension = extensionOf(file.getOriginalFilename());
        String relativePath = DateTimeFormatter.ofPattern("yyyyMM/dd").format(LocalDateTime.now()) + "/"
                + UUID.randomUUID().toString().replace("-", "") + (extension.isEmpty() ? "" : "." + extension);
        Path destination = root.resolve(relativePath);
        try {
            Files.createDirectories(destination.getParent());
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "文件保存失败");
        }

        Material material = new Material();
        material.setTitle(request.getTitle());
        material.setDescription(request.getDescription());
        material.setType(request.getType().toUpperCase(Locale.ROOT));
        material.setCategoryId(request.getCategoryId());
        material.setOwnerId(principal.getId());
        material.setVisibility(request.getVisibility().toUpperCase(Locale.ROOT));
        material.setFileName(file.getOriginalFilename());
        material.setStoragePath(destination.toString());
        material.setPreviewPath(destination.toString());
        material.setMimeType(file.getContentType());
        material.setFormat(extension);
        material.setSizeBytes(file.getSize());
        material.setDurationSeconds(request.getDurationSeconds());
        material.setResolution(request.getResolution());
        material.setDownloadCount(0L);
        material.setFavoriteCount(0L);
        material.setShareCount(0L);
        material.setCreatedAt(LocalDateTime.now());
        material.setUpdatedAt(LocalDateTime.now());
        materialMapper.insert(material);

        material.setSourceType("USER_UPLOAD");
        material.setOwnershipType("PERSONAL");
        material.setIsDeleted(0);
        material.setProjectUsageCount(0L);
        material.setCollectionUsageCount(0L);
        materialMapper.updateById(material);

        resetTags(material.getId(), request.getTagIds());

        auditLogService.log(principal, "UPLOAD_MATERIAL", "MATERIAL", String.valueOf(material.getId()),
                "上传素材: " + material.getTitle());
        return material;
    }

    @Override
    @Transactional
    public Material update(UserPrincipal principal, Long materialId, MaterialUpdateRequest request) {
        Material material = mustGet(materialId);
        ensureEditable(principal, material);

        String oldTitle = material.getTitle();
        String oldDescription = material.getDescription();
        String oldVisibility = material.getVisibility();
        Long oldCategoryId = material.getCategoryId();
        Integer oldDurationSeconds = material.getDurationSeconds();
        String oldResolution = material.getResolution();

        if (StringUtils.hasText(request.getTitle())) {
            material.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            material.setDescription(request.getDescription());
        }
        if (StringUtils.hasText(request.getVisibility())) {
            material.setVisibility(request.getVisibility().toUpperCase(Locale.ROOT));
        }
        if (request.getCategoryId() != null) {
            material.setCategoryId(request.getCategoryId());
        }
        if (request.getDurationSeconds() != null) {
            material.setDurationSeconds(request.getDurationSeconds());
        }
        if (request.getResolution() != null) {
            material.setResolution(request.getResolution());
        }
        material.setUpdatedAt(LocalDateTime.now());

        traceabilitySvc.logModification(material.getId(), principal, "title",
                oldTitle != null ? oldTitle : "", material.getTitle() != null ? material.getTitle() : "");
        traceabilitySvc.logModification(material.getId(), principal, "description",
                oldDescription != null ? oldDescription : "", material.getDescription() != null ? material.getDescription() : "");
        traceabilitySvc.logModification(material.getId(), principal, "visibility",
                oldVisibility != null ? oldVisibility : "", material.getVisibility() != null ? material.getVisibility() : "");
        traceabilitySvc.logModification(material.getId(), principal, "categoryId",
                oldCategoryId != null ? String.valueOf(oldCategoryId) : "", material.getCategoryId() != null ? String.valueOf(material.getCategoryId()) : "");
        traceabilitySvc.logModification(material.getId(), principal, "durationSeconds",
                oldDurationSeconds != null ? String.valueOf(oldDurationSeconds) : "", material.getDurationSeconds() != null ? String.valueOf(material.getDurationSeconds()) : "");
        traceabilitySvc.logModification(material.getId(), principal, "resolution",
                oldResolution != null ? oldResolution : "", material.getResolution() != null ? material.getResolution() : "");

        materialMapper.updateById(material);
        if (request.getTagIds() != null) {
            resetTags(materialId, request.getTagIds());
        }

        auditLogService.log(principal, "UPDATE_MATERIAL", "MATERIAL", String.valueOf(materialId), "更新素材信息");
        return material;
    }

    @Override
    @Transactional
    public void delete(UserPrincipal principal, Long materialId) {
        Material material = mustGet(materialId);
        ensureEditable(principal, material);

        materialMapper.deleteById(materialId);
        materialTagRelMapper.delete(new LambdaQueryWrapper<MaterialTagRel>().eq(MaterialTagRel::getMaterialId, materialId));
        favoriteMapper.delete(new LambdaQueryWrapper<Favorite>().eq(Favorite::getMaterialId, materialId));
        shareLinkMapper.delete(new LambdaQueryWrapper<ShareLink>().eq(ShareLink::getMaterialId, materialId));

        if (material.getStoragePath() != null) {
            try {
                Files.deleteIfExists(Path.of(material.getStoragePath()));
            } catch (IOException ignored) {
                // 文件删除失败不影响业务主流程
            }
        }

        auditLogService.log(principal, "DELETE_MATERIAL", "MATERIAL", String.valueOf(materialId), "删除素材");
    }

    @Override
    public List<Map<String, Object>> search(UserPrincipal principal, MaterialSearchRequest request) {
        LambdaQueryWrapper<Material> wrapper = new LambdaQueryWrapper<Material>()
                .orderByDesc(Material::getCreatedAt);

        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.and(w -> w.like(Material::getTitle, request.getKeyword())
                    .or().like(Material::getDescription, request.getKeyword()));
        }
        if (request.getCategoryId() != null) {
            wrapper.eq(Material::getCategoryId, request.getCategoryId());
        }
        if (StringUtils.hasText(request.getType())) {
            wrapper.eq(Material::getType, request.getType().toUpperCase(Locale.ROOT));
        }
        if (request.getStartAt() != null) {
            wrapper.ge(Material::getCreatedAt, request.getStartAt());
        }
        if (request.getEndAt() != null) {
            wrapper.le(Material::getCreatedAt, request.getEndAt());
        }

        final Set<Long> tagMaterialIds;
        if (request.getTagId() != null) {
            List<MaterialTagRel> rels = materialTagRelMapper.selectList(new LambdaQueryWrapper<MaterialTagRel>()
                    .eq(MaterialTagRel::getTagId, request.getTagId()));
            tagMaterialIds = rels.stream().map(MaterialTagRel::getMaterialId).collect(Collectors.toSet());
        } else {
            tagMaterialIds = null;
        }

        final Set<Long> collectionMaterialIds;
        if (request.getCollectionId() != null) {
            List<CollectionMaterialRel> cRels = collectionMaterialRelMapper.selectList(
                    new LambdaQueryWrapper<CollectionMaterialRel>()
                            .eq(CollectionMaterialRel::getCollectionId, request.getCollectionId()));
            collectionMaterialIds = cRels.stream().map(CollectionMaterialRel::getMaterialId).collect(Collectors.toSet());
        } else if (Boolean.TRUE.equals(request.getInMyCollections())) {
            List<Collection> userCollections = collectionMapper.selectList(
                    new LambdaQueryWrapper<Collection>().eq(Collection::getOwnerId, principal.getId()));
            Set<Long> allCollectionIds = userCollections.stream()
                    .map(Collection::getId).collect(Collectors.toSet());
            if (allCollectionIds.isEmpty()) {
                return List.of();
            }
            List<CollectionMaterialRel> cRels = collectionMaterialRelMapper.selectList(
                    new LambdaQueryWrapper<CollectionMaterialRel>()
                            .in(CollectionMaterialRel::getCollectionId, allCollectionIds));
            collectionMaterialIds = cRels.stream().map(CollectionMaterialRel::getMaterialId).collect(Collectors.toSet());
        } else {
            collectionMaterialIds = null;
        }

        List<Material> raw = materialMapper.selectList(wrapper);
        List<Material> filtered = raw.stream()
                .filter(material -> canAccess(principal, material))
                .filter(material -> tagMaterialIds == null || tagMaterialIds.contains(material.getId()))
                .filter(material -> collectionMaterialIds == null || collectionMaterialIds.contains(material.getId()))
                .toList();

        int page = Math.max(1, request.getPage());
        int size = Math.max(1, request.getSize());
        int from = (page - 1) * size;
        if (from >= filtered.size()) {
            return List.of();
        }
        int to = Math.min(filtered.size(), from + size);

        return filtered.subList(from, to).stream().map(this::materialToMap).toList();
    }

    @Override
    public Map<String, Object> detail(UserPrincipal principal, Long materialId) {
        Material material = mustGet(materialId);
        if (!canAccess(principal, material)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "素材无访问权限");
        }
        return materialToMap(material);
    }

    @Override
    @Transactional
    public Map<String, Object> toggleFavorite(UserPrincipal principal, Long materialId) {
        Material material = mustGet(materialId);
        if (!canAccess(principal, material)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "素材无访问权限");
        }

        Favorite existed = favoriteMapper.selectOne(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, principal.getId())
                .eq(Favorite::getMaterialId, materialId)
                .last("LIMIT 1"));

        boolean favorited;
        if (existed == null) {
            Favorite favorite = new Favorite();
            favorite.setUserId(principal.getId());
            favorite.setMaterialId(materialId);
            favorite.setCreatedAt(LocalDateTime.now());
            favoriteMapper.insert(favorite);
            material.setFavoriteCount(Optional.ofNullable(material.getFavoriteCount()).orElse(0L) + 1);
            material.setUpdatedAt(LocalDateTime.now());
            materialMapper.updateById(material);
            favorited = true;
            traceabilitySvc.logTrail(materialId, principal, "FAVORITE", "INTERACTION", null, null, "收藏素材", null, null);
        } else {
            favoriteMapper.deleteById(existed.getId());
            long current = Optional.ofNullable(material.getFavoriteCount()).orElse(0L);
            material.setFavoriteCount(Math.max(0, current - 1));
            material.setUpdatedAt(LocalDateTime.now());
            materialMapper.updateById(material);
            favorited = false;
            traceabilitySvc.logTrail(materialId, principal, "UNFAVORITE", "INTERACTION", null, null, "取消收藏", null, null);
        }

        auditLogService.log(principal, favorited ? "FAVORITE_MATERIAL" : "UNFAVORITE_MATERIAL",
                "MATERIAL", String.valueOf(materialId), "收藏状态变更");
        return Map.of("favorited", favorited);
    }

    @Override
    @Transactional
    public Map<String, Object> createShare(UserPrincipal principal, Long materialId, ShareRequest request) {
        Material material = mustGet(materialId);
        if (!canAccess(principal, material)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "素材无访问权限");
        }

        String code = UUID.randomUUID().toString().substring(0, 8);
        ShareLink shareLink = new ShareLink();
        shareLink.setMaterialId(materialId);
        shareLink.setSharedBy(principal.getId());
        shareLink.setShareCode(code);
        shareLink.setExpireAt(LocalDateTime.now().plusHours(Math.max(1, request.getExpireHours())));
        shareLink.setCreatedAt(LocalDateTime.now());
        shareLinkMapper.insert(shareLink);

        traceabilitySvc.logTrail(materialId, principal, "SHARE", "SHARING", null, null, "创建分享链接", null, null);

        material.setShareCount(Optional.ofNullable(material.getShareCount()).orElse(0L) + 1);
        material.setUpdatedAt(LocalDateTime.now());
        materialMapper.updateById(material);

        auditLogService.log(principal, "SHARE_MATERIAL", "MATERIAL", String.valueOf(materialId), "创建分享链接");
        return Map.of(
                "shareCode", code,
                "shareUrl", "/api/materials/share/" + code,
                "expireAt", shareLink.getExpireAt()
        );
    }

    @Override
    public Map<String, Object> getByShareCode(String code) {
        ShareLink link = shareLinkMapper.selectOne(new LambdaQueryWrapper<ShareLink>()
                .eq(ShareLink::getShareCode, code)
                .last("LIMIT 1"));
        if (link == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "分享链接不存在");
        }
        if (link.getExpireAt() != null && link.getExpireAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(HttpStatus.GONE, "分享链接已过期");
        }

        Material material = mustGet(link.getMaterialId());
        return materialToMap(material);
    }

    @Override
    public List<Map<String, Object>> listFavorites(UserPrincipal principal) {
        List<Favorite> favorites = favoriteMapper.selectList(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, principal.getId())
                .orderByDesc(Favorite::getCreatedAt));
        if (favorites.isEmpty()) {
            return List.of();
        }

        Map<Long, Material> materialMap = materialMapper.selectBatchIds(
                        favorites.stream().map(Favorite::getMaterialId).toList())
                .stream()
                .collect(Collectors.toMap(Material::getId, material -> material));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Favorite favorite : favorites) {
            Material material = materialMap.get(favorite.getMaterialId());
            if (material != null && canAccess(principal, material)) {
                result.add(materialToMap(material));
            }
        }
        return result;
    }

    @Override
    @Transactional
    public String resolveDownloadPath(UserPrincipal principal, Long materialId, String quality, String format) {
        Material material = mustGet(materialId);
        if (!canAccess(principal, material)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "素材无访问权限");
        }

        material.setDownloadCount(Optional.ofNullable(material.getDownloadCount()).orElse(0L) + 1);
        material.setUpdatedAt(LocalDateTime.now());
        materialMapper.updateById(material);

        traceabilitySvc.logTrail(materialId, principal, "DOWNLOAD", "LIFECYCLE", null, null, "下载素材", null, null);

        auditLogService.log(principal, "DOWNLOAD_MATERIAL", "MATERIAL", String.valueOf(materialId),
                "下载素材 quality=" + quality + ", format=" + format);
        return material.getStoragePath();
    }

    @Override
    public String resolvePreviewPath(UserPrincipal principal, Long materialId) {
        Material material = mustGet(materialId);
        if (!canAccess(principal, material)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "素材无访问权限");
        }
        return material.getPreviewPath();
    }

    private Material mustGet(Long materialId) {
        Material material = materialMapper.selectById(materialId);
        if (material == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "素材不存在");
        }
        return material;
    }

    private void ensureEditable(UserPrincipal principal, Material material) {
        if ("ADMIN".equals(principal.getRole()) || Objects.equals(principal.getId(), material.getOwnerId())) {
            return;
        }
        throw new BusinessException(HttpStatus.FORBIDDEN, "仅素材所有者或管理员可操作");
    }

    private boolean canAccess(UserPrincipal principal, Material material) {
        if ("ADMIN".equals(principal.getRole())) {
            return true;
        }
        if (Objects.equals(material.getOwnerId(), principal.getId())) {
            return true;
        }
        if ("PUBLIC".equalsIgnoreCase(material.getVisibility())) {
            return true;
        }
        if ("TEAM".equalsIgnoreCase(material.getVisibility()) && principal.getTeamId() != null) {
            User owner = userMapper.selectById(material.getOwnerId());
            return owner != null && Objects.equals(owner.getTeamId(), principal.getTeamId());
        }
        return false;
    }

    private void resetTags(Long materialId, List<Long> tagIds) {
        materialTagRelMapper.delete(new LambdaQueryWrapper<MaterialTagRel>().eq(MaterialTagRel::getMaterialId, materialId));
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        for (Long tagId : tagIds) {
            if (tagMapper.selectById(tagId) == null) {
                continue;
            }
            MaterialTagRel rel = new MaterialTagRel();
            rel.setMaterialId(materialId);
            rel.setTagId(tagId);
            materialTagRelMapper.insert(rel);
        }
    }

    private Map<String, Object> materialToMap(Material material) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", material.getId());
        result.put("title", material.getTitle());
        result.put("description", material.getDescription());
        result.put("type", material.getType());
        result.put("visibility", material.getVisibility());
        result.put("format", material.getFormat());
        result.put("sizeBytes", material.getSizeBytes());
        result.put("durationSeconds", material.getDurationSeconds());
        result.put("resolution", material.getResolution());
        result.put("downloadCount", material.getDownloadCount());
        result.put("favoriteCount", material.getFavoriteCount());
        result.put("shareCount", material.getShareCount());
        result.put("createdAt", material.getCreatedAt());

        Category category = material.getCategoryId() == null ? null : categoryMapper.selectById(material.getCategoryId());
        result.put("category", category == null ? null : Map.of("id", category.getId(), "name", category.getName()));

        List<Long> tagIds = materialTagRelMapper.selectList(new LambdaQueryWrapper<MaterialTagRel>()
                        .eq(MaterialTagRel::getMaterialId, material.getId()))
                .stream().map(MaterialTagRel::getTagId).toList();
        if (tagIds.isEmpty()) {
            result.put("tags", List.of());
        } else {
            result.put("tags", tagMapper.selectBatchIds(tagIds)
                    .stream()
                    .map(tag -> Map.of("id", tag.getId(), "name", tag.getName()))
                    .toList());
        }

        result.put("previewUrl", "/api/materials/" + material.getId() + "/preview");
        return result;
    }

    private String extensionOf(String filename) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
