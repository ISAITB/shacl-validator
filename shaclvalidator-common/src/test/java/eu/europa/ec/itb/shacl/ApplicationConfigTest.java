package eu.europa.ec.itb.shacl;

import org.apache.jena.riot.Lang;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationConfigTest {

    private static final List<String> DEFAULT_SYNTAXES = List.of("application/ld+json", "application/rdf+xml", "text/turtle", "application/n-triples");

    private ApplicationConfig config;

    @BeforeEach
    void setup() {
        config = new ApplicationConfig();
        config.setContentSyntax(new LinkedHashSet<>(DEFAULT_SYNTAXES));
    }

    @Test
    void testNoRestrictions() {
        config.resolveContentSyntaxRestrictions();
        assertEquals(DEFAULT_SYNTAXES, List.copyOf(config.getContentSyntax()));
        assertTrue(config.isContentSyntaxAccepted(Lang.TURTLE));
        assertTrue(config.isContentSyntaxAccepted(Lang.JSONLD));
        assertTrue(config.isContentSyntaxAccepted(Lang.TRIG));
        assertTrue(config.isContentSyntaxAccepted("application/ld+json"));
        assertTrue(config.isContentSyntaxAccepted((Lang) null));
    }

    @Test
    void testBlacklist() {
        config.setContentSyntaxBlacklist(Set.of("application/ld+json"));
        config.resolveContentSyntaxRestrictions();
        assertEquals(List.of("application/rdf+xml", "text/turtle", "application/n-triples"), List.copyOf(config.getContentSyntax()));
        assertFalse(config.isContentSyntaxAccepted(Lang.JSONLD));
        assertFalse(config.isContentSyntaxAccepted(Lang.JSONLD11));
        assertFalse(config.isContentSyntaxAccepted("application/ld+json"));
        assertTrue(config.isContentSyntaxAccepted(Lang.TURTLE));
        assertTrue(config.isContentSyntaxAccepted(Lang.TRIG));
        assertFalse(config.isContentSyntaxAccepted((Lang) null));
    }

    @Test
    void testBlacklistCoversEquivalentSyntaxes() {
        config.setContentSyntaxBlacklist(Set.of("application/json"));
        config.resolveContentSyntaxRestrictions();
        assertFalse(config.getContentSyntax().contains("application/ld+json"));
        assertFalse(config.isContentSyntaxAccepted(Lang.JSONLD11));
        assertFalse(config.isContentSyntaxAccepted("application/json"));
    }

    @Test
    void testWhitelist() {
        config.setContentSyntaxWhitelist(Set.of("text/turtle", "application/rdf+xml"));
        config.resolveContentSyntaxRestrictions();
        assertEquals(List.of("application/rdf+xml", "text/turtle"), List.copyOf(config.getContentSyntax()));
        assertTrue(config.isContentSyntaxAccepted(Lang.TURTLE));
        assertTrue(config.isContentSyntaxAccepted(Lang.RDFXML));
        assertTrue(config.isContentSyntaxAccepted("text/xml"));
        assertFalse(config.isContentSyntaxAccepted(Lang.JSONLD));
        assertFalse(config.isContentSyntaxAccepted(Lang.TRIG));
    }

    @Test
    void testWhitelistTakesPrecedenceOverBlacklist() {
        config.setContentSyntaxWhitelist(Set.of("text/turtle"));
        config.setContentSyntaxBlacklist(Set.of("text/turtle"));
        config.resolveContentSyntaxRestrictions();
        assertTrue(config.isContentSyntaxAccepted(Lang.TURTLE));
        assertFalse(config.isContentSyntaxAccepted(Lang.RDFXML));
        assertEquals(List.of("text/turtle"), List.copyOf(config.getContentSyntax()));
    }

    @Test
    void testBlankValuesAreIgnored() {
        config.setContentSyntaxWhitelist(Set.of(""));
        config.setContentSyntaxBlacklist(Set.of(" "));
        config.resolveContentSyntaxRestrictions();
        assertEquals(DEFAULT_SYNTAXES, List.copyOf(config.getContentSyntax()));
        assertTrue(config.isContentSyntaxAccepted(Lang.JSONLD));
    }

    @Test
    void testUnknownSyntaxFailsStartup() {
        config.setContentSyntaxBlacklist(Set.of("application/ld+jsn"));
        assertThrows(IllegalStateException.class, () -> config.resolveContentSyntaxRestrictions());
        config.setContentSyntaxBlacklist(null);
        config.setContentSyntaxWhitelist(Set.of("text/turtle", "application/pdf"));
        assertThrows(IllegalStateException.class, () -> config.resolveContentSyntaxRestrictions());
    }

}
