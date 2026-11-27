package com.usedcar.platform.report;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.usedcar.platform.report.dto.ReportMarketRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface InspectionReportMapper extends BaseMapper<InspectionReport> {

    @Select("""
            <script>
            select r.report_no, r.version, r.total_score, r.grade, r.price_low, r.price_high, r.issued_at,
                   v.brand, v.model, v.plate_no, v.mileage
            from inspection_report r
            join vehicle v on v.id = r.vehicle_id
            where r.status = 'ACTIVE'
            <if test="keyword != null and keyword != ''">
              and (v.brand like concat('%', #{keyword}, '%')
                   or v.model like concat('%', #{keyword}, '%')
                   or v.plate_no like concat('%', #{keyword}, '%'))
            </if>
            <if test="grade != null and grade != ''">
              and r.grade = #{grade}
            </if>
            order by r.issued_at desc
            </script>
            """)
    List<ReportMarketRow> selectMarket(@Param("keyword") String keyword, @Param("grade") String grade);
}
