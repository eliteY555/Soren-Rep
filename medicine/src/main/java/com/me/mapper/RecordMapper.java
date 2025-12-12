package com.me.mapper;

import com.me.pojo.Record;
import com.me.pojo.RecordDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface RecordMapper {
    // 新增病历
    int add(Record record);

    // 查询全部病历信息
    List<RecordDTO> queryAll(Map<String, Object> params);

    // 查询病历总数
    int countRecordList(Map<String, Object> params);

    // 查询病历详情
    Record queryRecordById(@Param("recordId") int recordId);

    // 更新病历
    int update(Record record);
}
