package org.jrtech.azure.speech.helper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import org.jrtech.audio.wave.WavHeader;
import org.jrtech.audio.wave.WavHeaderReader;

import com.microsoft.cognitiveservices.speech.CancellationReason;
import com.microsoft.cognitiveservices.speech.ResultReason;
import com.microsoft.cognitiveservices.speech.SessionEventArgs;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechRecognitionCanceledEventArgs;
import com.microsoft.cognitiveservices.speech.SpeechRecognitionEventArgs;
import com.microsoft.cognitiveservices.speech.SpeechRecognizer;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import com.microsoft.cognitiveservices.speech.audio.AudioInputStream;
import com.microsoft.cognitiveservices.speech.audio.AudioStreamFormat;
import com.microsoft.cognitiveservices.speech.audio.PushAudioInputStream;
import com.microsoft.cognitiveservices.speech.util.EventHandler;

public abstract class AbstractSpeechTranscriberHelper {

	private StringBuffer serviceLogBuffer = new StringBuffer();

	final protected String serviceKey;

	protected AbstractSpeechTranscriberHelper(String serviceKey) {
		this.serviceKey = serviceKey;
	}

	public String getServiceLog() {
		return serviceLogBuffer.toString();
	}

	public void resetServiceLog() {
		serviceLogBuffer = new StringBuffer();
	}

	protected void writeToServiceLogBuffer(String logEntry) {
		serviceLogBuffer.append(logEntry + "\n");
	}

	protected static byte[] readWavHeaderBytes(InputStream inputStream) throws IOException {
		final byte[] bytes = new byte[WavHeader.HEADER_SIZE];
		int res = inputStream.read(bytes);
		if (res != WavHeader.HEADER_SIZE) {
			throw new IOException("Could not read header.");
		}

		return bytes;
	}

	public String transcribe(InputStream inputStream) throws Exception {
		return transcribe(inputStream, "en-US");
	}

	public String transcribe(InputStream inputStream, String languageCode) throws Exception {
		if (inputStream == null)
			return "";

		// Read initial Wave file Headers -> 44 bytes
		final byte[] headerBytes = readWavHeaderBytes(inputStream);
		WavHeader audioHeader = WavHeaderReader.read(headerBytes);
		writeToServiceLogBuffer("WAV Headers:\n" + audioHeader);

		final StringBuffer resultBuffer = new StringBuffer();

		// Set audio format
		// long samplesPerSecond = 16000;
		// short bitsPerSample = 16;

		// Create the push stream
		PushAudioInputStream pushStream = AudioInputStream.createPushStream(AudioStreamFormat.getWaveFormatPCM(
				audioHeader.getSampleRate(), audioHeader.getBitsPerSample(), audioHeader.getNumChannels()));

		// Creates speech configuration with subscription information
		try (SpeechConfig speechConfig = getSpeechConfig()) {

			final MyCanceledEventHandler canceledHandler = new MyCanceledEventHandler() {
				@Override
				public void onEvent(Object service, SpeechRecognitionCanceledEventArgs event) {
					writeToServiceLogBuffer(
							"CANCELED: s-object=" + (service == null ? "[NULL]" : service.getClass().getName()));
					writeToServiceLogBuffer("CANCELED: Reason=" + event.getReason());

					if (event.getReason() == CancellationReason.Error) {
						setProcessingError(new ProcessingError("" + event.getErrorCode(), event.getErrorDetails()));
//
//						writeToServiceLogBuffer("CANCELED: ErrorCode=" + event.getErrorCode());
//						writeToServiceLogBuffer("CANCELED: ErrorDetails=" + event.getErrorDetails());
					}
					setCanceled(true);
				}
			};

			final EventHandler<SpeechRecognitionEventArgs> recognizedHandler = (service, event) -> {
				if (event.getResult().getReason() == ResultReason.RecognizedSpeech) {
					writeToServiceLogBuffer("RECOGNIZED: Text=" + event.getResult().getText());
					resultBuffer.append(event.getResult().getText() + "\n");
				} else if (event.getResult().getReason() == ResultReason.NoMatch) {
					writeToServiceLogBuffer("NOMATCH: Speech could not be recognized.");
				}
			};

			final EventHandler<SessionEventArgs> sessionStartedHandler = (service, event) -> {
				writeToServiceLogBuffer("*** Session started event.");
			};

			final EventHandler<SessionEventArgs> sessionStoppedHandler = (service, event) -> {
				writeToServiceLogBuffer("*** Session stopped event.");
			};

			// Creates conversation and transcriber objects using push stream as audio
			// input.
			try (AudioConfig audioInput = AudioConfig.fromStreamInput(pushStream);
					SpeechRecognizer recognizer = new SpeechRecognizer(speechConfig, languageCode, audioInput)) {

				// Subscribes to events.
				recognizer.recognized.addEventListener(recognizedHandler);
				recognizer.canceled.addEventListener(canceledHandler);
				recognizer.sessionStarted.addEventListener(sessionStartedHandler);
				recognizer.sessionStopped.addEventListener(sessionStoppedHandler);

				// Starts continuous recognition. Uses stopContinuousRecognitionAsync() to stop
				// recognition.
				recognizer.startContinuousRecognitionAsync().get();

				// push initial headerBytes;
				pushStream.write(headerBytes);

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

				while (!canceledHandler.isCanceled()) {
					Thread.sleep(100);
				}

				recognizer.stopContinuousRecognitionAsync().get();
				
				if (canceledHandler.hasError()) {
					writeToServiceLogBuffer("Transcription process has the following errors:");
					writeToServiceLogBuffer(canceledHandler.processingError.toString());
				} else {
					writeToServiceLogBuffer("Transcription process successful!");
				}

				recognizer.recognized.removeEventListener(recognizedHandler);
				recognizer.canceled.removeEventListener(canceledHandler);
				recognizer.sessionStarted.removeEventListener(sessionStartedHandler);
				recognizer.sessionStopped.removeEventListener(sessionStoppedHandler);
			} catch (ExecutionException ex) {
				writeToServiceLogBuffer("[Exception] - Unexpected exception: " + ex.getMessage());
			} finally {
				if (pushStream != null) {
					pushStream.close();
				}
			}

		}
		return resultBuffer.toString();
	}

	static class ProcessingError {
		public final String errorCode;
		public final String errorDetail;

		protected ProcessingError(String errorCode, String errorDetail) {
			this.errorCode = errorCode;
			this.errorDetail = errorDetail;
		}

		@Override
		public String toString() {
			return "ErrorCode = '" + errorCode + "' -> " + errorDetail;
		}
	}

	abstract static class MyCanceledEventHandler implements EventHandler<SpeechRecognitionCanceledEventArgs> {

		private boolean canceled = false;

		private ProcessingError processingError;

		public void setCanceled(boolean canceled) {
			this.canceled = canceled;
		}

		public boolean isCanceled() {
			return canceled;
		}

		protected void setProcessingError(ProcessingError processingError) {
			this.processingError = processingError;
		}

		public ProcessingError getProcessingError() {
			return processingError;
		}

		public boolean hasError() {
			return processingError != null;
		}
	}

	abstract static class MyStoppedEventHandler implements EventHandler<SessionEventArgs> {
		private boolean stopped = false;

		public void setStopped(boolean stopped) {
			this.stopped = stopped;
		}

		public boolean isStopped() {
			return stopped;
		}
	}

	abstract SpeechConfig getSpeechConfig() throws Exception;

	public static SpeechTranscriberByRegionHelper getSpeechTranscriberHelperByRegion(String serviceKey, String region) {
		return new SpeechTranscriberByRegionHelper(serviceKey, region);
	}

	public static SpeechTranscriberByEndpointHelper getSpeechTranscriberHelperByEndpoint(String serviceKey,
			String serviceEndpoint) throws URISyntaxException {
		return new SpeechTranscriberByEndpointHelper(serviceKey, serviceEndpoint);
	}
}
