package com.me.mapper;

import com.me.pojo.Diagnostic;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface DiagnosticMapper {
    Diagnostic queryDiagnosticById(int recordId);
    
    int updateDiagnostic(Diagnostic diagnostic);
    
    int insertDiagnostic(Diagnostic diagnostic);
    
    /**
     * 获取所有诊断记录及其详细信息（包括医生信息、评分等）
     * @return 诊断记录列表，每条记录包含诊断信息、医生信息、评分等
     */
    List<Map<String, Object>> getAllDiagnosticsWithDetails();
}
