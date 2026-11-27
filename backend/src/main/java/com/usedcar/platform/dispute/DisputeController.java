package com.usedcar.platform.dispute;

import com.usedcar.platform.auth.RequireRole;
import com.usedcar.platform.common.ApiResponse;
import com.usedcar.platform.dispute.dto.AddEvidenceRequest;
import com.usedcar.platform.dispute.dto.CreateDisputeRequest;
import com.usedcar.platform.dispute.dto.DisputeDetail;
import com.usedcar.platform.dispute.dto.DisputeRow;
import com.usedcar.platform.dispute.dto.ResolveDisputeRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/disputes")
public class DisputeController {

    private final DisputeService disputeService;

    public DisputeController(DisputeService disputeService) {
        this.disputeService = disputeService;
    }

    @GetMapping
    public ApiResponse<List<DisputeRow>> list(@RequestParam(required = false) String status) {
        return ApiResponse.ok(disputeService.list(status));
    }

    @GetMapping("/{id}")
    public ApiResponse<DisputeDetail> detail(@PathVariable Long id) {
        return ApiResponse.ok(disputeService.detail(id));
    }

    @PostMapping
    @RequireRole({"BUYER", "DEALER"})
    public ApiResponse<DisputeCase> create(@Valid @RequestBody CreateDisputeRequest request) {
        return ApiResponse.ok(disputeService.create(request));
    }

    @PostMapping("/{id}/claim")
    @RequireRole({"ARBITRATOR"})
    public ApiResponse<DisputeCase> claim(@PathVariable Long id) {
        return ApiResponse.ok(disputeService.claim(id));
    }

    @PostMapping("/{id}/evidence")
    public ApiResponse<DisputeEvidence> addEvidence(@PathVariable Long id,
                                                    @Valid @RequestBody AddEvidenceRequest request) {
        return ApiResponse.ok(disputeService.addEvidence(id, request));
    }

    @PostMapping("/{id}/close")
    public ApiResponse<DisputeCase> close(@PathVariable Long id) {
        return ApiResponse.ok(disputeService.close(id));
    }

    @PostMapping("/{id}/resolve")
    @RequireRole({"ARBITRATOR"})
    public ApiResponse<DisputeCase> resolve(@PathVariable Long id,
                                            @Valid @RequestBody ResolveDisputeRequest request) {
        return ApiResponse.ok(disputeService.resolve(id, request));
    }
}
