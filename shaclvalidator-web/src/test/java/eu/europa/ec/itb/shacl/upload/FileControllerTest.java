package eu.europa.ec.itb.shacl.upload;

import eu.europa.ec.itb.shacl.DomainConfig;
import eu.europa.ec.itb.shacl.DomainConfigCache;
import eu.europa.ec.itb.shacl.validation.FileManager;
import eu.europa.ec.itb.validation.commons.ValidatorChannel;
import eu.europa.ec.itb.validation.commons.test.BaseTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class FileControllerTest extends BaseTest {

    FileManager fileManager;
    DomainConfigCache domainConfigCache;

    @BeforeEach
    protected void setup() throws IOException {
        super.setup();
        fileManager = mock(FileManager.class);
        domainConfigCache = mock(DomainConfigCache.class);
    }

    @AfterEach
    protected void teardown() {
        super.teardown();
        reset(fileManager, domainConfigCache);
    }

    private FileController createFileController() throws Exception {
        var fileController = new FileController();
        var fileManagerField = FileController.class.getDeclaredField("fileManager");
        fileManagerField.setAccessible(true);
        fileManagerField.set(fileController, fileManager);
        var domainConfigCacheField = FileController.class.getDeclaredField("domainConfigCache");
        domainConfigCacheField.setAccessible(true);
        domainConfigCacheField.set(fileController, domainConfigCache);
        return fileController;
    }

    @Test
    void testGetReport() throws Exception {
        var domainConfig = mock(DomainConfig.class);
        doReturn(domainConfig).when(domainConfigCache).getConfigForDomainName(any());
        doReturn(Set.of(ValidatorChannel.FORM)).when(domainConfig).getChannels();
        doReturn(tmpFolder.toFile()).when(fileManager).getWebTmpFolder();
        doReturn("xml").when(fileManager).getFileExtension(anyString());
        var httpRequest = mock(HttpServletRequest.class);
        var httpResponse = mock(HttpServletResponse.class);
        var testFile = createFileWithContents(Path.of(tmpFolder.toString(), "id1", UploadController.FILE_NAME_INPUT +".xml"), "CONTENT");
        var controller = createFileController();
        var result = controller.getReport("domain1", "id1", UploadController.DOWNLOAD_TYPE_CONTENT, "application/rdf+xml", httpRequest, httpResponse);
        assertNotNull(result);
        assertNotNull(result.getFile());
        assertEquals(testFile, result.getFile().toPath());
    }

}
