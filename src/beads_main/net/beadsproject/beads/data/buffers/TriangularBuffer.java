package net.beadsproject.beads.data.buffers;

import java.util.Arrays;

import net.beadsproject.beads.data.Buffer;
import net.beadsproject.beads.data.BufferFactory;

/**
 * Filter to be used for smoothing data (see OnsetDetector)
 * 
 * @author ben
 *
 */
public class TriangularBuffer extends BufferFactory {

	@Override
	public Buffer generateBuffer(int bufferSize) {
		// TODO Auto-generated method stub
		Buffer b = new Buffer(bufferSize);
		
		for (int i=0;i<bufferSize;i++)
		{
			b.buf[i] = tri((i+0.5f)/bufferSize)/bufferSize; 
		}
		return b;
	}
	
	protected float tri(float x)
	{
		if (x<.5)
			return 4*x;		
		else		
			return 4*(1-x);	
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "MeanFilter";
	}

}