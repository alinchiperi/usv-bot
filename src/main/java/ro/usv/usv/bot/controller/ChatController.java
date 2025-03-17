package ro.usv.usv.bot.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ChatClient chatClient;
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    @Value("${user.advice}")
    private String userAdvice;

    public ChatController(ChatClient.Builder builder, VectorStore vectorStore) {
        this.chatClient = builder
                .defaultAdvisors(
                        new QuestionAnswerAdvisor(vectorStore,
                                SearchRequest.builder().similarityThreshold(0.5).build(),
                                userAdvice))
                .build();
    }

    @GetMapping("/")
    private String chat(@RequestParam String question) {
        logger.info("Raw question: {}", question);
        String content = chatClient.prompt()
                .user(question)
                .call()
                .content();
        logger.info("AI response: {}", content);
        return content;
    }
}
