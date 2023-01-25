package com.webcambiofeedback;

import java.util.ArrayList;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

import processing.core.PGraphics;
import processing.core.PApplet;

public class Graph {
	public ArrayList<Point> phist;
	int histCount;
	static final int SIZE = 2048;
	public static int graphPoint = 300, counter=0;
	public static int x=0, y=30, direction=0;
	public static final int INHALING = 1;
	public static final int EXHALING = -1;
	
	Point currentPointY;
	
	//-- Processing
	public PGraphics circles;
	
	public Graph(PApplet pApplet) {
		phist = new ArrayList<>();
		histCount = 0;
		
		//-- Processing
		circles = pApplet.createGraphics(800, 600);
		//circles.background(0);
		circles.noStroke();
		
		circles.fill(0,255,0);
	}
	
	public double getDirection(int how_far_back, int how_far_back_second_point) {
		if(phist.size() < 10) return 1;
		int s = phist.size()-how_far_back;
		int t = phist.size()-how_far_back_second_point;
		Point a = phist.get(s);
		Point b = phist.get(t);
		
		//System.out.printf("a.y: %.1f,  b.y: %.1f\n", a.y,b.y);
		return a.y - b.y; //+is going up, -ve is going down
	}
	
	public void addPoint(int x, int y) {
		if(histCount < SIZE) {
			histCount++;
			currentPointY = new Point(x,y);
			//System.out.printf("p.x: %.1f, p.y: %.1f\n", p.x, p.y);
			phist.add(currentPointY);
		} else {
			System.out.println("Graph.addPoint() fail");
		}
		//debugPhist();
	}
	
	private void debugPhist() {
		for(Point p : phist) {
			System.out.printf("p.x: %.1f, p.y: %.1f ", p.x,p.y);
		}
		System.out.printf("\n---------------------------------------\n");
	}
	
	//-- unused --
	public void drawGraph() {
		circles.beginDraw();
		circles.fill(0,255,0);
		for(int x=0; x<phist.size(); x++) {
			circles.ellipse(x,(float)phist.get(x).y,5,5);
		}
		circles.endDraw();
	}
	
	public double getCurrentPointY() {
		if(currentPointY==null) return 300.00;
		return currentPointY.y;
	}
	
	public void clear() {
		phist.clear();
		//circles.clear();
		//circles.background(0);
		histCount=0;
	}
	
	public void shiftGraph(int direction) {
		for(int i=0; i < histCount; i++) {
			Point p = phist.get(i);
			if(direction == EXHALING) p.y -= 150; //move graph up
			else p.y += 150;  //move graph down
			phist.set(i, p);
		}
	}
	
	
	
}
