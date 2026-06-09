package com.cliphub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cliphub.common.BusinessException;
import com.cliphub.dto.CategoryRequest;
import com.cliphub.dto.NotificationRequest;
import com.cliphub.dto.SettingRequest;
import com.cliphub.dto.TagRequest;
import com.cliphub.entity.Category;
import com.cliphub.entity.Notification;
import com.cliphub.entity.SystemSetting;
import com.cliphub.entity.Tag;
import com.cliphub.mapper.CategoryMapper;
import com.cliphub.mapper.NotificationMapper;
import com.cliphub.mapper.SystemSettingMapper;
import com.cliphub.mapper.TagMapper;
import com.cliphub.security.UserPrincipal;
import com.cliphub.service.AdminService;
import com.cliphub.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final SystemSettingMapper settingMapper;
    private final NotificationMapper notificationMapper;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public Map<String, Object> saveCategory(CategoryRequest request, Long id, UserPrincipal principal) {
        Category category = id == null ? new Category() : categoryMapper.selectById(id);
        if (id != null && category == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "分类不存在");
        }

        category.setName(request.getName());
        category.setParentId(request.getParentId());
        category.setDescription(request.getDescription());
        category.setUpdatedAt(LocalDateTime.now());
        if (id == null) {
            category.setCreatedAt(LocalDateTime.now());
            categoryMapper.insert(category);
        } else {
            categoryMapper.updateById(category);
        }

        auditLogService.log(principal, id == null ? "CREATE_CATEGORY" : "UPDATE_CATEGORY", "CATEGORY",
                String.valueOf(category.getId()), "维护素材分类");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", category.getId());
        result.put("name", category.getName());
        result.put("parentId", category.getParentId());
        result.put("description", category.getDescription());
        return result;
    }

    @Override
    @Transactional
    public void deleteCategory(Long id, UserPrincipal principal) {
        categoryMapper.deleteById(id);
        auditLogService.log(principal, "DELETE_CATEGORY", "CATEGORY", String.valueOf(id), "删除素材分类");
    }

    @Override
    public List<Map<String, Object>> listCategories() {
        return categoryMapper.selectList(new LambdaQueryWrapper<Category>().orderByAsc(Category::getId))
                .stream()
                .map(category -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", category.getId());
                    item.put("name", category.getName());
                    item.put("parentId", category.getParentId());
                    item.put("description", category.getDescription() == null ? "" : category.getDescription());
                    return item;
                })
                .toList();
    }

    @Override
    @Transactional
    public Map<String, Object> saveTag(TagRequest request, Long id, UserPrincipal principal) {
        Tag tag = id == null ? new Tag() : tagMapper.selectById(id);
        if (id != null && tag == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "标签不存在");
        }

        tag.setName(request.getName().toLowerCase(Locale.ROOT));
        tag.setUpdatedAt(LocalDateTime.now());
        if (id == null) {
            tag.setCreatedAt(LocalDateTime.now());
            tagMapper.insert(tag);
        } else {
            tagMapper.updateById(tag);
        }

        auditLogService.log(principal, id == null ? "CREATE_TAG" : "UPDATE_TAG", "TAG",
                String.valueOf(tag.getId()), "维护素材标签");
        return Map.of("id", tag.getId(), "name", tag.getName());
    }

    @Override
    @Transactional
    public void deleteTag(Long id, UserPrincipal principal) {
        tagMapper.deleteById(id);
        auditLogService.log(principal, "DELETE_TAG", "TAG", String.valueOf(id), "删除素材标签");
    }

    @Override
    public List<Map<String, Object>> listTags() {
        return tagMapper.selectList(new LambdaQueryWrapper<Tag>().orderByAsc(Tag::getId))
                .stream()
                .map(tag -> Map.<String, Object>of("id", tag.getId(), "name", tag.getName()))
                .toList();
    }

    @Override
    @Transactional
    public Map<String, Object> saveSetting(SettingRequest request, UserPrincipal principal) {
        SystemSetting setting = settingMapper.selectOne(new LambdaQueryWrapper<SystemSetting>()
                .eq(SystemSetting::getSettingKey, request.getSettingKey())
                .last("LIMIT 1"));

        if (setting == null) {
            setting = new SystemSetting();
            setting.setSettingKey(request.getSettingKey());
        }
        setting.setSettingValue(request.getSettingValue());
        setting.setDescription(request.getDescription());
        setting.setUpdatedBy(principal.getId());
        setting.setUpdatedAt(LocalDateTime.now());

        if (setting.getId() == null) {
            settingMapper.insert(setting);
        } else {
            settingMapper.updateById(setting);
        }

        auditLogService.log(principal, "SAVE_SETTING", "SYSTEM_SETTING", String.valueOf(setting.getId()),
                "更新系统配置: " + setting.getSettingKey());

        return Map.of(
                "id", setting.getId(),
                "settingKey", setting.getSettingKey(),
                "settingValue", setting.getSettingValue(),
                "description", setting.getDescription() == null ? "" : setting.getDescription(),
                "updatedAt", setting.getUpdatedAt()
        );
    }

    @Override
    public List<Map<String, Object>> listSettings() {
        return settingMapper.selectList(new LambdaQueryWrapper<SystemSetting>().orderByAsc(SystemSetting::getId))
                .stream()
                .map(setting -> Map.<String, Object>of(
                        "id", setting.getId(),
                        "settingKey", setting.getSettingKey(),
                        "settingValue", setting.getSettingValue(),
                        "description", setting.getDescription() == null ? "" : setting.getDescription(),
                        "updatedAt", setting.getUpdatedAt()))
                .toList();
    }

    @Override
    @Transactional
    public Map<String, Object> saveNotification(NotificationRequest request, Long id, UserPrincipal principal) {
        Notification notification = id == null ? new Notification() : notificationMapper.selectById(id);
        if (id != null && notification == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "通知不存在");
        }

        notification.setTitle(request.getTitle());
        notification.setContent(request.getContent());
        notification.setLevel(request.getLevel().toUpperCase(Locale.ROOT));
        notification.setStatus(request.getStatus().toUpperCase(Locale.ROOT));
        notification.setPublishAt(request.getPublishAt() == null ? LocalDateTime.now() : request.getPublishAt());

        if (id == null) {
            notification.setCreatedAt(LocalDateTime.now());
            notificationMapper.insert(notification);
        } else {
            notificationMapper.updateById(notification);
        }

        auditLogService.log(principal, id == null ? "CREATE_NOTIFICATION" : "UPDATE_NOTIFICATION", "NOTIFICATION",
                String.valueOf(notification.getId()), "维护系统通知");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", notification.getId());
        result.put("title", notification.getTitle());
        result.put("content", notification.getContent());
        result.put("level", notification.getLevel());
        result.put("status", notification.getStatus());
        result.put("publishAt", notification.getPublishAt());
        return result;
    }

    @Override
    @Transactional
    public void deleteNotification(Long id, UserPrincipal principal) {
        notificationMapper.deleteById(id);
        auditLogService.log(principal, "DELETE_NOTIFICATION", "NOTIFICATION", String.valueOf(id), "删除系统通知");
    }

    @Override
    public List<Map<String, Object>> listNotifications() {
        return notificationMapper.selectList(new LambdaQueryWrapper<Notification>().orderByDesc(Notification::getPublishAt))
                .stream()
                .map(item -> Map.<String, Object>of(
                        "id", item.getId(),
                        "title", item.getTitle(),
                        "content", item.getContent(),
                        "level", item.getLevel(),
                        "status", item.getStatus(),
                        "publishAt", item.getPublishAt()))
                .toList();
    }

    @Override
    public List<Map<String, Object>> listPublicNotifications() {
        LocalDateTime now = LocalDateTime.now();
        return notificationMapper.selectList(new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getStatus, "ENABLED")
                        .le(Notification::getPublishAt, now)
                        .orderByDesc(Notification::getPublishAt))
                .stream()
                .map(item -> Map.<String, Object>of(
                        "id", item.getId(),
                        "title", item.getTitle(),
                        "content", item.getContent(),
                        "level", item.getLevel(),
                        "publishAt", item.getPublishAt()))
                .toList();
    }
}
