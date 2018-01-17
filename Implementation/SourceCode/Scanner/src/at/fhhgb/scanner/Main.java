package at.fhhgb.scanner;

import java.io.File;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import lejos.hardware.BrickFinder;
import lejos.hardware.Button;
import lejos.hardware.Sound;
import lejos.hardware.lcd.GraphicsLCD;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.Port;
import lejos.remote.ev3.RMIRegulatedMotor;
import lejos.remote.ev3.RemoteEV3;
import lejos.robotics.RegulatedMotor;
import lejos.utility.Delay;

public class Main {
	
	//Tested with: 200 speed * 53s
	public static final int RING_DISTANCE = 10600000;
	
	//Save scan offset distance (percentage of ring distance)
	public static final double SAVE_SCAN_OFFSET =  0.0188679245283018867924528301887;
	
	public static void main(String[] args) {
		
		RemoteEV3 brick;
		try {
			int speed = 200;
			int duration = (int)((RING_DISTANCE + RING_DISTANCE * SAVE_SCAN_OFFSET) / speed);
			
			brick = new RemoteEV3("10.0.1.1");
			brick.setDefault();
			
			brick.getAudio().playTone(440, 200);
			brick.getAudio().playTone(440, 554);
			brick.getAudio().playTone(659, 200);
			
			RMIRegulatedMotor m1 = brick.createRegulatedMotor("B", 'L');
			RMIRegulatedMotor m2 = brick.createRegulatedMotor("C", 'L');
			
			m1.setSpeed(speed);
			m2.setSpeed(speed);
			
			m1.forward();
			m2.forward();
			
			Delay.msDelay(duration);
			
			m1.stop(true);
			m2.stop(true);
			
			m1.close();
			m2.close();
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (NotBoundException e) {
			e.printStackTrace();
		}
	}
}