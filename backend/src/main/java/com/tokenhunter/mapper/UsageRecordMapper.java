package com.tokenhunter.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tokenhunter.entity.UsageRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
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

    @Select("SELECT DATE(created_at) AS day, COALESCE(SUM(total_tokens), 0) AS totalTokens " +
            "FROM usage_record " +
            "WHERE status = 0 " +
            "AND created_at >= CURDATE() - INTERVAL #{days} DAY " +
            "AND created_at < CURDATE() + INTERVAL 1 DAY " +
            "GROUP BY DATE(created_at) " +
            "ORDER BY day ASC")
    List<Map<String, Object>> selectDailyTrend(@Param("days") int days);

    @Select("SELECT model, COALESCE(SUM(total_tokens), 0) AS totalTokens " +
            "FROM usage_record " +
            "WHERE status = 0 " +
            "GROUP BY model " +
            "ORDER BY totalTokens DESC")
    List<Map<String, Object>> selectModelUsage();

    @Select("SELECT provider, COALESCE(SUM(total_tokens), 0) AS totalTokens " +
            "FROM usage_record " +
            "WHERE status = 0 " +
            "GROUP BY provider " +
            "ORDER BY totalTokens DESC")
    List<Map<String, Object>> selectProviderUsage();
}
