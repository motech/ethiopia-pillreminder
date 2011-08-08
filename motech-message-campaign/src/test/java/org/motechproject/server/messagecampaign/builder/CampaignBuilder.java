package org.motechproject.server.messagecampaign.builder;

import org.joda.time.DateTime;
import org.motechproject.server.messagecampaign.domain.campaign.AbsoluteCampaign;
import org.motechproject.server.messagecampaign.domain.campaign.CronBasedCampaign;
import org.motechproject.server.messagecampaign.domain.campaign.OffsetCampaign;
import org.motechproject.server.messagecampaign.domain.campaign.RepeatingCampaign;
import org.motechproject.server.messagecampaign.domain.message.AbsoluteCampaignMessage;
import org.motechproject.server.messagecampaign.domain.message.CronBasedCampaignMessage;
import org.motechproject.server.messagecampaign.domain.message.OffsetCampaignMessage;
import org.motechproject.server.messagecampaign.domain.message.RepeatingCampaignMessage;

import java.util.Date;
import java.util.LinkedList;

public class CampaignBuilder {


    public AbsoluteCampaign defaultAbsoluteCampaign() {
        AbsoluteCampaign campaign = new AbsoluteCampaign();
        campaign.name("testCampaign");
        Date tomorrow = new DateTime(new Date()).plusDays(1).toDate();

        final AbsoluteCampaignMessage absoluteCampaignMessage1 = new CampaignMessageBuilder().absoluteCampaignMessage("AM1", new Date(), "random-1");
        final AbsoluteCampaignMessage absoluteCampaignMessage2 = new CampaignMessageBuilder().absoluteCampaignMessage("AM2", tomorrow, "random-2");

        LinkedList<AbsoluteCampaignMessage> campaignMessages = new LinkedList<AbsoluteCampaignMessage>() {{
            add(absoluteCampaignMessage1);
            add(absoluteCampaignMessage2);
        }};

        campaign.messages(campaignMessages);
        return campaign;
    }

    public CronBasedCampaign defaultCronBasedCampaign() {
        CronBasedCampaign campaign = new CronBasedCampaign();
        campaign.name("testCampaign");

        final CronBasedCampaignMessage campaignMessage1 = new CampaignMessageBuilder().cronBasedCampaignMessage("CM1", "0 11 11 11 11 ?", "cron-message1");
        final CronBasedCampaignMessage campaignMessage2 = new CampaignMessageBuilder().cronBasedCampaignMessage("CM2", "0 11 11 11 12 ?", "cron-message2");

        LinkedList<CronBasedCampaignMessage> campaignMessages = new LinkedList<CronBasedCampaignMessage>() {{
            add(campaignMessage1);
            add(campaignMessage2);
        }};

        campaign.messages(campaignMessages);
        return campaign;
    }

    public OffsetCampaign defaultOffsetCampaign() {
        OffsetCampaign campaign = new OffsetCampaign();
        campaign.name("testCampaign");

        final OffsetCampaignMessage offsetCampaignMessage1 = new CampaignMessageBuilder().offsetCampaignMessage("OM1", "1 Week", "child-info-week-1");
        final OffsetCampaignMessage offsetCampaignMessage2 = new CampaignMessageBuilder().offsetCampaignMessage("OM2", "2 Weeks", "child-info-week-1a");

        LinkedList<OffsetCampaignMessage> campaignMessages = new LinkedList<OffsetCampaignMessage>() {{
            add(offsetCampaignMessage1);
            add(offsetCampaignMessage2);
        }};

        campaign.messages(campaignMessages);
        return campaign;
    }

    public RepeatingCampaign defaultRepeatingCampaign() {
        RepeatingCampaign campaign = new RepeatingCampaign();
        campaign.name("testCampaign");
        campaign.maxDuration("2 Weeks");

        final RepeatingCampaignMessage repeatingCampaignMessage1 = new CampaignMessageBuilder().repeatingCampaignMessage("OM1", "1 Week", "child-info-week-{Offset}-1");
        final RepeatingCampaignMessage repeatingCampaignMessage2 = new CampaignMessageBuilder().repeatingCampaignMessage("OM2", "12 Days", "child-info-week-{Offset}-2");

        LinkedList<RepeatingCampaignMessage> campaignMessages = new LinkedList<RepeatingCampaignMessage>() {{
            add(repeatingCampaignMessage1);
            add(repeatingCampaignMessage2);
        }};

        campaign.messages(campaignMessages);
        return campaign;
    }
}