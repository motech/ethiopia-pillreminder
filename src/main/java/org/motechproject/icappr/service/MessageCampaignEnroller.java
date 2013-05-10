package org.motechproject.icappr.service;

import org.joda.time.DateTime;
import org.motechproject.commons.date.model.Time;
import org.motechproject.commons.date.util.DateUtil;
import org.motechproject.icappr.PillReminderSettings;
import org.motechproject.icappr.form.model.PillReminderUpdate;
import org.motechproject.messagecampaign.contract.CampaignRequest;
import org.motechproject.messagecampaign.service.CampaignEnrollmentsQuery;
import org.motechproject.messagecampaign.service.MessageCampaignService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MessageCampaignEnroller {

    private static final String TIME_DELIMITER = ":";

    private MessageCampaignService messageCampaignService;

    private PillReminderSettings settings;

    @Autowired
    public MessageCampaignEnroller(MessageCampaignService messageCampaignService, PillReminderSettings settings) {
        this.messageCampaignService = messageCampaignService;
        this.settings = settings;
    }

    public void unenroll(String externalId){
        CampaignEnrollmentsQuery query = new CampaignEnrollmentsQuery();
        query = query.withExternalId(externalId);
        messageCampaignService.stopAll(query);
    }

    public void enrollInDailyMessageCampaign(String caseId, String preferredCallTime) {
        CampaignRequest request = new CampaignRequest();
        request.setCampaignName("DailyMessageCampaign");
        request.setExternalId(caseId);
        request.setReferenceDate(DateUtil.now().toLocalDate());
        request.setReferenceTime(new Time(DateTime.now().getHourOfDay(), DateTime.now().getMinuteOfHour()));
        Time preferredTime = getPreferredTime(preferredCallTime);
        request.setStartTime(preferredTime); 

        messageCampaignService.startFor(request);
    }

    public void enrollInWeeklyMessageCampaign(PillReminderUpdate update) {
        CampaignRequest request = new CampaignRequest();
        request.setExternalId(update.getCaseId());
        request.setReferenceDate(DateUtil.now().toLocalDate());
        request.setReferenceTime(new Time(DateTime.now().getHourOfDay(), DateTime.now().getMinuteOfHour()));

        Time preferredTime = getPreferredTime(update.getPreferredCallTime());

        request.setStartTime(preferredTime);

        String dayOfWeek = update.getPreferredReminderDay();

        switch (dayOfWeek.toLowerCase()) {
            case "monday" : request.setCampaignName("MondayMessageCampaign"); break;
            case "tuesday" : request.setCampaignName("TuesdayMessageCampaign"); break;
            case "wednesday" : request.setCampaignName("WednesdayMessageCampaign"); break;
            case "thursday" : request.setCampaignName("ThursdayMessageCampaign"); break;
            case "friday" : request.setCampaignName("FridayMessageCampaign"); break;
            case "saturday" : request.setCampaignName("SaturdayMessageCampaign"); break;
            case "sunday" : request.setCampaignName("SundayMessageCampaign"); break;
        }

        messageCampaignService.startFor(request);
    }

    private Time getPreferredTime(String timeString) {
        String[] timeArray = timeString.split(TIME_DELIMITER);
        int hour = Integer.parseInt(timeArray[0]);
        int minute = Integer.parseInt(timeArray[1]);

        return new Time(hour - settings.getEATtoUTCHourDifference(), minute);
    }
}
