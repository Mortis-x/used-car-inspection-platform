package com.usedcar.platform.inspection.dto;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.usedcar.platform.inspection.InspectionItem;
import com.usedcar.platform.inspection.InspectionOrder;
import com.usedcar.platform.vehicle.Vehicle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderQueryMapper {

    @Select("""
            <script>
            select o.id, o.order_no, o.vehicle_id, o.template_id, o.inspector_id, o.dealer_id,
                   o.status, o.remark, o.withdraw_reason, o.submitted_at, o.issued_at, o.created_at,
                   v.brand, v.model, v.plate_no, v.vin,
                   u.real_name as inspector_name,
                   r.report_no
            from inspection_order o
            join vehicle v on v.id = o.vehicle_id
            left join sys_user u on u.id = o.inspector_id
            left join inspection_report r on r.order_id = o.id and r.status = 'ACTIVE'
            where 1 = 1
            <if test="status != null and status != ''">
              and o.status = #{status}
            </if>
            <if test="vehicleId != null">
              and o.vehicle_id = #{vehicleId}
            </if>
            <if test="inspectorId != null">
              and o.inspector_id = #{inspectorId}
            </if>
            <if test="keyword != null and keyword != ''">
              and (o.order_no like concat('%', #{keyword}, '%')
                or v.vin like concat('%', #{keyword}, '%')
                or v.plate_no like concat('%', #{keyword}, '%')
                or v.brand like concat('%', #{keyword}, '%')
                or v.model like concat('%', #{keyword}, '%'))
            </if>
            order by o.id desc
            </script>
            """)
    Page<OrderRow> selectOrderPage(Page<OrderRow> page,
                                   @Param("status") String status,
                                   @Param("vehicleId") Long vehicleId,
                                   @Param("inspectorId") Long inspectorId,
                                   @Param("keyword") String keyword);

    @Select("""
            <script>
            select o.id, o.order_no, o.vehicle_id, o.template_id, o.inspector_id, o.dealer_id,
                   o.status, o.remark, o.withdraw_reason, o.submitted_at, o.issued_at, o.created_at,
                   v.brand, v.model, v.plate_no, v.vin,
                   u.real_name as inspector_name,
                   r.report_no
            from inspection_order o
            join vehicle v on v.id = o.vehicle_id
            left join sys_user u on u.id = o.inspector_id
            left join inspection_report r on r.order_id = o.id and r.status = 'ACTIVE'
            where o.vehicle_id = #{vehicleId}
            order by o.id desc
            </script>
            """)
    List<OrderRow> selectByVehicle(@Param("vehicleId") Long vehicleId);
}
