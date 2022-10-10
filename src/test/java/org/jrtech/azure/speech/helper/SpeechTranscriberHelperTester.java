package org.jrtech.azure.speech.helper;

import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.util.StopWatch;

public class SpeechTranscriberHelperTester {

	private String[][] testInputFiles = new String[][] {
		// @formatter:off
			{"enrollment_audio_katie.wav", "1"},
			{"enrollment_audio_steve.wav", "1"},
			{"katiesteve.wav", "8"}
		// @formatter:on
	};

	@Test
	public void transcribe() {
		SpeechTranscriberHelper helper = new SpeechTranscriberHelper(TesterConstants.SERVICE_KEY, TesterConstants.REGION_WEU);
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
			} catch (InterruptedException | IOException e) {
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
}
