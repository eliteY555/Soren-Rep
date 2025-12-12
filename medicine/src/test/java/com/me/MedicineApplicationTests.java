package com.me;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.AutoConfigureMybatis;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;

@SpringBootTest
@AutoConfigureMybatis
class MedicineApplicationTests {

    @Autowired
    private EmbeddingStore<TextSegment> embeddingStore;
    @Autowired
    private EmbeddingModel embeddingModel;

    @Test
    @Disabled("文件不存在，暂时禁用此测试")
    void contextLoads() {
        Document document1 = FileSystemDocumentLoader.loadDocument("D:\\y-space\\PROJECT\\Chinese_Medicine-main\\docs\\doctors_profiles.md");
        Document document2 = FileSystemDocumentLoader.loadDocument("D:\\y-space\\PROJECT\\Chinese_Medicine-main\\docs\\hospitals_introduction.md");
        List<Document> documentList = Arrays.asList(document1, document2);

        EmbeddingStoreIngestor.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .build()
                .ingest(documentList);
    }


    @Test
    public void testEmbeddingModel() {
        Response<Embedding> embed = embeddingModel.embed("你好");

        System.out.println("向量维度：" + embed.content().vector().length);
        System.out.println("向量输出：" + embed.toString());
    }

}
