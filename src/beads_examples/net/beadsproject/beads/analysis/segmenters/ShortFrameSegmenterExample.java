package net.beadsproject.beads.analysis.segmenters;

import net.beadsproject.beads.analysis.segmenters.ShortFrameSegmenter;
import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.data.buffers.SineBuffer;
import net.beadsproject.beads.ugens.WavePlayer;

public class ShortFrameSegmenterExample {
	
	public static void main(String[] args) {
		AudioContext ac = new AudioContext(512);
		ShortFrameSegmenter sfs = new ShortFrameSegmenter(ac) {
			public void segment(double startTime, double endTime, float[] data) {
				super.segment(startTime, endTime, data);
				for(int i = 0; i < data.length; i++) {
					System.out.print(data[i] + " ");
				}
				System.out.println();
			}
		};
		WavePlayer wp = new WavePlayer(ac, 500f, new SineBuffer().getDefault());
		sfs.addInput(wp);
		ac.out.addDependent(sfs);
		ac.out.addInput(wp);
		ac.start();
	}

}
