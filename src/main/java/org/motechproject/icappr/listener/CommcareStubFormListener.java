package org.motechproject.icappr.listener;

import java.util.Map;
import org.motechproject.commcare.domain.CommcareForm;
import org.motechproject.commcare.domain.FormValueElement;
import org.motechproject.commcare.events.constants.EventDataKeys;
import org.motechproject.commcare.events.constants.EventSubjects;
import org.motechproject.commcare.service.CommcareFormService;
import org.motechproject.event.MotechEvent;
import org.motechproject.event.listener.annotations.MotechListener;
import org.motechproject.icappr.PillReminderSettings;
import org.motechproject.icappr.constants.CaseConstants;
import org.motechproject.icappr.constants.MotechConstants;
import org.motechproject.icappr.handlers.DemoFormHandler;
import org.motechproject.icappr.handlers.RegistrationFormHandler;
import org.motechproject.icappr.handlers.StopFormHandler;
import org.motechproject.icappr.handlers.UpdateFormHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Came from:
 * http://code.google.com/p/motech-usm/source/browse/modules/commcare-
 * openmrs-mapping
 * /commcare-to-openmrs-mapper/src/main/java/org/motechproject/mapper
 * /listeners/FormListener.java?repo=ethiopia-1d&name=Branch_motech-0.17
 * 
 */
@Component
public class CommcareStubFormListener {

    @Autowired
    private CommcareFormService formService;

    @Autowired
    private RegistrationFormHandler registrationFormHandler;

    @Autowired
    private DemoFormHandler demoFormHandler;

    @Autowired
    private UpdateFormHandler updateFormHandler;

    @Autowired
    private StopFormHandler stopFormHandler;

    @Autowired
    private PillReminderSettings settings;

    private Logger logger = LoggerFactory.getLogger("motech-icappr");

    @MotechListener(subjects = EventSubjects.FORM_STUB_EVENT)
    public void handleStubForm(MotechEvent event) {
        logger.debug("Pill Reminder module handling form stub event...");

        Map<String, Object> parameters = event.getParameters();

        String formId = (String) parameters.get(EventDataKeys.FORM_ID);

        CommcareForm form = null;

        if (formId != null && formId.trim().length() > 0) {
            form = formService.retrieveForm(formId);
            logger.debug("Successfully retrieved form with formID..." + formId);
        }

        FormValueElement rootElement = null;

        if (form != null) {
            rootElement = form.getForm();
        }

        if (rootElement != null) {
            handleForm(form);
        } else {
            logger.debug("Root element was null...not handling form.");
        }
    }

    private void handleForm(CommcareForm form) {
        String xmlns = form.getForm().getAttributes().get(MotechConstants.FORM_XMLNS_ATTRIBUTE);

        logger.debug("Handling form with xmlns" + xmlns);

        FormValueElement caseElement = form.getForm().getElementByName("case");

        if (caseElement == null) {
            logger.info("No case element found");
            return;
        }

        String caseId = caseElement.getAttributes().get("case_id");
        //CaseInfo caseInfo = caseService.getCaseByCaseId(caseId);
        form.getForm().addAttribute(CaseConstants.FORM_CASE_ID, caseId);
        logger.debug("Successfully retrieved Case ID " + caseId);

        if (settings.getRegistrationFormXmlns().equals(xmlns)) {
            // delegate to registration form handler
            registrationFormHandler.handleForm(form, caseId);
        }

        else if (settings.getUpdateFormXmlns().equals(xmlns)) {
            // delegate to update form handler
            updateFormHandler.handleForm(form, caseId);
        }

        else if (settings.getStopFormXmlns().equals(xmlns)) {
            // delegate to stop form handler
            stopFormHandler.handleForm(form, caseId);
        }

        else if (settings.getDemoFormXmlns().equals(xmlns)) {
            // delegate to ivr test form handler
            demoFormHandler.handleForm(form);
        }
    }
}
