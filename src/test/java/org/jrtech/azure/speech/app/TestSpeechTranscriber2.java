package org.jrtech.azure.speech.app;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

import org.jrtech.azure.speech.helper.TesterConstants;

import com.microsoft.cognitiveservices.speech.CancellationReason;
import com.microsoft.cognitiveservices.speech.ResultReason;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechRecognizer;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import com.microsoft.cognitiveservices.speech.audio.AudioInputStream;
import com.microsoft.cognitiveservices.speech.audio.AudioStreamFormat;
import com.microsoft.cognitiveservices.speech.audio.PushAudioInputStream;

public class TestSpeechTranscriber2 {

	public static void main(String[] args) throws IOException, InterruptedException {
		// Replace below with your own subscription key
		String subscriptionKey = TesterConstants.SERVICE_KEY;
		// Replace below with your own service region (e.g., "centralus").
		String serviceRegion = TesterConstants.REGION_WEU;

		// This sample expects a wavfile which is captured using a supported Speech SDK
		// devices (8 channel, 16kHz, 16-bit PCM)
		// See
		// https://docs.microsoft.com/azure/cognitive-services/speech-service/speech-devices-sdk-microphone
		// InputStream inputStream = new FileInputStream("katiesteve.wav");
		InputStream inputStream = TestSpeechTranscriber2.class.getClassLoader()
				.getResourceAsStream("testdata/wav/" + "katiesteve.wav");

		// Set audio format
		long samplesPerSecond = 16000;
		short bitsPerSample = 16;
		short channels = 8;

		// Create the push stream
		PushAudioInputStream pushStream = AudioInputStream
				.createPushStream(AudioStreamFormat.getWaveFormatPCM(samplesPerSecond, bitsPerSample, channels));

		// Creates speech configuration with subscription information
		try (SpeechConfig speechConfig = SpeechConfig.fromSubscription(subscriptionKey, serviceRegion)) {

			// Creates conversation and transcriber objects using push stream as audio
			// input.
			try (AudioConfig audioInput = AudioConfig.fromStreamInput(pushStream);
					SpeechRecognizer recognizer = new SpeechRecognizer(speechConfig, "en-US", audioInput)) {

				System.out.println("Starting transcribing...");

				// Subscribes to events.
				recognizer.recognizing.addEventListener((s, e) -> {
					System.out.println("RECOGNIZING: Text=" + e.getResult().getText());
				});

				recognizer.recognized.addEventListener((s, e) -> {
					if (e.getResult().getReason() == ResultReason.RecognizedSpeech) {
						System.out.println("RECOGNIZED: Text=" + e.getResult().getText());
					} else if (e.getResult().getReason() == ResultReason.NoMatch) {
						System.out.println("NOMATCH: Speech could not be recognized.");
					}
				});

				recognizer.canceled.addEventListener((s, e) -> {
					System.out.println("CANCELED: Reason=" + e.getReason());

					if (e.getReason() == CancellationReason.Error) {
						System.out.println("CANCELED: ErrorCode=" + e.getErrorCode());
						System.out.println("CANCELED: ErrorDetails=" + e.getErrorDetails());
						System.out.println("CANCELED: Did you update the subscription info?");
					}
				});

				recognizer.sessionStarted.addEventListener((s, e) -> {
					System.out.println("\n    Session started event.");
				});

				recognizer.sessionStopped.addEventListener((s, e) -> {
					System.out.println("\n    Session stopped event.");
				});

				// Starts continuous recognition. Uses stopContinuousRecognitionAsync() to stop
				// recognition.
				System.out.println("Say something...");
				recognizer.startContinuousRecognitionAsync().get();

				// Arbitrary buffer size.
				byte[] readBuffer = new byte[4096];

				// Push audio read from the file into the PushStream.
				// The audio can be pushed into the stream before, after, or during recognition
				// and recognition will continue as data becomes available.
				int bytesRead;
				while ((bytesRead = inputStream.read(readBuffer)) != -1) {
					if (bytesRead == readBuffer.length) {
						pushStream.write(readBuffer);
					} else {
						// Last buffer read from the WAV file is likely to have less bytes
						pushStream.write(Arrays.copyOfRange(readBuffer, 0, bytesRead));
					}
				}

				pushStream.close();
				inputStream.close();

				System.out.println("Press any key to stop");
				try (Scanner scanner = new Scanner(System.in)) {
					scanner.nextLine();
				}

				recognizer.stopContinuousRecognitionAsync().get();
			} catch (ExecutionException ex) {
				System.out.println("Unexpected exception: " + ex.getMessage());

				assert (false);
				System.exit(1);
			} finally {
				if (pushStream != null) {
					pushStream.close();
				}
			}

		}
	}

}
