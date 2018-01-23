package at.fhhgb.scannerev3;

import lejos.hardware.Button;
import lejos.hardware.Sound;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.port.MotorPort;
import lejos.robotics.RegulatedMotor;
import lejos.utility.Delay;

/// Main program that is saved on the brick running the default scan procedure.
/**
 * @author Fuchs, Schmutz
 */
public class Scanner {

	public static void main(String[] args) {
		//Create default configuration
		ScannerConfiguration configuration = new ScannerConfiguration();
		
		//Setup motors
		RegulatedMotor motor1 = new EV3LargeRegulatedMotor(MotorPort.B);
		RegulatedMotor motor2 = new EV3LargeRegulatedMotor(MotorPort.C);
		RegulatedMotor motors[] = new RegulatedMotor[] { motor1, motor2 };
		
		//Synchronize motors with each other
		motor1.synchronizeWith(new RegulatedMotor[] { motor2 });
		
		//Get motor speed
		int speed = configuration.getSpeedScanForward();
		
		//Set the duration of countdown in ms before motors start
		int countdown = 3000;
		
		//Set the interval time for checking button presses in ms
		int delay = 20;
		
		//Calculate the required iterations for the countdown loop
		int iterations = countdown / delay;
		
		//Calculate the scan duration
		int scanDuration = configuration.getScanDistanceWithOffset() / speed;
		
		//Configure the speed of the motors
		for (int i=0; i<motors.length; i++) {
			motors[i].setSpeed(speed);
		}
		
		//Countdown before scanning and beep every second; end program if any button is pressed
		for (int i=0; i<iterations; i++) {
			if (Button.readButtons() != 0) {
				return;
			}
			if (i * delay % 1000 == 0) {
				Sound.beep();
			}
			Delay.msDelay(delay);
		}
		
		//Start motors
		for (int i=0; i<motors.length; i++) {
			motors[i].forward();
		}
		
		//Save start time
		long startTime = System.currentTimeMillis();
		
		//Scan until duration is reached or any button is pressed
		while (true) {
			if (Button.readButtons() != 0 || (int)(System.currentTimeMillis() - startTime) > scanDuration) {
				break;
			}
			Delay.msDelay(delay);
		}
		
		//Stop motors
		for (int i=0; i<motors.length; i++) {
			motors[i].stop(true);
		}
		
		//End motor synchronization
		motor1.endSynchronization();
		
		//Beep twice to indicate that the scanning process ended
		Sound.twoBeeps();
	}
}