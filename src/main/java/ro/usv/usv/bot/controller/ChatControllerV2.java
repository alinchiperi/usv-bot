package ro.usv.usv.bot.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ro.usv.usv.bot.model.Response;
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
    public Response response(@RequestBody UserMessage userMessage,
                             @RequestParam(defaultValue = "3") int topK,
                             @RequestParam(defaultValue = "0.6") double similarityThreshold) {
        return advisorService.call(userMessage.message(), topK, similarityThreshold);
    }
}
