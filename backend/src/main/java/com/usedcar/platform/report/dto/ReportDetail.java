package com.usedcar.platform.report.dto;

import com.usedcar.platform.attachment.dto.AttachmentView;
import com.usedcar.platform.evidence.dto.DefectEntry;
import com.usedcar.platform.inspection.InspectionItem;
import com.usedcar.platform.inspection.InspectionOrder;
import com.usedcar.platform.report.InspectionReport;
import com.usedcar.platform.vehicle.Vehicle;

import java.util.List;

public record ReportDetail(
        InspectionReport report,
        Vehicle vehicle,
        InspectionOrder order,
        List<InspectionItem> items,
        List<DefectEntry> defects,
        List<AttachmentView> attachments
) {
}
