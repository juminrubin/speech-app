package org.jrtech.audi.wave;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.jrtech.audio.wave.WavHeader;
import org.jrtech.audio.wave.WavHeaderReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class WavHeaderReaderTester {

	private String[] testInputFiles = new String[] {
			// @formatter:off
				"enrollment_audio_katie.wav",
				"enrollment_audio_steve.wav",
				"katiesteve.wav"
			// @formatter:on
	};

	@Test
	public void read() {
		for (String inputFileName : testInputFiles) {
			InputStream inputStream = getClass().getClassLoader().getResourceAsStream("testdata/wav/" + inputFileName);

			WavHeader wavHeader;
			try {
				System.out.println("### Processing file: " + inputFileName);
				WavHeaderReader wavHeaderReader = new WavHeaderReader(inputStream);
				wavHeader = wavHeaderReader.read();
				System.out.println(wavHeader.toString());
				System.out.println("\n\n");
			} catch (IOException e) {
				Assertions.fail("Error: " + e.getMessage(), e);
			} finally {
				if (inputStream != null) {
					try {
						inputStream.close();
					} catch (IOException e) {
						// ignore error when closing
					}
				}
			}
		}
	}
	
}
