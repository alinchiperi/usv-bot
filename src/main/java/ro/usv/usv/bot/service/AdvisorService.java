package ro.usv.usv.bot.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.generation.augmentation.QueryAugmenter;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import ro.usv.usv.bot.advisor.UsvQueryAugmenter;
import ro.usv.usv.bot.model.ResponseDemo;

import java.util.List;

@Service
public class AdvisorService {
    private static final Logger log = LoggerFactory.getLogger(AdvisorService.class);
    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final MeterRegistry registry;

    public AdvisorService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore, MeterRegistry registry) {
        this.chatClient = chatClientBuilder.defaultAdvisors(new SimpleLoggerAdvisor()).build();
        this.vectorStore = vectorStore;
        this.registry = registry;
    }

    public ResponseDemo call(String userMessage, int topK, double similarityThreshold) {
        log.info("Processing user message: {}", userMessage);
        Timer.Sample sample = Timer.start(registry);
        DocumentRetriever documentRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .similarityThreshold(similarityThreshold)
                .topK(topK)
                .build();

        QueryAugmenter queryAugmenter = new UsvQueryAugmenter();

        RetrievalAugmentationAdvisor retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(documentRetriever)
                .queryAugmenter(queryAugmenter)
                .build();


        Query query = new Query(userMessage);

        List<Document> documents = Timer
                .builder("function.execution")
                .tag("function", "documentRetrieval")
                .register(registry)
                .record(() -> documentRetriever.retrieve(query)
                );

        String content = Timer
                .builder("function.execution")
                .tag("function", "chatCall")
                .register(registry)
                .record(() -> this.chatClient.prompt()
                        .advisors(retrievalAugmentationAdvisor)
                        .user(userMessage)
                        .call()
                        .content()
                );
        sample.stop(Timer.builder("function.execution.total").register(registry));

        return new ResponseDemo(documents, content);
    }
}
