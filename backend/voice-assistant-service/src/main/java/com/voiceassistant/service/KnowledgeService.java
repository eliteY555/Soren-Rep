package com.voiceassistant.service;

import com.voiceassistant.llm.config.PineconeClient;
import com.voiceassistant.llm.config.PineconeClient.VectorEntry;
import com.voiceassistant.llm.config.PineconeClient.Match;
import com.voiceassistant.repo.mysql.entity.DocumentInfo;
import com.voiceassistant.repo.mysql.repository.DocumentInfoRepository;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
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
    private final EmbeddingModel embeddingModel;

    private static final int CHUNK_SIZE = 500;
    private static final int CHUNK_OVERLAP = 50;
    private static final int MAX_SEARCH_RESULTS = 5;

    public DocumentInfo uploadDocument(String fileName, byte[] fileBytes) {
        String content = extractContent(fileName, fileBytes);
        Document document = Document.from(content);
        DocumentSplitter splitter = DocumentSplitters.recursive(CHUNK_SIZE, CHUNK_OVERLAP);
        List<TextSegment> segments = splitter.split(document);
        log.info("Document '{}' split into {} chunks", fileName, segments.size());

        // Embed all chunks client-side
        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();

        // Build vector entries with text stored in metadata for retrieval
        String docId = "doc-" + UUID.randomUUID();
        List<VectorEntry> vectors = new java.util.ArrayList<>();
        for (int i = 0; i < segments.size(); i++) {
            float[] values = new float[embeddings.get(i).vector().length];
            for (int j = 0; j < values.length; j++) {
                values[j] = embeddings.get(i).vector()[j];
            }
            vectors.add(new VectorEntry(
                    docId + "-" + i,
                    values,
                    Map.of("docId", docId, "fileName", fileName, "text", segments.get(i).text())
            ));
        }

        pineconeClient.upsert(vectors);

        DocumentInfo docInfo = new DocumentInfo();
        docInfo.setFileName(fileName);
        docInfo.setFileSize((long) fileBytes.length);
        docInfo.setFileType(getFileType(fileName));
        docInfo.setChunkCount(segments.size());
        DocumentInfo saved = docRepo.save(docInfo);

        log.info("Document '{}' ingested: {} chunks → Pinecone", fileName, segments.size());
        return saved;
    }

    public List<String> search(String query) {
        Embedding queryEmbedding = embeddingModel.embed(query).content();
        float[] queryVector = new float[queryEmbedding.vector().length];
        for (int i = 0; i < queryVector.length; i++) {
            queryVector[i] = queryEmbedding.vector()[i];
        }

        List<Match> matches = pineconeClient.query(queryVector, MAX_SEARCH_RESULTS);
        return matches.stream()
                .map(Match::getText)
                .filter(t -> t != null)
                .toList();
    }

    public List<DocumentInfo> listDocuments() {
        return docRepo.findAllByOrderByUploadedAtDesc();
    }

    public void deleteDocument(Long id) {
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
