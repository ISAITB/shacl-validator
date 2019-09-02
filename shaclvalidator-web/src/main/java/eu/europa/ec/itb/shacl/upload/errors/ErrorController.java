package eu.europa.ec.itb.shacl.upload.errors;

import eu.europa.ec.itb.shacl.upload.UploadController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
public class ErrorController implements org.springframework.boot.web.servlet.error.ErrorController {

    @RequestMapping("/error")
    public ModelAndView handleError(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Boolean isMinimalUI = (Boolean)request.getAttribute(UploadController.IS_MINIMAL);
        if (isMinimalUI == null) {
            isMinimalUI = false;
        }
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("minimalUI", isMinimalUI);
        if (status != null) {
            Integer statusCode = Integer.valueOf(status.toString());
                attributes.put("errorMessage", "The requested path or resource does not exist.");
            if(statusCode == HttpStatus.NOT_FOUND.value()) {
            } else {
                attributes.put("errorMessage", "An internal server error occurred.");
            }
        } else {
            attributes.put("errorMessage", "-");
        }
        return new ModelAndView("error", attributes);
    }

    @Override
    public String getErrorPath() {
        return "/error";
    }
}