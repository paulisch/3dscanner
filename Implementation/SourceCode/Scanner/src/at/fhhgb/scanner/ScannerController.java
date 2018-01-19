package at.fhhgb.scanner;

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import javax.swing.JFrame;
import lejos.hardware.Audio;
import lejos.hardware.DeviceException;
import lejos.remote.ev3.RMIRegulatedMotor;
import lejos.remote.ev3.RemoteEV3;

public class ScannerController {
	
	private static final int DELAY_SENSE_SCAN = 3500;
	private static final int PROGRESS_UPDATE_RATE = 30;
	
	private ScannerObservable scannerObservable = null;
	private ScannerConfiguration configuration = null;
	
	private RemoteEV3 brick = null;
	private Thread connectionThread = null;
	private boolean brickConnected = false;
	private boolean brickConnecting = false;
	
	private ArrayList<RMIRegulatedMotor> motors = null;
	private boolean isScanRunning = false;
	private boolean canStopSense = false;
	
	private Thread scannerThread = null;
	
	private Thread audioThread = null;
	private BlockingQueue<Runnable> audioQueue = null;
	private boolean audioQueueRunning = false;
	
	@SuppressWarnings("unused")
	private JFrame window = null;
	
	public ScannerController(ScannerObservable scannerObservable) {
		this.scannerObservable = scannerObservable;
		this.configuration = scannerObservable.getConfiguration();
		this.audioQueue = new LinkedBlockingQueue<>();
	}
	
	public void setWindow(JFrame window) {
		this.window = window;
	}
	
	public ScannerObservable getScannerObservable() {
		return scannerObservable;
	}
	
	public void onConnectBrick() {
		if (!brickConnecting) {
			brickConnecting = true;
			connectionThread = new Thread() {
				public void run() {
					try {
						System.out.println("Connecting...");
						scannerObservable.setConnecting(brickConnecting);
						closeMotors();
						motors = new ArrayList<>(configuration.getMotorsCount());
						brick = new RemoteEV3(configuration.getIp());
						brick.setDefault();
						for(String motorPort : configuration.getPortsMotors()) {
							motors.add(brick.createRegulatedMotor(motorPort, 'L'));
						}
						brickConnected = true;
					} catch (DeviceException e) {
						System.out.println("Ports in use, reset brick.");
						brickConnected = false;
						scannerObservable.setShouldResetBrick(true);
					} catch (RemoteException e) {
						brickConnected = false;
					} catch (MalformedURLException e) {
						brickConnected = false;
					} catch (NotBoundException e) {
						brickConnected = false;
					} finally {
						brickConnecting = false;
						scannerObservable.setConnecting(brickConnecting);
						if (Thread.interrupted()) {
							System.out.println("Connecting interrupted, going to close.");
							close();
						}
						else {
							scannerObservable.setConnected(brickConnected);
						}
						
						if (brickConnected) {
							System.out.println("Connected successfully.");
						}
					}
				}
			};
			connectionThread.start();
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
			System.out.println("Failed to scan, brick is not connected.");
			return;
		}
		if (isScanRunning) {
			System.out.println("Failed to scan, scan already running.");
			return;
		}
		
		isScanRunning = true;			
		scannerThread = new Thread() {
			public void run() {
				System.out.println("Scanning started.");
				
				//Start sense scan
				startSense();
				
				//Wait for Sense countdown and play sounds to notify scanned person that scan starts soon
				int millis = 1000;
				int n = DELAY_SENSE_SCAN / millis;
				int remainingSleep = DELAY_SENSE_SCAN % millis;
				for (int i=0; i<n; i++) {
					try {
						System.out.print(n-i + " ");
						playBeep();
						Thread.sleep(millis + (i == n - 1 ? remainingSleep : 0));
					} catch (InterruptedException e) {
						System.out.println();
						System.out.println("Scan countdown was interrupted.");
						isScanRunning = false;
						scannerObservable.setScanning(isScanRunning);
						//window.setState(Frame.NORMAL);
						return;
					}
				}
				
				//window.setState(Frame.NORMAL);
				System.out.println();				
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
				
				while (true) {
					long now = System.currentTimeMillis();
					long timeUntilEnd = startTime + scanDuration - now;
					
					//Set progress
					scannerObservable.setCurrentTime(now);
					
					//Stop waiting if scan is complete
					if (Thread.interrupted()) {
						System.out.println("Scan was interrupted.");
						isScanRunning = false;
						scannerObservable.setScanning(isScanRunning);
						return;
					}
					if (timeUntilEnd <= 0) {
						break;
					}
					else {
						//Sleep for a while
						try {
							Thread.sleep(Math.min(timeUntilEnd + 1, delay));
						} catch (InterruptedException e) {
							System.out.println("Scan was interrupted during sleep.");
							isScanRunning = false;
							scannerObservable.setScanning(isScanRunning);
							return;
						}
					}
				}
				
				scannerObservable.setScanning(false);
				
				//Stop sense scan
				stopSense();
				
				//Scan is complete - stop motors
				stopMotors();
				
				
				
				//Drive back to starting position				
				//Wait before driving back
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					System.out.println("Drive back was interrupted.");
					isScanRunning = false;
					return;
				}
				
				//Play drive back indicator
				playDoubleBeep();
				
				//Wait again
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					System.out.println("Drive back was interrupted.");
					isScanRunning = false;
					return;
				}
				
				//Start driving back
				int driveBackDuration = configuration.getScanDistanceWithOffset() / configuration.getSpeedScanBackward();
				startMotors(configuration.getSpeedScanBackward(), false);
				startTime = System.currentTimeMillis();
				
				while(true) {
					long now = System.currentTimeMillis();
					long timeUntilEnd = startTime + driveBackDuration - now;
					
					//Stop waiting if scan is complete
					if (Thread.interrupted()) {
						System.out.println("Drive back was interrupted.");
						isScanRunning = false;
						return;
					}
					if (timeUntilEnd <= 0) {
						break;
					}
					else {
						//Sleep for a while
						try {
							Thread.sleep(Math.min(timeUntilEnd + 1, delay));
						} catch (InterruptedException e) {
							System.out.println("Drive back was interrupted.");
							isScanRunning = false;
							return;
						}
					}
				}
				
				//Driving back complete - stop motors
				stopMotors();
				
				//Play sound to indicate, that another scanning process can be started now
				playBeep();
				
				isScanRunning = false;
			}
		};
		scannerThread.start();
	}
	
	public void close() {
		if (connectionThread != null && connectionThread.isAlive() && brickConnecting) {
			connectionThread.interrupt();
		}
		else {
			if (scannerThread != null) {
				scannerThread.interrupt();
			}
			if (audioThread != null) {
				audioThread.interrupt();
			}
			if (canStopSense) {
				stopSense();
			}
			stopMotors();
			closeMotors();
			System.out.println("Closed connection.");
			brickConnected = false;
			scannerObservable.setConnected(brickConnected);
		}
	}
	
	public void playBeep() {
		playSystemSound(0);
	}
	
	public void playDoubleBeep() {
		playSystemSound(1);
	}
	
	private void playSystemSound(final int code) {
		enqueueAudio(new Runnable() {
			public void run() {
				Audio a = getAudio();
				if (a != null) {
					a.systemSound(code);
				}
			}
		});
	}
	
	private Audio getAudio() {
		if (brick != null) {
			return brick.getAudio();
		}
		return null;
	}
	
	private void enqueueAudio(Runnable runnable) {
		try {
			audioQueue.put(runnable);
		} catch (InterruptedException e) {
		}
		
		if (!audioQueueRunning) {
			audioQueueRunning = true;
			audioThread = new Thread() {
				public void run() {
					try {
						while(true) {
							Runnable r = audioQueue.take();
							r.run();
						}
					} catch(InterruptedException ex) {
						audioQueueRunning = false;
					}
				}
			};
			audioThread.start();
		}
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
				System.out.println("Started motors.");
			} catch (RemoteException e) {
				System.out.println("Failed to start motors.");
				brickConnected = false;
				scannerObservable.setConnected(brickConnected);
			}
		}
	}
	
	private void startSense() {
		//Click on scan in Sense software with robot
		if (isSenseRunning()) {
			//Robot robot = new Robot();
		    //Rectangle captureSize = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
		    //BufferedImage bufferedImage = robot.createScreenCapture(captureSize);
		    // ...

		    //int color = image.getRGB(x, y);

		    //int  red = (color & 0x00ff0000) >> 16;
		    //int  green = (color & 0x0000ff00) >> 8;
		    //int  blue = color & 0x000000ff;
			
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			Point mouseLocation = MouseInfo.getPointerInfo().getLocation();
			try {
				Robot bot = new Robot();
				//window.setState(Frame.ICONIFIED);
				bot.mouseMove(screenSize.width / 2, (int) (screenSize.height * 0.93));
				bot.mousePress(InputEvent.BUTTON1_MASK);
				bot.mouseRelease(InputEvent.BUTTON1_MASK);
				bot.mouseMove(mouseLocation.x, mouseLocation.y);
			} catch (AWTException e) {				
			}
		}
	}
	
	private void stopMotors() {
		if (brickConnected) {
			try {
				for(int i=0; i<motors.size(); i++) {
					RMIRegulatedMotor motor = motors.get(i);
					motor.stop(true);
				}
				System.out.println("Stopped motors.");
			} catch (RemoteException e) {
				System.out.println("Failed to stop motors.");
				brickConnected = false;
				scannerObservable.setConnected(brickConnected);
			}
		}
	}
	
	private void stopSense() {
		//Click on stop in Sense software with robot
		if (isSenseRunning()) {
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			Point mouseLocation = MouseInfo.getPointerInfo().getLocation();
			try {
				Robot bot = new Robot();
				bot.mouseMove(screenSize.width / 2, (int) (screenSize.height * 0.93));
				bot.mousePress(InputEvent.BUTTON1_MASK);
				bot.mouseRelease(InputEvent.BUTTON1_MASK);
				bot.mouseMove(mouseLocation.x, mouseLocation.y);
			} catch (AWTException e) {				
			}
		}
		canStopSense = false;
	}
	
	private void closeMotors() {
		if (motors != null) {
			try {
				for(int i=0; i<motors.size(); i++) {
					RMIRegulatedMotor motor = motors.get(i);
					motor.close();
				}
				System.out.println("Closed motors.");
			} catch (RemoteException e) {
				System.out.println("Failed to close motors.");
				brickConnected = false;
				scannerObservable.setConnected(brickConnected);
			} finally {
				motors = null;
			}
		}
	}
	
	private boolean isSenseRunning() {
		List<String> processes = getRunningProcesses();
		if (processes != null) {
			for(String process : processes) {
				if (process.startsWith("Sense.exe")) {
					return true;
				}
			}
		}
		return false;
	}
	
	private List<String> getRunningProcesses() {
		ArrayList<String> result = null;
		String line = null;
		BufferedReader input = null;

		try {
			Process p = Runtime.getRuntime().exec(System.getenv("windir") +"\\system32\\"+"tasklist.exe");
	
			input =  new BufferedReader(new InputStreamReader(p.getInputStream()));
			result = new ArrayList<String>();
	
			while ((line = input.readLine()) != null) {
				result.add(line);
			}
		} catch(Exception e) {
			result = null;
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
				}
			}
		}
		return result;
	}
}