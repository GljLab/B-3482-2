package com.cliphub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cliphub.common.BusinessException;
import com.cliphub.dto.*;
import com.cliphub.entity.*;
import com.cliphub.entity.Collection;
import com.cliphub.mapper.*;
import com.cliphub.security.UserPrincipal;
import com.cliphub.service.AuditLogService;
import com.cliphub.service.CollectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CollectionServiceImpl implements CollectionService {

    private final CollectionMapper collectionMapper;
    private final CollectionMaterialRelMapper collectionMaterialRelMapper;
    private final CollectionShareMapper collectionShareMapper;
    private final MaterialMapper materialMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final MaterialTagRelMapper materialTagRelMapper;
    private final FavoriteMapper favoriteMapper;
    private final AuditLogService auditLogService;
    @Lazy
    private final TraceabilityServiceImpl traceabilitySvc;

    @Override
    @Transactional
    public Map<String, Object> create(UserPrincipal principal, CollectionCreateRequest request) {
        Collection collection = new Collection();
        collection.setName(request.getName());
        collection.setDescription(request.getDescription() == null ? "" : request.getDescription());
        collection.setCoverMaterialId(request.getCoverMaterialId());
        collection.setOwnerId(principal.getId());
        collection.setVisibility(request.getVisibility() == null ? "PRIVATE" : request.getVisibility().toUpperCase(Locale.ROOT));
        collection.setSortOrder(0);
        collection.setIsDefault(0);
        collection.setCreatedAt(LocalDateTime.now());
        collection.setUpdatedAt(LocalDateTime.now());
        collectionMapper.insert(collection);

        auditLogService.log(principal, "CREATE_COLLECTION", "COLLECTION", String.valueOf(collection.getId()),
                "创建素材集: " + collection.getName());
        return collectionToMap(collection);
    }

    @Override
    @Transactional
    public Map<String, Object> update(UserPrincipal principal, Long collectionId, CollectionUpdateRequest request) {
        Collection collection = mustGetCollection(collectionId);
        ensureOwnerOrAdmin(principal, collection);

        if (StringUtils.hasText(request.getName())) {
            collection.setName(request.getName());
        }
        if (request.getDescription() != null) {
            collection.setDescription(request.getDescription());
        }
        if (request.getCoverMaterialId() != null) {
            collection.setCoverMaterialId(request.getCoverMaterialId());
        }
        if (StringUtils.hasText(request.getVisibility())) {
            collection.setVisibility(request.getVisibility().toUpperCase(Locale.ROOT));
        }
        if (request.getSortOrder() != null) {
            collection.setSortOrder(request.getSortOrder());
        }
        collection.setUpdatedAt(LocalDateTime.now());
        collectionMapper.updateById(collection);

        auditLogService.log(principal, "UPDATE_COLLECTION", "COLLECTION", String.valueOf(collectionId), "更新素材集");
        return collectionToMap(collection);
    }

    @Override
    @Transactional
    public void delete(UserPrincipal principal, Long collectionId, boolean removeFavorites) {
        Collection collection = mustGetCollection(collectionId);
        ensureOwnerOrAdmin(principal, collection);

        if (collection.getIsDefault() == 1) {
            throw new BusinessException("默认收藏素材集不可删除");
        }

        List<CollectionMaterialRel> rels = collectionMaterialRelMapper.selectList(
                new LambdaQueryWrapper<CollectionMaterialRel>().eq(CollectionMaterialRel::getCollectionId, collectionId));
        List<Long> materialIds = rels.stream().map(CollectionMaterialRel::getMaterialId).toList();

        collectionMaterialRelMapper.delete(new LambdaQueryWrapper<CollectionMaterialRel>()
                .eq(CollectionMaterialRel::getCollectionId, collectionId));
        collectionShareMapper.delete(new LambdaQueryWrapper<CollectionShare>()
                .eq(CollectionShare::getCollectionId, collectionId));
        collectionMapper.deleteById(collectionId);

        if (removeFavorites) {
            for (Long materialId : materialIds) {
                favoriteMapper.delete(new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, principal.getId())
                        .eq(Favorite::getMaterialId, materialId));
            }
        }

        auditLogService.log(principal, "DELETE_COLLECTION", "COLLECTION", String.valueOf(collectionId),
                "删除素材集: " + collection.getName() + (removeFavorites ? "（同时取消收藏）" : ""));
    }

    @Override
    public List<Map<String, Object>> list(UserPrincipal principal, String keyword, String sortBy, String visibility) {
        LambdaQueryWrapper<Collection> wrapper = new LambdaQueryWrapper<Collection>()
                .eq(Collection::getOwnerId, principal.getId());

        if (StringUtils.hasText(keyword)) {
            wrapper.like(Collection::getName, keyword);
        }
        if (StringUtils.hasText(visibility)) {
            wrapper.eq(Collection::getVisibility, visibility.toUpperCase(Locale.ROOT));
        }

        if ("materialCount".equalsIgnoreCase(sortBy)) {
            wrapper.orderByDesc(Collection::getUpdatedAt);
        } else if ("name".equalsIgnoreCase(sortBy)) {
            wrapper.orderByAsc(Collection::getName);
        } else {
            wrapper.orderByAsc(Collection::getSortOrder).orderByDesc(Collection::getUpdatedAt);
        }

        List<Collection> collections = collectionMapper.selectList(wrapper);
        return collections.stream().map(this::collectionToMap).toList();
    }

    @Override
    public Map<String, Object> detail(UserPrincipal principal, Long collectionId) {
        Collection collection = mustGetCollection(collectionId);
        if (!"ADMIN".equals(principal.getRole()) && !Objects.equals(collection.getOwnerId(), principal.getId())) {
            if (!"PUBLIC".equalsIgnoreCase(collection.getVisibility())) {
                throw new BusinessException(HttpStatus.FORBIDDEN, "无权访问该素材集");
            }
        }

        Map<String, Object> result = collectionToMap(collection);

        List<CollectionMaterialRel> rels = collectionMaterialRelMapper.selectList(
                new LambdaQueryWrapper<CollectionMaterialRel>()
                        .eq(CollectionMaterialRel::getCollectionId, collectionId)
                        .orderByAsc(CollectionMaterialRel::getSortOrder)
                        .orderByDesc(CollectionMaterialRel::getCreatedAt));

        List<Map<String, Object>> materials = new ArrayList<>();
        for (CollectionMaterialRel rel : rels) {
            Material material = materialMapper.selectById(rel.getMaterialId());
            Map<String, Object> materialMap;
            if (material == null) {
                materialMap = new LinkedHashMap<>();
                materialMap.put("id", rel.getMaterialId());
                materialMap.put("title", "已失效素材");
                materialMap.put("invalid", true);
            } else {
                materialMap = materialToMap(material);
            }
            materialMap.put("note", rel.getNote());
            materialMap.put("sortOrder", rel.getSortOrder());
            materialMap.put("addedAt", rel.getCreatedAt());
            materials.add(materialMap);
        }

        result.put("materials", materials);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> addMaterial(UserPrincipal principal, Long collectionId, Long materialId, String note) {
        Collection collection = mustGetCollection(collectionId);
        ensureOwnerOrAdmin(principal, collection);

        Material material = materialMapper.selectById(materialId);
        if (material == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "素材不存在");
        }

        CollectionMaterialRel existed = collectionMaterialRelMapper.selectOne(
                new LambdaQueryWrapper<CollectionMaterialRel>()
                        .eq(CollectionMaterialRel::getCollectionId, collectionId)
                        .eq(CollectionMaterialRel::getMaterialId, materialId)
                        .last("LIMIT 1"));

        if (existed == null) {
            Integer maxSort = collectionMaterialRelMapper.selectList(
                            new LambdaQueryWrapper<CollectionMaterialRel>()
                                    .eq(CollectionMaterialRel::getCollectionId, collectionId)
                                    .orderByDesc(CollectionMaterialRel::getSortOrder)
                                    .last("LIMIT 1"))
                    .stream().map(CollectionMaterialRel::getSortOrder).findFirst().orElse(0);

            CollectionMaterialRel rel = new CollectionMaterialRel();
            rel.setCollectionId(collectionId);
            rel.setMaterialId(materialId);
            rel.setSortOrder(maxSort + 1);
            rel.setNote(note == null ? "" : note);
            rel.setAddedBy(principal.getId());
            rel.setAddedAt(LocalDateTime.now());
            rel.setCreatedAt(LocalDateTime.now());
            collectionMaterialRelMapper.insert(rel);
        } else {
            if (StringUtils.hasText(note)) {
                existed.setNote(note);
                collectionMaterialRelMapper.updateById(existed);
            }
        }

        collection.setUpdatedAt(LocalDateTime.now());
        collectionMapper.updateById(collection);

        auditLogService.log(principal, "ADD_COLLECTION_MATERIAL", "COLLECTION", String.valueOf(collectionId),
                "素材 " + materialId + " 加入素材集");

        if (collection.getIsDefault() == 1) {
            Favorite favExisted = favoriteMapper.selectOne(new LambdaQueryWrapper<Favorite>()
                    .eq(Favorite::getUserId, principal.getId())
                    .eq(Favorite::getMaterialId, materialId)
                    .last("LIMIT 1"));
            if (favExisted == null) {
                Favorite favorite = new Favorite();
                favorite.setUserId(principal.getId());
                favorite.setMaterialId(materialId);
                favorite.setCreatedAt(LocalDateTime.now());
                favoriteMapper.insert(favorite);
                material.setFavoriteCount(Optional.ofNullable(material.getFavoriteCount()).orElse(0L) + 1);
                material.setUpdatedAt(LocalDateTime.now());
                materialMapper.updateById(material);
            }
        }

        traceabilitySvc.logTrail(materialId, principal, "ADD_TO_COLLECTION", "COLLECTION_USAGE", "COLLECTION", String.valueOf(collectionId), collection.getName(), null, null);
        traceabilitySvc.updateMaterialCounters(materialId);

        return Map.of("collectionId", collectionId, "materialId", materialId, "added", true);
    }

    @Override
    @Transactional
    public void removeMaterial(UserPrincipal principal, Long collectionId, Long materialId) {
        Collection collection = mustGetCollection(collectionId);
        ensureOwnerOrAdmin(principal, collection);

        collectionMaterialRelMapper.delete(new LambdaQueryWrapper<CollectionMaterialRel>()
                .eq(CollectionMaterialRel::getCollectionId, collectionId)
                .eq(CollectionMaterialRel::getMaterialId, materialId));

        collection.setUpdatedAt(LocalDateTime.now());
        collectionMapper.updateById(collection);

        if (collection.getIsDefault() == 1) {
            favoriteMapper.delete(new LambdaQueryWrapper<Favorite>()
                    .eq(Favorite::getUserId, principal.getId())
                    .eq(Favorite::getMaterialId, materialId));
            Material material = materialMapper.selectById(materialId);
            if (material != null) {
                long current = Optional.ofNullable(material.getFavoriteCount()).orElse(0L);
                material.setFavoriteCount(Math.max(0, current - 1));
                material.setUpdatedAt(LocalDateTime.now());
                materialMapper.updateById(material);
            }
        }

        auditLogService.log(principal, "REMOVE_COLLECTION_MATERIAL", "COLLECTION", String.valueOf(collectionId),
                "素材 " + materialId + " 移出素材集");

        traceabilitySvc.logTrail(materialId, principal, "REMOVE_FROM_COLLECTION", "COLLECTION_USAGE", "COLLECTION", String.valueOf(collectionId), collection.getName(), null, null);
        traceabilitySvc.updateMaterialCounters(materialId);
    }

    @Override
    @Transactional
    public Map<String, Object> updateMaterialNote(UserPrincipal principal, Long collectionId, Long materialId, String note) {
        Collection collection = mustGetCollection(collectionId);
        ensureOwnerOrAdmin(principal, collection);

        CollectionMaterialRel rel = collectionMaterialRelMapper.selectOne(
                new LambdaQueryWrapper<CollectionMaterialRel>()
                        .eq(CollectionMaterialRel::getCollectionId, collectionId)
                        .eq(CollectionMaterialRel::getMaterialId, materialId)
                        .last("LIMIT 1"));

        if (rel == null) {
            throw new BusinessException("该素材不在当前素材集中");
        }

        rel.setNote(note == null ? "" : note);
        collectionMaterialRelMapper.updateById(rel);

        return Map.of("collectionId", collectionId, "materialId", materialId, "note", rel.getNote());
    }

    @Override
    @Transactional
    public void sortMaterials(UserPrincipal principal, Long collectionId, MaterialSortRequest request) {
        Collection collection = mustGetCollection(collectionId);
        ensureOwnerOrAdmin(principal, collection);

        if (request.getItems() == null || request.getItems().isEmpty()) {
            return;
        }

        for (MaterialSortRequest.MaterialSortItem item : request.getItems()) {
            collectionMaterialRelMapper.update(null, new LambdaUpdateWrapper<CollectionMaterialRel>()
                    .set(CollectionMaterialRel::getSortOrder, item.getSortOrder())
                    .eq(CollectionMaterialRel::getCollectionId, collectionId)
                    .eq(CollectionMaterialRel::getMaterialId, item.getMaterialId()));
        }

        collection.setUpdatedAt(LocalDateTime.now());
        collectionMapper.updateById(collection);
    }

    @Override
    @Transactional
    public Map<String, Object> setMaterialCollections(UserPrincipal principal, Long materialId, List<Long> collectionIds) {
        Material material = materialMapper.selectById(materialId);
        if (material == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "素材不存在");
        }

        List<CollectionMaterialRel> existingRels = collectionMaterialRelMapper.selectList(
                new LambdaQueryWrapper<CollectionMaterialRel>()
                        .eq(CollectionMaterialRel::getMaterialId, materialId));

        Map<Long, CollectionMaterialRel> existingMap = existingRels.stream()
                .collect(Collectors.toMap(CollectionMaterialRel::getCollectionId, rel -> rel, (a, b) -> a));

        Set<Long> targetSet = collectionIds == null ? Set.of() : new HashSet<>(collectionIds);

        for (CollectionMaterialRel rel : existingRels) {
            Collection col = collectionMapper.selectById(rel.getCollectionId());
            if (col != null && Objects.equals(col.getOwnerId(), principal.getId()) && !targetSet.contains(rel.getCollectionId())) {
                collectionMaterialRelMapper.deleteById(rel.getId());
                col.setUpdatedAt(LocalDateTime.now());
                collectionMapper.updateById(col);
            }
        }

        if (collectionIds != null) {
            for (Long colId : collectionIds) {
                Collection col = collectionMapper.selectById(colId);
                if (col == null || !Objects.equals(col.getOwnerId(), principal.getId())) {
                    continue;
                }
                if (existingMap.containsKey(colId)) {
                    continue;
                }
                Integer maxSort = collectionMaterialRelMapper.selectList(
                                new LambdaQueryWrapper<CollectionMaterialRel>()
                                        .eq(CollectionMaterialRel::getCollectionId, colId)
                                        .orderByDesc(CollectionMaterialRel::getSortOrder)
                                        .last("LIMIT 1"))
                        .stream().map(CollectionMaterialRel::getSortOrder).findFirst().orElse(0);

                CollectionMaterialRel newRel = new CollectionMaterialRel();
                newRel.setCollectionId(colId);
                newRel.setMaterialId(materialId);
                newRel.setSortOrder(maxSort + 1);
                newRel.setNote("");
                newRel.setAddedBy(principal.getId());
                newRel.setAddedAt(LocalDateTime.now());
                newRel.setCreatedAt(LocalDateTime.now());
                collectionMaterialRelMapper.insert(newRel);
                col.setUpdatedAt(LocalDateTime.now());
                collectionMapper.updateById(col);
            }
        }

        Collection defaultCol = getDefaultCollection(principal);
        boolean inDefault = defaultCol != null && targetSet.contains(defaultCol.getId());

        Favorite existedFav = favoriteMapper.selectOne(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, principal.getId())
                .eq(Favorite::getMaterialId, materialId)
                .last("LIMIT 1"));

        if (inDefault && existedFav == null) {
            Favorite fav = new Favorite();
            fav.setUserId(principal.getId());
            fav.setMaterialId(materialId);
            fav.setCreatedAt(LocalDateTime.now());
            favoriteMapper.insert(fav);
            material.setFavoriteCount(Optional.ofNullable(material.getFavoriteCount()).orElse(0L) + 1);
            material.setUpdatedAt(LocalDateTime.now());
            materialMapper.updateById(material);
        } else if (!inDefault && existedFav != null) {
            favoriteMapper.deleteById(existedFav.getId());
            long current = Optional.ofNullable(material.getFavoriteCount()).orElse(0L);
            material.setFavoriteCount(Math.max(0, current - 1));
            material.setUpdatedAt(LocalDateTime.now());
            materialMapper.updateById(material);
        }

        auditLogService.log(principal, "SET_MATERIAL_COLLECTIONS", "MATERIAL", String.valueOf(materialId),
                "设置素材归属素材集");

        return Map.of("materialId", materialId, "collectionIds", targetSet);
    }

    @Override
    public List<Map<String, Object>> getMaterialCollections(UserPrincipal principal, Long materialId) {
        List<CollectionMaterialRel> rels = collectionMaterialRelMapper.selectList(
                new LambdaQueryWrapper<CollectionMaterialRel>()
                        .eq(CollectionMaterialRel::getMaterialId, materialId));

        List<Map<String, Object>> result = new ArrayList<>();
        for (CollectionMaterialRel rel : rels) {
            Collection col = collectionMapper.selectById(rel.getCollectionId());
            if (col != null && (Objects.equals(col.getOwnerId(), principal.getId()) || "ADMIN".equals(principal.getRole()))) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", col.getId());
                map.put("name", col.getName());
                map.put("visibility", col.getVisibility());
                map.put("isDefault", col.getIsDefault());
                map.put("note", rel.getNote());
                result.add(map);
            }
        }
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> createShare(UserPrincipal principal, Long collectionId, CollectionShareRequest request) {
        Collection collection = mustGetCollection(collectionId);
        ensureOwnerOrAdmin(principal, collection);

        String code = UUID.randomUUID().toString().substring(0, 10);
        CollectionShare share = new CollectionShare();
        share.setCollectionId(collectionId);
        share.setSharedBy(principal.getId());
        share.setShareCode(code);
        share.setPassword(request.getPassword() == null ? "" : request.getPassword());
        share.setAccessCount(0L);
        share.setStatus("ACTIVE");
        share.setCreatedAt(LocalDateTime.now());

        if (request.getExpireDays() != null && request.getExpireDays() > 0) {
            share.setExpireAt(LocalDateTime.now().plusDays(request.getExpireDays()));
        }

        collectionShareMapper.insert(share);

        auditLogService.log(principal, "SHARE_COLLECTION", "COLLECTION", String.valueOf(collectionId),
                "创建素材集分享链接");

        return Map.of(
                "shareId", share.getId(),
                "shareCode", code,
                "shareUrl", "/api/collections/share/" + code,
                "expireAt", share.getExpireAt() == null ? "" : share.getExpireAt(),
                "hasPassword", StringUtils.hasText(share.getPassword())
        );
    }

    @Override
    @Transactional
    public Map<String, Object> getByShareCode(String code, String password) {
        CollectionShare share = collectionShareMapper.selectOne(
                new LambdaQueryWrapper<CollectionShare>()
                        .eq(CollectionShare::getShareCode, code)
                        .last("LIMIT 1"));

        if (share == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "分享链接不存在");
        }
        if ("REVOKED".equalsIgnoreCase(share.getStatus())) {
            throw new BusinessException("分享链接已失效");
        }
        if (share.getExpireAt() != null && share.getExpireAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(HttpStatus.GONE, "分享链接已过期");
        }
        if (StringUtils.hasText(share.getPassword()) && !share.getPassword().equals(password)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "访问密码错误");
        }

        share.setAccessCount(Optional.ofNullable(share.getAccessCount()).orElse(0L) + 1);
        share.setLastAccessAt(LocalDateTime.now());
        collectionShareMapper.updateById(share);

        Collection collection = mustGetCollection(share.getCollectionId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("name", collection.getName());
        result.put("description", collection.getDescription());

        List<CollectionMaterialRel> rels = collectionMaterialRelMapper.selectList(
                new LambdaQueryWrapper<CollectionMaterialRel>()
                        .eq(CollectionMaterialRel::getCollectionId, collection.getId())
                        .orderByAsc(CollectionMaterialRel::getSortOrder)
                        .orderByDesc(CollectionMaterialRel::getCreatedAt));

        List<Map<String, Object>> materials = new ArrayList<>();
        for (CollectionMaterialRel rel : rels) {
            Material material = materialMapper.selectById(rel.getMaterialId());
            if (material != null) {
                Map<String, Object> materialMap = new LinkedHashMap<>();
                materialMap.put("id", material.getId());
                materialMap.put("title", material.getTitle());
                materialMap.put("description", material.getDescription());
                materialMap.put("type", material.getType());
                materialMap.put("format", material.getFormat());
                materialMap.put("sizeBytes", material.getSizeBytes());
                materialMap.put("durationSeconds", material.getDurationSeconds());
                materialMap.put("resolution", material.getResolution());
                materialMap.put("note", rel.getNote());
                materialMap.put("previewUrl", "/api/materials/" + material.getId() + "/preview");
                materials.add(materialMap);
            }
        }
        result.put("materials", materials);
        return result;
    }

    @Override
    @Transactional
    public void revokeShare(UserPrincipal principal, Long shareId) {
        CollectionShare share = collectionShareMapper.selectById(shareId);
        if (share == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "分享记录不存在");
        }

        Collection collection = mustGetCollection(share.getCollectionId());
        ensureOwnerOrAdmin(principal, collection);

        share.setStatus("REVOKED");
        collectionShareMapper.updateById(share);

        auditLogService.log(principal, "REVOKE_COLLECTION_SHARE", "COLLECTION", String.valueOf(collection.getId()),
                "失效素材集分享链接");
    }

    @Override
    public List<Map<String, Object>> listShares(UserPrincipal principal, Long collectionId) {
        Collection collection = mustGetCollection(collectionId);
        ensureOwnerOrAdmin(principal, collection);

        List<CollectionShare> shares = collectionShareMapper.selectList(
                new LambdaQueryWrapper<CollectionShare>()
                        .eq(CollectionShare::getCollectionId, collectionId)
                        .orderByDesc(CollectionShare::getCreatedAt));

        return shares.stream().map(share -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", share.getId());
            map.put("shareCode", share.getShareCode());
            map.put("shareUrl", "/api/collections/share/" + share.getShareCode());
            map.put("hasPassword", StringUtils.hasText(share.getPassword()));
            map.put("expireAt", share.getExpireAt());
            map.put("accessCount", share.getAccessCount());
            map.put("lastAccessAt", share.getLastAccessAt());
            map.put("status", share.getStatus());
            map.put("createdAt", share.getCreatedAt());
            return map;
        }).toList();
    }

    @Override
    @Transactional
    public void sortCollections(UserPrincipal principal, CollectionSortRequest request) {
        if (request.getOrderedIds() == null || request.getOrderedIds().isEmpty()) {
            return;
        }
        for (int i = 0; i < request.getOrderedIds().size(); i++) {
            Long colId = request.getOrderedIds().get(i);
            Collection col = collectionMapper.selectById(colId);
            if (col != null && Objects.equals(col.getOwnerId(), principal.getId())) {
                col.setSortOrder(i);
                col.setUpdatedAt(LocalDateTime.now());
                collectionMapper.updateById(col);
            }
        }
    }

    @Override
    @Transactional
    public void ensureDefaultCollection(UserPrincipal principal) {
        Collection existed = collectionMapper.selectOne(new LambdaQueryWrapper<Collection>()
                .eq(Collection::getOwnerId, principal.getId())
                .eq(Collection::getIsDefault, 1)
                .last("LIMIT 1"));

        if (existed == null) {
            Collection defaultCol = new Collection();
            defaultCol.setName("我的收藏");
            defaultCol.setDescription("默认收藏素材集");
            defaultCol.setOwnerId(principal.getId());
            defaultCol.setVisibility("PRIVATE");
            defaultCol.setSortOrder(0);
            defaultCol.setIsDefault(1);
            defaultCol.setCreatedAt(LocalDateTime.now());
            defaultCol.setUpdatedAt(LocalDateTime.now());
            collectionMapper.insert(defaultCol);
        }
    }

    @Override
    public List<Map<String, Object>> collectionStats(UserPrincipal principal) {
        List<Collection> collections = collectionMapper.selectList(
                new LambdaQueryWrapper<Collection>()
                        .eq(Collection::getOwnerId, principal.getId()));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Collection col : collections) {
            List<CollectionMaterialRel> rels = collectionMaterialRelMapper.selectList(
                    new LambdaQueryWrapper<CollectionMaterialRel>()
                            .eq(CollectionMaterialRel::getCollectionId, col.getId()));

            long totalBytes = 0;
            Map<String, Long> typeCount = new LinkedHashMap<>();
            int validCount = 0;

            for (CollectionMaterialRel rel : rels) {
                Material material = materialMapper.selectById(rel.getMaterialId());
                if (material != null) {
                    totalBytes += Optional.ofNullable(material.getSizeBytes()).orElse(0L);
                    typeCount.merge(material.getType(), 1L, Long::sum);
                    validCount++;
                }
            }

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", col.getId());
            map.put("name", col.getName());
            map.put("isDefault", col.getIsDefault());
            map.put("materialCount", validCount);
            map.put("totalBytes", totalBytes);
            map.put("typeDistribution", typeCount);
            map.put("updatedAt", col.getUpdatedAt());
            map.put("createdAt", col.getCreatedAt());
            result.add(map);
        }

        result.sort((a, b) -> Long.compare((Integer) b.get("materialCount"), (Integer) a.get("materialCount")));
        return result;
    }

    private Collection mustGetCollection(Long collectionId) {
        Collection collection = collectionMapper.selectById(collectionId);
        if (collection == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "素材集不存在");
        }
        return collection;
    }

    private void ensureOwnerOrAdmin(UserPrincipal principal, Collection collection) {
        if ("ADMIN".equals(principal.getRole()) || Objects.equals(principal.getId(), collection.getOwnerId())) {
            return;
        }
        throw new BusinessException(HttpStatus.FORBIDDEN, "仅素材集所有者或管理员可操作");
    }

    private Collection getDefaultCollection(UserPrincipal principal) {
        return collectionMapper.selectOne(new LambdaQueryWrapper<Collection>()
                .eq(Collection::getOwnerId, principal.getId())
                .eq(Collection::getIsDefault, 1)
                .last("LIMIT 1"));
    }

    private Map<String, Object> collectionToMap(Collection collection) {
        long materialCount = collectionMaterialRelMapper.selectCount(
                new LambdaQueryWrapper<CollectionMaterialRel>()
                        .eq(CollectionMaterialRel::getCollectionId, collection.getId()));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", collection.getId());
        result.put("name", collection.getName());
        result.put("description", collection.getDescription());
        result.put("coverMaterialId", collection.getCoverMaterialId());
        result.put("ownerId", collection.getOwnerId());
        result.put("visibility", collection.getVisibility());
        result.put("sortOrder", collection.getSortOrder());
        result.put("isDefault", collection.getIsDefault());
        result.put("materialCount", materialCount);
        result.put("createdAt", collection.getCreatedAt());
        result.put("updatedAt", collection.getUpdatedAt());

        if (collection.getCoverMaterialId() != null) {
            Material cover = materialMapper.selectById(collection.getCoverMaterialId());
            if (cover != null) {
                result.put("coverPreviewUrl", "/api/materials/" + cover.getId() + "/preview");
            }
        }

        return result;
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
}
