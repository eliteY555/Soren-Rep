package com.voiceassistant.web.controller;

import com.voiceassistant.common.dto.ApiResponse;
import com.voiceassistant.repo.mysql.entity.DocumentInfo;
import com.voiceassistant.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    @PostMapping("/documents")
    public ApiResponse<DocumentInfo> uploadDocument(@RequestParam("file") MultipartFile file) {
        try {
            DocumentInfo doc = knowledgeService.uploadDocument(
                    file.getOriginalFilename(),
                    file.getBytes()
            );
            return ApiResponse.success(doc);
        } catch (IOException e) {
            log.error("Failed to read uploaded file", e);
            return ApiResponse.error(500, "文件读取失败: " + e.getMessage());
        }
    }

    @GetMapping("/documents")
    public ApiResponse<List<DocumentInfo>> listDocuments() {
        return ApiResponse.success(knowledgeService.listDocuments());
    }

    @DeleteMapping("/documents/{id}")
    public ApiResponse<Void> deleteDocument(@PathVariable Long id) {
        knowledgeService.deleteDocument(id);
        return ApiResponse.success(null);
    }

    @PostMapping("/search")
    public ApiResponse<List<String>> search(@RequestParam String query, @RequestParam(defaultValue = "5") int topK) {
        var results = knowledgeService.search(query);
        List<String> texts = results.stream()
                .limit(topK)
                .map(dev.langchain4j.data.segment.TextSegment::text)
                .toList();
        return ApiResponse.success(texts);
    }
}
