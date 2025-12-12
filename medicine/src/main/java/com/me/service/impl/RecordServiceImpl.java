package com.me.service.impl;

import com.me.mapper.DiagnosticMapper;
import com.me.mapper.RecordMapper;
import com.me.pojo.Diagnostic;
import com.me.pojo.Record;
import com.me.pojo.RecordDTO;
import com.me.pojo.RecordQueryDTO;
import com.me.service.RecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RecordServiceImpl implements RecordService {
    @Autowired
    private RecordMapper recordMapper;
    
    @Autowired
    private DiagnosticMapper diagnosticMapper;

    @Override
    public int add(Record record) {
        // 添加对 phone 字段的非空验证
        if (record.getPhone() == null || record.getPhone().trim().isEmpty()) {
            // 如果为空，可以设置一个默认值或抛出异常
            throw new IllegalArgumentException("手机号码不能为空");
        }
        return recordMapper.add(record);
    }

    @Override
    public Map<String, Object> getRecordList(RecordQueryDTO recordQueryDTO) {
        // 计算偏移量
        int offset = (recordQueryDTO.getPage() - 1) * recordQueryDTO.getPageSize();

        // 构建查询参数
        Map<String, Object> params = new HashMap<>();
        params.put("status", recordQueryDTO.getStatus());
        params.put("offset", offset);
        params.put("pageSize", recordQueryDTO.getPageSize());
        params.put("doctorId", recordQueryDTO.getDoctorId());
        params.put("patientId", recordQueryDTO.getPatientId());
        
        System.out.println("查询参数: " + params);

        // 查询数据
        List<RecordDTO> recordList = recordMapper.queryAll(params);
        int total = recordMapper.countRecordList(params);
        
        System.out.println("查询结果: 总数=" + total + ", 记录数=" + recordList.size());

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("recordList", recordList);
        return result;
    }

    @Override
    public Record queryRecordById(int recordId) {
        return recordMapper.queryRecordById(recordId);
    }

    @Override
    @Transactional
    public int update(Record record) {
        System.out.println("更新记录: recordId=" + record.getRecordId() + ", status=" + record.getStatus() + ", score=" + record.getScore());
        
        // 更新诊断信息
        if (record.getDiagnostic() != null && record.getStatus() == 2) {
            Diagnostic diagnostic = record.getDiagnostic();
            diagnostic.setRecordId(record.getRecordId());
            
            // 检查是否已存在诊断信息
            Diagnostic existingDiagnostic = diagnosticMapper.queryDiagnosticById(record.getRecordId());
            if (existingDiagnostic != null) {
                // 更新现有诊断
                diagnosticMapper.updateDiagnostic(diagnostic);
                System.out.println("更新现有诊断: recordId=" + record.getRecordId());
            } else {
                // 插入新诊断
                diagnosticMapper.insertDiagnostic(diagnostic);
                System.out.println("插入新诊断: recordId=" + record.getRecordId());
            }
        }
        
        int result = recordMapper.update(record);
        System.out.println("更新结果: " + result + " 行受影响");
        return result;
    }
}
