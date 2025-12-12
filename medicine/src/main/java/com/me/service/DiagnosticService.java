package com.me.service;

import com.me.pojo.Diagnostic;
import java.util.List;
import java.util.Map;

public interface DiagnosticService {
    Diagnostic queryDiagnosticById(int recordId);
    
    /**
     * 根据诊断结果查询相似的诊断记录
     * @param diagnosisText 诊断结果文本
     * @param limit 返回结果数量限制
     * @return 相似诊断列表，包含评分信息
     */
    List<Map<String, Object>> findSimilarDiagnosis(String diagnosisText, int limit);
}
