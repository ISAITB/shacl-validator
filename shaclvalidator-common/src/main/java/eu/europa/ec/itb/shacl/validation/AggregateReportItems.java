package eu.europa.ec.itb.shacl.validation;

import com.gitb.tr.BAR;
import com.gitb.tr.ObjectFactory;
import com.gitb.tr.TestAssertionReportType;
import eu.europa.ec.itb.validation.commons.LocalisationHelper;

import javax.xml.bind.JAXBElement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility class to aggregate validation report items in order to produce an aggregate report.
 */
class AggregateReportItems {

    private final Map<String, AggregateReportItem> itemMap = new LinkedHashMap<>();
    private final ObjectFactory objectFactory;
    private final LocalisationHelper localiser;

    /**
     * Constructor.
     *
     * @param objectFactory The JAXB factory to build report items with.
     * @param localiser The localisation helper to use to localise texts.
     */
    AggregateReportItems(ObjectFactory objectFactory, LocalisationHelper localiser) {
        this.objectFactory = objectFactory;
        this.localiser = localiser;
    }

    /**
     * Update aggregates for the provided report item. Aggregation takes place by comparing
     * the items' shape, description and severity level.
     *
     * @param shape The shape relevant to this item (might be null).
     * @param severity The item's severity.
     * @param element The JAXB element from the detailed report.
     */
    void updateForReportItem(String shape, Severity severity, JAXBElement<TestAssertionReportType> element) {
        String key = String.format("[%s]|[%s]|[%s]", shape, severity, ((BAR)element.getValue()).getDescription());
        itemMap.computeIfAbsent(key, k -> new AggregateReportItem(cloneElement(severity, element))).addOne();
    }

    /**
     * Get the list of report items for the aggregated report.
     *
     * @return The report items.
     */
    List<JAXBElement<TestAssertionReportType>> getReportItems() {
        return itemMap.values().stream().map(aggregateItem -> {
            var bar = (BAR)aggregateItem.firstItem.getValue();
            if (aggregateItem.counter > 1) {
                bar.setDescription(String.format("[%s] %s", localiser.localise("validator.label.reportItemTotalOccurrences", aggregateItem.counter), bar.getDescription()));
            }
            return aggregateItem.firstItem;
        }).collect(Collectors.toList());
    }

    /**
     * Clone the provided report item element.
     *
     * @param severity The severity.
     * @param element The original item's element.
     * @return The cloned element.
     */
    private JAXBElement<TestAssertionReportType> cloneElement(Severity severity, JAXBElement<TestAssertionReportType> element) {
        if (element.getValue() instanceof BAR) {
            var source = (BAR)element.getValue();
            var target = new BAR();
            target.setDescription(source.getDescription());
            target.setAssertionID(source.getAssertionID());
            target.setType(source.getType());
            target.setValue(source.getValue());
            target.setLocation(source.getLocation());
            target.setTest(source.getTest());
            switch (severity) {
                case ERROR: return objectFactory.createTestAssertionGroupReportsTypeError(target);
                case WARNING: return objectFactory.createTestAssertionGroupReportsTypeWarning(target);
                default: return objectFactory.createTestAssertionGroupReportsTypeInfo(target);
            }
        } else {
            throw new IllegalStateException("Report items encountered having an unexpected class type ["+element.getValue().getClass()+"]");
        }
    }

    /**
     * Helper enum to avoid calculating the severity repeatedly.
     */
    enum Severity {
        ERROR, WARNING, MESSAGE
    }

    /**
     * Class to encapsulate the information for an aggregated report item (first occurrence and total count).
     */
    private static class AggregateReportItem {

        private final JAXBElement<TestAssertionReportType> firstItem;
        private long counter = 0;

        /**
         * Constructor.
         *
         * @param firstItem The first item to display.
         */
        private AggregateReportItem(JAXBElement<TestAssertionReportType> firstItem) {
            this.firstItem = firstItem;
        }

        /**
         * Increment the occurences.
         */
        private void addOne() {
            counter += 1;
        }

    }

}
