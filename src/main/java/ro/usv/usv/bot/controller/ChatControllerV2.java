package ro.usv.usv.bot.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import ro.usv.usv.bot.model.ResponseDemo;
import ro.usv.usv.bot.model.UserMessage;
import ro.usv.usv.bot.service.AdvisorService;

@RestController
@RequestMapping("/api/v2/chat")
@CrossOrigin
public class ChatControllerV2 {
    private final AdvisorService advisorService;

    public ChatControllerV2(AdvisorService advisorService) {
        this.advisorService = advisorService;
    }

    @PostMapping("")
    public ResponseDemo response(@RequestBody UserMessage userMessage,
                                 @RequestParam(defaultValue = "2") int topK,
                                 @RequestParam(defaultValue = "0.5") double similarityThreshold) {
        return advisorService.call(userMessage.message(), topK, similarityThreshold);
    }
}
