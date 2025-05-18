package ro.usv.usv.bot.service;

import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class AdvisorService {
    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public AdvisorService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        this.chatClient = chatClientBuilder.defaultAdvisors(new SimpleLoggerAdvisor()).build();
        this.vectorStore = vectorStore;
    }

    public ResponseDemo call(String userMessage, int topK, double similarityThreshold) {

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

        List<Document> documents = documentRetriever.retrieve(query);

        String content = this.chatClient.prompt()
                .advisors(retrievalAugmentationAdvisor)
                .user(userMessage)
                .call()
                .content();

        return new ResponseDemo(documents, content);
    }
}
