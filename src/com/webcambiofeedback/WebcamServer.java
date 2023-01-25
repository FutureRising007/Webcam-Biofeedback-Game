package com.webcambiofeedback;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;


public class WebcamServer extends WebSocketServer {
	public static boolean isAutosend = false;
	
	public WebcamServer(int port){
	    super(new InetSocketAddress(port));
	    
	  }

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		conn.send("Welcome to the webcamserver!"); // This method sends a message to the new client
		broadcast("new connection: " + handshake.getResourceDescriptor()); // This method sends a message to all clients
																			// connected
		System.out.println(conn.getRemoteSocketAddress().getAddress().getHostAddress() + " entered webcamserver!");
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		broadcast(conn + " has left!");
		System.out.println(conn + " has left!");
	}

	@Override
	public void onMessage(WebSocket conn, String message) {
		//broadcast(message);
		System.out.println(conn + ": " + message);
		if(message.equals("shutdown")) {
			//sendText("ok shuting down");
			conn.send("ok shyyting down");
			conn.close();
			try {
				stop();
			}catch(IOException eio) {
				System.out.println("eio: " + eio.toString());
				BreathDetect.toShutdown = true;
			}catch(InterruptedException einterrupt) {
				System.out.println("einterrupt: " + einterrupt.toString());
				BreathDetect.toShutdown = true;
			}
			//System.exit(0);
			
		}else if(message.equals("getvalue")) {
			conn.send("value " + BreathDetect.currentPointY);
		}else if(message.equals("getdirection")) {
			conn.send("direction " + BreathDetect.currentDirection);
		}else if(message.equals("autogeton")) {
			System.out.println( "Received autogeton: " + message);
			isAutosend = true;
		}else if(message.equals("autogetoff")) {
			System.out.println( "Received autogetoff: " + message);
			isAutosend = false;
		}else {
			//conn.send("else part");
			//broadcast(message);
		}
		
//		if(isAutosend) {
//			sendValue(conn);
//			sendDirection(conn);
//		}
	}

	public void sendValue() {
		broadcast("value " + BreathDetect.currentPointY);
		System.out.println( "Autosent value");
	}
	
	public void sendDirection() {
		broadcast("direction " + BreathDetect.currentDirection);
		System.out.println( "Autosent Diretion");
	}
	
	public void sendOutduration() {
		broadcast("outduration " + BreathDetect.outduration);
		System.out.println( "Autosent outduration");
	}
	
	public void sendInduration() {
		broadcast("induration " + BreathDetect.induration);
		System.out.println( "Autosent induration");
	}
	
	public void sendPeriod() {
		broadcast("period " + String.format("%.1f",BreathDetect.prevGoodPeriod));
		System.out.println( "Autosent period");
	}
	
	public void sendFrequency() {
		broadcast("frequency " + String.format("%.1f",BreathDetect.freq));
		System.out.println( "Autosent requency");
	}
	
	@Override
	public void onMessage(WebSocket conn, ByteBuffer message) {
		System.out.println(conn + ": " + message);
		//broadcast(message.array());
		if(message.equals("shutdown")) {
			//sendText("ok shuting down");
			//conn.send("ok shuting down");
			broadcast("ok shuting down");
		} else {
			//conn.send("else part");
			broadcast("elseie part");
		}
		
	}

	@Override
	public void onError(WebSocket conn, Exception ex) {
		ex.printStackTrace();
		if (conn != null) {
			// some errors like port binding failed may not be assignable to a specific
			// websocket
		}
	}

	@Override
	public void onStart() {
		System.out.println("Server started!");
		setConnectionLostTimeout(0);
		setConnectionLostTimeout(100);
	}
}
