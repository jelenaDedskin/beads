/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.ugens;

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.UGen;
import net.beadsproject.beads.data.Buffer;
import net.beadsproject.beads.data.buffers.SineBuffer;

/**
 * An OscillatorBank sums the output of a set of oscillators with assignable frequencies and amplitudes. The frequencies and amplitudes of the set of oscillators can be assigned using arrays.
 *
 * @beads.category synth
 * @author ollie
 */
public class OscillatorBank extends UGen {

    /** The array of frequencies of individual oscillators. */
    private float[] frequency;
    
    /** The array of gains of individual oscillators. */
    private float[] gains;
    
    /** The array of current positions of individual oscillators. */
    private float[] point;
    
    /** The array of increment rates of individual oscillators, given their frequencies. */
    private double[] increment;
    
    /** The buffer used by all oscillators. */
    private Buffer buffer;
    
    /** The number of oscillators. */
    private int numOscillators;
    
    /** The sample rate and master gain of the OscillatorBank. */
    private float sr, gain;
    
    /**
     * Instantiates a new OscillatorBank.
     * 
     * @param context the AudioContext.
     * @param buffer the buffer used as a lookup table by the oscillators.
     * @param numOscillators the number of oscillators.
     */
    public OscillatorBank(AudioContext context, Buffer buffer, int numOscillators) {
        super(context, 1, 1);
        this.buffer = buffer;
        sr = context.getSampleRate();
        setNumOscillators(numOscillators);
        gain = 1f / (float)numOscillators;
    }
    
    
    /**
     * Sets the number of oscillators.
     * 
     * @param numOscillators the new number of oscillators.
     */
    public void setNumOscillators(int numOscillators) {
    	this.numOscillators = numOscillators;
		float[] old = frequency;
		frequency = new float[numOscillators];
		increment = new double[numOscillators];
		int min = 0;
		if(old != null) min = Math.min(frequency.length, old.length);
		for(int i = 0; i < min; i++) {
			frequency[i] = old[i];
            increment[i] = frequency[i] / context.getSampleRate();
		}
		for(int i = min; i < frequency.length; i++) {
			frequency[i] = 0f;
            increment[i] = frequency[i] / context.getSampleRate();
		}
		old = gains;
		gains = new float[numOscillators];
		for(int i = 0; i < min; i++) {
			gains[i] = old[i];
		}
		for(int i = min; i < gains.length; i++) {
			gains[i] = 1f;
		}    		
		old = point;
		point = new float[numOscillators];
		for(int i = 0; i < min; i++) {
			point[i] = old[i];
		}
		for(int i = min; i < point.length; i++) {
			point[i] = 0f;
		}
    }
    
    /**
     * Sets the frequencies of all oscillators.
     * 
     * @param frequencies the new frequencies.
     */
    public void setFrequencies(float[] frequencies) {
        for(int i = 0; i < numOscillators; i++) {
        	if(i < frequencies.length) {
        		frequency[i] = Math.abs(frequencies[i]);
        	} else {
        		frequency[i] = 0f;
        	}
            increment[i] = frequency[i] / context.getSampleRate();
        }
    }
    
    /**
     * Gets the array of frequencies.
     * @return array of frequencies.
     */
    public float[] getFrequencies() {
    	return frequency;
    }
    
    /**
     * Sets the gains of all oscillators.
     * 
     * @param gains the new gains.
     */
    public void setGains(float[] gains) {
    	for(int i = 0; i < numOscillators; i++) {
        	if(i < gains.length) {
        		this.gains[i] = gains[i];
        	} else {
        		this.gains[i] = 0f;
        	}
        }
    }
    /**
     * Gets the array of gains.
     * @return array of gains.
     */
    public float[] getGains() {
    	return gains;
    }

    /* (non-Javadoc)
     * @see com.olliebown.beads.core.UGen#calculateBuffer()
     */
    @Override
    public void calculateBuffer() {
        zeroOuts();
        for(int i = 0; i < bufferSize; i++) {
            for(int j = 0; j < numOscillators; j++) {
                point[j] = (float)(point[j] + increment[j]) % 1f;
                bufOut[0][i] += gains[j] * buffer.getValueFraction(point[j]);
            }
            bufOut[0][i] *= gain;
        }
    }   

    
}




