package com.usedcar.platform.vehicle;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.usedcar.platform.auth.RequireRole;
import com.usedcar.platform.common.ApiResponse;
import com.usedcar.platform.vehicle.dto.MaintenanceRequest;
import com.usedcar.platform.vehicle.dto.VehicleSaveRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @GetMapping
    public ApiResponse<Page<Vehicle>> page(@RequestParam(required = false) String keyword,
                                           @RequestParam(defaultValue = "1") long page,
                                           @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(vehicleService.page(keyword, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<Vehicle> detail(@PathVariable Long id) {
        return ApiResponse.ok(vehicleService.get(id));
    }

    @PostMapping
    @RequireRole({"DEALER", "INSPECTOR", "OPERATOR"})
    public ApiResponse<Vehicle> create(@Valid @RequestBody VehicleSaveRequest request) {
        return ApiResponse.ok(vehicleService.create(request));
    }

    @PutMapping("/{id}")
    @RequireRole({"DEALER", "INSPECTOR", "OPERATOR"})
    public ApiResponse<Vehicle> update(@PathVariable Long id,
                                       @Valid @RequestBody VehicleSaveRequest request) {
        return ApiResponse.ok(vehicleService.update(id, request));
    }

    @GetMapping("/{id}/maintenance")
    public ApiResponse<List<MaintenanceRecord>> maintenance(@PathVariable Long id) {
        return ApiResponse.ok(vehicleService.maintenanceList(id));
    }

    @PostMapping("/{id}/maintenance")
    @RequireRole({"INSPECTOR", "OPERATOR"})
    public ApiResponse<MaintenanceRecord> addMaintenance(@PathVariable Long id,
                                                         @Valid @RequestBody MaintenanceRequest request) {
        return ApiResponse.ok(vehicleService.addMaintenance(id, request));
    }
}
