package org.motechproject.icappr.handlers;

import org.joda.time.DateTime;
import org.motechproject.commcare.domain.CommcareForm;
import org.motechproject.commcare.domain.FormValueElement;
import org.motechproject.commons.date.util.DateUtil;
import org.motechproject.couch.mrs.model.CouchPerson;
import org.motechproject.icappr.couchdb.CouchMrsConstants;
import org.motechproject.icappr.couchdb.CouchPersonUtil;
import org.motechproject.icappr.domain.IVREnrollmentRequest;
import org.motechproject.icappr.service.IVRUIEnroller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class IVRUITestFormHandler {

	private final IVRUIEnroller enroller;
	private final CouchPersonUtil couchPersonUtil;

	@Autowired
	public IVRUITestFormHandler(IVRUIEnroller enroller, CouchPersonUtil couchPersonUtil) {
		this.enroller = enroller;
		this.couchPersonUtil = couchPersonUtil;
	}

	public void handleForm(CommcareForm form) {
		FormValueElement topFormElement = form.getForm();

		if (topFormElement == null) {
			return;
		}

		String phoneNumber = getValue(topFormElement, "phone_number");
		String pin = getValue(topFormElement, "pin");
		CouchPerson person = couchPersonUtil.createAndSavePerson(phoneNumber, pin);
		enrollInCalls(person);
	}

	private void enrollInCalls(CouchPerson person) {
		IVREnrollmentRequest request = new IVREnrollmentRequest();
		request.setPhoneNumber(couchPersonUtil.getAttribute(person, CouchMrsConstants.PHONE_NUMBER).getValue());
		request.setPin(couchPersonUtil.getAttribute(person, CouchMrsConstants.PERSON_PIN).getValue());
		request.setMotechID(person.getId());
		DateTime dateTime = DateUtil.now().plusMinutes(2);
		request.setCallStartTime(String.format("%02d:%02d",
				dateTime.getHourOfDay(), dateTime.getMinuteOfHour()));
		enroller.enrollPerson(request);
	}

	private String getValue(FormValueElement formElement, String elementName) {
		FormValueElement ivrElement = formElement.getElementByName(elementName);
		if (ivrElement == null) {
			return null;
		}
		return ivrElement.getValue();
	}

}