package com.cliphub.service;

import java.util.List;
import java.util.Map;

public interface StatsService {

    List<Map<String, Object>> userActivityStats();

    List<Map<String, Object>> materialUsageStats();

    Map<String, Object> storageReport();

    List<Map<String, Object>> hotMaterials();
}
