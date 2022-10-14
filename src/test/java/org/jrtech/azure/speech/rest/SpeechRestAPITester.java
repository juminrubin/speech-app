package org.jrtech.azure.speech.rest;

//// This sample uses the Apache HTTP client from HTTP Components (http://hc.apache.org/httpcomponents-client-ga/)
import java.net.URI;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jrtech.azure.speech.helper.TesterConstants;

public class SpeechRestAPITester {
	public static void main(String[] args) {
		final HttpClient httpclient = HttpClients.createDefault();

		try {
			URIBuilder builder = new URIBuilder("https://weu-speech-service.cognitiveservices.azure.com/speechtotext/v3.0/endpoints");
			builder.setParameter("skip", "0");
			builder.setParameter("top", "1");

			URI uri = builder.build();
			System.out.println("URI: " + uri.toString());
			HttpGet request = new HttpGet(uri);
			request.setHeader("Ocp-Apim-Subscription-Key", TesterConstants.SERVICE_KEY);

			// Request body
//			StringEntity reqEntity = new StringEntity("{body}");
//			request.setEntity(reqEntity);

			HttpResponse response = httpclient.execute(request);
			HttpEntity entity = response.getEntity();

			if (entity != null) {
				System.out.println(EntityUtils.toString(entity));
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
}
