package com.usedcar.platform.inspection;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.usedcar.platform.audit.AuditLogService;
import com.usedcar.platform.auth.AuthContext;
import com.usedcar.platform.auth.Role;
import com.usedcar.platform.auth.User;
import com.usedcar.platform.auth.UserMapper;
import com.usedcar.platform.auth.UserPrincipal;
import com.usedcar.platform.common.BizException;
import com.usedcar.platform.inspection.dto.CreateOrderRequest;
import com.usedcar.platform.inspection.dto.ItemUpdateRequest;
import com.usedcar.platform.inspection.dto.OrderDetail;
import com.usedcar.platform.inspection.dto.OrderQueryMapper;
import com.usedcar.platform.inspection.dto.OrderRow;
import com.usedcar.platform.template.InspectionTemplateItem;
import com.usedcar.platform.template.TemplateService;
import com.usedcar.platform.vehicle.Vehicle;
import com.usedcar.platform.vehicle.VehicleMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class InspectionOrderService {

    /** 缺陷等级对应的默认扣分：无 / 轻微 / 一般 / 严重 */
    private static final int[] LEVEL_DEDUCT = {0, 3, 6, 10};

    private final InspectionOrderMapper orderMapper;
    private final InspectionItemMapper itemMapper;
    private final OrderQueryMapper orderQueryMapper;
    private final VehicleMapper vehicleMapper;
    private final UserMapper userMapper;
    private final TemplateService templateService;
    private final AuditLogService auditLogService;

    public InspectionOrderService(InspectionOrderMapper orderMapper,
                                  InspectionItemMapper itemMapper,
                                  OrderQueryMapper orderQueryMapper,
                                  VehicleMapper vehicleMapper,
                                  UserMapper userMapper,
                                  TemplateService templateService,
                                  AuditLogService auditLogService) {
        this.orderMapper = orderMapper;
        this.itemMapper = itemMapper;
        this.orderQueryMapper = orderQueryMapper;
        this.vehicleMapper = vehicleMapper;
        this.userMapper = userMapper;
        this.templateService = templateService;
        this.auditLogService = auditLogService;
    }

    public Page<OrderRow> page(String status, String keyword, Long vehicleId, long pageNum, long pageSize) {
        UserPrincipal principal = AuthContext.require();
        Long inspectorId = Role.INSPECTOR.name().equals(principal.role()) ? principal.id() : null;
        return orderQueryMapper.selectOrderPage(new Page<>(pageNum, pageSize),
                status, vehicleId, inspectorId, keyword);
    }

    public List<OrderRow> listByVehicle(Long vehicleId) {
        return orderQueryMapper.selectByVehicle(vehicleId);
    }

    public OrderDetail detail(Long id) {
        InspectionOrder order = requireOrder(id);
        Vehicle vehicle = vehicleMapper.selectById(order.getVehicleId());
        List<InspectionItem> items = itemMapper.selectList(Wrappers.<InspectionItem>lambdaQuery()
                .eq(InspectionItem::getOrderId, id)
                .orderByAsc(InspectionItem::getSort));
        return new OrderDetail(order, vehicle, items, OrderStatus.labelOf(order.getStatus()));
    }

    @Transactional
    public InspectionOrder create(CreateOrderRequest request) {
        UserPrincipal principal = AuthContext.require();
        Vehicle vehicle = vehicleMapper.selectById(request.vehicleId());
        if (vehicle == null) {
            throw new BizException(404, "车辆不存在，请先建档");
        }
        templateService.get(request.templateId());
        User inspector = userMapper.selectById(request.inspectorId());
        if (inspector == null || !Role.INSPECTOR.name().equals(inspector.getRole())) {
            throw new BizException("请选择有效的检测师");
        }

        InspectionOrder order = new InspectionOrder();
        order.setOrderNo(nextOrderNo());
        order.setVehicleId(request.vehicleId());
        order.setTemplateId(request.templateId());
        order.setInspectorId(request.inspectorId());
        order.setDealerId(Role.DEALER.name().equals(principal.role()) ? principal.id() : null);
        order.setStatus(OrderStatus.PENDING.name());
        order.setRemark(request.remark());
        order.setCreatedBy(principal.id());
        insertWithOrderNoRetry(order);

        List<InspectionTemplateItem> templateItems = templateService.items(request.templateId());
        if (templateItems.isEmpty()) {
            throw new BizException("检测模板下没有检测项，无法创建检测单");
        }
        for (InspectionTemplateItem templateItem : templateItems) {
            InspectionItem item = new InspectionItem();
            item.setOrderId(order.getId());
            item.setTemplateItemId(templateItem.getId());
            item.setCategory(templateItem.getCategory());
            item.setItemCode(templateItem.getItemCode());
            item.setItemName(templateItem.getItemName());
            item.setWeight(templateItem.getWeight());
            item.setSort(templateItem.getSort());
            itemMapper.insert(item);
        }

        auditLogService.record("CREATE", "ORDER", order.getId(),
                "创建检测单 " + order.getOrderNo() + "，共 " + templateItems.size() + " 个检测项");
        return order;
    }

    @Transactional
    public InspectionOrder start(Long id) {
        InspectionOrder order = requireOrder(id);
        requireAssignee(order);
        if (!OrderStatus.PENDING.name().equals(order.getStatus())) {
            throw new BizException("只有待检测状态可以开始检测");
        }
        order.setStatus(OrderStatus.INSPECTING.name());
        orderMapper.updateById(order);
        auditLogService.record("START", "ORDER", id, "开始检测");
        return order;
    }

    @Transactional
    public InspectionOrder submit(Long id) {
        InspectionOrder order = requireOrder(id);
        requireAssignee(order);
        if (!OrderStatus.INSPECTING.name().equals(order.getStatus())) {
            throw new BizException("只有检测中的检测单可以提交");
        }
        Long unfinished = itemMapper.selectCount(Wrappers.<InspectionItem>lambdaQuery()
                .eq(InspectionItem::getOrderId, id)
                .and(wrapper -> wrapper.isNull(InspectionItem::getScore)
                        .or().isNull(InspectionItem::getDefectLevel)));
        if (unfinished != null && unfinished > 0) {
            throw new BizException("还有 " + unfinished + " 个检测项未填写完整，不能提交");
        }
        order.setStatus(OrderStatus.PENDING_REPORT.name());
        order.setSubmittedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        auditLogService.record("SUBMIT", "ORDER", id, "提交检测结果");
        return order;
    }

    @Transactional
    public InspectionOrder withdraw(Long id, String reason) {
        InspectionOrder order = requireOrder(id);
        UserPrincipal principal = AuthContext.require();
        boolean allowed = principal.hasRole(Role.OPERATOR.name(), Role.ADMIN.name())
                || Objects.equals(order.getDealerId(), principal.id())
                || Objects.equals(order.getInspectorId(), principal.id());
        if (!allowed) {
            throw new BizException(403, "无权撤回该检测单");
        }
        if (OrderStatus.ISSUED.name().equals(order.getStatus())
                || OrderStatus.WITHDRAWN.name().equals(order.getStatus())) {
            throw new BizException("已出具报告或已撤回的检测单不能再撤回");
        }
        order.setStatus(OrderStatus.WITHDRAWN.name());
        order.setWithdrawReason(reason);
        orderMapper.updateById(order);
        auditLogService.record("WITHDRAW", "ORDER", id, "撤回检测单：" + reason);
        return order;
    }

    /**
     * 检测项录入：只允许被指派的检测师在检测中状态下修改。
     * 缺项校验在提交时统一执行，这里逐项保存便于前端草稿暂存。
     */
    @Transactional
    public InspectionItem updateItem(Long orderId, Long itemId, ItemUpdateRequest request) {
        InspectionOrder order = requireOrder(orderId);
        requireAssignee(order);
        if (!OrderStatus.INSPECTING.name().equals(order.getStatus())) {
            throw new BizException("只有检测中状态可以录入检测项");
        }
        InspectionItem item = itemMapper.selectById(itemId);
        if (item == null || !Objects.equals(item.getOrderId(), orderId)) {
            throw new BizException(404, "检测项不存在");
        }
        if (request.score() != null && (request.score() < 0 || request.score() > 100)) {
            throw new BizException("评分必须在 0 到 100 之间");
        }
        if (request.defectLevel() != null && (request.defectLevel() < 0 || request.defectLevel() > 3)) {
            throw new BizException("缺陷等级不合法");
        }

        item.setScore(request.score());
        item.setDefectLevel(request.defectLevel());
        item.setRemark(request.remark());
        item.setDefectDeduct(request.defectLevel() == null ? null : LEVEL_DEDUCT[request.defectLevel()]);
        itemMapper.updateById(item);
        return item;
    }

    private InspectionOrder requireOrder(Long id) {
        InspectionOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw new BizException(404, "检测单不存在");
        }
        return order;
    }

    private void requireAssignee(InspectionOrder order) {
        UserPrincipal principal = AuthContext.require();
        boolean allowed = principal.hasRole(Role.ADMIN.name())
                || Objects.equals(order.getInspectorId(), principal.id());
        if (!allowed) {
            throw new BizException(403, "只有被指派的检测师可以执行该操作");
        }
    }

    private void insertWithOrderNoRetry(InspectionOrder order) {
        for (int attempt = 0; attempt < 5; attempt++) {
            try {
                orderMapper.insert(order);
                return;
            } catch (DuplicateKeyException e) {
                order.setOrderNo(nextOrderNo());
            }
        }
        throw new BizException("检测单号生成失败，请重试");
    }

    private String nextOrderNo() {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        int random = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return "IO" + date + random;
    }
}
