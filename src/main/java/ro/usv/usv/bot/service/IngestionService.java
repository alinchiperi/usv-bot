package ro.usv.usv.bot.service;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);
    public final VectorStore vectorStore;

    public IngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public void processFile(MultipartFile file) throws IOException {
        log.info("Start processing file: {}", file.getOriginalFilename());
        var resource = convertMultipartFileToResource(file);
        var reader = new TikaDocumentReader(resource);
        TextSplitter textSplitter = new TokenTextSplitter();
        vectorStore.accept(textSplitter.apply(reader.get()));
        log.info("VectorStore loaded with data!");
    }

    private Resource convertMultipartFileToResource(MultipartFile multipartFile) throws IOException {
        return multipartFile.getResource();
    }

}
