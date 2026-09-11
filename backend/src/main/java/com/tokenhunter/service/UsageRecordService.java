package com.tokenhunter.service;

import com.tokenhunter.entity.UsageRecord;
import com.tokenhunter.mapper.UsageRecordMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsageRecordService {

    private final UsageRecordMapper usageRecordMapper;

    public UsageRecordService(UsageRecordMapper usageRecordMapper) {
        this.usageRecordMapper = usageRecordMapper;
    }

    public void save(UsageRecord record) {
        usageRecordMapper.insert(record);
    }

    public List<UsageRecord> listAll() {
        return usageRecordMapper.selectList(null);
    }
}
