package eu.europa.ec.itb.shacl.standalone;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ValidationInputTest {

    @Test
    void testValidationInput() {
        var file = new File("/tmp/test.txt");
        var name = "aName.txt";
        var type = "type1";
        var syntax = "syntax1";
        var input = new ValidationInput(file, type, name, syntax);
        assertEquals(file, input.getInputFile());
        assertEquals(name, input.getFileName());
        assertEquals(syntax, input.getContentSyntax());
    }

}
