package at.fhhgb.scanner;

import java.util.Observable;

public class ScannerObservable extends Observable {
	
	private ScannerConfiguration configuration;
	
	//Scanning state
	private int duration = -1;
	private long currentTime = -1;
	private long startTime = -1;
	private boolean isScanning = false;
	
	//Connection state
	private boolean isConnected = false;
	private boolean isConnecting = false;
	
	public ScannerObservable(ScannerConfiguration configuration) {
		super();
		this.configuration = configuration;
	}
	
	public ScannerConfiguration getConfiguration() {
		return configuration;
	}

	public int getDuration() {
		return duration;
	}

	public void setDuration(int duration) {
		this.duration = duration;
		setChanged();
		notifyObservers();
	}

	public long getCurrentTime() {
		return currentTime;
	}

	public void setCurrentTime(long currentTime) {
		this.currentTime = currentTime;
		setChanged();
		notifyObservers();
	}

	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
		setChanged();
		notifyObservers();
	}

	public boolean isScanning() {
		return isScanning;
	}

	public void setScanning(boolean isScanning) {
		this.isScanning = isScanning;
		setChanged();
		notifyObservers();
	}

	public boolean isConnected() {
		return isConnected;
	}

	public void setConnected(boolean isConnected) {
		this.isConnected = isConnected;
		setChanged();
		notifyObservers();
	}

	public boolean isConnecting() {
		return isConnecting;
	}

	public void setConnecting(boolean isConnecting) {
		this.isConnecting = isConnecting;
		setChanged();
		notifyObservers();
	}
	
	public int getProgress() {
		float progress = ((float)currentTime - startTime) / duration;
		if (progress < 0) {
			progress = 0;
		}
		else if (progress > 1) {
			progress = 1;
		}
		return (int)(progress * 100);
	}
	
	public int getTimePassed() {
		return (int)(currentTime - startTime);
	}
	
	public int getTimeRemaining() {
		return (duration - getTimePassed());
	}
}