package com.tokenhunter.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tokenhunter.entity.UsageRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UsageRecordMapper extends BaseMapper<UsageRecord> {
}
