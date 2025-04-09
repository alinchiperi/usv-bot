package ro.usv.usv.bot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);
    public final VectorStore vectorStore;

    public IngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public void processFile(MultipartFile file) throws IOException {
        var resource = convertMultipartFileToResource(file);
        processResource(resource);
    }

    public void processResource(Resource resource) {
        log.info("Start processing resource: {}", resource.getFilename());

        var reader = new TikaDocumentReader(resource);
        TextSplitter textSplitter = new TokenTextSplitter();
        vectorStore.accept(textSplitter.apply(reader.get()));

        log.info("File {} processed successfully", resource.getFilename());

    }

    private Resource convertMultipartFileToResource(MultipartFile multipartFile) {
        return multipartFile.getResource();
    }

}
