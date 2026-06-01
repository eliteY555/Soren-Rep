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
@RequestMapping(value = "/record")
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
        Map<String, Object> result = recordService.getRecordList(recordQueryDTO);
        return Result.success(result);
    }

    /** 查询病历详情 — 附带身份校验 */
    @GetMapping("/{recordId}")
    public Result queryRecordById(@PathVariable("recordId") int recordId,
                                   @RequestParam(required = false) Integer currentUserId,
                                   @RequestParam(required = false) Integer role) {
        try {
            Record record = recordService.queryRecordById(recordId);
            if (record == null) {
                return Result.error("未找到记录");
            }

            // 身份校验：患者只能看自己的病历，医生只能看自己接诊的病历
            if (currentUserId != null && role != null) {
                if (role == 0 && !currentUserId.equals(record.getPatientId())) {
                    return Result.error("无权限查看该病历");
                }
                if (role == 1 && !currentUserId.equals(record.getDoctorId())) {
                    return Result.error("无权限查看该病历");
                }
            }

            Diagnostic diagnostic = diagnosticService.queryDiagnosticById(recordId);
            record.setDiagnostic(diagnostic);
            return Result.success(record);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取病历详情失败: " + e.getMessage());
        }
    }

    /** 更新病历 — 附带身份校验 */
    @PostMapping("/update")
    public Result update(@RequestBody Record record) {
        // 校验权限：只有接诊医生或患者本人可以更新
        if (record.getDoctorId() != null && record.getPatientId() != null) {
            Record existing = recordService.queryRecordById(record.getRecordId());
            if (existing != null && existing.getDoctorId() != null
                    && !record.getDoctorId().equals(existing.getDoctorId())) {
                return Result.error("无权限修改该病历");
            }
        }

        int i = recordService.update(record);
        if (i > 0) {
            return Result.success(true);
        } else {
            return Result.error("更新失败");
        }
    }

    /**
     * 根据诊断结果获取推荐的相似诊断
     */
    @PostMapping("/recommend")
    public Result getRecommendations(@RequestBody Map<String, Object> params) {
        try {
            String diagnosisText = (String) params.get("diagnosisText");
            if (diagnosisText == null || diagnosisText.trim().isEmpty()) {
                return Result.error("诊断结果不能为空");
            }

            Integer limit = params.get("limit") != null ?
                    Integer.parseInt(params.get("limit").toString()) : 5;

            List<Map<String, Object>> recommendations =
                    diagnosticService.findSimilarDiagnosis(diagnosisText, limit);

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
