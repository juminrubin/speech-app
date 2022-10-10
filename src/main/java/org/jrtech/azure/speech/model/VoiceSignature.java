package org.jrtech.azure.speech.model;

// Class which defines VoiceSignature as specified under
// https://aka.ms/cts/signaturegenservice.
public class VoiceSignature {
	public String Status;
	public VoiceSignatureData Signature;
	public String Transcription;

	// Class which defines VoiceSignatureData which is used when creating/adding
	// participants
	public static class VoiceSignatureData {
		public String Version;
		public String Tag;
		public String Data;
	}
}
