package com.usedcar.platform.approval;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.usedcar.platform.approval.dto.ApprovalRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ApprovalTaskMapper extends BaseMapper<ApprovalTask> {

    @Select("""
            <script>
            select t.id, t.biz_type, t.biz_id, t.order_id, t.status,
                   t.apply_user_id, t.apply_time, t.apply_reason,
                   t.approve_user_id, t.approve_time, t.opinion,
                   o.order_no, v.brand, v.model, v.plate_no,
                   r.report_no, r.total_score, r.grade,
                   u.real_name as apply_name
            from approval_task t
            join inspection_order o on o.id = t.order_id
            join vehicle v on v.id = o.vehicle_id
            left join inspection_report r on r.id = t.biz_id
            left join sys_user u on u.id = t.apply_user_id
            <where>
              <if test="status != null and status != ''">
                t.status = #{status}
              </if>
            </where>
            order by t.id desc
            </script>
            """)
    List<ApprovalRow> selectRows(@Param("status") String status);
}
