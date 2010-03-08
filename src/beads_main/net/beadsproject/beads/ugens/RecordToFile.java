package net.beadsproject.beads.ugens;
/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;

import org.tritonus.share.sampled.AudioSystemShadow;
import org.tritonus.share.sampled.file.AudioOutputStream;

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.AudioUtils;
import net.beadsproject.beads.core.UGen;
import net.beadsproject.beads.data.Sample;
import net.beadsproject.beads.data.SampleManager;

/**
 * RecordToFile records audio into a file.
 * 
 * You must {@link #kill() kill} this object when finished to finalise the writing of the file header.
 * 
 * IMPORTANT NOTE: At the moment only WAVE files seem to work. 
 * 
 * @beads.category utilities
 * @author bp
 */
public class RecordToFile extends UGen {
	static final private boolean DEBUG = false; 
	
	/** The stream the input is output to. */
	private AudioOutputStream audioOutputStream;
	
	/** The file object the constructor was called with */
	private File file;
	
	/** The audio format of the output file. */
	private AudioFormat audioFormat;
	
	/** The output file type. */
	private AudioFileFormat.Type type;
	
	/**
	 * Instantiates a recorder for file recording.
	 * 
	 * @param context 
	 * 				The AudioContext 	
	 * @param numberOfChannels 
	 * 				The number of channels
	 * @param file
	 * 				The file to output to
	 * @param type
	 * 				The type of the file
	 * @throws IOException 
	 * 				if the audio format is not supported on this machine.
	 * 				
	 */
	public RecordToFile(AudioContext context, int numberOfChannels, File file, AudioFileFormat.Type type) throws IOException {
		super(context,numberOfChannels,0);
		
		this.file = file;
		this.type = type;
		
		audioFormat = new AudioFormat(
				AudioFormat.Encoding.PCM_SIGNED,
				context.getSampleRate(), 
				16 /* number of bits per sample */, 
				getIns(), 
				2 * getIns(), /* bytes per frame */
				context.getSampleRate(), 
				false /* big-endian? */
				);
		
		audioOutputStream = AudioSystemShadow.getAudioOutputStream(
				type, 
				audioFormat, 
				AudioSystem.NOT_SPECIFIED, 
				file);				
	}
	
	@Override
	public void calculateBuffer() {
		// INV: bufIn[0] exists
		
		int length = bufIn[0].length;
		byte bytes[] = new byte[(int) (getIns()*length*2)];
		
		if (getIns() > 1)
		{
			float interleaved[] = new float[(int) (getIns()*length)];
			AudioUtils.interleave(bufIn, getIns(), length, 0, interleaved);
			AudioUtils.floatToByte(bytes, interleaved, false);
		}
		else
		{		
			AudioUtils.floatToByte(bytes, bufIn[0], false);
		}
		
		try {
			int numBytesWritten = audioOutputStream.write(bytes, 0, bytes.length);
			if (DEBUG)
				System.out.printf("Wrote %d bytes\n",numBytesWritten);
		} catch (IOException e) {			
			e.printStackTrace();			
		}		
		
		if (DEBUG)			
		{	
			long usedMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			System.out.printf("%dm mem used\n", usedMem/(1024*1024));
		}
	}
	
	public void kill()
	{
		super.kill();
		try {
			audioOutputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
}
