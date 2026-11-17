package eu.europa.ec.itb.shacl.validation;

import eu.europa.ec.itb.shacl.ModelManager;
import eu.europa.ec.itb.validation.commons.FileInfo;
import eu.europa.ec.itb.shacl.DomainConfig;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import org.apache.jena.riot.Lang;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Check that when aggregating multiple inputs the input that cannot be read is identified.
 */
class FileManagerAggregateInputsTest {

    private static final String VALID = "<http://example.org/a> <http://example.org/p> \"value\" .\n";
    // Syntax error: the object is missing before the final dot (see ISAITB/shacl-validator#24).
    private static final String INVALID = "<http://example.org/a> <http://example.org/p> .\n";

    @TempDir
    Path tempDir;

    private FileManager fileManager;
    private DomainConfig domainConfig;

    @BeforeEach
    void setUp() {
        var appConfig = mock(eu.europa.ec.itb.shacl.ApplicationConfig.class);
        when(appConfig.isContentSyntaxAccepted(any(Lang.class))).thenReturn(true);
        when(appConfig.getTmpFolder()).thenReturn(tempDir.toString());
        when(appConfig.getResourceRoot()).thenReturn(tempDir.resolve("resources").toString());
        fileManager = new FileManager();
        ReflectionTestUtils.setField(fileManager, "appConfig", appConfig);
        domainConfig = mock(DomainConfig.class);
    }

    private FileInfo input(String name, String content, String reference) throws IOException {
        var file = tempDir.resolve(name);
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return new FileInfo(file.toFile(), Lang.NTRIPLES.getContentType().getContentTypeStr(), null, null, reference);
    }

    @Test
    void testValidInputsAreAggregated() throws IOException {
        var inputs = List.of(input("a.nt", VALID, "contentToValidate[0]"), input("b.nt", VALID, "contentToValidate[1]"));
        var modelManager = new ModelManager(fileManager);
        try {
            var aggregate = fileManager.aggregateInputs(tempDir.toFile(), inputs, null, modelManager, domainConfig);
            assertNotNull(aggregate);
            // An aggregate of multiple inputs cannot be attributed to a single one.
            assertNull(aggregate.getInputReference());
        } finally {
            modelManager.close();
        }
    }

    @Test
    void testFaultyInputIsIdentified() throws IOException {
        var inputs = List.of(input("a.nt", VALID, "contentToValidate[0]"), input("b.nt", INVALID, "contentToValidate[1]"));
        var modelManager = new ModelManager(fileManager);
        try {
            var error = assertThrows(ValidatorException.class, () -> fileManager.aggregateInputs(tempDir.toFile(), inputs, null, modelManager, domainConfig));
            assertEquals("contentToValidate[1]", error.getInputReference());
            assertTrue(error.getMessage().contains("contentToValidate[1]"));
        } finally {
            modelManager.close();
        }
    }

    @Test
    void testSingleInputIsReturnedWithItsReference() throws IOException {
        var single = input("a.nt", INVALID, "contentToValidate[0]");
        var modelManager = new ModelManager(fileManager);
        try {
            var result = fileManager.aggregateInputs(tempDir.toFile(), List.of(single), null, modelManager, domainConfig);
            assertSame(single, result);
            assertEquals("contentToValidate[0]", result.getInputReference());
        } finally {
            modelManager.close();
        }
    }
}
