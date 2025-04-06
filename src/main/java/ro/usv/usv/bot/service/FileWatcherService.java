package ro.usv.usv.bot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class FileWatcherService {
    private static final Logger log = LoggerFactory.getLogger(FileWatcherService.class.getName());

    @Value("${app.directory.watch}")
    private String watchDirectoryPath;

    @Value("${app.directory.archive}")
    private String archiveDirectoryPath;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final IngestionService ingestionService;
    private final ResourceLoader resourceLoader;

    public FileWatcherService(IngestionService ingestionService, ResourceLoader resourceLoader) {
        this.ingestionService = ingestionService;
        this.resourceLoader = resourceLoader;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startWatching() {
        executorService.submit(() -> {
            try {
                log.info("Starting to watch directory: {}", watchDirectoryPath);
                Path watchDir = getAbsolutePath(watchDirectoryPath);
                Path archiveDir = getAbsolutePath(archiveDirectoryPath);

                Files.createDirectories(watchDir);
                Files.createDirectories(archiveDir);

                processExistingFiles(watchDir);

                watchDirectory(watchDir);

            } catch (Exception e) {
                log.error("Error while watching directory: {}", e.getMessage());
            }
        });
    }

    private void watchDirectory(Path watchDir) throws IOException, InterruptedException {
        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            watchDir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
            log.info("Started watching directory: {}", watchDir);

            while (true) {
                var key = watchService.take();

                for (var event : key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                        Path fileName = (Path) event.context();
                        Path fullPath = watchDir.resolve(fileName);

                        Thread.sleep(1000);

                        log.info("New file detected: {}", fullPath);
                        Resource resource = resourceLoader.getResource("file:" + fullPath.toAbsolutePath());
                        processResource(resource, fullPath);
                    }
                }
                if (!key.reset()) {
                    log.warn("Watch key no longer valid");
                    break;
                }
            }
        }
    }

    private void processExistingFiles(Path watchDir) throws IOException {
        log.info("Looking for existing files in: {}", watchDir);

        Files.list(watchDir)
                .filter(Files::isRegularFile)
                .forEach(filePath -> {
                    try {
                        Resource resource = resourceLoader.getResource("file:" + filePath.toAbsolutePath());
                        processResource(resource, filePath);
                    } catch (IOException e) {
                        log.error("Error processing existing file: {}", filePath, e);
                    }
                });
    }

    private void processResource(Resource resource, Path sourcePath) throws IOException {
        try {
            log.info("Processing file: {}", resource.getFilename());

            ingestionService.processResource(resource);

            Path targetPath = Paths.get(getAbsolutePath(archiveDirectoryPath).toString(),
                    sourcePath.getFileName().toString());
            Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);

            log.info("Resource processed and moved to archive: {}", targetPath);
        } catch (Exception e) {
            log.error("Error processing resource: {}", resource.getDescription(), e);
        }
    }

    private Path getAbsolutePath(String resourcePath) throws IOException {
        if (resourcePath.startsWith("classpath:")) {
            Resource resource = resourceLoader.getResource(resourcePath);
            return resource.getFile().toPath();
        } else {
            return Paths.get(resourcePath);
        }
    }
}
