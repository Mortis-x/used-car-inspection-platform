package com.usedcar.platform.attachment;

import com.usedcar.platform.attachment.dto.AttachmentView;
import com.usedcar.platform.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
public class AttachmentController {

    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @GetMapping("/inspections/{orderId}/attachments")
    public ApiResponse<List<AttachmentView>> list(@PathVariable Long orderId) {
        return ApiResponse.ok(attachmentService.list(orderId));
    }

    @PostMapping("/inspections/{orderId}/attachments")
    public ApiResponse<AttachmentView> upload(@PathVariable Long orderId,
                                               @RequestParam("file") MultipartFile file,
                                               @RequestParam(required = false) Long itemId) {
        return ApiResponse.ok(attachmentService.upload(orderId, itemId, file));
    }

    @GetMapping("/attachments/{id}/url")
    public ApiResponse<String> url(@PathVariable Long id,
                                   @RequestParam(defaultValue = "thumb") String type) {
        return ApiResponse.ok(attachmentService.presignedUrl(id, type));
    }
}
