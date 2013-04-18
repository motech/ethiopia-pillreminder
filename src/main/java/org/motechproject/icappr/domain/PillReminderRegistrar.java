package org.motechproject.icappr.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.joda.time.DateTime;
import org.motechproject.commons.date.model.Time;
import org.motechproject.commons.date.util.DateUtil;
import org.motechproject.icappr.domain.AdherenceCallEnrollmentRequest;
import org.motechproject.icappr.mrs.MRSPersonUtil;
import org.motechproject.icappr.mrs.MrsConstants;
import org.motechproject.icappr.service.AdherenceCallEnroller;
import org.motechproject.messagecampaign.contract.CampaignRequest;
import org.motechproject.messagecampaign.service.MessageCampaignService;
import org.motechproject.mrs.domain.MRSAttribute;
import org.motechproject.mrs.domain.MRSFacility;
import org.motechproject.mrs.domain.MRSPatient;
import org.motechproject.mrs.domain.MRSPerson;
import org.motechproject.mrs.model.MRSAttributeDto;
import org.motechproject.mrs.model.MRSPatientDto;
import org.motechproject.mrs.model.MRSPersonDto;
import org.motechproject.mrs.services.MRSFacilityAdapter;
import org.motechproject.mrs.services.MRSPatientAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PillReminderRegistrar {
	private Logger logger = LoggerFactory.getLogger("motech-icappr");

    private MRSPatientAdapter patientAdapter;
    private MRSFacilityAdapter facilityAdapter;
    private MessageCampaignService messageCampaignService;
    private AdherenceCallEnroller adherenceCallEnroller;
    
    private static final Map<String, String> clinicMappings = new HashMap<>();

    static {
        clinicMappings.put("clinic_a", "Clinic A");
        clinicMappings.put("clinic_b", "Clinic B");
    }

    @Autowired
    public PillReminderRegistrar(MRSPatientAdapter patientAdapter, MRSFacilityAdapter facilityAdapter,
            MessageCampaignService messageCampaignService, AdherenceCallEnroller adherenceCallEnroller) {
        this.patientAdapter = patientAdapter;
        this.facilityAdapter = facilityAdapter;
        this.messageCampaignService = messageCampaignService;
        this.adherenceCallEnroller = adherenceCallEnroller;
    }

    public void register(PillReminderRegistration registration) {
        logger.debug("Starting Patient Registration");
        createGenericPatient(registration);
        logger.debug("Finishing Patient Registration");
        enrollInDailyMessageCampaign(registration);
//        enrollInAdherenceCall(registration);
    }

    private void enrollInDailyMessageCampaign(PillReminderRegistration registration) {
        CampaignRequest request = new CampaignRequest();
        request.setCampaignName("DailyMessageCampaign");
        request.setExternalId(registration.getPatientId());
        request.setReferenceDate(DateUtil.now().toLocalDate());
        request.setReferenceTime(new Time(DateTime.now().getHourOfDay(), DateTime.now().getMinuteOfHour()));
        messageCampaignService.startFor(request);
    }

    private void enrollInAdherenceCall(PillReminderRegistration registration) {
        AdherenceCallEnrollmentRequest request = new AdherenceCallEnrollmentRequest();
        request.setMotechID(registration.getPatientId());
        request.setPhoneNumber(registration.getPhoneNumber());
        request.setPin(registration.getPin());
        DateTime dateTime = DateUtil.now().plusMinutes(2);
        // will change based on information in form
        request.setDosageStartTime(String.format("%02d:%02d", dateTime.getHourOfDay(), dateTime.getMinuteOfHour()));
        adherenceCallEnroller.enrollPatientWithId(request);
    }

    private void createGenericPatient(PillReminderRegistration registration) {
        List<? extends MRSFacility> facilities = facilityAdapter
                .getFacilities(clinicMappings.get(registration.getClinic()));
        if (facilities.size() == 0) {
            throw new RuntimeException("Could not find OpenMRS Facility with name: "
                    + clinicMappings.get(registration.getClinic()));
        }
        MRSFacility facility = facilities.get(0);

        MRSPerson person = new MRSPersonDto();
        person.setFirstName("MOTECH Generic Patient");
        person.setLastName("MOTECH Generic Patient");
        person.setGender("M");
        person.setDateOfBirth(DateUtil.now());
        List<MRSAttribute> attributes = new ArrayList<MRSAttribute>();
        attributes.add(new MRSAttributeDto(MrsConstants.PERSON_LANGUAGE_ATTR, registration.getPreferredLanguage()));
        attributes.add(new MRSAttributeDto(MrsConstants.PERSON_PHONE_NUMBER_ATTR, registration.getPhoneNumber()));
        attributes.add(new MRSAttributeDto(MrsConstants.PERSON_PIN_ATTR, registration.getPin()));
        attributes.add(new MRSAttributeDto(MrsConstants.PERSON_NEXT_CAMPAIGN_ATTR, registration.nextCampaign()));

        person.setAttributes(attributes);

        MRSPatient patient = new MRSPatientDto(null, facility, person, registration.getPatientId());
        patientAdapter.savePatient(patient);
    }

    public PillReminderRegistration getRegistrationForPatient(String patientId) {
        MRSPatient patient = patientAdapter.getPatientByMotechId(patientId);
        if (patient == null) {
            return null;
        }

        PillReminderRegistration registration = new PillReminderRegistration();
        registration.setClinic(patient.getFacility().getName());
        registration.setPatientId(patientId);

        List<MRSAttribute> attrs = patient.getPerson().getAttributes();
        registration.setNextCampaign(MRSPersonUtil.getAttrValue(MrsConstants.PERSON_NEXT_CAMPAIGN_ATTR, attrs));
        registration.setPhoneNumber(MRSPersonUtil.getAttrValue(MrsConstants.PERSON_PHONE_NUMBER_ATTR, attrs));
        registration.setPin(MRSPersonUtil.getAttrValue(MrsConstants.PERSON_PIN_ATTR, attrs));

        return registration;
    }

}
