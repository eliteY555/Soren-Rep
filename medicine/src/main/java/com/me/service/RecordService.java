package com.me.service;

import com.me.pojo.Record;
import com.me.pojo.RecordQueryDTO;

import java.util.Map;

public interface RecordService {
    int add(Record record);

    // 分页查询病历列表
    Map<String, Object> getRecordList(RecordQueryDTO recordQueryDTO);

    Record queryRecordById(int recordId);

    int update(Record record);
}
