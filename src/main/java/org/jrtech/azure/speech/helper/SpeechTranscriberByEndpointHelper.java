package org.jrtech.azure.speech.helper;

import java.net.URI;
import java.net.URISyntaxException;

import com.microsoft.cognitiveservices.speech.SpeechConfig;

public class SpeechTranscriberByEndpointHelper extends AbstractSpeechTranscriberHelper {

	final private URI serviceEndpointURI;
	
	public SpeechTranscriberByEndpointHelper(String serviceKey, String serviceEndpoint) throws URISyntaxException {
		super(serviceKey);
		this.serviceEndpointURI = new URI(serviceEndpoint);
	}

	@Override
	SpeechConfig getSpeechConfig() throws URISyntaxException {
		return SpeechConfig.fromEndpoint(serviceEndpointURI, serviceKey);
	}

}
