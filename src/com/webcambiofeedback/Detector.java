package com.webcambiofeedback;

import org.opencv.core.*;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PImage;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import static org.opencv.core.Core.*;
import static org.opencv.highgui.Highgui.*;
import static org.opencv.imgproc.Imgproc.*;
import static org.opencv.video.Video.*;
import processing.core.PImage;
import processing.core.PConstants;

public class Detector {
	int barWidth = 5;
	int lastBar = -1;
	int videoHeight, videoWidth;
	int last = 0;
	// number of cyclic frame buffer used for motion detection
	// (should, probably, depend on FPS)
	final int N = 4;
	final double MHI_DURATION = 1;
	final double MAX_TIME_DELTA = 0.5;
	final double MIN_TIME_DELTA = 0.05;
	Mat image = new Mat(), motion, mhi, orient, mask, segmask;
	Mat[] buf;
	VideoCapture capture;
	Size size;
	double magnitude, startTime = 0;
	
	Graph graph;
	boolean isDrawClockFace = true;
	
	//Mat img;
	
	
	
	public Detector(PApplet pApplet) {
		graph = new Graph(pApplet);
		
		capture = new VideoCapture(0);
		if(capture.isOpened() == false) {
			System.out.println("Unable to open camera");
			capture.release();
			System.exit(0);
		}
		videoWidth = (int) capture.get(CV_CAP_PROP_FRAME_WIDTH);
		videoHeight = (int) capture.get(CV_CAP_PROP_FRAME_HEIGHT);
		size = new Size(videoWidth, videoHeight);
		//img = new Mat();
		
		//--motemplate setup
		buf = new Mat[N];
		for (int i = 0; i < N; i++) {
			buf[i] = Mat.zeros(size, CvType.CV_8UC1);
		}
		motion = Mat.zeros(size, CvType.CV_8UC3);
		mhi = Mat.zeros(size, CvType.CV_32FC1);
		orient = Mat.zeros(size, CvType.CV_32FC1);
		segmask = Mat.zeros(size, CvType.CV_32FC1);
		mask = Mat.zeros(size, CvType.CV_8UC1);
		startTime = System.nanoTime();
	}
	
	public PImage getPImage() {
		PImage pimg = new PImage();
		if(capture.read(image)) {
			update_mhi(image, motion, 30);
			pimg = MatToPImage(motion);
		}
		return pimg;
	}
	
	//-- unused
	public PGraphics getGraphCircles() {
		graph.drawGraph();
		return graph.circles;
	}
	

	public ArrayList<Point> getGraphPoints() {
		return graph.phist;
	}
	
	public int getDirection() {
		return Graph.direction;
	}
	
	public double getCurrentPointY() {
		return graph.getCurrentPointY();
	}
	
	private void update_mhi(Mat img, Mat dst, int diff_threshold) {
		double timestamp = (System.nanoTime() - startTime) / 1e9;
		int idx1 = last, idx2;
		Mat silh;
		cvtColor(img, buf[last], COLOR_BGR2GRAY);
		double angle, count;

		idx2 = (last + 1) % N; // index of (last - (N-1))th frame
		last = idx2;

		silh = buf[idx2];
		absdiff(buf[idx1], buf[idx2], silh);
		threshold(silh, silh, diff_threshold, 1, THRESH_BINARY);
		updateMotionHistory(silh, mhi, timestamp, MHI_DURATION);
		mhi.convertTo(mask, mask.type(), 255.0 / MHI_DURATION, (MHI_DURATION - timestamp) * 255.0 / MHI_DURATION);
		dst.setTo(new Scalar(0));
		List<Mat> list = new ArrayList<Mat>(3);
		list.add(mask);
		list.add(Mat.zeros(mask.size(), mask.type()));
		list.add(Mat.zeros(mask.size(), mask.type()));
		
		merge(list, dst);
		
		calcMotionGradient(mhi, mask, orient, MAX_TIME_DELTA, MIN_TIME_DELTA, 3);
		MatOfRect roi = new MatOfRect();
		segmentMotion(mhi, segmask, roi, timestamp, MAX_TIME_DELTA);
		int total = roi.toArray().length;
		Rect[] rois = roi.toArray();
		Rect comp_rect;
		Scalar color;
		for (int i = -1; i < total; i++) {
			if (i < 0) {
				comp_rect = new Rect(0, 0, videoWidth, videoHeight); 
				color = new Scalar(255, 255, 255);
				magnitude = 100;
			} else {
				comp_rect = rois[i];
				if (comp_rect.width + comp_rect.height < 50) // reject very small components
					continue;
				color = new Scalar(0, 0, 255);
				magnitude = 30;
			}

			Mat silhROI = silh.submat(comp_rect);
			Mat mhiROI = mhi.submat(comp_rect);
			Mat orientROI = orient.submat(comp_rect);
			Mat maskROI = mask.submat(comp_rect);

			angle = calcGlobalOrientation(orientROI, maskROI, mhiROI, timestamp, MHI_DURATION);
			angle = 360.0 - angle;
			count = Core.norm(silhROI, NORM_L1);

			silhROI.release();
			mhiROI.release();
			orientROI.release();
			maskROI.release();
//			if (count < comp_rect.height * comp_rect.width * 0.05) {
//				continue;
//			}
			Point center = new Point((comp_rect.x + comp_rect.width / 2), (comp_rect.y + comp_rect.height / 2));
			Core.circle(dst, center, (int) Math.round(magnitude * 1.2), color, 3, LINE_AA, 0);
			Core.line(dst, center, new Point(Math.round(center.x + magnitude * Math.cos(angle * PConstants.PI / 180)),
					Math.round(center.y - magnitude * Math.sin(angle * PConstants.PI / 180))), color, 3, LINE_AA, 0);
			
			//--graph
			int delta = (int) Math.round(center.y - magnitude*Math.sin(angle*PConstants.PI/180));
			if(delta<center.y) Graph.graphPoint -= 1;
			else if(delta>center.y) Graph.graphPoint += 1;
			if(Graph.graphPoint<=100) {Graph.graphPoint+=150; graph.shiftGraph(1); }//1 down
			if(Graph.graphPoint>=400) {Graph.graphPoint-=150;graph.shiftGraph(-1);} //-1 up
			
			
			
			Graph.counter++; if(Graph.counter > 100) Graph.counter = 0;
			
			if(Graph.counter % 2 == 0) {
				//System.out.printf("Graph.x: %d,  Graph.graphPoint: %d\n", Graph.x, Graph.graphPoint);
				
				graph.addPoint(Graph.x++,Graph.graphPoint);	
				double dir = graph.getDirection(5,2);
				//System.out.printf("dir: %.1f\n", dir);
				
				if(dir > 0) { 
					Graph.direction=Graph.INHALING;
					//System.out.println("Inhaling");
				}
				else { 
					Graph.direction=Graph.EXHALING;
					//System.out.println("EXHALING");
				}
			}
			
			
			if( Graph.x > 800) { Graph.x = 0; graph.clear(); }
		}

	}
	
	private static PImage MatToPImage(Mat mat) {
		if (mat != null && !mat.empty()) {
			MatOfByte matOfByte = new MatOfByte();
			
			Highgui.imencode(".jpg", mat, matOfByte);   // for OpenCV 2.4.9
			byte[] byteArray = matOfByte.toArray();
			try {
				InputStream in = new ByteArrayInputStream(byteArray);
				BufferedImage bimg = ImageIO.read(in);
				PImage img = new PImage(bimg.getWidth(), bimg.getHeight(), PConstants.ARGB);
				bimg.getRGB(0, 0, img.width, img.height, img.pixels, 0, img.width);
				img.updatePixels();
				bimg = null;
				return img;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
}
