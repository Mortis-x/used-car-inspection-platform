package com.usedcar.platform.vehicle;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.usedcar.platform.audit.AuditLogService;
import com.usedcar.platform.auth.AuthContext;
import com.usedcar.platform.common.BizException;
import com.usedcar.platform.vehicle.dto.MaintenanceRequest;
import com.usedcar.platform.vehicle.dto.VehicleSaveRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VehicleService {

    private final VehicleMapper vehicleMapper;
    private final MaintenanceRecordMapper maintenanceRecordMapper;
    private final AuditLogService auditLogService;

    public VehicleService(VehicleMapper vehicleMapper,
                          MaintenanceRecordMapper maintenanceRecordMapper,
                          AuditLogService auditLogService) {
        this.vehicleMapper = vehicleMapper;
        this.maintenanceRecordMapper = maintenanceRecordMapper;
        this.auditLogService = auditLogService;
    }

    public Page<Vehicle> page(String keyword, long pageNum, long pageSize) {
        Page<Vehicle> page = new Page<>(pageNum, pageSize);
        return vehicleMapper.selectPage(page, Wrappers.<Vehicle>lambdaQuery()
                .and(keyword != null && !keyword.isBlank(), wrapper -> wrapper
                        .like(Vehicle::getVin, keyword)
                        .or().like(Vehicle::getPlateNo, keyword)
                        .or().like(Vehicle::getBrand, keyword)
                        .or().like(Vehicle::getModel, keyword))
                .orderByDesc(Vehicle::getId));
    }

    public Vehicle get(Long id) {
        Vehicle vehicle = vehicleMapper.selectById(id);
        if (vehicle == null) {
            throw new BizException(404, "车辆不存在");
        }
        return vehicle;
    }

    @Transactional
    public Vehicle create(VehicleSaveRequest request) {
        ensureVinUnique(request.vin(), null);
        Vehicle vehicle = new Vehicle();
        apply(vehicle, request);
        vehicle.setCreatedBy(AuthContext.userId());
        vehicleMapper.insert(vehicle);
        auditLogService.record("CREATE", "VEHICLE", vehicle.getId(),
                "建档 " + vehicle.getBrand() + " " + vehicle.getModel());
        return vehicle;
    }

    @Transactional
    public Vehicle update(Long id, VehicleSaveRequest request) {
        Vehicle vehicle = get(id);
        ensureVinUnique(request.vin(), id);
        apply(vehicle, request);
        vehicleMapper.updateById(vehicle);
        auditLogService.record("UPDATE", "VEHICLE", id, "修改车辆档案");
        return vehicle;
    }

    @Transactional
    public MaintenanceRecord addMaintenance(Long vehicleId, MaintenanceRequest request) {
        get(vehicleId);
        MaintenanceRecord record = new MaintenanceRecord();
        record.setVehicleId(vehicleId);
        record.setRecordType(request.recordType());
        record.setRecordDate(request.recordDate());
        record.setMileage(request.mileage());
        record.setProvider(request.provider());
        record.setContent(request.content());
        record.setAmount(request.amount());
        maintenanceRecordMapper.insert(record);
        auditLogService.record("CREATE", "MAINTENANCE", record.getId(), "新增维保记录");
        return record;
    }

    public List<MaintenanceRecord> maintenanceList(Long vehicleId) {
        return maintenanceRecordMapper.selectList(Wrappers.<MaintenanceRecord>lambdaQuery()
                .eq(MaintenanceRecord::getVehicleId, vehicleId)
                .orderByDesc(MaintenanceRecord::getRecordDate));
    }

    private void ensureVinUnique(String vin, Long excludeId) {
        Long count = vehicleMapper.selectCount(Wrappers.<Vehicle>lambdaQuery()
                .eq(Vehicle::getVin, vin)
                .ne(excludeId != null, Vehicle::getId, excludeId));
        if (count != null && count > 0) {
            throw new BizException("该 VIN 已存在车辆档案，请勿重复建档");
        }
    }

    private void apply(Vehicle vehicle, VehicleSaveRequest request) {
        vehicle.setVin(request.vin().trim());
        vehicle.setPlateNo(request.plateNo());
        vehicle.setBrand(request.brand());
        vehicle.setModel(request.model());
        vehicle.setColor(request.color());
        vehicle.setFirstRegDate(request.firstRegDate());
        vehicle.setMileage(request.mileage());
        vehicle.setTransferCount(request.transferCount() == null ? 0 : request.transferCount());
    }
}
