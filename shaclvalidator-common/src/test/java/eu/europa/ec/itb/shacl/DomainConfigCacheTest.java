package eu.europa.ec.itb.shacl;

import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DomainConfigCacheTest {

    private ApplicationConfig appConfig;
    private DomainConfigCache cache;
    private DomainConfig domainConfig;
    private Configuration config;

    @BeforeEach
    void setup() {
        appConfig = new ApplicationConfig();
        appConfig.setContentSyntax(new LinkedHashSet<>(List.of("application/ld+json", "application/rdf+xml", "text/turtle", "application/n-triples")));
        cache = new DomainConfigCache();
        ReflectionTestUtils.setField(cache, "appConfig", appConfig);
        domainConfig = new DomainConfig();
        config = new BaseConfiguration();
    }

    @Test
    void testApplicationDefaultsUsedWhenDomainDoesNotConfigureSyntaxes() {
        appConfig.resolveContentSyntaxRestrictions();
        cache.addWebContentSyntax(domainConfig, config);
        assertEquals(List.of("application/ld+json", "application/rdf+xml", "text/turtle", "application/n-triples"), domainConfig.getWebContentSyntax());
    }

    @Test
    void testDomainSyntaxesAreKeptWhenNoRestrictions() {
        config.setProperty("validator.contentSyntax", "text/turtle, application/ld+json");
        appConfig.resolveContentSyntaxRestrictions();
        cache.addWebContentSyntax(domainConfig, config);
        assertEquals(List.of("text/turtle", "application/ld+json"), domainConfig.getWebContentSyntax());
    }

    @Test
    void testApplicationBlacklistAppliesToApplicationDefaults() {
        appConfig.setContentSyntaxBlacklist(Set.of("application/ld+json"));
        appConfig.resolveContentSyntaxRestrictions();
        cache.addWebContentSyntax(domainConfig, config);
        assertEquals(List.of("application/rdf+xml", "text/turtle", "application/n-triples"), domainConfig.getWebContentSyntax());
    }

    @Test
    void testApplicationBlacklistCannotBeOverriddenByDomain() {
        // The domain explicitly lists JSON-LD, and even tries to set the (application-level) blacklist.
        config.setProperty("validator.contentSyntax", "text/turtle, application/ld+json");
        config.setProperty("validator.contentSyntaxBlacklist", "text/turtle");
        config.setProperty("validator.contentSyntaxWhitelist", "application/ld+json");
        appConfig.setContentSyntaxBlacklist(Set.of("application/ld+json"));
        appConfig.resolveContentSyntaxRestrictions();
        cache.addWebContentSyntax(domainConfig, config);
        assertEquals(List.of("text/turtle"), domainConfig.getWebContentSyntax());
    }

    @Test
    void testApplicationWhitelistNarrowsDomainSyntaxes() {
        config.setProperty("validator.contentSyntax", "text/turtle, application/ld+json");
        appConfig.setContentSyntaxWhitelist(Set.of("text/turtle", "application/rdf+xml"));
        appConfig.resolveContentSyntaxRestrictions();
        cache.addWebContentSyntax(domainConfig, config);
        assertEquals(List.of("text/turtle"), domainConfig.getWebContentSyntax());
    }

}
