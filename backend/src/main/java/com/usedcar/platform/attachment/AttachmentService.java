package com.usedcar.platform.attachment;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.usedcar.platform.attachment.dto.AttachmentView;
import com.usedcar.platform.audit.AuditLogService;
import com.usedcar.platform.auth.AuthContext;
import com.usedcar.platform.auth.Role;
import com.usedcar.platform.auth.UserPrincipal;
import com.usedcar.platform.common.BizException;
import com.usedcar.platform.inspection.InspectionOrder;
import com.usedcar.platform.inspection.InspectionOrderMapper;
import com.usedcar.platform.inspection.OrderStatus;
import com.usedcar.platform.storage.ObjectStorageService;
import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadPoolExecutor;

@Service
public class AttachmentService {

    private static final Logger log = LoggerFactory.getLogger(AttachmentService.class);

    private final AttachmentMapper attachmentMapper;
    private final InspectionOrderMapper orderMapper;
    private final ObjectStorageService objectStorageService;
    private final AuditLogService auditLogService;
    private final ThreadPoolExecutor attachmentExecutor;

    public AttachmentService(AttachmentMapper attachmentMapper,
                             InspectionOrderMapper orderMapper,
                             ObjectStorageService objectStorageService,
                             AuditLogService auditLogService,
                             @Qualifier("attachmentExecutor") ThreadPoolExecutor attachmentExecutor) {
        this.attachmentMapper = attachmentMapper;
        this.orderMapper = orderMapper;
        this.objectStorageService = objectStorageService;
        this.auditLogService = auditLogService;
        this.attachmentExecutor = attachmentExecutor;
    }

    public List<AttachmentView> list(Long orderId) {
        List<Attachment> attachments = attachmentMapper.selectList(Wrappers.<Attachment>lambdaQuery()
                .eq(Attachment::getOrderId, orderId)
                .orderByAsc(Attachment::getId));
        return attachments.stream().map(this::toView).toList();
    }

    public AttachmentView upload(Long orderId, Long itemId, MultipartFile file) {
        InspectionOrder order = requireEditableOrder(orderId);
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择要上传的文件");
        }

        String fileName = file.getOriginalFilename() == null ? "attachment" : file.getOriginalFilename();
        String contentType = file.getContentType();

        Path tempFile = null;
        String hash;
        try {
            tempFile = Files.createTempFile("car-attach-", ".upload");
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = new DigestInputStream(file.getInputStream(), digest);
                 OutputStream output = Files.newOutputStream(tempFile)) {
                input.transferTo(output);
            }
            hash = HexFormat.of().formatHex(digest.digest());
        } catch (Exception e) {
            throw new BizException("附件接收失败: " + e.getMessage());
        }

        Attachment attachment = new Attachment();
        attachment.setOrderId(orderId);
        attachment.setItemId(itemId);
        attachment.setFileName(fileName);
        attachment.setContentType(contentType);
        attachment.setFileSize(file.getSize());
        attachment.setOriginHash(hash);
        attachment.setStatus("PENDING");
        attachment.setRetryCount(0);
        attachmentMapper.insert(attachment);

        Path taskFile = tempFile;
        attachmentExecutor.execute(() -> process(attachment.getId(), orderId, taskFile, contentType));
        auditLogService.record("UPLOAD", "ATTACHMENT", attachment.getId(),
                "上传附件 " + fileName + "（" + file.getSize() + " 字节）");
        return toView(attachment);
    }

    public String presignedUrl(Long attachmentId, String type) {
        Attachment attachment = attachmentMapper.selectById(attachmentId);
        if (attachment == null) {
            throw new BizException(404, "附件不存在");
        }
        String path = "thumb".equalsIgnoreCase(type) ? attachment.getThumbPath() : attachment.getOriginPath();
        if (path == null) {
            throw new BizException("该附件尚未生成可访问文件");
        }
        return objectStorageService.presignedUrl(path, 60);
    }

    /**
     * 异步任务：压缩原图、生成缩略图并上传对象存储。
     */
    private void process(Long attachmentId, Long orderId, Path tempFile, String contentType) {
        Attachment attachment = new Attachment();
        attachment.setId(attachmentId);
        attachment.setStatus("PROCESSING");
        attachmentMapper.updateById(attachment);

        try {
            String base = "orders/" + orderId + "/" + attachmentId;
            String originPath;
            String thumbPath = null;

            boolean isImage = contentType != null && contentType.startsWith("image/");
            BufferedImage image = isImage ? ImageIO.read(tempFile.toFile()) : null;

            if (image != null) {
                byte[] originBytes = compress(image);
                originPath = base + "_origin.jpg";
                objectStorageService.upload(originPath, new ByteArrayInputStream(originBytes),
                        originBytes.length, "image/jpeg");

                byte[] thumbBytes = thumbnail(image);
                thumbPath = base + "_thumb.jpg";
                objectStorageService.upload(thumbPath, new ByteArrayInputStream(thumbBytes),
                        thumbBytes.length, "image/jpeg");
            } else {
                originPath = base + "_origin" + extensionOf(contentType);
                long size = Files.size(tempFile);
                try (InputStream input = Files.newInputStream(tempFile)) {
                    objectStorageService.upload(originPath, input, size, contentType);
                }
            }

            Attachment done = new Attachment();
            done.setId(attachmentId);
            done.setStatus("DONE");
            done.setOriginPath(originPath);
            done.setThumbPath(thumbPath);
            attachmentMapper.updateById(done);
        } catch (Exception e) {
            log.warn("附件处理失败 id={}: {}", attachmentId, e.getMessage());
            Attachment failed = new Attachment();
            failed.setId(attachmentId);
            failed.setStatus("FAILED");
            failed.setErrorMsg(truncate(e.getMessage()));
            attachmentMapper.updateById(failed);
        } finally {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ignored) {
                // 临时文件清理失败不影响业务
            }
        }
    }

    private byte[] compress(BufferedImage image) throws IOException {
        double scale = Math.min(1.0, 1920.0 / Math.max(image.getWidth(), image.getHeight()));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Thumbnails.of(image).scale(scale).outputFormat("jpg").outputQuality(0.8).toOutputStream(output);
        return output.toByteArray();
    }

    private byte[] thumbnail(BufferedImage image) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Thumbnails.of(image).size(320, 320).outputFormat("jpg").outputQuality(0.75).toOutputStream(output);
        return output.toByteArray();
    }

    private String extensionOf(String contentType) {
        if (contentType == null) {
            return ".bin";
        }
        if (contentType.contains("mp4")) {
            return ".mp4";
        }
        if (contentType.contains("quicktime")) {
            return ".mov";
        }
        if (contentType.contains("png")) {
            return ".png";
        }
        return ".bin";
    }

    private InspectionOrder requireEditableOrder(Long orderId) {
        InspectionOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(404, "检测单不存在");
        }
        if (!OrderStatus.INSPECTING.name().equals(order.getStatus())) {
            throw new BizException("只有检测中的检测单可以上传附件");
        }
        UserPrincipal principal = AuthContext.require();
        boolean allowed = principal.hasRole(Role.ADMIN.name())
                || Objects.equals(order.getInspectorId(), principal.id());
        if (!allowed) {
            throw new BizException(403, "只有被指派的检测师可以上传附件");
        }
        return order;
    }

    private AttachmentView toView(Attachment attachment) {
        return new AttachmentView(
                attachment.getId(),
                attachment.getItemId(),
                attachment.getFileName(),
                attachment.getContentType(),
                attachment.getFileSize(),
                attachment.getStatus(),
                attachment.getOriginHash(),
                safeUrl(attachment.getOriginPath()),
                safeUrl(attachment.getThumbPath()),
                attachment.getErrorMsg(),
                attachment.getCreatedAt());
    }

    private String safeUrl(String path) {
        if (path == null) {
            return null;
        }
        try {
            return objectStorageService.presignedUrl(path, 60);
        } catch (Exception e) {
            return null;
        }
    }

    private String truncate(String message) {
        if (message == null) {
            return "未知错误";
        }
        return message.length() > 400 ? message.substring(0, 400) : message;
    }
}
