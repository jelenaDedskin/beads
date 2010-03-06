package net.beadsproject.beads.data;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.tritonus.share.sampled.file.TAudioFileFormat;

import net.beadsproject.beads.core.AudioUtils;

/**
 * Represents an audio file. Handles loading and format conversion.
 * 
 * AudioFile is generally not used directly. Rather {@link net.beadsproject.beads.data.Sample Sample} 
 * should be used as it provides many useful features for playing audio files.
 * 
 * NOTE: At the moment certain .wav files will be not be able to be loaded. 
 * This is due to mp3spi recognizing them incorrectly as mp3s.
 * This will hopefull be fixed in the future, but until then 
 * you can resave your .wavs using a different audio util. 
 * 
 * @author ben
 */
public class AudioFile {	
	// AudioFile encapsulates one of the following. 
	// The one it encapsulates is none-null, the others are null.
	protected File file;
	protected URL url;
	protected AudioInputStream audioInputStream;
	
	// the name of this audiofile - corresponds to the name of file, url, or a inputstream generated id
	protected String name;
	
	protected AudioFileFormat audioFileFormat;
	/** The number of channels. */
	protected int nChannels;
	
	/** The total number of frames.
	 *  If it equals AudioSystem.NOT_SPECIFIED then the length is unknown. 
	 **/
	protected long nFrames;
	/** Length of the file in milliseconds */
	protected float length;
	
	private long nTotalFramesRead = 0; // also a pointer into the current pos
	private boolean finished = false;
	
	// stream-specific stuff
	private Map<String,Object> audioInfo;
	private long lengthInBytes; // length of file in bytes
	private AudioFormat encodedFormat;
	private AudioFormat decodedFormat;
	private AudioInputStream encodedStream;
	private AudioInputStream decodedStream;	
	private int numBytes = 2; // number of bytes per frame per channel (e.g., numBytes=2 for a 16-bit audio file)	
	private boolean isEncoded = false; // is the audio file encoded 
	private int bufferSize;
	
	/** Advanced
	 * 
	 * Trace the open, closing, and resetting of this audio file. Useful to debug and tune the parameters of AudioFile and Sample. 
	 * */ 
	public boolean trace = false;
	
	/**
	 * Load an audio file from disk. 
	 * The audiofile needs to be open()'ed before it's data can be read.
	 * Note: AudioFile provides low-level access to audio files -- If you just want to access the data of a sound file use a Sample.
	 * @see Sample 
	 * 
	 * @param filename The name of the file to open.
	 *
	 * @throws IOException If the file cannot be found or opened.  
	 * @throws UnsupportedAudioFileException If the file is of an unsupported audio type.
	 */
	public AudioFile(String filename) throws IOException, UnsupportedAudioFileException {
		this(filename,-1);
	}

	/**
	 * Advanced: Create an input stream from a file, but don't keep more than numBytes of data in memory.
	 * 
	 * @param filename
	 * @param bufferSize The maximum number of bytes the AudioFile can keep in memory. 
	 *                   If it is <0 then the length of the audio file is used.
	 *                   
	 * @throws IOException If the file cannot be found or opened.  
	 * @throws UnsupportedAudioFileException If the file is of an unsupported audio type.
	 */
	public AudioFile(String filename, int bufferSize) throws IOException, UnsupportedAudioFileException {
		//first try to interpret string as URL, then as local file
		try {
			url = new URL(filename);
			file = null;
			name = url.getFile();
		} catch(Exception e) {
			file = new File(filename);
			url = null;
			name = file.getPath();
		}
		audioInputStream = null;
		
		// check if .wav ending and detected as .mp3, if so it is bad!
		if (!name.endsWith(".mp3"))
		{
			if ((url!=null && AudioSystem.getAudioFileFormat(url) instanceof TAudioFileFormat) ||
				(file!=null && AudioSystem.getAudioFileFormat(file) instanceof TAudioFileFormat))
			{
				throw(new UnsupportedAudioFileException("Cannot read \"" + name + "\". If it is a .wav then try re-converting it in a different audio program."));
			}
		}		

		if (url!=null)
			audioFileFormat = AudioSystem.getAudioFileFormat(url);
		else if (file!=null)
			audioFileFormat = AudioSystem.getAudioFileFormat(file);
		
		// common init
		init(bufferSize);
		
	}

	public AudioFile(InputStream stream) throws IOException, UnsupportedAudioFileException {
		this(stream, -1);
	}
	
	public AudioFile(InputStream stream, int bufferSize) throws IOException, UnsupportedAudioFileException {
		BufferedInputStream bis = new BufferedInputStream(stream);
		audioFileFormat = AudioSystem.getAudioFileFormat(bis);
		audioInputStream = AudioSystem.getAudioInputStream(bis);
		url = null;
		file = null;
		name = stream.toString();
		init(bufferSize);		
	}
	
	private void init(int bufferSize) throws UnsupportedAudioFileException, IOException
	{
		nFrames = audioFileFormat.getFrameLength();
		
		if (audioFileFormat instanceof TAudioFileFormat && bufferSize < 0)
		{
			this.bufferSize = audioFileFormat.getByteLength() + 1024; // plus a little bit in case length is off... 
			// 1024*1024*10;
		}
		else if (bufferSize < 0)
		{
			this.bufferSize = 0;
		}
		else
		{		
			this.bufferSize = bufferSize;
		}
		nTotalFramesRead = 0;
		encodedStream = null;
		decodedStream = null;	
	}

	/**
	 * Reset the audio input stream. 
	 * 
	 * For some audio formats, this may involve re-opening the associated file.
	 */
	public void reset()
	{
		if (trace) System.err.printf("AudioFile \"%s\" reset\n",name);
		
		try{
			if (encodedStream.markSupported())		
			{		
				try{
					encodedStream.reset();
					if (finished)
						reopen();
					nTotalFramesRead = 0;
					finished = false;					
				}
				catch(IOException e)
				{
					reopen();
					nTotalFramesRead = 0;
					finished = false;
				}		
			}
			else
			{
				close();			
				reopen();
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Skips a number of frames.
	 * Note: this function skips frames, not bytes.
	 * Doesn't work for vbr!
	 * 
	 * Known issue, MP3 seeking is not precise.
	 * TODO: Fix this issue.
	 *
	 * @param frames Number of frames to skip
	 */
	public void skip(long frames)
	{
		if (frames<=0) return;
		if (trace) System.err.printf("AudioFile skip %d frames\n",frames);
		
		try {
			if (isEncoded && nFrames!=AudioSystem.NOT_SPECIFIED)
			{
				if (!audioInfo.containsKey("mp3.vbr") || (Boolean)audioInfo.get("mp3.vbr"))
				{
					System.out.println("Beads does not currently support seeking on variable bit rate mp3s.");			
					//TODO throw an error here rather than System.exit();
				}							
				
				/* test method, _read_ n frames */
				//byte[] foo = new byte[(int) (frames*nChannels*2)];
				//read(foo);				
				
				// skip by a proportion of the file
				// this technique is used in jlGui
				double rate = 1.0*frames/nFrames;
				long skipBytes = (long) Math.round(lengthInBytes * rate);
                long totalSkipped = 0;
                while (totalSkipped < skipBytes)
                {
                    long skipped = encodedStream.skip(skipBytes - totalSkipped);
                    totalSkipped += skipped;
                    if (skipped == 0) break;
                }             
                //System.out.printf("skip want: %db, got %db\n",skipBytes,totalSkipped);                
			}
			else
			{
				@SuppressWarnings("unused")
				long skipped = decodedStream.skip(nChannels*numBytes*frames);
				//System.out.printf("skip want: %db, got %db\n",nChannels*numBytes*frames,skipped);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		nTotalFramesRead += frames;
	}

	/**
	 * Seek to a specific frame number. Note that seeking is slower than skipping forward.
	 * 
	 * @param frame The frame number, relative to the start of the audio data.
	 */
	public void seek(int frame)
	{
		if (frame>=nTotalFramesRead)
		{
			skip(frame-nTotalFramesRead);
		}
		else
		{
			reset();
			skip(frame);
		}
	}

	/**
	 * Opens the audio file, ready for data access.
	 * 
	 * @throws UnsupportedAudioFileException
	 * @throws IOException
	 */
	public void open() throws UnsupportedAudioFileException, IOException
	{
		if (trace) System.err.printf("AudioFile \"%s\" open\n",name);
		finished = false;
		nTotalFramesRead = 0;
		 
		encodedStream = getStream();
		encodedFormat = encodedStream.getFormat();
		decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
				encodedFormat.getSampleRate(),
				16,
				encodedFormat.getChannels(),
				encodedFormat.getChannels()*2, // 2*8 = 16-bits per sample per channel
				44100,
				encodedFormat.isBigEndian());

		audioInfo = decodedFormat.properties();
		
		if (AudioSystem.isConversionSupported(decodedFormat, encodedFormat))
		{
			//TODO this might not be true - and sometimes we get stupid values for duration
			isEncoded = true;
			decodedStream = AudioSystem.getAudioInputStream(decodedFormat, encodedStream);
			nFrames = AudioSystem.NOT_SPECIFIED;
			//System.out.printf("frames (encoded): %d\n",(int)(encodedStream.getFrameLength()));
			//System.out.printf("frames (decoded): %d\n",(int)(decodedStream.getFrameLength()));
			//System.out.println("estimated duration " + (Long)audioFileFormat.properties().get("duration") / 60000000.);

			lengthInBytes = audioFileFormat.getByteLength();
			audioInfo = audioFileFormat.properties();
			length = getTimeLengthEstimation(audioInfo);
//			System.out.println("audio info " + audioInfo);
//			System.out.println("length " + length);
			if (length<0)
			{
				System.out.println("AudioFile cannot determine the duration of the file. Is it missing the duration tag?");
//				System.exit(1); //Ollie -- anything but sysexit! Not good for gigs.
				throw new UnsupportedAudioFileException("AudioFile cannot determine the duration of the file.");
			}
			else
			{
				nFrames = (long)(decodedFormat.getSampleRate() * (length/1000.));
			}			
		}
		else
		{
			// try to use the undecoded format
			isEncoded = false;
			decodedFormat = encodedFormat;
			decodedStream = encodedStream;
			
			nFrames = (int)(decodedStream.getFrameLength());
			length = 1000.f * decodedStream.getFrameLength() / decodedFormat.getSampleRate();
		}

		numBytes = decodedFormat.getSampleSizeInBits()/8;
		nChannels = decodedFormat.getChannels();
		

		if (nFrames==AudioSystem.NOT_SPECIFIED)
		{
			System.out.println("Cannot determine the length of the audio file: " + url.getFile());
			System.out.println("AudioFile needs to know the length in order to operate appropriately.");
			System.out.println("Now exiting.");
			System.exit(1);
		}
		
		if (file != null && encodedStream.markSupported()) {
			encodedStream.mark(Math.min(bufferSize,(int) file.length()));
		}
	}
	
	/// re-opens a file, resetting the file pointers, etc..
	/// note that this will not recalculate length, etc. 
	private void reopen() throws UnsupportedAudioFileException, IOException
	{
		if (trace) System.err.printf("AudioFile %s reopen\n",name);
		finished = false;
		nTotalFramesRead = 0;
		
//		if (file.exists())
		encodedStream = getStream();
		if (isEncoded)		
			decodedStream = AudioSystem.getAudioInputStream(decodedFormat, encodedStream);
		else
			decodedStream = encodedStream;

		if (file != null && encodedStream.markSupported())
			encodedStream.mark(Math.min(bufferSize,(int) file.length()));
	}	
	
	private AudioInputStream getStream() throws UnsupportedAudioFileException, IOException {
		if (file!=null)
			return AudioSystem.getAudioInputStream(file);
		else if(url != null)
			return AudioSystem.getAudioInputStream(url);
		else
			return audioInputStream;		
	}
	
	public String getName()
	{
		return name;
	}
	
	/*
	 * Returns some useful information about this audiofile.
	 */
	public String info()
	{
		if (!isOpen())
		{
			String str = "Filename: " + getName() + "\n";
			str += "File not open.\n";
			return str;
		}
		else
		{		
			String str = "Filename: " + getName() + "\n";
			str += "Number of channels: " + nChannels + "\n";
			str += "Number of frames: " + nFrames + "\n";
			// str += "Number of bytes per frame per channel: " + numBytes + "\n";
			// str += "Is encoded? " + isEncoded + "\n";
			str += "Audio File Format\n" + audioFileFormat.toString() + "\n";
			if (isEncoded)
			{
				str += "Audio Format (Encoded)\n" + encodedFormat.toString();
				str += "\nAudio Format (Decoded)\n" + decodedFormat.toString();
			}
			else
			{
				str += "Audio Format\n" + decodedFormat.toString();
			}			
			str += "\nAudio Properties {\n";
			for (String key: audioInfo.keySet())
			{			
				str += "\t" + key + ": " + audioInfo.get(key) + "\n";
			}
			str += "}\n";
			return str;		
		}
	}

	/**
	 * Close the audio file. Can be re-opened.
	 * 
	 * @throws IOException
	 */
	public void close() throws IOException
	{
		if (trace) System.err.printf("AudioFile \"%s\" closing\n",name);
		
		if (isEncoded)
			decodedStream.close();
		encodedStream.close();
		encodedStream = null;
		decodedStream = null;		
	}

	/**
	 * Is the file stream open?
	 */
	public boolean isOpen()
	{
		return !isClosed();
	}

	/**
	 * Is the file stream closed?
	 */
	public boolean isClosed()
	{
		return encodedStream==null;
	}

	/**
	 * @return The number of bytes per frame per channel.
	 */
	public int byteDepth()
	{
		return this.numBytes;
	}

	public AudioFormat getDecodedFormat()
	{
		return decodedFormat;
	}

	public AudioFormat getEncodedFormat() 
	{
		return encodedFormat;
	}

	/**
	 * Read bytes directly from the decoded audiofile.
	 * The bytes will be in an interleaved format. It is the responsibility of the caller to interpret this data correctly.  
	 *  
	 * The number of bytes read is equal to the size of the byte buffer. 
	 * If that many bytes aren't available the buffer will only be partially filled.   
	 * 
	 * @param buffer A buffer to fill.
	 * 
	 * @return The number of bytes read. A value of -1 indicates the file has no data left.
	 */
	public int read(byte[] buffer) {
		if (finished) 
		{
			if (trace) System.out.println("AudioFile finished!");
			return -1;
		}

		// read the next bufferSize frames from the input stream		
		int actualBytesRead = -1;
		try {
			// loop while reading data in
			int totalBytesRead = 0;
			while (totalBytesRead < buffer.length)
			{
				actualBytesRead = decodedStream.read(buffer,totalBytesRead,buffer.length-totalBytesRead);
				if (actualBytesRead == -1)
				{
//					System.err.print("!");	//TODO 
					finished = true;
					if (totalBytesRead>0)
						actualBytesRead = totalBytesRead;
					break;
				}
				else
					totalBytesRead += actualBytesRead;
			}
			if (totalBytesRead==buffer.length)
				actualBytesRead = totalBytesRead;
			
		} catch (IOException e) {
			finished = true;
		}

		if (finished || actualBytesRead==-1)
		{
			finished = true;
			return actualBytesRead;
		}
		
		nTotalFramesRead += actualBytesRead / (2*nChannels);
		return actualBytesRead;
	}
	
	/**
	 * Read decoded audio data in a non-interleaved, Beads-friendly format.
	 * 
	 * Note: This function is <b>extremely inefficient</b> if the buffer size is constant. Use Sample, it is very efficient!
	 * 
	 * @param buffer The buffer to fill. After execution buffer[i][j] will contain the sample in channel i, frame j. Buffer has size (numChannels,numFramesRequested). 
	 * 
	 * @return The number of <u>frames</u> read.
	 */
	public int read(float[][] buffer)
	{	
		// TODO: Make this more efficient, too many memory allocations
		if (buffer.length!=nChannels || buffer[0].length==0) return 0;			
		else if (finished) return 0;
		// else, read the data

		// read the next bufferSize frames from the input stream		
		byte[] byteBuffer = new byte[buffer[0].length * nChannels * numBytes];
		int actualBytesRead = -1;
		try {
			actualBytesRead = decodedStream.read(byteBuffer,0,byteBuffer.length);
		} catch (IOException e) {
			finished = true;
		}

		if (finished || actualBytesRead==-1)
		{
			finished = true;
			return 0;
		}

		int numFramesJustRead = actualBytesRead / (2*nChannels); 
		nTotalFramesRead += numFramesJustRead;	

		float[] floatbuf = new float[buffer[0].length*nChannels*numFramesJustRead];
		AudioUtils.byteToFloat(floatbuf, byteBuffer, decodedFormat.isBigEndian(),numFramesJustRead*nChannels);    		
		AudioUtils.deinterleave(floatbuf,nChannels,numFramesJustRead,buffer);

		return numFramesJustRead;
	}
	
	 /**
	  * THIS CODE IS FROM jlGui PlayerUI.java.
	  * jlGui can be obtained at: http://www.javazoom.net/jlgui/jlgui.html
	  */
    public long getTimeLengthEstimation(Map properties)
    {
        long milliseconds = -1;
        int byteslength = -1;
        if (properties != null)
        {
            if (properties.containsKey("audio.length.bytes"))
            {
                byteslength = ((Integer) properties.get("audio.length.bytes")).intValue();
            }
            if (properties.containsKey("duration"))
            {
                milliseconds = (((Long) properties.get("duration")).longValue()) / 1000;
            }
            else
            {
                // Try to compute duration
                int bitspersample = -1;
                int channels = -1;
                float samplerate = -1.0f;
                int framesize = -1;
                if (properties.containsKey("audio.samplesize.bits"))
                {
                    bitspersample = ((Integer) properties.get("audio.samplesize.bits")).intValue();
                }
                if (properties.containsKey("audio.channels"))
                {
                    channels = ((Integer) properties.get("audio.channels")).intValue();
                }
                if (properties.containsKey("audio.samplerate.hz"))
                {
                    samplerate = ((Float) properties.get("audio.samplerate.hz")).floatValue();
                }
                if (properties.containsKey("audio.framesize.bytes"))
                {
                    framesize = ((Integer) properties.get("audio.framesize.bytes")).intValue();
                }
                if (bitspersample > 0)
                {
                    milliseconds = (int) (1000.0f * byteslength / (samplerate * channels * (bitspersample / 8)));
                }
                else
                {
                    milliseconds = (int) (1000.0f * byteslength / (samplerate * framesize));
                }
            }
        }
        return milliseconds;
    }
    
}

