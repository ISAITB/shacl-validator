package eu.europa.ec.itb.shacl.standalone;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShaclValidationInputTest {

    @Test
    void testValidationInput() {
        var file = new File("/tmp/test.txt");
        var name = "aName.txt";
        var syntax = "syntax1";
        var input = new ShaclValidationInput(file, name, syntax);
        assertEquals(file, input.getInputFile());
        assertEquals(name, input.getFileName());
        assertEquals(syntax, input.getContentSyntax());
    }

}
