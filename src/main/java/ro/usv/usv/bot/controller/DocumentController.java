package ro.usv.usv.bot.controller;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ro.usv.usv.bot.service.IngestionService;

@RestController
@RequestMapping("/api/v1/document")
public class DocumentController {

    private final IngestionService ingestionService;

    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);

    public DocumentController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadDocument(@RequestParam("file") MultipartFile file) {
        log.info("Received file: {}", file.getOriginalFilename());
        try {
            ingestionService.processFile(file);
            return ResponseEntity.ok("File processed successfully");
        } catch (Exception e) {
            log.error("Error processing file", e);
            return ResponseEntity.badRequest().body("Error processing file");
        }
    }
}
