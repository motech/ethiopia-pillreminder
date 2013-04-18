package org.motechproject.icappr.listener;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import org.motechproject.event.MotechEvent;
import org.motechproject.event.listener.EventRelay;
import org.motechproject.event.listener.annotations.MotechListener;
import org.motechproject.icappr.PillReminderSettings;
import org.motechproject.icappr.mrs.MrsConstants;
import org.motechproject.icappr.mrs.MRSPersonUtil;
import org.motechproject.icappr.support.CallRequestDataKeys;
import org.motechproject.ivr.service.CallRequest;
import org.motechproject.ivr.service.IVRService;
import org.motechproject.messagecampaign.EventKeys;
import org.motechproject.mrs.domain.MRSPatient;
import org.motechproject.mrs.services.MRSPatientAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SendCampaignMessageListener {

	private MRSPatientAdapter patientAdapter;
	private IVRService ivrService;
	private PillReminderSettings pillReminderSettings;
	
	@Autowired
	private EventRelay eventRelay;

	@Autowired
	public SendCampaignMessageListener(MRSPatientAdapter patientAdapter, IVRService ivrService, PillReminderSettings pillReminderSettings) {
		this.patientAdapter = patientAdapter;
		this.ivrService = ivrService;
		this.pillReminderSettings = pillReminderSettings;
	}

	@MotechListener(subjects = { EventKeys.SEND_MESSAGE })
	public void sendCampaignMessage(MotechEvent event) {
		String patientId = event.getParameters().get(EventKeys.EXTERNAL_ID_KEY).toString();
		MRSPatient patient = patientAdapter.getPatientByMotechId(patientId);

		String phoneNumber = MRSPersonUtil.getAttrValue(MrsConstants.PERSON_PHONE_NUMBER_ATTR, patient.getPerson()
				.getAttributes());
		CallRequest callRequest = new CallRequest(phoneNumber, 120, pillReminderSettings.getVerboiceChannelName());

		Map<String, String> payload = callRequest.getPayload();

		payload.put("motechId", patientId);
		
		String language = MRSPersonUtil.getAttrValue(MrsConstants.PERSON_LANGUAGE_ATTR, patient.getPerson()
                .getAttributes());

		String callbackUrl = pillReminderSettings.getMotechUrl() + "/module/icappr/campaign-message?language=%s";
		
		try {
			payload.put(CallRequestDataKeys.CALLBACK_URL, 
			        URLEncoder.encode(String.format(callbackUrl, language), "UTF-8"));
		} catch (UnsupportedEncodingException e) {
		}

		try {
			ivrService.initiateCall(callRequest);
		} catch (Exception e) {
			MotechEvent exceptionEvent = new MotechEvent("icappr.failed.verboice.call");
			exceptionEvent.getParameters().put("exception_message", e.getMessage());
			exceptionEvent.getParameters().put("exception_type", e.getClass().getName());
		}
	}
}
