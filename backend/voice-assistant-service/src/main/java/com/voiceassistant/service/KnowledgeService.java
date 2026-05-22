package com.voiceassistant.service;

import com.voiceassistant.llm.config.PineconeClient;
import com.voiceassistant.repo.mysql.entity.DocumentInfo;
import com.voiceassistant.repo.mysql.repository.DocumentInfoRepository;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeService {

    private final DocumentInfoRepository docRepo;
    private final PineconeClient pineconeClient;

    private static final int CHUNK_SIZE = 500;
    private static final int CHUNK_OVERLAP = 50;
    private static final int MAX_SEARCH_RESULTS = 5;

    public DocumentInfo uploadDocument(String fileName, byte[] fileBytes) {
        // 1. Parse document content
        String content = extractContent(fileName, fileBytes);

        // 2. Split into chunks
        Document document = Document.from(content);
        DocumentSplitter splitter = DocumentSplitters.recursive(CHUNK_SIZE, CHUNK_OVERLAP);
        List<TextSegment> segments = splitter.split(document);
        log.info("Document '{}' split into {} chunks", fileName, segments.size());

        // 3. Send chunks to Pinecone (Pinecone embeds server-side via integrated embedding)
        String docId = "doc-" + UUID.randomUUID();
        List<PineconeClient.Record> records = segments.stream()
                .map(seg -> PineconeClient.Record.builder()
                        .id(docId + "-" + segments.indexOf(seg))
                        .text(seg.text())
                        .metadata(Map.of("docId", docId, "fileName", fileName))
                        .build())
                .toList();
        pineconeClient.upsert(records);

        // 4. Save metadata to MySQL
        DocumentInfo docInfo = new DocumentInfo();
        docInfo.setFileName(fileName);
        docInfo.setFileSize((long) fileBytes.length);
        docInfo.setFileType(getFileType(fileName));
        docInfo.setChunkCount(segments.size());
        DocumentInfo saved = docRepo.save(docInfo);

        log.info("Document '{}' ingested: {} chunks → Pinecone (server-side embedding)", fileName, segments.size());
        return saved;
    }

    public List<String> search(String query) {
        List<PineconeClient.Hit> hits = pineconeClient.search(query, MAX_SEARCH_RESULTS);
        return hits.stream()
                .map(PineconeClient.Hit::getText)
                .filter(t -> t != null)
                .toList();
    }

    public List<DocumentInfo> listDocuments() {
        return docRepo.findAllByOrderByUploadedAtDesc();
    }

    public void deleteDocument(Long id) {
        DocumentInfo docInfo = docRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found: " + id));
        log.info("Deleting document '{}' — vectors managed by Pinecone", docInfo.getFileName());
        docRepo.deleteById(id);
    }

    public String buildRagPrompt(String query) {
        List<String> results = search(query);
        if (results.isEmpty()) {
            return query;
        }

        StringBuilder context = new StringBuilder();
        context.append("以下是相关的知识库内容，请基于这些内容回答问题：\n\n");
        for (int i = 0; i < results.size(); i++) {
            context.append("【参考资料 ").append(i + 1).append("】\n");
            context.append(results.get(i)).append("\n\n");
        }
        context.append("用户问题：").append(query);
        context.append("\n\n请基于以上参考资料回答问题。如果参考资料中没有相关信息，请如实告知。");

        return context.toString();
    }

    private String extractContent(String fileName, byte[] fileBytes) {
        String type = getFileType(fileName);
        try {
            return switch (type) {
                case "txt", "md" -> new String(fileBytes, StandardCharsets.UTF_8);
                case "pdf" -> extractPdfContent(fileBytes);
                default -> throw new RuntimeException("Unsupported file type: " + type);
            };
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract content from " + fileName, e);
        }
    }

    private String extractPdfContent(byte[] fileBytes) {
        ApachePdfBoxDocumentParser parser = new ApachePdfBoxDocumentParser();
        Document doc = parser.parse(new ByteArrayInputStream(fileBytes));
        return doc.text();
    }

    private String getFileType(String fileName) {
        String name = fileName.toLowerCase();
        if (name.endsWith(".pdf")) return "pdf";
        if (name.endsWith(".md")) return "md";
        if (name.endsWith(".txt")) return "txt";
        return name.substring(name.lastIndexOf('.') + 1);
    }
}
