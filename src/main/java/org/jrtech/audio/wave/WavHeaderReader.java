package org.jrtech.audio.wave;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class WavHeaderReader {
    private byte[] buffer;
    private WavHeader header;
    private InputStream inputStream;

    public WavHeaderReader() {
    }

    public WavHeaderReader(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public WavHeader read() throws IOException {
    	buffer = new byte[WavHeader.HEADER_SIZE];
        int res = inputStream.read(buffer);
        if (res != WavHeader.HEADER_SIZE) {
            throw new IOException("Could not read header.");
        }
        
        header = read(buffer);
        
        return header;
    }
    
    public static final WavHeader read(byte[] headerBytes) throws IOException {
    	WavHeader header = new WavHeader();
        header.setChunkID(Arrays.copyOfRange(headerBytes, 0, 4));
        if (new String(header.getChunkID()).compareTo("RIFF") != 0) {
            throw new IOException("Illegal format.");
        }
        header.setChunkSize(toInt(headerBytes, 4, false));
        header.setFormat(Arrays.copyOfRange(headerBytes, 8, 12));
        header.setSubChunk1ID(Arrays.copyOfRange(headerBytes, 12, 16));
        header.setSubChunk1Size(toInt(headerBytes, 16, false));
        header.setAudioFormat(toShort(headerBytes, 20, false));
        header.setNumChannels(toShort(headerBytes, 22, false));
        header.setSampleRate(toInt(headerBytes, 24, false));
        header.setByteRate(toInt(headerBytes, 28, false));
        header.setBlockAlign(toShort(headerBytes, 32, false));
        header.setBitsPerSample(toShort(headerBytes, 34, false));
        header.setSubChunk2ID(Arrays.copyOfRange(headerBytes, 36, 40));
        header.setSubChunk2Size(toInt(headerBytes, 40, false));
        
        return header;
    }

    /**
     * Convert byte[] array to int number
     *
     * @param start  start position of the buffer
     * @param endian <code>true</code> for big-endian
     *               <code>false</code> for little-endian
     * @return converted number
     */
    private static int toInt(byte[] headerBytes, int start, boolean endian) {
        int k = (endian) ? 1 : -1;
        if (!endian) {
            start += 3;
        }
        return (headerBytes[start] << 24) + (headerBytes[start + k * 1] << 16) +
                (headerBytes[start + k * 2] << 8) + headerBytes[start + k * 3];
    }

    /**
     * Convert byte[] array to short number
     *
     * @param start  start position of the buffer
     * @param endian <code>true</code> for big-endian
     *               <code>false</code> for little-endian
     * @return converted number
     */
    private static short toShort(byte[] headerBytes, int start, boolean endian) {
        short k = (endian) ? (short) 1 : -1;
        if (!endian) {
            start++;
        }
        return (short) ((headerBytes[start] << 8) + (headerBytes[start + k * 1]));
    }

    /**
     * Return the buffer which contain wave header
     *
     * @return buffer
     */
    public byte[] getBuffer() {
        return buffer;
    }

    /**
     * Return WavHeader object which contain wave header
     *
     * @return WavHeader object
     */
    public WavHeader getHeader() {
        return header;
    }

}
