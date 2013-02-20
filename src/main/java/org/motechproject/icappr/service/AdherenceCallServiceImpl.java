package org.motechproject.icappr.service;

import java.util.Arrays;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.motechproject.commons.date.util.DateUtil;
import org.motechproject.icappr.domain.AdherenceCallResponse;
import org.motechproject.server.pillreminder.api.contract.DailyPillRegimenRequest;
import org.motechproject.server.pillreminder.api.contract.DosageRequest;
import org.motechproject.server.pillreminder.api.contract.DosageResponse;
import org.motechproject.server.pillreminder.api.contract.MedicineRequest;
import org.motechproject.server.pillreminder.api.contract.PillRegimenResponse;
import org.motechproject.server.pillreminder.api.service.PillReminderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This class is responsible for interacting with MOTECH's {#link
 * {@link PillReminderService}
 */
@Component
public class AdherenceCallServiceImpl implements AdherenceCallService {

    private static final String NO_RESPONSE_CAPTURED_YET = "No response captured yet";

    private static final int PILL_WINDOW_IN_HOURS = 1;
    private static final int REMINDER_RETRY_INTERVAL_IN_MINUTES = 4;
    private static final int REMINDER_BUFFER_TIME_IN_MINUTES = 1;

    private PillReminderService pillReminderService;

    @Autowired
    public AdherenceCallServiceImpl(PillReminderService pillReminderService) {
        this.pillReminderService = pillReminderService;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.motechproject.demo.pillreminder.support.PillReminders#
     * findPillReminderByMotechId(java.lang.String)
     */
    @Override
    public AdherenceCallResponse findAdherenceCallByMotechId(String motechId) {
        PillRegimenResponse regimen = pillReminderService.getPillRegimen(motechId);
        return getPillReminderResponseFromRegimen(regimen);
    }

    private AdherenceCallResponse getPillReminderResponseFromRegimen(PillRegimenResponse regimen) {
        AdherenceCallResponse response = new AdherenceCallResponse();
        if (regimen == null) {
            return response;
        }

        DosageResponse dosageResponse = regimen.getDosages().get(0);
        response.setStartTime(dosageResponse.getDosageHour() + ":"
                + String.format("%02d", dosageResponse.getDosageMinute()));

        response.setLastCapturedDate(getMessageFromLastCapturedDate(dosageResponse.getResponseLastCapturedDate()));
        return response;
    }

    private String getMessageFromLastCapturedDate(LocalDate responseLastCapturedDate) {
        if (responseLastCapturedDate == null) {
            return NO_RESPONSE_CAPTURED_YET;
        }

        return responseLastCapturedDate.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.motechproject.demo.pillreminder.support.PillReminders#deletePillReminder
     * (java.lang.String)
     */
    @Override
    public void deleteAdherenceCall(String motechId) {
        pillReminderService.remove(motechId);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.motechproject.demo.pillreminder.support.PillReminders#
     * setDosageStatusKnownForPatient(java.lang.String)
     */
    @Override
    public void setDosageStatusKnownForPatient(String motechId) {
        PillRegimenResponse response = pillReminderService.getPillRegimen(motechId);
        String regimenId = response.getPillRegimenId();
        String dosageId = getDosageId(response.getDosages());

        pillReminderService.dosageStatusKnown(regimenId, dosageId, LocalDate.now());
    }

    private String getDosageId(List<DosageResponse> dosages) {
        return dosages.get(0).getDosageId();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.motechproject.demo.pillreminder.support.PillReminders#
     * patientInPillRegimen(java.lang.String)
     */
    @Override
    public boolean isPatientInCallRegimen(String motechId) {
        return pillReminderService.getPillRegimen(motechId) != null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.motechproject.demo.pillreminder.support.PillReminders#registerNewPatient
     * (java.lang.String, java.lang.String)
     */
    @Override
    public String registerNewPatientIntoAdherenceCallRegimen(String motechId, String dosageStartTime) {
        DosageRequest dosageRequest = buildDosageRequest(dosageStartTime);
        DailyPillRegimenRequest regimenRequest = new DailyPillRegimenRequest(motechId, PILL_WINDOW_IN_HOURS,
                REMINDER_RETRY_INTERVAL_IN_MINUTES, REMINDER_BUFFER_TIME_IN_MINUTES, Arrays.asList(dosageRequest));

        pillReminderService.createNew(regimenRequest);

        return actualStartTime(dosageRequest);
    }

    private String actualStartTime(DosageRequest dosageRequest) {
        return dosageRequest.getStartHour() + ":"
                + String.format("%02d", (dosageRequest.getStartMinute() + REMINDER_BUFFER_TIME_IN_MINUTES));
    }

    private DosageRequest buildDosageRequest(String dosageStartTime) {
        DateTime currentTime = DateUtil.now();
        DateTime tomorrow = currentTime.plusDays(1);
        String[] time = dosageStartTime.split(":");

        MedicineRequest medicineRequest = new MedicineRequest("Demo Prescription", currentTime.toLocalDate(),
                tomorrow.toLocalDate());

        DosageRequest request = new DosageRequest(Integer.parseInt(time[0]), Integer.parseInt(time[1]),
                Arrays.asList(medicineRequest));

        return request;
    }
}
