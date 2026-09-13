package com.tokenhunter.service;

import com.tokenhunter.mapper.UsageRecordMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final UsageRecordMapper usageRecordMapper;

    public DashboardService(UsageRecordMapper usageRecordMapper) {
        this.usageRecordMapper = usageRecordMapper;
    }

    public Map<String, Object> getTodaySummary() {
        return usageRecordMapper.selectTodaySummary();
    }

    public List<Map<String, Object>> getDailyTrend(int days) {
        List<Map<String, Object>> sqlResult = usageRecordMapper.selectDailyTrend(days);

        // Build a lookup map from SQL result: LocalDate -> totalTokens
        Map<LocalDate, Long> lookup = new LinkedHashMap<>();
        for (Map<String, Object> row : sqlResult) {
            LocalDate day;
            Object dayObj = row.get("day");
            if (dayObj instanceof java.sql.Date) {
                day = ((java.sql.Date) dayObj).toLocalDate();
            } else if (dayObj instanceof LocalDate) {
                day = (LocalDate) dayObj;
            } else {
                day = LocalDate.parse(row.get("day").toString());
            }

            long totalTokens;
            Object tokensObj = row.get("totalTokens");
            if (tokensObj instanceof Long) {
                totalTokens = (Long) tokensObj;
            } else if (tokensObj instanceof Integer) {
                totalTokens = ((Integer) tokensObj).longValue();
            } else if (tokensObj instanceof java.math.BigInteger) {
                totalTokens = ((java.math.BigInteger) tokensObj).longValue();
            } else {
                totalTokens = Long.parseLong(tokensObj.toString());
            }
            lookup.put(day, totalTokens);
        }

        // Fill the complete date sequence
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            long tokens = lookup.getOrDefault(date, 0L);
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("day", date.format(DateTimeFormatter.ISO_LOCAL_DATE));
            entry.put("totalTokens", tokens);
            result.add(entry);
        }
        return result;
    }

    /**
     * Return model usage with percentage of total tokens.
     * percentage = Math.round(tokens * 10000.0 / total) / 100.0  (2 decimal places).
     * If total == 0, all percentages are 0.0 to avoid division by zero.
     */
    public List<Map<String, Object>> getModelUsage() {
        List<Map<String, Object>> sqlResult = usageRecordMapper.selectModelUsage();

        // 先算总数，用于计算百分比
        long total = 0;
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map<String, Object> row : sqlResult) {
            Object tokensObj = row.get("totalTokens");
            long tokens;
            if (tokensObj instanceof Long) {
                tokens = (Long) tokensObj;
            } else if (tokensObj instanceof Integer) {
                tokens = ((Integer) tokensObj).longValue();
            } else if (tokensObj instanceof java.math.BigInteger) {
                tokens = ((java.math.BigInteger) tokensObj).longValue();
            } else {
                tokens = Long.parseLong(tokensObj.toString());
            }
            total += tokens;

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("model", row.get("model"));
            entry.put("totalTokens", tokens);
            entry.put("percentage", 0.0); // 占位，后面更新
            rows.add(entry);
        }

        // 计算百分比
        for (Map<String, Object> row : rows) {
            long tokens = (Long) row.get("totalTokens");
            double percentage = (total == 0) ? 0.0 : Math.round(tokens * 10000.0 / total) / 100.0;
            row.put("percentage", percentage);
        }

        return rows;
    }
}
