package eu.europa.ec.itb.shacl.webhook;

import eu.europa.ec.itb.shacl.upload.UploadController;
import org.aspectj.lang.JoinPoint;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatisticReportingAspectTest {

    @Test
    void testUploadPointcut() {
        final boolean[] aspectCalled = {false};
        final boolean[] targetCalled = {false};
        // Use subclasses as AspectJ proxies cannot be made over Mockito mocks and spies.
        var target = new UploadController() {
            @Override
            public ModelAndView handleUpload(String domain, MultipartFile file, String uri, String string, String contentType, String validationType, String contentSyntaxType, String[] externalContentType, MultipartFile[] externalFiles, String[] externalUri, String[] externalFilesSyntaxType, Boolean loadImportsValue, String contentQuery, String contentQueryEndpoint, Boolean contentQueryAuthenticate, String contentQueryUsername, String contentQueryPassword, HttpServletRequest request, HttpServletResponse response) {
                assertEquals("domain1", domain);
                assertTrue(aspectCalled[0]); // We expect the aspect to have been called before the method.
                // We only want to check if this was called.
                targetCalled[0] = true;
                return null;
            }
        };
        var aspect = new StatisticReportingAspect() {
            @Override
            public void getUploadContext(JoinPoint joinPoint) {
                assertEquals("domain1", joinPoint.getArgs()[0]);
                aspectCalled[0] = true;
            }
        };
        var aspectFactory = new AspectJProxyFactory(target);
        aspectFactory.addAspect(aspect);
        UploadController controller = aspectFactory.getProxy();
        controller.handleUpload("domain1", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        assertTrue(aspectCalled[0]);
        assertTrue(targetCalled[0]);
    }

    @Test
    void testMinimalUploadPointcut() {
        final boolean[] aspectCalled = {false};
        final boolean[] targetCalled = {false};
        // Use subclasses as AspectJ proxies cannot be made over Mockito mocks and spies.
        var target = new UploadController() {
            @Override
            public ModelAndView handleUploadM(String domain, MultipartFile file, String uri, String string, String contentType, String validationType, String contentSyntaxType, String[] externalContentType, MultipartFile[] externalFiles, String[] externalUri, String[] externalFilesSyntaxType, Boolean loadImportsValue, String contentQuery, String contentQueryEndpoint, Boolean contentQueryAuthenticate, String contentQueryUsername, String contentQueryPassword, HttpServletRequest request, HttpServletResponse response) {
                assertEquals("domain1", domain);
                assertTrue(aspectCalled[0]); // We expect the aspect to have been called before the method.
                // We only want to check if this was called.
                targetCalled[0] = true;
                return null;
            }
        };
        var aspect = new StatisticReportingAspect() {
            @Override
            public void getUploadContext(JoinPoint joinPoint) {
                assertEquals("domain1", joinPoint.getArgs()[0]);
                aspectCalled[0] = true;
            }
        };
        var aspectFactory = new AspectJProxyFactory(target);
        aspectFactory.addAspect(aspect);
        UploadController controller = aspectFactory.getProxy();
        controller.handleUploadM("domain1", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        assertTrue(aspectCalled[0]);
        assertTrue(targetCalled[0]);
    }

}
