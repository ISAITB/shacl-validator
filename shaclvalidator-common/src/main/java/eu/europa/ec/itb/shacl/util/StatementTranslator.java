/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

package eu.europa.ec.itb.shacl.util;

import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;

import java.util.*;

/**
 * Helper class used to determine the applicable statement for a given locale.
 */
public class StatementTranslator {

    private final Map<Locale, Statement> messageMap = new LinkedHashMap<>();
    private final Map<String, Statement> invalidMessageMap = new LinkedHashMap<>();
    private Statement defaultMessage;

    /**
     * Process the provided statement.
     *
     * @param statement The statement.
     */
    public void processStatement(Statement statement) {
        var languageCode = getLanguageCode(statement);
        if (languageCode.isEmpty()) {
            defaultMessage = statement;
        } else {
            try {
                messageMap.put(LocaleUtils.toLocale(languageCode), statement);
            } catch (IllegalArgumentException e) {
                invalidMessageMap.put(languageCode, statement);
            }
        }
    }

    /**
     * Get the statements that have not been considered as the translation to use.
     *
     * @return The statements.
     */
    private List<Statement> getRemainingStatements() {
        var list = new ArrayList<Statement>();
        list.addAll(messageMap.values());
        list.addAll(invalidMessageMap.values());
        if (defaultMessage != null) {
            list.add(defaultMessage);
        }
        return list;
    }

    /**
     * Extract the language code for the given statement.
     *
     * @param statement The statement.
     * @return The language code (as defined in Locale objects).
     */
    private String getLanguageCode(Statement statement) {
        RDFNode node = statement.getObject();
        if (node.isLiteral()) {
            if (node.asLiteral().getLanguage() == null) {
                return "";
            } else {
                return StringUtils.replaceChars(node.asLiteral().getLanguage(), '-', '_');
            }
        }
        return "";
    }

    /**
     * Get the translation information for the collected statements.
     *
     * This method must be called after all relevant statements have been processed. Once called it should not be called again
     * as calling it adapts its collected state.
     *
     * @param locale The locale to look for.
     * @return The translation information.
     */
    public StatementTranslation getTranslation(Locale locale) {
        Statement statementToReturn = null;
        if (messageMap.containsKey(locale)) {
            // Exact match.
            statementToReturn = messageMap.remove(locale);
        } else {
            var matchedLanguage = messageMap.entrySet().stream().filter(entry -> entry.getKey().getLanguage().equals(locale.getLanguage())).findFirst();
            if (matchedLanguage.isPresent()) {
                // Message for same language.
                statementToReturn = messageMap.remove(matchedLanguage.get().getKey());
            } else if (defaultMessage != null) {
                // Message defined without a language code.
                statementToReturn = defaultMessage;
                defaultMessage = null;
            } else if (!messageMap.isEmpty()) {
                // The first defined message with a valid language code.
                statementToReturn = messageMap.remove(messageMap.keySet().stream().findFirst().orElseThrow());
            } else if (!invalidMessageMap.isEmpty()) {
                // The first defined message even with an invalid language code.
                statementToReturn = invalidMessageMap.remove(invalidMessageMap.keySet().stream().findFirst().orElseThrow());
            }
        }
        return new StatementTranslation(statementToReturn, getRemainingStatements());
    }

    /**
     * Helper class to warp the statements for a given translation.
     */
    public static class StatementTranslation {

        private final Statement matchedStatement;
        private final List<Statement> unmatchedStatements;

        /**
         * Constructor.
         *
         * @param matchedStatement The statement that was matched.
         * @param unmatchedStatements The statements that were not matched.
         */
        StatementTranslation(Statement matchedStatement, List<Statement> unmatchedStatements) {
            this.matchedStatement = matchedStatement;
            this.unmatchedStatements = unmatchedStatements;
        }

        /**
         * @return The statement that was matched (may be null).
         */
        public Statement getMatchedStatement() {
            return matchedStatement;
        }

        /**
         * @return The statements that were not matched (not null but may be empty).
         */
        public List<Statement> getUnmatchedStatements() {
            return unmatchedStatements;
        }

    }
}
