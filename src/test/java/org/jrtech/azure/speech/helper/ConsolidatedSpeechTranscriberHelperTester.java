package org.jrtech.azure.speech.helper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.text.NumberFormat;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.util.StopWatch;

public class ConsolidatedSpeechTranscriberHelperTester {

	private String[][] testInputFiles = new String[][] {
		// @formatter:off
			{"enrollment_audio_katie.wav", "1"},
//			{"enrollment_audio_steve.wav", "1"},
//			{"katiesteve.wav", "8"}
		// @formatter:on
	};
	
	private void runTranscriptionTest(AbstractSpeechTranscriberHelper helper) {
		final StopWatch sw = new StopWatch();
		final NumberFormat nf = NumberFormat.getIntegerInstance();
		for (String[] inputFileDef : testInputFiles) {
			InputStream inputStream = getClass().getClassLoader().getResourceAsStream("testdata/wav/" + inputFileDef[0]);

			try {
				System.out.println("File: " + inputFileDef[0]);
				sw.start(inputFileDef[0]);
				String textTranscription = helper.transcribe(inputStream);
				sw.stop();
				System.out.println("Transcription result within " + nf.format(sw.getLastTaskTimeMillis()) + " ms.:\n" + textTranscription);
				System.out.println("Processing log:\n" + helper.getServiceLog());
				helper.resetServiceLog();
			} catch (Exception e) {
				Assertions.fail("Fail to transcribe input file: " + inputFileDef, e);
			} finally {
				if (inputStream != null) {
					try {
						inputStream.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	@Test
	public void transcribeByRegion() throws URISyntaxException {
		System.out.println("### Transcribe by Region");
		AbstractSpeechTranscriberHelper helper = AbstractSpeechTranscriberHelper.getSpeechTranscriberHelperByRegion(TesterConstants.SERVICE_KEY, TesterConstants.REGION_WEU);
		runTranscriptionTest(helper);
	}
	
	@Test
	public void transcribeByEndpoint() throws URISyntaxException {
		System.out.println("### Transcribe by Endpoint");
		AbstractSpeechTranscriberHelper helper = AbstractSpeechTranscriberHelper.getSpeechTranscriberHelperByEndpoint(TesterConstants.SERVICE_KEY, TesterConstants.SERVICE_ENDPOINT);
		runTranscriptionTest(helper);
	}
}
