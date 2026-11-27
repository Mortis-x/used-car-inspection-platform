package com.usedcar.platform.template;

import com.usedcar.platform.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/templates")
public class TemplateController {

    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping
    public ApiResponse<List<InspectionTemplate>> list() {
        return ApiResponse.ok(templateService.listActive());
    }

    @GetMapping("/{id}/items")
    public ApiResponse<List<InspectionTemplateItem>> items(@PathVariable Long id) {
        return ApiResponse.ok(templateService.items(id));
    }
}
