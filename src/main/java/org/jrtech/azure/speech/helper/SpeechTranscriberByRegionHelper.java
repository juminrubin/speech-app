package org.jrtech.azure.speech.helper;

import com.microsoft.cognitiveservices.speech.SpeechConfig;

public class SpeechTranscriberByRegionHelper extends AbstractSpeechTranscriberHelper {

	final private String serviceRegion;
	
	public SpeechTranscriberByRegionHelper(String serviceKey, String serviceRegion) {
		super(serviceKey);
		this.serviceRegion = serviceRegion;
	}

	@Override
	SpeechConfig getSpeechConfig() {
		return SpeechConfig.fromSubscription(serviceKey, serviceRegion);
	}

}
