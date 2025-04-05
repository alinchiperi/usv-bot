package ro.usv.usv.bot.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.usv.usv.bot.advisor.UsvAdvisor;
import ro.usv.usv.bot.model.UserMessage;

@RestController
@RequestMapping("/api/v1/chat")
@CrossOrigin
public class ChatController {

    private final ChatClient chatClient;
    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    public ChatController(ChatClient.Builder builder, VectorStore vectorStore, @Value("${user.advice}") String userAdvice) {
        this.chatClient = builder
                .defaultAdvisors(
                        new UsvAdvisor(vectorStore,
                                SearchRequest.builder().similarityThreshold(0.5).build()),
                        new SimpleLoggerAdvisor())
                .build();
    }

    @PostMapping("")
    private org.springframework.http.ResponseEntity<UserMessage> chat(@RequestBody UserMessage userMessage) {
        String message = userMessage.message();
        log.info("Raw question: {}", message);
        String content = chatClient.prompt()
                .user(message)
                .call()
                .content();
        log.info("AI response: {}", content);
        return ResponseEntity.ok(new UserMessage(content));
    }
}
