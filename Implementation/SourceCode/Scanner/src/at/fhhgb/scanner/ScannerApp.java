package at.fhhgb.scanner;

public class ScannerApp {

	public static void main(String[] args) {
		new ScannerApp().start();
	}
	
	private void start() {
		ScannerConfiguration configuration = new ScannerConfiguration();
		ScannerObservable scannerObservable = new ScannerObservable(configuration);
		ScannerController controller = new ScannerController(scannerObservable);
		ScannerWindow window = new ScannerWindow(controller);
		window.setVisible(true);
		window.setLocationRelativeTo(null);
		controller.onConnectBrick();
	}
}