package com.me.service.impl;

import com.me.mapper.DiagnosticMapper;
import com.me.mapper.DoctorMapper;
import com.me.mapper.RecordMapper;
import com.me.pojo.Diagnostic;
import com.me.pojo.Doctor;
import com.me.service.DiagnosticService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DiagnosticServiceImpl implements DiagnosticService {
    @Autowired
    DiagnosticMapper diagnosticMapper;
    
    @Autowired
    DoctorMapper doctorMapper;
    
    @Autowired
    RecordMapper recordMapper;

    @Override
    public Diagnostic queryDiagnosticById(int recordId) {
        return diagnosticMapper.queryDiagnosticById(recordId);
    }
    
    @Override
    public List<Map<String, Object>> findSimilarDiagnosis(String diagnosisText, int limit) {
        if (diagnosisText == null || diagnosisText.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        // 获取所有诊断记录
        List<Map<String, Object>> allDiagnostics = diagnosticMapper.getAllDiagnosticsWithDetails();
        if (allDiagnostics == null || allDiagnostics.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 对诊断文本进行预处理（转为小写、去除标点符号等）
        String processedQuery = preprocessText(diagnosisText);
        
        // 提取关键词
        Set<String> queryKeywords = extractKeywords(processedQuery);
        
        // 计算相似度并排序
        List<Map<String, Object>> sortedResults = allDiagnostics.stream()
            .filter(diagnostic -> diagnostic.get("result") != null)
            .map(diagnostic -> {
                String resultText = (String) diagnostic.get("result");
                String processedResult = preprocessText(resultText);
                
                // 计算相似度分数
                double similarityScore = calculateSimilarity(queryKeywords, extractKeywords(processedResult));
                
                // 添加相似度分数到结果中
                diagnostic.put("similarityScore", similarityScore);
                return diagnostic;
            })
            .filter(diagnostic -> (double) diagnostic.get("similarityScore") > 0.1) // 过滤掉相似度太低的结果
            .sorted((d1, d2) -> {
                // 首先按相似度排序
                double score1 = (double) d1.get("similarityScore");
                double score2 = (double) d2.get("similarityScore");
                
                int scoreCompare = Double.compare(score2, score1); // 降序
                
                // 如果相似度相同，则按患者评分排序
                if (scoreCompare == 0) {
                    // 修复类型转换错误：处理可能是Float或Double的情况
                    Number patientScore1 = (Number) d1.get("score");
                    Number patientScore2 = (Number) d2.get("score");
                    
                    // 处理可能为null的情况，并统一转换为double
                    double score1Value = patientScore1 == null ? 0.0 : patientScore1.doubleValue();
                    double score2Value = patientScore2 == null ? 0.0 : patientScore2.doubleValue();
                    
                    return Double.compare(score2Value, score1Value); // 降序
                }
                
                return scoreCompare;
            })
            .limit(limit)
            .collect(Collectors.toList());
            
        return sortedResults;
    }
    
    /**
     * 对文本进行预处理
     */
    private String preprocessText(String text) {
        if (text == null) return "";
        // 转为小写
        String processed = text.toLowerCase();
        // 去除标点符号
        processed = processed.replaceAll("[\\p{P}\\p{S}]", " ");
        // 去除多余空格
        processed = processed.replaceAll("\\s+", " ").trim();
        return processed;
    }
    
    /**
     * 提取关键词（简单实现，将文本分割为词语）
     */
    private Set<String> extractKeywords(String text) {
        if (text == null || text.isEmpty()) {
            return new HashSet<>();
        }
        
        // 分词（简单实现，按空格分割）
        String[] words = text.split("\\s+");
        
        // 过滤掉停用词和太短的词
        return Arrays.stream(words)
            .filter(word -> word.length() > 1 && !isStopWord(word))
            .collect(Collectors.toSet());
    }
    
    /**
     * 判断是否为停用词（简单实现）
     */
    private boolean isStopWord(String word) {
        // 常见中文停用词
        Set<String> stopWords = new HashSet<>(Arrays.asList(
            "的", "了", "和", "与", "及", "或", "而", "但", "所", "以",
            "于", "之", "则", "为", "在", "由", "这", "那", "是", "有"
        ));
        return stopWords.contains(word);
    }
    
    /**
     * 计算两组关键词的相似度（使用Jaccard相似系数）
     */
    private double calculateSimilarity(Set<String> keywords1, Set<String> keywords2) {
        if (keywords1.isEmpty() || keywords2.isEmpty()) {
            return 0.0;
        }
        
        // 计算交集大小
        Set<String> intersection = new HashSet<>(keywords1);
        intersection.retainAll(keywords2);
        
        // 计算并集大小
        Set<String> union = new HashSet<>(keywords1);
        union.addAll(keywords2);
        
        // Jaccard相似系数 = 交集大小 / 并集大小
        return (double) intersection.size() / union.size();
    }
}
