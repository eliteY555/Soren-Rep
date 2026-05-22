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
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeService {

    private final DocumentInfoRepository docRepo;
    private final PineconeClient pineconeClient;
    private final EmbeddingModel embeddingModel;

    // Larger chunks for better context preservation
    private static final int CHUNK_SIZE = 800;
    private static final int CHUNK_OVERLAP = 120;
    private static final int MAX_SEARCH_RESULTS = 8;
    // Minimum similarity score to consider a result relevant (Pinecone cosine)
    private static final double MIN_SCORE_THRESHOLD = 0.35;

    public DocumentInfo uploadDocument(String fileName, byte[] fileBytes) {
        String content = extractContent(fileName, fileBytes);
        Document document = Document.from(content);
        DocumentSplitter splitter = DocumentSplitters.recursive(CHUNK_SIZE, CHUNK_OVERLAP);
        List<TextSegment> segments = splitter.split(document);
        log.info("Document '{}' split into {} chunks (size={}, overlap={})",
                fileName, segments.size(), CHUNK_SIZE, CHUNK_OVERLAP);

        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();

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
        docInfo.setPineconeDocId(docId);   // link MySQL record ↔ Pinecone vectors
        DocumentInfo saved = docRepo.save(docInfo);

        log.info("Document '{}' ingested: {} chunks → Pinecone", fileName, segments.size());
        return saved;
    }

    /**
     * Search knowledge base with score filtering and deduplication.
     */
    public List<ChunkResult> searchWithScores(String query) {
        Embedding queryEmbedding = embeddingModel.embed(query).content();
        float[] queryVector = new float[queryEmbedding.vector().length];
        for (int i = 0; i < queryVector.length; i++) {
            queryVector[i] = queryEmbedding.vector()[i];
        }

        List<Match> matches = pineconeClient.query(queryVector, MAX_SEARCH_RESULTS);
        return matches.stream()
                .filter(m -> m.getScore() >= MIN_SCORE_THRESHOLD)
                .filter(m -> m.getText() != null && !m.getText().isBlank())
                .map(m -> new ChunkResult(m.getText(), m.getScore()))
                .collect(Collectors.toList());
    }

    public List<String> search(String query) {
        return searchWithScores(query).stream()
                .map(ChunkResult::text)
                .toList();
    }

    public List<DocumentInfo> listDocuments() {
        return docRepo.findAllByOrderByUploadedAtDesc();
    }

    public void deleteDocument(Long id) {
        DocumentInfo doc = docRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found: " + id));
        // Delete vectors from Pinecone (if docId exists — old records may not have one)
        if (doc.getPineconeDocId() != null && !doc.getPineconeDocId().isBlank()) {
            pineconeClient.deleteByDocId(doc.getPineconeDocId());
        } else {
            log.warn("Document '{}' has no pineconeDocId — vectors may be orphaned", doc.getFileName());
        }
        // Then delete metadata from MySQL
        docRepo.deleteById(id);
        log.info("Document '{}' deleted (Pinecone docId={}, {} chunks)",
                doc.getFileName(), doc.getPineconeDocId(), doc.getChunkCount());
    }

    /**
     * Build RAG prompt that ENHANCES rather than RESTRICTS the LLM.
     *
     * Key principles:
     * 1. Knowledge base content is PRIMARY reference, not the ONLY source
     * 2. LLM should synthesize KB content with its own knowledge for completeness
     * 3. If KB has relevant info → prioritize it, expand with reasoning
     * 4. If KB has no relevant info → use general knowledge, mention KB gap
     * 5. Ask for structured, complete answers — not just quotes
     */
    public String buildRagPrompt(String query) {
        List<ChunkResult> results = searchWithScores(query);

        if (results.isEmpty()) {
            // No relevant KB content — let LLM answer freely but mention the gap
            return """
                    用户正在使用知识库增强模式提问，但知识库中未检索到与当前问题高度相关的内容。
                    请直接根据你的知识回答以下问题，并在回答开头简要说明"知识库中暂未找到相关内容，以下为通用知识回答"。

                    用户问题：%s
                    """.formatted(query);
        }

        // Build high-quality context from retrieved chunks
        StringBuilder kb = new StringBuilder();
        kb.append("【知识库参考资料】\n");
        for (int i = 0; i < results.size(); i++) {
            ChunkResult r = results.get(i);
            kb.append("── 片段 ").append(i + 1)
              .append(" (相关度: ").append(String.format("%.0f%%", r.score() * 100)).append(") ──\n");
            kb.append(r.text()).append("\n\n");
        }

        // Prompt that encourages synthesis, completeness, and critical thinking
        String instruction = """
                你是一个智能助手，当前处于"知识库增强模式"。以下是知识库中检索到的参考资料：

                %s

                请根据以上参考资料和你的知识，回答用户问题。要求：
                1. **优先使用参考资料**：如果参考资料包含相关信息，以它为主要依据
                2. **补充完整回答**：如果参考资料只覆盖了部分内容，用你的知识补充完整，使回答全面、连贯
                3. **区分来源**：如果某个信息来自参考资料，可以标注"根据知识库…"；补充的内容可以说"另外…"
                4. **结构化回答**：使用清晰的段落、列表或小标题组织内容，确保可读性
                5. **主动扩展**：在回答完核心问题后，可以简要补充相关的背景知识或注意事项

                用户问题：%s
                """.formatted(kb.toString(), query);

        return instruction;
    }

    /**
     * A retrieved chunk with its similarity score.
     */
    public record ChunkResult(String text, double score) {}

    // --- File parsing helpers ---

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
