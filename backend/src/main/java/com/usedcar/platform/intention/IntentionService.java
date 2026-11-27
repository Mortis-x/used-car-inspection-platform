package com.usedcar.platform.intention;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.usedcar.platform.audit.AuditLogService;
import com.usedcar.platform.auth.AuthContext;
import com.usedcar.platform.auth.Role;
import com.usedcar.platform.auth.UserPrincipal;
import com.usedcar.platform.common.BizException;
import com.usedcar.platform.intention.dto.CreateIntentionRequest;
import com.usedcar.platform.intention.dto.IntentionRow;
import com.usedcar.platform.report.InspectionReport;
import com.usedcar.platform.report.InspectionReportMapper;
import com.usedcar.platform.vehicle.Vehicle;
import com.usedcar.platform.vehicle.VehicleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class IntentionService {

    private final IntentionOrderMapper intentionOrderMapper;
    private final InspectionReportMapper reportMapper;
    private final VehicleMapper vehicleMapper;
    private final AuditLogService auditLogService;

    public IntentionService(IntentionOrderMapper intentionOrderMapper,
                            InspectionReportMapper reportMapper,
                            VehicleMapper vehicleMapper,
                            AuditLogService auditLogService) {
        this.intentionOrderMapper = intentionOrderMapper;
        this.reportMapper = reportMapper;
        this.vehicleMapper = vehicleMapper;
        this.auditLogService = auditLogService;
    }

    public List<IntentionRow> list() {
        UserPrincipal principal = AuthContext.require();
        Long buyerId = Role.BUYER.name().equals(principal.role()) ? principal.id() : null;
        return intentionOrderMapper.selectRows(buyerId);
    }

    @Transactional
    public IntentionOrder create(CreateIntentionRequest request) {
        InspectionReport report = reportMapper.selectOne(Wrappers.<InspectionReport>lambdaQuery()
                .eq(InspectionReport::getReportNo, request.reportNo())
                .last("limit 1"));
        if (report == null || !"ACTIVE".equals(report.getStatus())) {
            throw new BizException("只有已生效报告可以发起购车意向");
        }
        if (request.offerPrice() == null || request.offerPrice() <= 0) {
            throw new BizException("出价必须大于 0");
        }
        Long duplicate = intentionOrderMapper.selectCount(Wrappers.<IntentionOrder>lambdaQuery()
                .eq(IntentionOrder::getReportId, report.getId())
                .eq(IntentionOrder::getBuyerId, AuthContext.userId())
                .in(IntentionOrder::getStatus, List.of("PENDING", "COUNTERED")));
        if (duplicate != null && duplicate > 0) {
            throw new BizException("你对该报告已有进行中的意向单");
        }

        IntentionOrder intention = new IntentionOrder();
        intention.setReportId(report.getId());
        intention.setBuyerId(AuthContext.userId());
        intention.setOfferPrice(request.offerPrice());
        intention.setStatus("PENDING");
        intention.setMessage(request.message());
        intentionOrderMapper.insert(intention);

        auditLogService.record("CREATE", "INTENTION", intention.getId(),
                "买家对报告 " + report.getReportNo() + " 出价 " + request.offerPrice() + " 分");
        return intention;
    }

    @Transactional
    public IntentionOrder counter(Long id, Long price) {
        IntentionOrder intention = require(id);
        requireDealer(intention);
        if (!List.of("PENDING", "COUNTERED").contains(intention.getStatus())) {
            throw new BizException("当前状态不支持还价");
        }
        if (price == null || price <= 0) {
            throw new BizException("还价必须大于 0");
        }
        intention.setDealerReplyPrice(price);
        intention.setStatus("COUNTERED");
        intentionOrderMapper.updateById(intention);
        auditLogService.record("UPDATE", "INTENTION", id, "车商还价 " + price + " 分");
        return intention;
    }

    @Transactional
    public IntentionOrder accept(Long id) {
        IntentionOrder intention = require(id);
        requireDealer(intention);
        if (!List.of("PENDING", "COUNTERED").contains(intention.getStatus())) {
            throw new BizException("当前状态不支持接受");
        }
        intention.setStatus("ACCEPTED");
        intentionOrderMapper.updateById(intention);
        auditLogService.record("UPDATE", "INTENTION", id, "车商接受意向");
        return intention;
    }

    @Transactional
    public IntentionOrder reject(Long id) {
        IntentionOrder intention = require(id);
        requireDealer(intention);
        if (!List.of("PENDING", "COUNTERED").contains(intention.getStatus())) {
            throw new BizException("当前状态不支持拒绝");
        }
        intention.setStatus("REJECTED");
        intentionOrderMapper.updateById(intention);
        auditLogService.record("UPDATE", "INTENTION", id, "车商拒绝意向");
        return intention;
    }

    /**
     * 买家接受车商还价：还价后必须由买家确认，意向单才真正成交。
     */
    @Transactional
    public IntentionOrder confirmCounter(Long id) {
        IntentionOrder intention = require(id);
        requireBuyerOwner(intention);
        if (!"COUNTERED".equals(intention.getStatus())) {
            throw new BizException("当前没有待确认的车商还价");
        }
        intention.setStatus("ACCEPTED");
        intentionOrderMapper.updateById(intention);
        auditLogService.record("UPDATE", "INTENTION", id, "买家接受车商还价，意向成交");
        return intention;
    }

    @Transactional
    public IntentionOrder declineCounter(Long id) {
        IntentionOrder intention = require(id);
        requireBuyerOwner(intention);
        if (!"COUNTERED".equals(intention.getStatus())) {
            throw new BizException("当前没有待确认的车商还价");
        }
        intention.setStatus("REJECTED");
        intentionOrderMapper.updateById(intention);
        auditLogService.record("UPDATE", "INTENTION", id, "买家拒绝车商还价");
        return intention;
    }

    @Transactional
    public IntentionOrder cancel(Long id) {
        IntentionOrder intention = require(id);
        if (!Objects.equals(intention.getBuyerId(), AuthContext.userId())) {
            throw new BizException(403, "只能取消自己的意向单");
        }
        if (!List.of("PENDING", "COUNTERED").contains(intention.getStatus())) {
            throw new BizException("当前状态不支持取消");
        }
        intention.setStatus("CANCELLED");
        intentionOrderMapper.updateById(intention);
        auditLogService.record("UPDATE", "INTENTION", id, "买家取消意向");
        return intention;
    }

    private IntentionOrder require(Long id) {
        IntentionOrder intention = intentionOrderMapper.selectById(id);
        if (intention == null) {
            throw new BizException(404, "意向单不存在");
        }
        return intention;
    }

    private void requireBuyerOwner(IntentionOrder intention) {
        if (!Objects.equals(intention.getBuyerId(), AuthContext.userId())) {
            throw new BizException(403, "只能操作自己的意向单");
        }
    }

    /**
     * 车商响应权限：车辆登记了归属车商时校验归属，未登记则任一车商可响应（便于演示）。
     */
    private void requireDealer(IntentionOrder intention) {
        UserPrincipal principal = AuthContext.require();
        if (principal.hasRole("OPERATOR", "ADMIN")) {
            return;
        }
        if (!Role.DEALER.name().equals(principal.role())) {
            throw new BizException(403, "只有车商可以响应意向单");
        }
        InspectionReport report = reportMapper.selectById(intention.getReportId());
        Vehicle vehicle = report == null ? null : vehicleMapper.selectById(report.getVehicleId());
        if (vehicle != null && vehicle.getDealerId() != null
                && !Objects.equals(vehicle.getDealerId(), principal.id())) {
            throw new BizException(403, "该车辆不属于你，无法响应");
        }
    }
}
