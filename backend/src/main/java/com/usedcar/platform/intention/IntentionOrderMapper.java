package com.usedcar.platform.intention;

import com.usedcar.platform.intention.dto.IntentionRow;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface IntentionOrderMapper extends BaseMapper<IntentionOrder> {

    @Select("""
            <script>
            select i.id, i.report_id, i.buyer_id, i.offer_price, i.dealer_reply_price,
                   i.status, i.message, i.created_at, i.updated_at,
                   r.report_no, r.order_id, v.brand, v.model, v.plate_no, v.mileage,
                   b.real_name as buyer_name, d.real_name as dealer_name
            from intention_order i
            join inspection_report r on r.id = i.report_id
            join vehicle v on v.id = r.vehicle_id
            left join sys_user b on b.id = i.buyer_id
            left join sys_user d on d.id = v.dealer_id
            <where>
              <if test="buyerId != null">i.buyer_id = #{buyerId}</if>
            </where>
            order by i.id desc
            </script>
            """)
    List<IntentionRow> selectRows(@Param("buyerId") Long buyerId);
}
