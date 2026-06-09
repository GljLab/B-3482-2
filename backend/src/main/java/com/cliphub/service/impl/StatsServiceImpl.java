package com.cliphub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cliphub.entity.*;
import com.cliphub.mapper.*;
import com.cliphub.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final OperationLogMapper operationLogMapper;
    private final MaterialMapper materialMapper;
    private final ProjectMaterialRelMapper projectMaterialRelMapper;
    private final SystemSettingMapper settingMapper;
    private final UserMapper userMapper;

    @Override
    public List<Map<String, Object>> userActivityStats() {
        List<OperationLog> logs = operationLogMapper.selectList(new LambdaQueryWrapper<OperationLog>()
                .ge(OperationLog::getCreatedAt, LocalDateTime.now().minusDays(30)));

        Map<Long, Long> countByUser = logs.stream()
                .filter(log -> log.getUserId() != null)
                .collect(Collectors.groupingBy(OperationLog::getUserId, Collectors.counting()));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, Long> entry : countByUser.entrySet()) {
            User user = userMapper.selectById(entry.getKey());
            if (user == null) {
                continue;
            }
            result.add(Map.of(
                    "userId", user.getId(),
                    "username", user.getUsername(),
                    "displayName", user.getDisplayName(),
                    "activityCount", entry.getValue()
            ));
        }

        result.sort((a, b) -> Long.compare((Long) b.get("activityCount"), (Long) a.get("activityCount")));
        return result.stream().limit(20).toList();
    }

    @Override
    public List<Map<String, Object>> materialUsageStats() {
        List<Material> materials = materialMapper.selectList(new LambdaQueryWrapper<Material>()
                .orderByDesc(Material::getDownloadCount));

        Map<Long, Long> projectUsageMap = projectMaterialRelMapper.selectList(new LambdaQueryWrapper<ProjectMaterialRel>())
                .stream()
                .collect(Collectors.groupingBy(ProjectMaterialRel::getMaterialId, Collectors.counting()));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Material material : materials) {
            result.add(Map.of(
                    "materialId", material.getId(),
                    "title", material.getTitle(),
                    "type", material.getType(),
                    "downloadCount", material.getDownloadCount(),
                    "favoriteCount", material.getFavoriteCount(),
                    "shareCount", material.getShareCount(),
                    "projectUsage", projectUsageMap.getOrDefault(material.getId(), 0L)
            ));
        }

        return result.stream().limit(50).toList();
    }

    @Override
    public Map<String, Object> storageReport() {
        List<Material> materials = materialMapper.selectList(new LambdaQueryWrapper<Material>());

        long totalBytes = materials.stream().map(Material::getSizeBytes).filter(Objects::nonNull).reduce(0L, Long::sum);

        Map<String, Long> typeBytes = new HashMap<>();
        for (Material material : materials) {
            String type = material.getType() == null ? "UNKNOWN" : material.getType();
            typeBytes.merge(type, Optional.ofNullable(material.getSizeBytes()).orElse(0L), Long::sum);
        }

        SystemSetting limitSetting = settingMapper.selectOne(new LambdaQueryWrapper<SystemSetting>()
                .eq(SystemSetting::getSettingKey, "storage_limit_gb")
                .last("LIMIT 1"));

        double limitGb = limitSetting == null ? 5D : Double.parseDouble(limitSetting.getSettingValue());
        double usedGb = bytesToGb(totalBytes);

        return Map.of(
                "totalBytes", totalBytes,
                "usedGb", round(usedGb),
                "limitGb", limitGb,
                "usagePercent", round(limitGb == 0 ? 0 : (usedGb / limitGb * 100)),
                "alert", usedGb >= limitGb,
                "breakdown", typeBytes.entrySet().stream()
                        .map(entry -> Map.of(
                                "type", entry.getKey(),
                                "bytes", entry.getValue(),
                                "gb", round(bytesToGb(entry.getValue()))))
                        .toList()
        );
    }

    @Override
    public List<Map<String, Object>> hotMaterials() {
        List<Material> materials = materialMapper.selectList(new LambdaQueryWrapper<Material>());
        Map<Long, Long> projectUsageMap = projectMaterialRelMapper.selectList(new LambdaQueryWrapper<ProjectMaterialRel>())
                .stream()
                .collect(Collectors.groupingBy(ProjectMaterialRel::getMaterialId, Collectors.counting()));

        List<Map<String, Object>> scored = new ArrayList<>();
        for (Material material : materials) {
            long downloads = Optional.ofNullable(material.getDownloadCount()).orElse(0L);
            long favorites = Optional.ofNullable(material.getFavoriteCount()).orElse(0L);
            long shares = Optional.ofNullable(material.getShareCount()).orElse(0L);
            long projectUses = projectUsageMap.getOrDefault(material.getId(), 0L);
            long score = downloads * 3 + favorites * 4 + shares * 2 + projectUses * 5;

            scored.add(Map.of(
                    "materialId", material.getId(),
                    "title", material.getTitle(),
                    "type", material.getType(),
                    "downloads", downloads,
                    "favorites", favorites,
                    "shares", shares,
                    "projectUses", projectUses,
                    "score", score
            ));
        }

        scored.sort((a, b) -> Long.compare((Long) b.get("score"), (Long) a.get("score")));
        return scored.stream().limit(10).toList();
    }

    private double bytesToGb(long bytes) {
        return bytes / 1024D / 1024D / 1024D;
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
