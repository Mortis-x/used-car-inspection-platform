package com.usedcar.platform.inspection;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.usedcar.platform.auth.RequireRole;
import com.usedcar.platform.common.ApiResponse;
import com.usedcar.platform.inspection.dto.CreateOrderRequest;
import com.usedcar.platform.inspection.dto.ItemUpdateRequest;
import com.usedcar.platform.inspection.dto.OrderDetail;
import com.usedcar.platform.inspection.dto.OrderRow;
import com.usedcar.platform.inspection.dto.WithdrawRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inspections")
public class InspectionOrderController {

    private final InspectionOrderService orderService;

    public InspectionOrderController(InspectionOrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ApiResponse<Page<OrderRow>> page(@RequestParam(required = false) String status,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Long vehicleId,
                                            @RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(orderService.page(status, keyword, vehicleId, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderDetail> detail(@PathVariable Long id) {
        return ApiResponse.ok(orderService.detail(id));
    }

    @PostMapping
    @RequireRole({"DEALER", "OPERATOR"})
    public ApiResponse<InspectionOrder> create(@Valid @RequestBody CreateOrderRequest request) {
        return ApiResponse.ok(orderService.create(request));
    }

    @PostMapping("/{id}/start")
    @RequireRole({"INSPECTOR"})
    public ApiResponse<InspectionOrder> start(@PathVariable Long id) {
        return ApiResponse.ok(orderService.start(id));
    }

    @PostMapping("/{id}/submit")
    @RequireRole({"INSPECTOR"})
    public ApiResponse<InspectionOrder> submit(@PathVariable Long id) {
        return ApiResponse.ok(orderService.submit(id));
    }

    @PostMapping("/{id}/withdraw")
    public ApiResponse<InspectionOrder> withdraw(@PathVariable Long id,
                                                 @Valid @RequestBody WithdrawRequest request) {
        return ApiResponse.ok(orderService.withdraw(id, request.reason()));
    }

    @PutMapping("/{id}/items/{itemId}")
    @RequireRole({"INSPECTOR"})
    public ApiResponse<InspectionItem> updateItem(@PathVariable Long id,
                                                  @PathVariable Long itemId,
                                                  @RequestBody ItemUpdateRequest request) {
        return ApiResponse.ok(orderService.updateItem(id, itemId, request));
    }
}
