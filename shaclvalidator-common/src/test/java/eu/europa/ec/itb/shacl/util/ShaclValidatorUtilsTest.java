package eu.europa.ec.itb.shacl.util;

import org.apache.jena.riot.Lang;
import org.junit.jupiter.api.Test;

import static eu.europa.ec.itb.shacl.util.ShaclValidatorUtils.handleEquivalentContentSyntaxes;
import static eu.europa.ec.itb.shacl.util.ShaclValidatorUtils.isRdfContentSyntax;
import static eu.europa.ec.itb.shacl.util.ShaclValidatorUtils.toNormalisedRdfLanguage;
import static org.junit.jupiter.api.Assertions.*;

class ShaclValidatorUtilsTest {

    @Test
    void testHandleEquivalentContentSyntaxes() {
        assertEquals("application/rdf+xml", handleEquivalentContentSyntaxes("application/rdf+xml"));
        assertEquals("application/rdf+xml", handleEquivalentContentSyntaxes("application/xml"));
        assertEquals("application/rdf+xml", handleEquivalentContentSyntaxes("text/xml"));
        assertEquals("application/rdf+xml", handleEquivalentContentSyntaxes("text/xml; charset=utf-8"));
        assertEquals("application/ld+json", handleEquivalentContentSyntaxes("application/json"));
        assertEquals("application/ld+json", handleEquivalentContentSyntaxes("application/ld+json"));
        assertEquals("text/plain", handleEquivalentContentSyntaxes("text/plain"));
        assertNull(handleEquivalentContentSyntaxes((String)null));
        assertEquals("", handleEquivalentContentSyntaxes(""));
    }

    @Test
    void testToNormalisedRdfLanguage() {
        assertEquals(Lang.JSONLD11, toNormalisedRdfLanguage("application/ld+json"));
        assertEquals(Lang.JSONLD11, toNormalisedRdfLanguage("application/json"));
        assertEquals(Lang.RDFXML, toNormalisedRdfLanguage("application/rdf+xml"));
        assertEquals(Lang.RDFXML, toNormalisedRdfLanguage("text/xml; charset=utf-8"));
        assertEquals(Lang.TURTLE, toNormalisedRdfLanguage("text/turtle"));
        assertEquals(Lang.NTRIPLES, toNormalisedRdfLanguage("application/n-triples"));
        assertNull(toNormalisedRdfLanguage("application/pdf"));
        assertNull(toNormalisedRdfLanguage("not a mime type"));
        assertNull(toNormalisedRdfLanguage(""));
        assertNull(toNormalisedRdfLanguage(null));
    }

    @Test
    void testIsRdfContentSyntax() {
        assertTrue(isRdfContentSyntax("application/rdf+xml"));
        assertTrue(isRdfContentSyntax("application/xml"));
        assertTrue(isRdfContentSyntax("text/xml"));
        assertTrue(isRdfContentSyntax("text/xml; charset=utf-8"));
        assertTrue(isRdfContentSyntax("application/json"));
        assertTrue(isRdfContentSyntax("application/ld+json"));
        assertFalse(isRdfContentSyntax("application/pdf"));
        assertFalse(isRdfContentSyntax("text/plain"));
        assertFalse(isRdfContentSyntax(""));
        assertFalse(isRdfContentSyntax(null));
    }

}
