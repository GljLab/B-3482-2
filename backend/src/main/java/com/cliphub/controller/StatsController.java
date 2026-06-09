package com.cliphub.controller;

import com.cliphub.common.ApiResponse;
import com.cliphub.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/user-activity")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> userActivity() {
        return ResponseEntity.ok(ApiResponse.ok(statsService.userActivityStats()));
    }

    @GetMapping("/material-usage")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> materialUsage() {
        return ResponseEntity.ok(ApiResponse.ok(statsService.materialUsageStats()));
    }

    @GetMapping("/storage")
    public ResponseEntity<ApiResponse<Map<String, Object>>> storage() {
        return ResponseEntity.ok(ApiResponse.ok(statsService.storageReport()));
    }

    @GetMapping("/hot-materials")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> hotMaterials() {
        return ResponseEntity.ok(ApiResponse.ok(statsService.hotMaterials()));
    }
}
