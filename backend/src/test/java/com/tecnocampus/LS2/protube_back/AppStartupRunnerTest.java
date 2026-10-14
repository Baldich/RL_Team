package com.tecnocampus.LS2.protube_back;

import com.tecnocampus.LS2.protube_back.services.VideoImportService;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import static org.mockito.Mockito.*;

class AppStartupRunnerTest {
    @Test
    void importIsControlledByExistingInitialDataFlag() {
        var importer = mock(VideoImportService.class);
        new AppStartupRunner(importer, "./", false).run(null);
        verifyNoInteractions(importer);
        new AppStartupRunner(importer, "./", true).run(null);
        verify(importer).importVideos(Path.of("./"));
    }
}
