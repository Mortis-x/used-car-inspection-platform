package com.usedcar.platform.evidence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ChainHeadMapper extends BaseMapper<ChainHead> {

    /**
     * 行级锁串行化链头读取：并发出证时保证 seq 连续、prev_hash 不断裂。
     */
    @Select("SELECT id, seq_no, last_hash, updated_at FROM chain_head WHERE id = 1 FOR UPDATE")
    ChainHead selectForUpdate();
}
