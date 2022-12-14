package org.jrtech.azure.speech.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import org.jrtech.azure.speech.helper.AbstractSpeechTranscriberHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StopWatch;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.event.MouseEvent;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vlayout;

public class HomeComposer extends GenericForwardComposer<Vlayout> {

	private static final long serialVersionUID = 1L;

	private static final Logger LOGGER = LoggerFactory.getLogger(HomeComposer.class);

	public static final int DEFAULT_BUFFER_SIZE = 8192;
	
	public static final String VAR_AUDIO_TEMP_FILE_URI = "app-var-audio-temp-file-uri"; 

	private Combobox serviceRegion;

	private Textbox serviceKey;
	
	private Textbox consoleLog;

	private Label audioFile;

	private Combobox language;

	private File uploadedAudioFile = null;
	
	private final NumberFormat nf = NumberFormat.getIntegerInstance(Locale.ENGLISH);
	
	private final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");  
	
	@Override
	public void doAfterCompose(Vlayout comp) throws Exception {
		super.doAfterCompose(comp);
	}

	public void onUpload$uploadButton(UploadEvent event) {
		uploadedAudioFile = null;
		if (event.getMedias() != null) {
			for (Media media : event.getMedias()) {
				try {
					uploadedAudioFile = File.createTempFile("speech-", "." + media.getFormat());
				} catch (IOException ex) {
					LOGGER.error("Fail to create temp file.", ex);
					break;
				}

				try (FileOutputStream fos = new FileOutputStream(uploadedAudioFile)) {
					int read;
					byte[] bytes = new byte[DEFAULT_BUFFER_SIZE];
					final InputStream is = media.getStreamData();
					while ((read = is.read(bytes)) != -1) {
						fos.write(bytes, 0, read);
					}
				} catch (IOException ex) {
					LOGGER.error("Fail to process to temp file.", ex);
					break;
				}
				
				audioFile.setValue(media.getName());
				
				writeToConsoleLog("Uploading '" + media.getName() + "' to a temporary file: " + uploadedAudioFile.toURI().toString());
				writeToConsoleLog("Content Type: " + media.getContentType());
				writeToConsoleLog("Format: " + media.getFormat());

				
				// only take 1 media
				break;
			}
		}
	}
	
	private void writeToConsoleLog(String text) {
		if (consoleLog.getText().length() > 0) {
			consoleLog.setText(consoleLog.getText() + "\n");
		}
		consoleLog.setText(consoleLog.getText() + text);
	}

	public void onClick$transcribeButton(MouseEvent event) {
		if (uploadedAudioFile == null) {
			Messagebox.show("No audio file uploaded in system. Please upload an Audio file to allow transcription.");
			return;
		}
		
		final AbstractSpeechTranscriberHelper speechServiceHelper = AbstractSpeechTranscriberHelper.getSpeechTranscriberHelperByRegion(serviceKey.getText(), serviceRegion.getText());

		StopWatch sw = new StopWatch();
		try (final InputStream is = new FileInputStream(uploadedAudioFile)) {
			sw.start();
			String textTranscription = speechServiceHelper.transcribe(is, language.getText());
			sw.stop();
			writeToConsoleLog("\n" + df.format(Calendar.getInstance().getTime()) + " Transcription result within " + nf.format(sw.getLastTaskTimeMillis()) + " ms.:\n" + textTranscription);
			writeToConsoleLog("Processing log:\n" + speechServiceHelper.getServiceLog());
			speechServiceHelper.resetServiceLog();
		} catch (Exception e) {
			final String message = "Failure in transcribing audio media: " + uploadedAudioFile.getName();
			LOGGER.error(message, e);
			Messagebox.show(message, "Processing Failure", Messagebox.OK, Messagebox.ERROR);
		}
	}
	
	public void onClick$clearConsoleButton(MouseEvent event) {
		consoleLog.setText("");
	}
}
