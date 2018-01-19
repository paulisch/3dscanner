package at.fhhgb.scanner;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

public class ScannerWindow extends JFrame implements Observer {
	private static final long serialVersionUID = 1L;
	
	private static final Dimension WINDOW_SIZE = new Dimension(800, 600);
	
	private static final Color COLOR_CONNECTED = new Color(160, 230, 110);
	private static final Color COLOR_CONNECTING = new Color(160, 160, 160);
	private static final Color COLOR_DISCONNECTED = new Color(230, 140, 140);
	
	private static final int PROGRESS_RESOLUTION = 100000000;
	
	//Model
	private ScannerController controller = null;
	private ScannerObservable scannerObservable = null;
	
	//View
	private JPanel northPanel = null;
	private JLabel statusLabel = null;
	private JButton reconnectButton = null;
	
	private JProgressBar progressBar = null;
	private JLabel timePassedLabel = null;
	private JLabel timeRemainingLabel = null;
	
	public ScannerWindow(ScannerController controller) {
		super();
		this.controller = controller;
		this.controller.setWindow(this);
		this.scannerObservable = controller.getScannerObservable();
		this.scannerObservable.addObserver(this);
		init();
	}
	
	private void init() {		
		//Handle window closing event
		addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e)
            {
            	e.getWindow().dispose();
            	controller.close();
            }
        });
		
		setTitle("3D-Scanner - Fuchs, Schmutz");
		setIconImages(loadIconImages(new String[] { "/resources/logo_32.png", "/resources/logo_64.png", "/resources/logo_128.png" }));
		setAlwaysOnTop(true);
		
		//Set size
		setMaximumSize(WINDOW_SIZE);
		setMinimumSize(WINDOW_SIZE);
		setPreferredSize(WINDOW_SIZE);
		setSize(WINDOW_SIZE);
		setResizable(false);
		
		//Build UI
		JPanel contentPanel = new JPanel();
		contentPanel.setLayout(new BorderLayout());		
		
		//North status
		northPanel = new JPanel();
		northPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 16, 16));
		statusLabel = new JLabel("");
		statusLabel.setVerticalAlignment(SwingConstants.CENTER);
		northPanel.add(statusLabel);
		
		reconnectButton = new JButton("Retry");
		reconnectButton.setVerticalAlignment(SwingConstants.CENTER);
		reconnectButton.setVisible(false);
		northPanel.add(reconnectButton);
		
		contentPanel.add(northPanel, BorderLayout.PAGE_START);
		
		//Center buttons
		JPanel centerPanel = new JPanel();
		centerPanel.setBorder(BorderFactory.createEmptyBorder(32,32,32,32));
		centerPanel.setLayout(new GridLayout(2, 2, 64, 64));
		JButton backwardButton = createButton("/resources/left.png", 100, "<");
		JButton forwardButton = createButton("/resources/right.png", 100, ">");
		JButton scanButton = createButton("/resources/play.png", 120, "SCAN");
		JButton stopButton = createButton("/resources/stop.png", 120, "STOP");
		centerPanel.add(backwardButton);
		centerPanel.add(forwardButton);
		centerPanel.add(scanButton);
		centerPanel.add(stopButton);
		contentPanel.add(centerPanel, BorderLayout.CENTER);
		
		//Bottom progress
		JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout(new GridLayout(2, 1));
		bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 32, 16, 32));
		UIManager.put("ProgressBar.selectionForeground", Color.black);
		UIManager.put("ProgressBar.selectionBackground", Color.black);
		progressBar = new JProgressBar(0, PROGRESS_RESOLUTION);
		progressBar.setBackground(Color.white);
		progressBar.setForeground(COLOR_CONNECTED);
		progressBar.setString("");
		progressBar.setStringPainted(true);
		progressBar.setValue(0);
		bottomPanel.add(progressBar);
		
		JPanel timePanel = new JPanel();
		timePanel.setLayout(new BorderLayout());
		timePassedLabel = new JLabel("");
		timePassedLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		timePanel.add(timePassedLabel, BorderLayout.LINE_START);
		timeRemainingLabel = new JLabel("");
		timeRemainingLabel.setHorizontalAlignment(SwingConstants.LEFT);
		timePanel.add(timeRemainingLabel, BorderLayout.LINE_END);
		
		bottomPanel.add(timePanel);
		
		contentPanel.add(bottomPanel, BorderLayout.PAGE_END);
		
		add(contentPanel);
		
		//Add button listeners
		reconnectButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!scannerObservable.isConnecting()) {
					controller.onConnectBrick();
				}
			}
		});
		
		forwardButton.addMouseListener(new MouseAdapter() {
			
			private boolean triggered = false;
			
			@Override
			public void mousePressed(MouseEvent e) {
				triggered = !scannerObservable.isScanning();
				if (!scannerObservable.isScanning()) {
					controller.onForward();
				}
			}
			
			@Override
			public void mouseReleased(MouseEvent e) {
				if (triggered) {
					controller.onStop();
				}
			}
		});
		
		backwardButton.addMouseListener(new MouseAdapter() {

			private boolean triggered = false;
			
			@Override
			public void mousePressed(MouseEvent e) {
				triggered = !scannerObservable.isScanning();
				if (!scannerObservable.isScanning()) {
					controller.onBackward();
				}
			}
			
			@Override
			public void mouseReleased(MouseEvent e) {
				if (triggered) {
					controller.onStop();
				}
			}
		});
		
		stopButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				controller.onStop();
			}
		});
		
		scanButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				controller.onScan();
			}
		});
		
		//Pack
		pack();
	}
	
	private JButton createButton(String iconPath, int height, String defaultText) {
		JButton button = new JButton();
		button.setBackground(Color.LIGHT_GRAY);
		button.setFocusable(false);
		
		Image img = getImage(iconPath, height);
		 
		if (img != null) {
			button.setIcon(new ImageIcon(img));
		}
		else {
			button.setText(defaultText);
		}
		 
		return button;
	}
	
	private Image getImage(String iconPath, int size) {
		Image img = null;
		try {
			 img = ImageIO.read(getClass().getResource(iconPath));
			 if (size > -1) {
				 int width = size * img.getWidth(null) / img.getHeight(null);
				 img = img.getScaledInstance(width, size, Image.SCALE_SMOOTH);
			 }
		 } catch (Exception ex) { }
		return img;
	}
	
	private List<Image> loadIconImages(String iconPaths[]) {
		ArrayList<Image> images = new ArrayList<>();
		for (String path : iconPaths) {
			images.add(getImage(path, -1));
		}
		return images;
	}

	@Override
	public void update(Observable observable, Object argument) {
		reconnectButton.setVisible(!scannerObservable.isConnecting() && !scannerObservable.isConnected());
		
		if (scannerObservable.isConnecting()) {
			if (!COLOR_CONNECTING.equals(northPanel.getBackground())) {
				northPanel.setBackground(COLOR_CONNECTING);
				statusLabel.setText("Connecting...");
			}
		}
		else if (scannerObservable.isConnected()) {
			if (!COLOR_CONNECTED.equals(northPanel.getBackground())) {
				northPanel.setBackground(COLOR_CONNECTED);
				statusLabel.setText("Connected");
				
				//Play sound to indicate that connection works
				controller.playBeep();
			}
		}
		else {
			if (scannerObservable.isShouldResetBrick()) {
				northPanel.setBackground(COLOR_DISCONNECTED);
				statusLabel.setText("Not connected. Ports in use. Select system > reset on the brick and retry.");
			}
			else {
				northPanel.setBackground(COLOR_DISCONNECTED);
				statusLabel.setText("Not connected");
			}
		}
		
		if (scannerObservable.isScanning()) {
			double progress = scannerObservable.getProgress();
			int progressPercent = (int)(progress * 100);
			progressBar.setValue((int)(progress * PROGRESS_RESOLUTION));
			progressBar.setString(progressPercent + "%");
			timePassedLabel.setText(scannerObservable.getTimePassed()/1000 + "s");
			timeRemainingLabel.setText(scannerObservable.getTimeRemaining()/1000 + "s");
		}
		else {
			progressBar.setValue(0);
			progressBar.setString("");
			timePassedLabel.setText("");
			timeRemainingLabel.setText("");
		}
	}
}