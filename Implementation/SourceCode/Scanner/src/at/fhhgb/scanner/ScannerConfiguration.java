package at.fhhgb.scanner;

import java.util.ArrayList;
import java.util.Arrays;

public class ScannerConfiguration {
	
	public static final int RING_DISTANCE               = 10600000;
	public static final double DEFAULT_DISTANCE_OFFSET  = 0.0188679;
	public static final int MIN_SPEED                   = 100;
	public static final int MAX_SPEED                   = 600;
	public static final int DEFAULT_SPEED_MANUAL        = 280;
	public static final int DEFAULT_SPEED_SCAN_FORWARD  = 200;
	public static final int DEFAULT_SPEED_SCAN_BACKWARD = 310;
	public static final boolean DEFAULT_GO_BACK         = true;
	public static final String DEFAULT_IP               = "10.0.1.1";
	public static final String DEFAULT_PORTS_MOTORS[]   = new String[] { "B", "C" };
	public static final String MOTOR_PORTS[]            = new String[] { "A", "B", "C", "D" };
	
	private int scanDistance;
	private double scanDistanceOffset;
	private int speedScanForward;
	private int speedScanBackward;
	private int speedManual;
	private boolean shouldGoBack;
	private String ip;
	private String portsMotors[];
	
	public ScannerConfiguration() {
		setScanDistance(RING_DISTANCE);
		setScanDistanceOffset(DEFAULT_DISTANCE_OFFSET);
		setSpeedScanForward(DEFAULT_SPEED_SCAN_FORWARD);
		setSpeedScanBackward(DEFAULT_SPEED_SCAN_BACKWARD);
		setSpeedManual(DEFAULT_SPEED_MANUAL);
		setShouldGoBack(DEFAULT_GO_BACK);
		setIp(DEFAULT_IP);
		setPortsMotors(DEFAULT_PORTS_MOTORS);
	}

	public int getScanDistance() {
		return scanDistance;
	}
	
	public int getScanDistanceWithOffset() {
		return (int)(scanDistance + scanDistance * getScanDistanceOffset());
	}

	private void setScanDistance(int scanDistance) {
		this.scanDistance = scanDistance;
	}

	public double getScanDistanceOffset() {
		return scanDistanceOffset;
	}

	public void setScanDistanceOffset(double scanDistanceOffset) {
		if (scanDistanceOffset < 0) {
			scanDistanceOffset = 0;
		}
		this.scanDistanceOffset = scanDistanceOffset;
	}

	public int getSpeedScanForward() {
		return speedScanForward;
	}

	public void setSpeedScanForward(int speedScanForward) {
		this.speedScanForward = correctSpeed(speedScanForward);
	}

	public int getSpeedScanBackward() {
		return speedScanBackward;
	}

	public void setSpeedScanBackward(int speedScanBackward) {
		this.speedScanBackward = correctSpeed(speedScanBackward);
	}

	public int getSpeedManual() {
		return speedManual;
	}

	public void setSpeedManual(int speedManual) {
		this.speedManual = correctSpeed(speedManual);
	}

	public boolean isShouldGoBack() {
		return shouldGoBack;
	}

	public void setShouldGoBack(boolean shouldGoBack) {
		this.shouldGoBack = shouldGoBack;
	}	
	
	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String[] getPortsMotors() {
		return Arrays.copyOf(portsMotors, portsMotors.length);
	}
	
	public int getMotorsCount() {
		return portsMotors.length;
	}

	public void setPortsMotors(String[] portsMotors) {
		if (portsMotors == null) {
			portsMotors = new String[] {};
		}
		
		ArrayList<String> validPorts = new ArrayList<>();
		for (int i=0; i<portsMotors.length; i++) {
			if (portsMotors[i] != null) {
				for(int j=0; j<MOTOR_PORTS.length; j++) {
					if (portsMotors[i].toUpperCase().equals(MOTOR_PORTS[j])) {
						validPorts.add(MOTOR_PORTS[j]);
					}
				}
			}
		}
		this.portsMotors = new String[validPorts.size()];
		validPorts.toArray(this.portsMotors);
	}

	private int correctSpeed(int speed) {
		if (speed > MAX_SPEED ) {
			speed = MAX_SPEED;
		}
		if (speed < MIN_SPEED) {
			speed = MIN_SPEED;
		}
		return speed;
	}
}