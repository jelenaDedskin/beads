/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.analysis;

import net.beadsproject.beads.core.Bead;

/**
 * FeatureExtractor is an abstract base class for classes that perform some kind of analysis on incoming data. Both the incoming data (P) and the generated data (R) are generic types. Implementing classes use the method {@link #process(P)} to process data. 
 * @author ollie
 */
public abstract class FeatureExtractor<R, P> extends Bead {
	
	/** The number of features. */
	protected int numFeatures;
	
	/** The current feature data. */
	protected R features;
	
	/** The name of the FeatureExtractor. */
	protected String name;
	
	/** An array of Strings providing descriptions of the feature data. */
	protected String[] featureDescriptions;
	
	/**
	 * Instantiates a new FeatureExtractor. This constructor names the FeatureExtractor with the name of the implementing class.
	 */
	public FeatureExtractor() {
		name = getClass().getSimpleName();
	}
	
	/**
	 * Process some data of type P (specified by the class def). This method must be overidden by implementing classes.
	 * 
	 * @param data the data.
	 */
	public abstract void process(P data);
	
	/**
	 * Gets the current features of type R, specified in the class def.
	 * 
	 * @return the features.
	 */
	public R getFeatures() {
		return features;
	}
	
	/**
	 * Gets the number of features.
	 * 
	 * @return the number of features.
	 */
	public int getNumberOfFeatures() {
		return numFeatures;
	}
	
	/**
	 * Sets the number of features.
	 * 
	 * @param numFeatures the new number of features.
	 */
	public void setNumberOfFeatures(int numFeatures) {
		this.numFeatures = numFeatures;
	}
	
	/**
	 * Sets the name.
	 * 
	 * @param name the new name.
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * Gets the name.
	 * 
	 * @return the name.
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Gets the feature descriptions. Implementing classes should make sure that this array has meaningful content.
	 * 
	 * @return the feature descriptions.
	 */
	public String[] getFeatureDescriptions() {
		return featureDescriptions;
	}

}

