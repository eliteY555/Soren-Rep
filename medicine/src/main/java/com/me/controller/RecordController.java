package com.me.controller;

import com.me.common.Result;
import com.me.pojo.Diagnostic;
import com.me.pojo.Record;
import com.me.pojo.RecordQueryDTO;
import com.me.service.DiagnosticService;
import com.me.service.RecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value="/record")
public class RecordController {
    @Autowired
    private RecordService recordService;
    @Autowired
    private DiagnosticService diagnosticService;

    @PostMapping("/add")
    public Result add(@RequestBody Record record) {
        try {
            int i = recordService.add(record);
            if (i > 0) {
                return Result.success(true);
            } else {
                return Result.error("病历创建失败");
            }
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            return Result.error("病历创建失败: " + e.getMessage());
        }
    }

    @PostMapping("/page")
    public Result getRecordList(@RequestBody RecordQueryDTO recordQueryDTO) {
        System.out.println("获取病历列表请求参数: " + recordQueryDTO);
        Map<String, Object> result = recordService.getRecordList(recordQueryDTO);
        return Result.success(result);
    }

    @GetMapping("/{recordId}")
    public Result queryRecordById(@PathVariable("recordId") int recordId) {
        try {
            Record record = recordService.queryRecordById(recordId);
            if (record == null) {
                return Result.error("未找到记录");
            }
            Diagnostic diagnostic = diagnosticService.queryDiagnosticById(recordId);
            record.setDiagnostic(diagnostic);
            return Result.success(record);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取病历详情失败: " + e.getMessage());
        }
    }

    @PostMapping("/update")
    public Result update(@RequestBody Record record) {
        int i = recordService.update(record);
        if (i > 0) {
            return Result.success(true);
        } else {
            return Result.error("更新失败");
        }
    }
    
    /**
     * 根据诊断结果获取推荐的相似诊断
     * @param params 包含诊断结果文本的参数
     * @return 推荐的诊断列表
     */
    @PostMapping("/recommend")
    public Result getRecommendations(@RequestBody Map<String, Object> params) {
        try {
            String diagnosisText = (String) params.get("diagnosisText");
            if (diagnosisText == null || diagnosisText.trim().isEmpty()) {
                return Result.error("诊断结果不能为空");
            }
            
            // 默认返回最多5条推荐
            Integer limit = params.get("limit") != null ? 
                            Integer.parseInt(params.get("limit").toString()) : 5;
            
            // 调用服务获取推荐列表
            List<Map<String, Object>> recommendations = 
                diagnosticService.findSimilarDiagnosis(diagnosisText, limit);
            
            // 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("recommendations", recommendations);
            result.put("total", recommendations.size());
            
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取推荐失败: " + e.getMessage());
        }
    }
}
