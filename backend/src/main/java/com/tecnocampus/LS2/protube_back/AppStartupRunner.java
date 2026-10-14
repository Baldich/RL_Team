package com.tecnocampus.LS2.protube_back;

import com.tecnocampus.LS2.protube_back.services.VideoImportService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import java.nio.file.Path;

@Component
public class AppStartupRunner implements ApplicationRunner {
    private final VideoImportService importer;
    private final Path store;
    private final boolean loadInitialData;

    public AppStartupRunner(VideoImportService importer,
                            @Value("${pro_tube.store.dir}") String store,
                            @Value("${pro_tube.load_initial_data:false}") boolean loadInitialData) {
        this.importer = importer;
        this.store = Path.of(store);
        this.loadInitialData = loadInitialData;
    }
    @Override
    public void run(ApplicationArguments args) {
        if (loadInitialData) importer.importVideos(store);
    }
}
