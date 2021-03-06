package org.motechproject.icappr.form.model;

import org.joda.time.DateTime;
import org.motechproject.icappr.service.MessageCampaignEnroller;
import org.motechproject.icappr.support.SchedulerUtil;
import org.motechproject.mrs.domain.MRSPatient;
import org.motechproject.mrs.services.MRSPatientAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PillReminderUpdater {

    private Logger logger = LoggerFactory.getLogger("motech-icappr");

    private MessageCampaignEnroller messageCampaignEnroller;
    private SchedulerUtil schedulerUtil;
    
    private MRSPatientAdapter patientAdapter;

    @Autowired
    public PillReminderUpdater(MessageCampaignEnroller messageCampaignEnroller, SchedulerUtil schedulerUtil, MRSPatientAdapter patientAdapter) {
        this.messageCampaignEnroller = messageCampaignEnroller;
        this.schedulerUtil = schedulerUtil;
        this.patientAdapter = patientAdapter;
    }

    public void reenroll(PillReminderUpdate update) {

        String phoneNumber = update.getPhoneNumber();
        String motechId = update.getCaseId();

        logger.debug("Re-enrolling: " + motechId);
        
        MRSPatient patient = patientAdapter.getPatientByMotechId(motechId);

        if (patient == null) {
            //this scenario only happens when a case has had test forms submitted against it (which creates a case), and then an update form is submitted. This should not be allowed due to no registration
            logger.debug("Received update form for ID: " + motechId + " but no corresponending patient exists");
            return;
        }

        messageCampaignEnroller.unenroll(motechId);
        schedulerUtil.unscheduleAllIcapprJobs(motechId);

        if (update.getPreferredReminderFrequency().matches("daily")) {
            messageCampaignEnroller.enrollInDailyMessageCampaign(update.getCaseId(), update.getPreferredCallTime());
        }

        if (update.getPreferredReminderFrequency().matches("weekly")) {
            messageCampaignEnroller.enrollInWeeklyMessageCampaign(update);
        }

        schedulerUtil.scheduleAdherenceSurvey(DateTime.parse(update.getTodaysDate()), motechId, false, phoneNumber);
        schedulerUtil.scheduleSideEffectsSurvey(DateTime.parse(update.getTodaysDate()), motechId, false, phoneNumber);
        schedulerUtil.scheduleAppointments(DateTime.parse(update.getNextAppointment()), motechId, false, phoneNumber);
    }
}
