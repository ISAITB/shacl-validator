package eu.europa.ec.itb.shacl.webhook;

import java.util.HashMap;
import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import eu.europa.ec.itb.commons.war.webhook.UsageData;
import eu.europa.ec.itb.commons.war.webhook.WebHook;
import eu.europa.ec.itb.shacl.validation.SHACLValidator;
import eu.europa.ec.itb.validation.commons.config.ApplicationConfig;

@Aspect
@Component
@ConditionalOnProperty(name = "validator.webhook.statistics")
public class StatisticReportingAspect {

	private static final Logger logger = LoggerFactory.getLogger(StatisticReportingAspect.class);

	@Autowired
	private WebHook webHook;

	@Autowired
	private ApplicationConfig config;

	private static ThreadLocal<Map<String, String>> adviceContext = new ThreadLocal<>();

	/**
	 * Advice to obtain the arguments passed to the web upload API call
	 * */
	@Before(value = "execution(public * eu.europa.ec.itb.shacl.upload.UploadController.handleUpload(..))")
	public void getUploadContext(JoinPoint joinPoint) throws Throwable {
		Map<String, String> contextParams = new HashMap<String,String>();
		contextParams.put("api", StatisticReportingConstants.WEB_API);
		adviceContext.set(contextParams);
	}
	
	/**
	 * Advice to obtain the arguments passed to the SOAP API call
	 * */
	@Before(value = "execution(public * eu.europa.ec.itb.shacl.gitb.ValidationServiceImpl.validate(..))")
	public void getSoapCallContext(JoinPoint joinPoint) throws Throwable{
		Map<String, String> contextParams = new HashMap<String,String>();
		contextParams.put("api", StatisticReportingConstants.SOAP_API);
		adviceContext.set(contextParams);
	}
	
	
	/**
	 * Advice to obtain the arguments passed to the REST API call
	 * */
	@Before(value = "execution(public * eu.europa.ec.itb.shacl.rest.ShaclController.validate(..))")
	public void getRestCallContext(JoinPoint joinPoint) throws Throwable{
		Map<String, String> contextParams = new HashMap<String,String>();
		contextParams.put("api", StatisticReportingConstants.REST_API);
		adviceContext.set(contextParams);
	}

	/**
	 * Advice to send the usage report 
	 * */
	@Around("execution(public * eu.europa.ec.itb.shacl.validation.SHACLValidator.validateAll(..))")
	public Object reportValidatorDataUsage(ProceedingJoinPoint joinPoint) throws Throwable {
		SHACLValidator validator = (SHACLValidator) joinPoint.getTarget();
		Object report = joinPoint.proceed();
		try{
			Map<String, String> usageParams = adviceContext.get();
			String validatorId = config.getIdentifier();
			String domain = validator.getDomain();
			String validationType = validator.getValidationType();
			String api = usageParams.get("api");
			Model reportModel = (Model)report;
			//obtain the result of the model
			UsageData.Result result = extractResult(reportModel);
			webHook.sendUsageData(new UsageData(validatorId, domain, api, validationType, result));
		}catch(Exception ex){
			// Ensure unexpected errors never block validation processing
			logger.warn("Unexpected error during statistics reporting", ex);
		}finally{
			return report;
		}
	}

	
	/**
	 * Method that receives the Jena Model report and extracts the amount of errors and warnings.
	 * */
	private UsageData.Result extractResult(Model report) {
		int infos = 0;
		int warnings = 0;
		int errors = 0;

		NodeIterator validationResults = report.listObjectsOfProperty(report.getProperty("http://www.w3.org/ns/shacl#result"));
   		while(validationResults.hasNext()) {
   			StmtIterator statements = report.listStatements(validationResults.next().asResource(), null, (RDFNode)null);
   	   		while(statements.hasNext()) {
   	   			Statement statement = statements.next();
   	   			String severity = "";
   	   			if(statement.getPredicate().hasURI("http://www.w3.org/ns/shacl#resultSeverity")) {
   	   				severity = statement.getObject().asResource().getURI();
   	    		}
   	   			if(!severity.isEmpty()) {
   	   				if(severity.equals("http://www.w3.org/ns/shacl#Info")) {
   	   					infos += 1;
   	   				} else if (severity.equals("http://www.w3.org/ns/shacl#Warning")) {
   	   					warnings += 1;
   	   				} else { 
   	   					errors += 1;
   	   				}   
   	   			}
   	   		}
   		}
   		if(errors > 0) return UsageData.Result.FAILURE; //ONE OR MORE ERRORS
   		else if(warnings > 0) return UsageData.Result.WARNING; //ONE OR MORE WARNINGS, NO ERRORS
   		else return UsageData.Result.SUCCESS; //ONLY INFOS
	}

}
