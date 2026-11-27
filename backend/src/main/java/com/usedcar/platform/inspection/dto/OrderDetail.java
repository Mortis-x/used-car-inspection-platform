package com.usedcar.platform.inspection.dto;

import com.usedcar.platform.inspection.InspectionItem;
import com.usedcar.platform.inspection.InspectionOrder;
import com.usedcar.platform.vehicle.Vehicle;

import java.util.List;

public record OrderDetail(
        InspectionOrder order,
        Vehicle vehicle,
        List<InspectionItem> items,
        String statusLabel
) {
}
