package com.webcambiofeedback;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

import org.opencv.core.Core;
import org.opencv.core.Point;

import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PGraphics;
import processing.core.PImage;


public class BreathDetect extends PApplet {
	
	public Queue<Double> periods;
	
	Detector detector;
	//PGraphics graphCircles;
	PFont f;
	ArrayList<Point> graphPoints;
	
	int webcamDirection = 0;
	public static int currentDirection = 0;
	int prevDirection = 0;
	int respirationCount = 0;
	int peak2, peak1;
	int peak2peak;
	long currentTime = 0;
	long prevTime = 0;
	
	public static double freq = 0;
	public static double period = 1;
	public static double prevGoodPeriod = 1;
	
	boolean isTimerStarted = false;
	public static boolean toShutdown = false;
	public static double currentPointY = 300.0f;
	
	WebcamServer webcamServer;
	int delaycounter = 0;
	public static int induration = 1;
	public static int outduration = 1;
	int comparingState = 1;  // 1 = in, 2 = out, 3 = end of out
	
	double sum = 0;
	double average = 0;
	
	
	
	public void settings() {
		size(800, 600);	
	}
	
	public void setup() {
		detector = new Detector(this);
		f = createFont("Arial",16,true);
		webcamServer = new WebcamServer(65333);
		webcamServer.start();
		System.out.println("WebcamServer started on port: " + webcamServer.getPort());
		periods = new LinkedList<>();
		initQueue();
	}

	private void initQueue() {
		for(int i = 0; i<5; i++) {
			periods.add(8.0);
		}
	}
	
	public void draw() {
		if(toShutdown) {
			System.exit(0);
		}
		PImage pimg = detector.getPImage();
		image(pimg, 0, 0, 800, 600);
		
		//graphCircles = detector.getGraphCircles();
		//image(graphCircles,0,0);
		
		graphPoints = detector.getGraphPoints();
		//println("graph: ", graphPoints.size());
		
		fill(0,255,255);
		stroke(255,200,0);
		strokeWeight(3);
		line(0,300,800,300);
		
		//fill(0,255,0);
		strokeWeight(1);
		stroke(0,255,0);
		for(int i=0; i<graphPoints.size(); i++) {
			ellipse(i,(float)graphPoints.get(i).y,2,2);
		}
		
		textFont(f,16);
		fill(255,255,0);
		
		currentPointY = detector.getCurrentPointY();
		
		text("value: " + detector.getCurrentPointY(), 10,120);
		
		currentDirection = detector.getDirection();
		
		if(comparingState==1 && currentDirection==Graph.INHALING) {
			text("inhaling " ,10,100);
			induration++;
		} else if(comparingState < 3 && currentDirection==Graph.EXHALING) {
			comparingState = 2;
			text("EXHALING...",10,100);
			outduration++;
		} else if (comparingState==2 && currentDirection==Graph.INHALING) {
			comparingState = 1;
			induration = outduration = 0;
		}
		
		calculateBreathRates();
		
		if(WebcamServer.isAutosend) {
			delaycounter++;
			if(delaycounter==5) {
				delaycounter = 0;
				webcamServer.sendValue();
				webcamServer.sendDirection();
				webcamServer.sendInduration();
				webcamServer.sendOutduration();
				webcamServer.sendPeriod();
				webcamServer.sendFrequency();
			}
		}
	}
	
	private void calculateBreathRates() {
		if(currentDirection==Graph.INHALING && !isTimerStarted) {
			isTimerStarted = true;
			
			currentTime = System.currentTimeMillis();
			
			period = ((double)(currentTime - prevTime)/1000);
			
			
			if(period > 1) {
				periods.remove();
				periods.add(period);	
				prevGoodPeriod = period;
				//freq = (double)(1/period)*60;
				//System.out.println("period: " + period + " bpm: " + nf((float)freq,0,1));
			}
			
			prevTime = currentTime;
			
		}else if(currentDirection==Graph.EXHALING) {
			isTimerStarted = false;
		}
		
		freq = averageBpm();
		String duration = nf((float)prevGoodPeriod,0,1);
		String bpm = nf((float)freq,0,1);
		text("duration: " + duration,300,20);
		text("bpm: " + bpm, 300, 50);
	}

	private double averageBpm() {
		for(int i = 0; i < periods.size(); i++) {
			double n = periods.remove();
			sum += n;
			periods.add(n); //add back to queue after removing it
			
		}
		average = (double)(sum/periods.size());
		freq = (double)(1.0/average)*60;
		//System.out.println("sum: " + sum + " queue size: " + periods.size() + " average: " + average + " freq: " + nf((float)freq,0,1));
		sum = 0;
		return freq;
	}
	
	public static void main(String[] args) {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		PApplet.main(BreathDetect.class.getName());
	}
}


