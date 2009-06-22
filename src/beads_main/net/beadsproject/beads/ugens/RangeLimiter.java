/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.ugens;

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.UGen;

/**
 * RangeLimiter forces a signal within the range [-1,1].
 * 
 * @beads.category effect
 */
public class RangeLimiter extends UGen {

	/**
	 * Instantiates a new RangeLimiter.
	 * 
	 * @param context
	 *            the AudioContext.
	 * @param inouts
	 *            the number of inputs (= number of outputs).
	 */
	public RangeLimiter(AudioContext context, int inouts) {
		super(context, inouts, inouts);
	}

	/* (non-Javadoc)
	 * @see com.olliebown.beads.core.UGen#calculateBuffer()
	 */
	@Override
	public void calculateBuffer() {
		for(int i = 0; i < bufferSize; i++) {
			for(int j = 0; j < ins; j++) {
				bufOut[j][i] = bufIn[j][i];
				if(bufOut[j][i] > 1.0f) bufOut[j][i] = 1.0f;
				else if(bufOut[j][i] < -1.0f) bufOut[j][i] = -1.0f;
			}
		}
	}

	
}
