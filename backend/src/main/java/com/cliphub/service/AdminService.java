package com.cliphub.service;

import com.cliphub.dto.CategoryRequest;
import com.cliphub.dto.NotificationRequest;
import com.cliphub.dto.SettingRequest;
import com.cliphub.dto.TagRequest;
import com.cliphub.security.UserPrincipal;

import java.util.List;
import java.util.Map;

public interface AdminService {

    Map<String, Object> saveCategory(CategoryRequest request, Long id, UserPrincipal principal);

    void deleteCategory(Long id, UserPrincipal principal);

    List<Map<String, Object>> listCategories();

    Map<String, Object> saveTag(TagRequest request, Long id, UserPrincipal principal);

    void deleteTag(Long id, UserPrincipal principal);

    List<Map<String, Object>> listTags();

    Map<String, Object> saveSetting(SettingRequest request, UserPrincipal principal);

    List<Map<String, Object>> listSettings();

    Map<String, Object> saveNotification(NotificationRequest request, Long id, UserPrincipal principal);

    void deleteNotification(Long id, UserPrincipal principal);

    List<Map<String, Object>> listNotifications();

    List<Map<String, Object>> listPublicNotifications();
}
