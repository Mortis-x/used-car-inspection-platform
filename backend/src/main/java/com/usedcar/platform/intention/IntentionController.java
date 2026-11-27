package com.usedcar.platform.intention;

import com.usedcar.platform.auth.RequireRole;
import com.usedcar.platform.common.ApiResponse;
import com.usedcar.platform.intention.dto.CounterRequest;
import com.usedcar.platform.intention.dto.CreateIntentionRequest;
import com.usedcar.platform.intention.dto.IntentionRow;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/intentions")
public class IntentionController {

    private final IntentionService intentionService;

    public IntentionController(IntentionService intentionService) {
        this.intentionService = intentionService;
    }

    @GetMapping
    public ApiResponse<List<IntentionRow>> list() {
        return ApiResponse.ok(intentionService.list());
    }

    @PostMapping
    @RequireRole({"BUYER"})
    public ApiResponse<IntentionOrder> create(@Valid @RequestBody CreateIntentionRequest request) {
        return ApiResponse.ok(intentionService.create(request));
    }

    @PostMapping("/{id}/counter")
    @RequireRole({"DEALER"})
    public ApiResponse<IntentionOrder> counter(@PathVariable Long id,
                                               @Valid @RequestBody CounterRequest request) {
        return ApiResponse.ok(intentionService.counter(id, request.price()));
    }

    @PostMapping("/{id}/accept")
    @RequireRole({"DEALER"})
    public ApiResponse<IntentionOrder> accept(@PathVariable Long id) {
        return ApiResponse.ok(intentionService.accept(id));
    }

    @PostMapping("/{id}/reject")
    @RequireRole({"DEALER"})
    public ApiResponse<IntentionOrder> reject(@PathVariable Long id) {
        return ApiResponse.ok(intentionService.reject(id));
    }

    @PostMapping("/{id}/confirm")
    @RequireRole({"BUYER"})
    public ApiResponse<IntentionOrder> confirm(@PathVariable Long id) {
        return ApiResponse.ok(intentionService.confirmCounter(id));
    }

    @PostMapping("/{id}/decline")
    @RequireRole({"BUYER"})
    public ApiResponse<IntentionOrder> decline(@PathVariable Long id) {
        return ApiResponse.ok(intentionService.declineCounter(id));
    }

    @PostMapping("/{id}/cancel")
    @RequireRole({"BUYER"})
    public ApiResponse<IntentionOrder> cancel(@PathVariable Long id) {
        return ApiResponse.ok(intentionService.cancel(id));
    }
}
