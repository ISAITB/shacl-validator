package eu.europa.ec.itb.shacl;

import org.apache.jena.rdf.model.Model;

/**
 * Wrapper class to hold the pair of a validation's RDF models (input and report).
 */
public class ModelPair {

    private final Model inputModel;
    private final Model reportModel;

    /**
     * Constructor.
     *
     * @param inputModel The RDF model that was validated.
     * @param reportModel The model for the SHACL validation report.
     */
    public ModelPair(Model inputModel, Model reportModel) {
        this.inputModel = inputModel;
        this.reportModel = reportModel;
    }

    /**
     * @return The input model.
     */
    public Model getInputModel() {
        return inputModel;
    }

    /**
     * @return The report model.
     */
    public Model getReportModel() {
        return reportModel;
    }
}
