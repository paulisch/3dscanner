package at.fhhgb.scanner;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import lejos.remote.ev3.RMIRegulatedMotor;
import lejos.remote.ev3.RemoteEV3;

public class ScannerController {
	
	private static final int DELAY_SENSE_SCAN = 3000;
	private static final int PROGRESS_UPDATE_RATE = 60;
	
	private ScannerObservable scannerObservable = null;
	private ScannerConfiguration configuration = null;
	
	private RemoteEV3 brick = null;
	private boolean brickConnected = false;
	private boolean brickConnecting = false;
	
	private ArrayList<RMIRegulatedMotor> motors = null;
	private boolean isScanRunning = false;
	private boolean canStopSense = false;
	
	private Thread scannerThread = null;
	
	public ScannerController(ScannerObservable scannerObservable) {
		this.scannerObservable = scannerObservable;
		this.configuration = scannerObservable.getConfiguration();
	}
	
	public ScannerObservable getScannerObservable() {
		return scannerObservable;
	}
	
	public void onConnectBrick() {
		if (!brickConnecting) {
			brickConnecting = true;
			new Thread() {
				public void run() {
					try {
						scannerObservable.setConnecting(brickConnecting);
						closeMotors();
						motors = new ArrayList<>(configuration.getMotorsCount());
						brick = new RemoteEV3(configuration.getIp());
						brick.setDefault();
						for(String motorPort : configuration.getPortsMotors()) {
							motors.add(brick.createRegulatedMotor(motorPort, 'L'));
						}
						brickConnected = true;
					} catch (RemoteException e) {
						brickConnected = false;
					} catch (MalformedURLException e) {
						brickConnected = false;
					} catch (NotBoundException e) {
						brickConnected = false;
					} finally {
						scannerObservable.setConnected(brickConnected);
						brickConnecting = false;
						scannerObservable.setConnecting(brickConnecting);
					}
				}
			}.start();
		}
	}
	
	public void onForward() {
		startMotors(configuration.getSpeedManual(), true);
	}
	
	public void onBackward() {
		startMotors(configuration.getSpeedManual(), false);
	}
	
	public void onStop() {
		if (scannerThread != null) {
			scannerThread.interrupt();
		}
		if (canStopSense) {
			stopSense();
		}
		stopMotors();
	}
	
	public void onScan() {
		if (!brickConnected) {
			return;
		}
		if (!isScanRunning) {
			return;
		}
		
		isScanRunning = true;			
		scannerThread = new Thread() {
			public void run() {					
				//Start sense scan
				startSense();
				
				//Wait for Sense countdown
				try {
					Thread.sleep(DELAY_SENSE_SCAN);
				} catch (InterruptedException e) {
				}
				
				//If scanning has been stopped meanwhile don't continue
				if (Thread.interrupted()) {
					isScanRunning = false;
					scannerObservable.setScanning(isScanRunning);
					return;
				}
				
				canStopSense = true;
				
				//Start motors
				startMotors(configuration.getSpeedScanForward(), true);
				
				//Calculate sleep time for progress updates
				int delay = 1000 / PROGRESS_UPDATE_RATE;
				
				//Calculate scan duration
				int scanDuration = configuration.getScanDistanceWithOffset() / configuration.getSpeedScanForward();
				long startTime = System.currentTimeMillis();
				scannerObservable.setStartTime(startTime);
				scannerObservable.setCurrentTime(startTime);
				scannerObservable.setDuration(scanDuration);
				scannerObservable.setScanning(isScanRunning);
				
				while (isScanRunning) {
					long now = System.currentTimeMillis();
					long timeUntilEnd = startTime + scanDuration - now;
					
					//Set progress
					scannerObservable.setCurrentTime(now);
					
					//Stop waiting if scan is complete
					if (timeUntilEnd <= 0 || Thread.interrupted()) {
						isScanRunning = false;
					}
					else {
						//Sleep for a while
						try {
							Thread.sleep(Math.min(timeUntilEnd + 1, delay));
						} catch (InterruptedException e) {
							isScanRunning = false;
						}
					}
				}				
				scannerObservable.setScanning(isScanRunning);
				
				if (!Thread.interrupted()) {
					//Stop sense scan
					stopSense();
					
					//Scan is complete - stop motors
					stopMotors();
				}
			}
		};
		scannerThread.start();
	}
	
	public void playBeep() {
		if (brick != null) {
			brick.getAudio().playTone(440, 500);
		}
	}
	
	public void close() {
		stopMotors();
		closeMotors();
	}
	
	private void startMotors(int speed, boolean forward) {
		if (brickConnected) {
			try {
				for(int i=0; i<motors.size(); i++) {
					RMIRegulatedMotor motor = motors.get(i);
					motor.setSpeed(speed);
					if (forward) {
						motor.forward();
					}
					else {
						motor.backward();
					}
				}
			} catch (RemoteException e) {
				brickConnected = false;
				scannerObservable.setConnected(brickConnected);
			}
		}
	}
	
	private void startSense() {
		//TODO: Click on scan in Sense software with robot
		
	}
	
	private void stopMotors() {
		if (brickConnected) {
			try {
				for(int i=0; i<motors.size(); i++) {
					RMIRegulatedMotor motor = motors.get(i);
					motor.stop(true);
				}
			} catch (RemoteException e) {
				brickConnected = false;
				scannerObservable.setConnected(brickConnected);
			}
		}
	}
	
	private void stopSense() {
		//TODO: Click on stop in Sense software with robot
		canStopSense = false;
	}
	
	private void closeMotors() {
		if (motors != null) {
			try {
				for(int i=0; i<motors.size(); i++) {
					RMIRegulatedMotor motor = motors.get(i);
					motor.close();
				}
			} catch (RemoteException e) {
				brickConnected = false;
				scannerObservable.setConnected(brickConnected);
			}
		}
	}
}