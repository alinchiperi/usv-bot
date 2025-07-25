package ro.usv.usv.bot.service;

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
import ro.usv.usv.bot.model.Response;

import java.util.List;

@Service
public class AdvisorService {
    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    private static final Logger log = LoggerFactory.getLogger(AdvisorService.class.getName());

    public AdvisorService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        this.chatClient = chatClientBuilder.defaultAdvisors(new SimpleLoggerAdvisor()).build();
        this.vectorStore = vectorStore;
    }

    public Response call(String userMessage, int topK, double similarityThreshold) {

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

        long startRetrieve = System.nanoTime();
        List<Document> documents = documentRetriever.retrieve(query);
        long endRetrieve = System.nanoTime();
        log.info("Time to retrieve documents: {} ms", (endRetrieve - startRetrieve) / 1_000_000);

        long startChat = System.nanoTime();
        String content = this.chatClient.prompt()
                .advisors(retrievalAugmentationAdvisor)
                .user(userMessage)
                .call()
                .content();
        long endChat = System.nanoTime();
        log.info("Time for chatClient call: {} ms", (endChat - startChat) / 1_000_000);

        return new Response(documents, content);
    }
}
