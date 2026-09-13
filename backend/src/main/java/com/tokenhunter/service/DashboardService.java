package com.tokenhunter.service;

import com.tokenhunter.mapper.UsageRecordMapper;
import org.springframework.stereotype.Service;

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
}
