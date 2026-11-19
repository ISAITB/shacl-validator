package eu.europa.ec.itb.shacl.validation;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.Lang;
import org.junit.jupiter.api.Test;
import org.topbraid.shacl.vocabulary.SH;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ResultMessageLabelFunctionTest {

    private static final String EX = "http://example.org/ns/";

    private ResultMessageLabelFunction function() {
        Model model = ModelFactory.createDefaultModel();
        model.setNsPrefix("ex", EX);
        model.setNsPrefix("exsub", EX + "sub/");
        return new ResultMessageLabelFunction(model);
    }

    private String label(ResultMessageLabelFunction function, String uri) {
        return function.apply(ModelFactory.createDefaultModel().createResource(uri));
    }

    @Test
    void testDigitLeadingLocalNameIsAbbreviated() {
        assertEquals("ex:7e7f-abc", label(function(), EX + "7e7f-abc"));
    }

    @Test
    void testLetterLeadingLocalNameIsAbbreviated() {
        assertEquals("ex:fff9-abc", label(function(), EX + "fff9-abc"));
    }

    @Test
    void testNoMatchingPrefixGivesFullUri() {
        assertEquals("<http://other.org/x>", label(function(), "http://other.org/x"));
    }

    @Test
    void testUnsafeLocalPartGivesFullUri() {
        assertEquals("<http://example.org/ns/a#b>", label(function(), EX + "a#b"));
    }

    @Test
    void testLongestNamespaceWins() {
        assertEquals("exsub:x", label(function(), EX + "sub/x"));
    }

    @Test
    void testLiteralGivesLexicalForm() {
        var literal = ModelFactory.createDefaultModel().createLiteral("hello");
        assertEquals("hello", function().apply(literal));
    }

    @Test
    void testEngineMessagesUsePrefixesConsistently() {
        String prefixes = """
                @prefix sh: <http://www.w3.org/ns/shacl#> .
                @prefix ex: <http://example.org/ns/> .
                """;
        Model shapes = ModelFactory.createDefaultModel();
        RDFDataMgr.read(shapes, new StringReader(prefixes + """
                ex:PersonShape a sh:NodeShape ;
                  sh:targetClass ex:Person ;
                  sh:property [
                    sh:path ex:knows ;
                    sh:class ex:7e7f-class ;
                    sh:qualifiedValueShape ex:fff9-shape ;
                    sh:qualifiedMinCount 1 ;
                  ] .
                ex:fff9-shape a sh:NodeShape ;
                  sh:property [ sh:path ex:name ; sh:minCount 1 ] .
                """), null, Lang.TURTLE);
        Model data = ModelFactory.createDefaultModel();
        data.setNsPrefixes(shapes.getNsPrefixMap());
        RDFDataMgr.read(data, new StringReader(prefixes + """
                ex:alice a ex:Person ; ex:knows ex:bob .
                """), null, Lang.TURTLE);

        Resource report = SHACLValidator.validateModelWithConsistentLabels(data, shapes);

        List<String> messages = new ArrayList<>();
        report.getModel().listObjectsOfProperty(SH.resultMessage).forEach(n -> messages.add(n.asLiteral().getString()));
        assertFalse(messages.isEmpty());
        assertTrue(messages.contains("Value must be an instance of ex:7e7f-class"), messages.toString());
        assertTrue(messages.contains("Less than 1 values have shape ex:fff9-shape"), messages.toString());
        assertTrue(messages.stream().noneMatch(m -> m.contains("<http://example.org")), messages.toString());
    }

}
