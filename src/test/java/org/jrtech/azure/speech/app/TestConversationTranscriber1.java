package org.jrtech.azure.speech.app;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Arrays;
import java.util.Scanner;
import java.util.UUID;

import org.jrtech.azure.speech.helper.TesterConstants;
import org.jrtech.azure.speech.model.VoiceSignature;

import com.google.gson.Gson;
import com.microsoft.cognitiveservices.speech.CancellationReason;
import com.microsoft.cognitiveservices.speech.ResultReason;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import com.microsoft.cognitiveservices.speech.audio.AudioInputStream;
import com.microsoft.cognitiveservices.speech.audio.AudioStreamFormat;
import com.microsoft.cognitiveservices.speech.audio.PushAudioInputStream;
import com.microsoft.cognitiveservices.speech.transcription.Conversation;
import com.microsoft.cognitiveservices.speech.transcription.ConversationTranscriber;
import com.microsoft.cognitiveservices.speech.transcription.Participant;

public class TestConversationTranscriber1 {

	public static VoiceSignature generateVoiceSignature(byte[] voiceSampleData, String subscriptionKey, String region)
			throws IOException, InterruptedException {
		String httpsURL = "https://signature." + region
				+ ".cts.speech.microsoft.com/api/v1/Signature/GenerateVoiceSignatureFromByteArray";
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(httpsURL))
				.header("Ocp-Apim-Subscription-Key", subscriptionKey).timeout(Duration.ofMinutes(1))
				.POST(BodyPublishers.ofByteArray(voiceSampleData)).build();
		HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
		Gson gson = new Gson();
		VoiceSignature voiceSignature = gson.fromJson(response.body(), VoiceSignature.class);
		return voiceSignature;
	}

	private static byte[] loadTestData(String testDataFile) throws IOException {
		try (InputStream is = TestConversationTranscriber1.class.getClassLoader()
				.getResourceAsStream("testdata/wav/" + testDataFile)) {
			return is.readAllBytes();
		}
	}

	private static void printVoiceSignature(String label, VoiceSignature person, String voiceSignatureString) {
		System.out.println(label);
		System.out.println("Status: " + person.Status);
		System.out.println("Transcription: " + person.Transcription);
		System.out.println("Voice Signature Data 1\n" + voiceSignatureString + "\n");
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		// Replace below with your own subscription key
		String subscriptionKey = TesterConstants.SERVICE_KEY;
		// Replace below with your own service region (e.g., "centralus").
		String serviceRegion = TesterConstants.REGION_WEU;

		// The input audio wave format for voice signatures is 16-bit samples, 16 kHz
		// sample rate, and a single channel (mono).
		// The recommended length for each sample is between thirty seconds and two
		// minutes.
		String voiceSignatureWaveForUser1 = "enrollment_audio_katie.wav";
		String voiceSignatureWaveForUser2 = "enrollment_audio_steve.wav";

		byte[] voiceSignatureWaveForUser1Bytes = loadTestData(voiceSignatureWaveForUser1);
		byte[] voiceSignatureWaveForUser2Bytes = loadTestData(voiceSignatureWaveForUser2);

		// Create voice signatures for the user1 and user2 and serialize it to json
		// string
		VoiceSignature voiceSignatureUser1 = generateVoiceSignature(voiceSignatureWaveForUser1Bytes, subscriptionKey,
				serviceRegion);

		VoiceSignature voiceSignatureUser2 = generateVoiceSignature(voiceSignatureWaveForUser2Bytes, subscriptionKey,
				serviceRegion);

		Gson gson = new Gson();
		String voiceSignatureStringUser1 = gson.toJson(voiceSignatureUser1.Signature);
		printVoiceSignature("Voice Signature 1", voiceSignatureUser1, voiceSignatureStringUser1);

		String voiceSignatureStringUser2 = gson.toJson(voiceSignatureUser2.Signature);
		printVoiceSignature("Voice Signature 2", voiceSignatureUser2, voiceSignatureStringUser2);

		// This sample expects a wavfile which is captured using a supported Speech SDK
		// devices (8 channel, 16kHz, 16-bit PCM)
		// See
		// https://docs.microsoft.com/azure/cognitive-services/speech-service/speech-devices-sdk-microphone
		// InputStream inputStream = new FileInputStream("katiesteve.wav");
		InputStream inputStream = TestConversationTranscriber1.class.getClassLoader()
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
			speechConfig.setProperty("ConversationTranscriptionInRoomAndOnline", "true");

			// Conversation identifier is required when creating conversation.
			UUID conversationId = UUID.randomUUID();

			// Creates conversation and transcriber objects using push stream as audio
			// input.
			try (Conversation conversation = Conversation
					.createConversationAsync(speechConfig, conversationId.toString()).get();
					AudioConfig audioInput = AudioConfig.fromStreamInput(pushStream);
					ConversationTranscriber transcriber = new ConversationTranscriber(audioInput)) {

				System.out.println("Starting conversation...");

				// Subscribes to events
				transcriber.transcribed.addEventListener((s, e) -> {
					// In successful transcription, UserId will show the id of the conversation
					// participant.
					if (e.getResult().getReason() == ResultReason.RecognizedSpeech) {
						System.out.println("TRANSCRIBED: Text=" + e.getResult().getText() + " UserId="
								+ e.getResult().getUserId());
					} else if (e.getResult().getReason() == ResultReason.NoMatch) {
						System.out.println("NOMATCH: Speech could not be recognized.");
					}
				});

				transcriber.canceled.addEventListener((s, e) -> {
					System.out.println("CANCELED: Reason=" + e.getReason());

					if (e.getReason() == CancellationReason.Error) {
						System.out.println("CANCELED: ErrorCode=" + e.getErrorCode());
						System.out.println("CANCELED: ErrorDetails=" + e.getErrorDetails());
						System.out.println("CANCELED: Did you update the subscription info?");
					}
				});

				transcriber.sessionStarted.addEventListener((s, e) -> {
					System.out.println("\n    Session started event.");
				});

				transcriber.sessionStopped.addEventListener((s, e) -> {
					System.out.println("\n    Session stopped event.");
				});

				// Add participants to the conversation with their voice signatures.
				Participant participant1 = Participant.from("katie@example.com", "en-us", voiceSignatureStringUser1);
				conversation.addParticipantAsync(participant1);
				Participant participant2 = Participant.from("stevie@example.com", "en-us", voiceSignatureStringUser2);
				conversation.addParticipantAsync(participant2);

				// Transcriber must be joined to the conversation before starting transcription.
				transcriber.joinConversationAsync(conversation).get();

				// Starts continuous transcription. Use stopTranscribingAsync() to stop
				// transcription.
				transcriber.startTranscribingAsync().get();

				// Arbitrary buffer size.
				byte[] readBuffer = new byte[4096];

				// Push audio reads from the file into the PushStream.
				// The audio can be pushed into the stream before, after, or during
				// transcription
				// and transcription will continue as data becomes available.
				int bytesRead;
				while ((bytesRead = inputStream.read(readBuffer)) != -1) {
					if (bytesRead == readBuffer.length) {
						pushStream.write(readBuffer);
					} else {
						// Last buffer read from the WAV file is likely to have less bytes.
						pushStream.write(Arrays.copyOfRange(readBuffer, 0, bytesRead));
					}
				}

				pushStream.close();
				inputStream.close();

				System.out.println("Press any key to stop transcription");
				try (Scanner scanner = new Scanner(System.in)) {
					scanner.nextLine();
				}

				transcriber.stopTranscribingAsync().get();

			} catch (Exception ex) {
				System.out.println("Unexpected exception: " + ex.getMessage());

				assert (false);
				System.exit(1);
			}
		}
	}

}
