package ro.usv.usv.bot.model;

import org.springframework.ai.document.Document;

import java.util.List;


public record ResponseDemo(List<Document> documents, String response) {
}
