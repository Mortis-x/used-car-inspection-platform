package com.usedcar.platform.dispute;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.usedcar.platform.dispute.dto.DisputeRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DisputeCaseMapper extends BaseMapper<DisputeCase> {

    @Select("""
            <script>
            select d.id, d.report_no, d.order_id, d.applicant_id, d.applicant_role,
                   d.reason, d.description, d.status, d.result, d.conclusion,
                   d.arbitrator_id, d.created_at, d.updated_at,
                   v.brand, v.model, v.plate_no,
                   u.real_name as applicant_name, a.real_name as arbitrator_name
            from dispute_case d
            join inspection_report r on r.id = d.report_id
            join vehicle v on v.id = r.vehicle_id
            left join sys_user u on u.id = d.applicant_id
            left join sys_user a on a.id = d.arbitrator_id
            <where>
              <if test="status != null and status != ''">d.status = #{status}</if>
              <if test="applicantId != null">d.applicant_id = #{applicantId}</if>
            </where>
            order by d.id desc
            </script>
            """)
    List<DisputeRow> selectRows(@Param("status") String status, @Param("applicantId") Long applicantId);
}
