package com.tokenhunter.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tokenhunter.entity.UsageRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface UsageRecordMapper extends BaseMapper<UsageRecord> {

    @Select("SELECT COALESCE(SUM(total_tokens), 0) AS totalTokens, " +
            "COUNT(*) AS requestCount, " +
            "COUNT(DISTINCT model) AS modelCount " +
            "FROM usage_record " +
            "WHERE status = 0 " +
            "AND created_at >= CURDATE() " +
            "AND created_at < CURDATE() + INTERVAL 1 DAY")
    Map<String, Object> selectTodaySummary();
}
