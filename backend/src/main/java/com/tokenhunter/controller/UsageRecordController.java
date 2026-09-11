package com.tokenhunter.controller;

import com.tokenhunter.entity.UsageRecord;
import com.tokenhunter.service.UsageRecordService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/usage-records")
public class UsageRecordController {

    private final UsageRecordService usageRecordService;

    public UsageRecordController(UsageRecordService usageRecordService) {
        this.usageRecordService = usageRecordService;
    }

    @PostMapping
    public void addRecord(@RequestBody UsageRecord record) {
        usageRecordService.save(record);
    }

    @GetMapping
    public List<UsageRecord> listRecords() {
        return usageRecordService.listAll();
    }
}
