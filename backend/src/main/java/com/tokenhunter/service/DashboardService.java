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
}
