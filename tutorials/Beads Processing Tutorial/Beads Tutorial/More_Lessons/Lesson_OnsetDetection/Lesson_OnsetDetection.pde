import net.beadsproject.beads.events.*;
import net.beadsproject.beads.data.*;
import net.beadsproject.beads.ugens.*;
import net.beadsproject.beads.analysis.segmenters.*;
import net.beadsproject.beads.analysis.featureextractors.*;
import net.beadsproject.beads.analysis.*;
import net.beadsproject.beads.data.buffers.*;
import net.beadsproject.beads.core.*;

AudioContext ac;
OnsetDetector od;

// In this example we detect onsets in the audio signal
// and pulse the screen when they occur. The brightness is controlled by 
// the following global variable. The draw() routine decreases it over time.
float brightness;
int time; // tracks the time 

void setup() {
  size(300,300);
  time = millis();
  
  /*
   * Set up the context and load a sample.
   */
  
  ac = new AudioContext();
  String audioFile = selectInput();
  SamplePlayer player = new SamplePlayer(ac, SampleManager.sample(audioFile));
  Gain g = new Gain(ac, 2, 0.2);
  g.addInput(player);
  ac.out.addInput(g);
  
  /*
   * To analyse a signal, build an analysis chain.
   * We also manually set parameters of the sfs.
   */
  ShortFrameSegmenter sfs = new ShortFrameSegmenter(ac);
  sfs.setChunkSize(2048);
  sfs.setHopSize(441);
  sfs.addInput(ac.out);
  FFT fft = new FFT();
  PowerSpectrum ps = new PowerSpectrum();
  sfs.addListener(fft);
  fft.addListener(ps);
  
  /*
   * Given the power spectrum we can now detect changes in spectral energy.
   */
  SpectralDifference sd = new SpectralDifference(ac.getSampleRate());
  ps.addListener(sd);
  od = new OnsetDetector();
  sd.addListener(od);
  /*
   * These parameters will need to be adjusted based on the 
   * type of music. This demo uses the mouse position to adjust 
   * them dynamically.
   * mouse.x controls Threshold, mouse.y controls Alpha
   */
  od.setThreshold(0.2f);
  od.setAlpha(.9f);
  
  /*
   * OnsetDetector sends messages whenever it detects an onset.
   */
  od.addMessageListener(
  	new Bead(){
  		protected void messageReceived(Bead b)
  		{
  		  brightness = 1.0;  		
  		}
  	}
  );
  
  ac.out.addDependent(sfs);
  //and begin
  ac.start();
}


/*
 * Draw a circle whenever we hear an onset change.
 */
void draw() {
	background(0);
	fill(brightness*255);
	ellipse(width/2,height/2,width/2,height/2);  
	
	// decrease brightness over time
	int dt = millis() - time;
	brightness -= (dt*0.01);
	if (brightness < 0) brightness = 0;
	time += dt;
	
	// set threshold and alpha to the mouse position
	od.setThreshold((float)mouseX/width);
	od.setAlpha((float)mouseY/height);
}
