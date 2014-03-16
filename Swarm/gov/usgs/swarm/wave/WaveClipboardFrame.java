package gov.usgs.swarm.wave;

import gov.usgs.swarm.FileSpec;
import gov.usgs.swarm.FileSpec.Component;
import gov.usgs.swarm.Icons;
import gov.usgs.swarm.Metadata;
import gov.usgs.swarm.Swarm;
import gov.usgs.swarm.SwarmFrame;
import gov.usgs.swarm.SwarmUtil;
import gov.usgs.swarm.SwingWorker;
import gov.usgs.swarm.Throbber;
import gov.usgs.swarm.TimeListener;
import gov.usgs.swarm.WaveFileSpec;
import gov.usgs.swarm.WaveLabelDialog;
import gov.usgs.swarm.data.CachedDataSource;
import gov.usgs.swarm.data.FileDataSource.FileType;
import gov.usgs.swarm.data.SeismicDataSource;
import gov.usgs.swarm.heli.HelicorderViewPanelListener;
import gov.usgs.util.Time;
import gov.usgs.util.Util;
import gov.usgs.util.png.PngEncoder;
import gov.usgs.util.png.PngEncoderB;
import gov.usgs.util.ui.ExtensionFileFilter;
import gov.usgs.vdx.data.wave.SAC;
import gov.usgs.vdx.data.wave.SeisanChannel;
import gov.usgs.vdx.data.wave.SeisanChannel.SimpleChannel;
import gov.usgs.vdx.data.wave.SeisanFile;
import gov.usgs.vdx.data.wave.WIN;
import gov.usgs.vdx.data.wave.Wave;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyVetoException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TimeZone;
import java.util.concurrent.Callable;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.xml.bind.JAXBException;

/**
 * The wave clipboard internal frame.
 * 
 * @author Dan Cervelli
 * @version $Id: WaveClipboardFrame.java,v 1.10 2007-05-21 02:46:14 dcervelli
 *          Exp $
 
 * @author chirag patel
 * @version $Id: WaveClipboardFrame.java,v 1.10 2014-03-14 02:46:14 cpatel
 *          Exp $
 */
public class WaveClipboardFrame extends SwarmFrame {
	public static final long serialVersionUID = -1;
	private static final Color SELECT_COLOR = new Color(200, 220, 241);
	private static final Color BACKGROUND_COLOR = new Color(0xf7, 0xf7, 0xf7);

	private JScrollPane scrollPane;
	private Box waveBox;
	private List<WaveViewPanel> waves;
	private Set<WaveViewPanel> selectedSet;
	private JToolBar toolbar;
	private JPanel mainPanel;
	private JLabel statusLabel;
	private JToggleButton linkButton;
	private JButton sizeButton;
	private JButton syncButton;
	private JButton sortButton;
	private JButton removeAllButton;
	private JButton saveButton;
	private JButton saveAllButton;
	private JButton openButton;
	private JButton captureButton;
	private JButton histButton;
	private DateFormat saveAllDateFormat;
	private static boolean isSeisanFile = false;

	private WaveViewPanelListener selectListener;
	private WaveViewSettingsToolbar waveToolbar;

	private JButton upButton;
	private JButton downButton;
	private JButton removeButton;
	private JButton compXButton;
	private JButton expXButton;
	private JButton copyButton;
	private JButton forwardButton;
	private JButton backButton;
	private JButton gotoButton;
	private WaveLabelDialog wvd;

	private JPopupMenu popup;

	private Map<WaveViewPanel, Stack<double[]>> histories;

	private HelicorderViewPanelListener linkListener;

	private boolean heliLinked = true;

	private Throbber throbber;

	private int waveHeight = -1;

	private int lastClickedIndex = -1;
	
	private WaveFileSpec waveFileSpec = new WaveFileSpec();

	public WaveClipboardFrame() {
		super("Wave Clipboard", true, true, true, false);
		this.setFocusable(true);
		selectedSet = new HashSet<WaveViewPanel>();
		saveAllDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
		saveAllDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		waves = new ArrayList<WaveViewPanel>();
		histories = new HashMap<WaveViewPanel, Stack<double[]>>();
		createUI();
		linkListener = new HelicorderViewPanelListener() {
			public void insetCreated(double st, double et) {
				if (heliLinked)
					repositionWaves(st, et);
			}
		};
	}
	
	public void saveFileSpec() {
		try {
			waveFileSpec.saveFileSpec();
		} catch (JAXBException e) {
			e.printStackTrace();
		}
	}

	public HelicorderViewPanelListener getLinkListener() {
		return linkListener;
	}

	private void createUI() {
		this.setFrameIcon(Icons.clipboard);
		this.setSize(Swarm.config.clipboardWidth, Swarm.config.clipboardHeight);
		this.setLocation(Swarm.config.clipboardX, Swarm.config.clipboardY);
		this.setDefaultCloseOperation(JInternalFrame.DO_NOTHING_ON_CLOSE);

		toolbar = SwarmUtil.createToolBar();
		mainPanel = new JPanel(new BorderLayout());

		createMainButtons();
		createWaveButtons();

		mainPanel.add(toolbar, BorderLayout.NORTH);

		waveBox = new Box(BoxLayout.Y_AXIS);
		scrollPane = new JScrollPane(waveBox);
		scrollPane
				.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane
				.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.getVerticalScrollBar().setUnitIncrement(40);
		mainPanel.add(scrollPane, BorderLayout.CENTER);

		statusLabel = new JLabel(" ");
		statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 1));
		mainPanel.add(statusLabel, BorderLayout.SOUTH);

		mainPanel.setBorder(BorderFactory.createEmptyBorder(0, 2, 1, 2));
		this.setContentPane(mainPanel);

		createListeners();
		doButtonEnables();
	}

	private void createMainButtons() {
		openButton = SwarmUtil.createToolBarButton(Icons.open,
				"Open a saved wave", new OpenActionListener());
		toolbar.add(openButton);

		saveButton = SwarmUtil.createToolBarButton(Icons.save,
				"Save selected wave", new SaveActionListener());
		saveButton.setEnabled(false);
		toolbar.add(saveButton);

		saveAllButton = SwarmUtil.createToolBarButton(
				Icons.saveall, "Save all waves",
				new SaveAllActionListener());
		saveAllButton.setEnabled(false);
		toolbar.add(saveAllButton);

		toolbar.addSeparator();

		linkButton = SwarmUtil.createToolBarToggleButton(
				Icons.helilink,
				"Synchronize times with helicorder wave", new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						heliLinked = linkButton.isSelected();
					}
				});
		linkButton.setSelected(heliLinked);
		toolbar.add(linkButton);

		syncButton = SwarmUtil.createToolBarButton(Icons.clock,
				"Synchronize times with selected wave", new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						syncChannels();
					}
				});
		syncButton.setEnabled(false);
		toolbar.add(syncButton);

		sortButton = SwarmUtil.createToolBarButton(Icons.geosort,
				"Sort waves by nearest to selected wave", new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						sortChannelsByNearest();
					}
				});
		sortButton.setEnabled(false);
		toolbar.add(sortButton);

		toolbar.addSeparator();

		sizeButton = SwarmUtil.createToolBarButton(Icons.resize,
				"Set clipboard wave size", new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						doSizePopup(sizeButton.getX(), sizeButton.getY() + 2
								* sizeButton.getHeight());
					}
				});
		toolbar.add(sizeButton);

		removeAllButton = SwarmUtil.createToolBarButton(
				Icons.deleteall, "Remove all waves from clipboard",
				new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						removeWaves();
					}
				});
		removeAllButton.setEnabled(false);
		toolbar.add(removeAllButton);

		toolbar.addSeparator();
		captureButton = SwarmUtil.createToolBarButton(Icons.camera,
				"Save clipboard image (P)", new CaptureActionListener());
		Util.mapKeyStrokeToButton(this, "P", "capture", captureButton);
		toolbar.add(captureButton);
	}

	// TODO: don't write image on event thread
	// TODO: unify with MapFrame.CaptureActionListener
	class CaptureActionListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			if (waves == null || waves.size() == 0)
				return;

			JFileChooser chooser = Swarm.getApplication().getFileChooser();
			File lastPath = new File(Swarm.config.lastPath);
			chooser.setCurrentDirectory(lastPath);
			chooser.setSelectedFile(new File("clipboard.png"));
			chooser.setDialogTitle("Save Clipboard Screen Capture");
			int result = chooser.showSaveDialog(Swarm.getApplication());
			File f = null;
			if (result == JFileChooser.APPROVE_OPTION) {
				f = chooser.getSelectedFile();

				if (f.exists()) {
					int choice = JOptionPane.showConfirmDialog(
							Swarm.getApplication(), "File exists, overwrite?",
							"Confirm", JOptionPane.YES_NO_OPTION);
					if (choice != JOptionPane.YES_OPTION)
						return;
				}
				Swarm.config.lastPath = f.getParent();
			}
			if (f == null)
				return;

			int height = 0;
			int width = waves.get(0).getWidth();
			for (WaveViewPanel panel : waves)
				height += panel.getHeight();

			BufferedImage image = new BufferedImage(width, height,
					BufferedImage.TYPE_4BYTE_ABGR);
			Graphics g = image.getGraphics();
			for (WaveViewPanel panel : waves) {
				panel.paint(g);
				g.translate(0, panel.getHeight());
			}
			try {
				PngEncoderB png = new PngEncoderB(image, false,
						PngEncoder.FILTER_NONE, 7);
				FileOutputStream out = new FileOutputStream(f);
				byte[] bytes = png.pngEncode();
				out.write(bytes);
				out.close();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	private void createWaveButtons() {
		toolbar.addSeparator();

		backButton = SwarmUtil.createToolBarButton(Icons.left,
				"Scroll back time 20% (Left arrow)", new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						shiftTime(-0.20);
					}
				});
		Util.mapKeyStrokeToButton(this, "LEFT", "backward1", backButton);
		toolbar.add(backButton);

		forwardButton = SwarmUtil.createToolBarButton(Icons.right,
				"Scroll forward time 20% (Right arrow)", new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						shiftTime(0.20);
					}
				});
		toolbar.add(forwardButton);
		Util.mapKeyStrokeToButton(this, "RIGHT", "forward1", forwardButton);

		gotoButton = SwarmUtil.createToolBarButton(Icons.gototime,
				"Go to time (Ctrl-G)", new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						String t = JOptionPane.showInputDialog(
								Swarm.getApplication(),
								"Input time in 'YYYYMMDDhhmm[ss]' format:",
								"Go to Time", JOptionPane.PLAIN_MESSAGE);
						if (t != null)
							gotoTime(t);
					}
				});
		toolbar.add(gotoButton);
		Util.mapKeyStrokeToButton(this, "ctrl G", "goto", gotoButton);

		compXButton = SwarmUtil.createToolBarButton(Icons.xminus,
				"Shrink sample time 20% (Alt-left arrow, +)",
				new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						scaleTime(0.20);
					}
				});
		toolbar.add(compXButton);
		Util.mapKeyStrokeToButton(this, "alt LEFT", "compx", compXButton);
		Util.mapKeyStrokeToButton(this, "EQUALS", "compx2", compXButton);
		Util.mapKeyStrokeToButton(this, "shift EQUALS", "compx2", compXButton);

		expXButton = SwarmUtil.createToolBarButton(Icons.xplus,
				"Expand sample time 20% (Alt-right arrow, -)",
				new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						scaleTime(-0.20);
					}
				});
		toolbar.add(expXButton);
		Util.mapKeyStrokeToButton(this, "alt RIGHT", "expx", expXButton);
		Util.mapKeyStrokeToButton(this, "MINUS", "expx", expXButton);

		histButton = SwarmUtil.createToolBarButton(Icons.timeback,
				"Last time settings (Backspace)", new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						back();
					}
				});
		Util.mapKeyStrokeToButton(this, "BACK_SPACE", "back", histButton);
		toolbar.add(histButton);
		toolbar.addSeparator();

		waveToolbar = new WaveViewSettingsToolbar(null, toolbar, this);

		copyButton = SwarmUtil.createToolBarButton(Icons.clipboard,
				"Place another copy of wave on clipboard (C or Ctrl-C)",
				new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						// TODO: implement
						// if (selected != null)
						// {
						// WaveViewPanel wvp = new WaveViewPanel(selected);
						// wvp.setBackgroundColor(BACKGROUND_COLOR);
						// addWave(wvp);
						// }
					}
				});
		Util.mapKeyStrokeToButton(this, "C", "clipboard1", copyButton);
		Util.mapKeyStrokeToButton(this, "control C", "clipboard2", copyButton);
		toolbar.add(copyButton);

		toolbar.addSeparator();

		upButton = SwarmUtil.createToolBarButton(Icons.up,
				"Move wave up in clipboard (Up arrow)", new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						moveUp();
					}
				});
		Util.mapKeyStrokeToButton(this, "UP", "up", upButton);
		toolbar.add(upButton);

		downButton = SwarmUtil.createToolBarButton(Icons.down,
				"Move wave down in clipboard (Down arrow)",
				new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						moveDown();
					}
				});
		Util.mapKeyStrokeToButton(this, "DOWN", "down", downButton);
		toolbar.add(downButton);

		removeButton = SwarmUtil.createToolBarButton(Icons.delete,
				"Remove wave from clipboard (Delete)", new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						remove();
					}
				});
		Util.mapKeyStrokeToButton(this, "DELETE", "remove", removeButton);
		toolbar.add(removeButton);

		toolbar.add(Box.createHorizontalGlue());

		throbber = new Throbber();
		toolbar.add(throbber);

		Util.mapKeyStrokeToAction(this, "control A", "selectAll",
				new AbstractAction() {
					private static final long serialVersionUID = 1L;

					public void actionPerformed(ActionEvent e) {
						for (WaveViewPanel wave : waves)
							select(wave);
					}
				});
	}

	private void createListeners() {
		this.addInternalFrameListener(new InternalFrameAdapter() {
			public void internalFrameActivated(InternalFrameEvent e) {
			}

			public void internalFrameDeiconified(InternalFrameEvent e) {
				resizeWaves();
			}

			public void internalFrameClosing(InternalFrameEvent e) {
				setVisible(false);
			}

			public void internalFrameClosed(InternalFrameEvent e) {
			}
		});

		this.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				resizeWaves();
			}
		});

		Swarm.getApplication().addTimeListener(new TimeListener() {
			public void timeChanged(double j2k) {
				for (WaveViewPanel panel : waves) {
					if (panel != null)
						panel.setCursorMark(j2k);
				}
			}
		});

		selectListener = new WaveViewPanelAdapter() {
			public void mousePressed(WaveViewPanel src, MouseEvent e, boolean dragging) {
				requestFocusInWindow();
				int thisIndex = getWaveIndex(src);
				if (!e.isControlDown() && !e.isShiftDown() && !e.isAltDown()) {
					deselectAll();
					select(src);
				} else if (e.isControlDown()) {
					if (selectedSet.contains(src))
						deselect(src);
					else
						select(src);
				} else if (e.isShiftDown()) {
					if (lastClickedIndex == -1)
						select(src);
					else {
						deselectAll();
						int min = Math.min(lastClickedIndex, thisIndex);
						int max = Math.max(lastClickedIndex, thisIndex);
						for (int i = min; i <= max; i++) {
							WaveViewPanel ps = (WaveViewPanel) waveBox
									.getComponent(i);
							select(ps);
						}
					}
				}
				lastClickedIndex = thisIndex;
			}

			public void waveZoomed(WaveViewPanel src, double st, double et,	double nst, double net) {
				double[] t = new double[] { st, et };
				addHistory(src, t);
				for (WaveViewPanel wvp : selectedSet) {
					if (wvp != src) {
						addHistory(wvp, t);
						wvp.zoom(nst, net);
					}
				}
			}

			public void waveClosed(WaveViewPanel src) {
				remove(src);
			}
		};
	}

	private int calculateWaveHeight() {
		if (waveHeight > 0)
			return waveHeight;

		int w = scrollPane.getViewport().getSize().width;
		int h = (int) Math.round((double) w * 60.0 / 300.0);
		h = Math.min(200, h);
		h = Math.max(h, 80);
		return h;
	}

	private void setWaveHeight(int s) {
		waveHeight = s;
		resizeWaves();
	}

	private void doSizePopup(int x, int y) {
		if (popup == null) {
			final String[] labels = new String[] { "Auto", null, "Tiny",
					"Small", "Medium", "Large" };
			final int[] sizes = new int[] { -1, -1, 50, 100, 160, 230 };
			popup = new JPopupMenu();
			ButtonGroup group = new ButtonGroup();
			for (int i = 0; i < labels.length; i++) {
				if (labels[i] != null) {
					final int size = sizes[i];
					JRadioButtonMenuItem mi = new JRadioButtonMenuItem(
							labels[i]);
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							setWaveHeight(size);
						}
					});
					if (waveHeight == size)
						mi.setSelected(true);
					group.add(mi);
					popup.add(mi);
				} else
					popup.addSeparator();
			}
		}
		popup.show(this, x, y);
	}

	private class OpenActionListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			JFileChooser chooser = Swarm.getApplication().getFileChooser();
			chooser.resetChoosableFileFilters();
			ExtensionFileFilter txtExt = new ExtensionFileFilter(".txt",
					"Matlab-readable text files");
			ExtensionFileFilter sacExt = new ExtensionFileFilter(".sac",
					"SAC files");
			ExtensionFileFilter seisanExt = new ExtensionFileFilter(".seisan",
					"SEISAN files");
			chooser.addChoosableFileFilter(txtExt);
			chooser.addChoosableFileFilter(sacExt);
			chooser.addChoosableFileFilter(seisanExt);
			chooser.setDialogTitle("Open Wave");
			chooser.setFileFilter(chooser.getAcceptAllFileFilter());
			File lastPath = new File(Swarm.config.lastPath);
			chooser.setCurrentDirectory(lastPath);
			chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			chooser.setMultiSelectionEnabled(true);
			int result = chooser.showOpenDialog(Swarm.getApplication());
			if (result == JFileChooser.APPROVE_OPTION) {
				File[] fs = chooser.getSelectedFiles();

				for (int i = 0; i < fs.length; i++) {
					if (fs[i].isDirectory()) {
						File[] dfs = fs[i].listFiles();
						for (int j = 0; j < dfs.length; j++)
							openFile(dfs[j]);
						Swarm.config.lastPath = fs[i].getParent();
					} else {
						openFile(fs[i]);
						Swarm.config.lastPath = fs[i].getParent();
					}
				}
			}
		}
	}

	private class SaveActionListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			WaveViewPanel selected = getSingleSelected();
			if (selected == null)
				return;

			JFileChooser chooser = Swarm.getApplication().getFileChooser();
			chooser.resetChoosableFileFilters();
			chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			chooser.setMultiSelectionEnabled(false);
			chooser.setDialogTitle("Save Wave");

			ExtensionFileFilter txtExt = new ExtensionFileFilter(".txt",
					"Matlab-readable text files");
			ExtensionFileFilter sacExt = new ExtensionFileFilter(".sac",
					"SAC files");
			chooser.addChoosableFileFilter(txtExt);
			chooser.addChoosableFileFilter(sacExt);
			chooser.setFileFilter(chooser.getAcceptAllFileFilter());

			File lastPath = new File(Swarm.config.lastPath);
			chooser.setCurrentDirectory(lastPath);
			chooser.setSelectedFile(new File(selected.getChannel() + ".txt"));
			int result = chooser.showSaveDialog(Swarm.getApplication());
			if (result == JFileChooser.APPROVE_OPTION) {
				File f = chooser.getSelectedFile();
				String path = f.getPath();
				if (!(path.endsWith(".txt") || path.endsWith(".sac"))) {
					if (chooser.getFileFilter() == sacExt)
						f = new File(path + ".sac");
					else
						f = new File(path + ".txt");
				}
				boolean confirm = true;
				if (f.exists()) {
					if (f.isDirectory()) {
						JOptionPane.showMessageDialog(Swarm.getApplication(),
								"You can not select an existing directory.",
								"Error", JOptionPane.ERROR_MESSAGE);
						return;
					}
					confirm = false;
					int choice = JOptionPane.showConfirmDialog(
							Swarm.getApplication(), "File exists, overwrite?",
							"Confirm", JOptionPane.YES_NO_OPTION);
					if (choice == JOptionPane.YES_OPTION)
						confirm = true;
				}

				if (confirm) {
					try {
						Swarm.config.lastPath = f.getParent();
						String fn = f.getPath().toLowerCase();
						if (fn.endsWith(".sac")) {
							SAC sac = selected.getWave().toSAC();
							SimpleChannel chan = selected.getChannel();
							sac.kstnm = chan.stationCode;// scn[0];
							sac.kcmpnm = chan.firstTwoComponentCode + chan.lastComponentCode;// scn[1];
							sac.knetwk = chan.networkName;// scn[2];
							sac.write(f);
						} else
							selected.getWave().exportToText(f.getPath());
					} catch (FileNotFoundException ex) {
						JOptionPane.showMessageDialog(Swarm.getApplication(),
								"Directory does not exist.", "Error",
								JOptionPane.ERROR_MESSAGE);
					} catch (IOException ex) {
						JOptionPane.showMessageDialog(Swarm.getApplication(),
								"Error writing file.", "Error",
								JOptionPane.ERROR_MESSAGE);
					}
				}
			}
		}
	}

	private class SaveAllActionListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			if (waves.size() <= 0)
				return;

			JFileChooser chooser = Swarm.getApplication().getFileChooser();
			chooser.resetChoosableFileFilters();
			chooser.setMultiSelectionEnabled(false);
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			chooser.setDialogTitle("Save SAC File Directory");

			File lastPath = new File(Swarm.config.lastPath);
			chooser.setCurrentDirectory(lastPath);
			int result = chooser.showSaveDialog(Swarm.getApplication());
			if (result == JFileChooser.CANCEL_OPTION)
				return;
			File f = chooser.getSelectedFile();
			if (f == null) {
				JOptionPane.showMessageDialog(Swarm.getApplication(),
						"You must select a directory.", "Error",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
			if (result == JFileChooser.APPROVE_OPTION) {
				try {
					if (f.exists() && !f.isDirectory())
						return;
					if (!f.exists())
						f.mkdir();
					for (WaveViewPanel wvp : waves) {
						Wave sw = wvp.getWave();

						if (sw != null) {
							sw = sw.subset(wvp.getStartTime(), wvp.getEndTime());
							String date = saveAllDateFormat.format(Util
									.j2KToDate(sw.getStartTime()));
							File dir = new File(f.getPath()
									+ File.separatorChar + date);
							if (!dir.exists())
								dir.mkdir();

							SAC sac = sw.toSAC();
							// String[] scn = wvp.getChannel().split(" ");
							// sac.kstnm = scn[0];
							// sac.kcmpnm = scn[1];
							// sac.knetwk = scn[2];
							sac.write(new File(dir.getPath()
									+ File.separatorChar
									+ wvp.getChannel().toString.replace(' ' , '.')) +".sac");
						}
					}
					Swarm.config.lastPath = f.getPath();
				} catch (FileNotFoundException ex) {
					JOptionPane.showMessageDialog(Swarm.getApplication(),
							"Directory does not exist.", "Error",
							JOptionPane.ERROR_MESSAGE);
				} catch (IOException ex) {
					JOptionPane.showMessageDialog(Swarm.getApplication(),
							"Error writing file.", "Error",
							JOptionPane.ERROR_MESSAGE);
				}
			}
		}
	}

	

	private SAC readSAC(File f) {
		SAC sac = new SAC();
		try {
			sac.read(f.getPath());
		} catch (Exception ex) {
			sac = null;
		}
		return sac;
	}
	
	private WIN readWIN(File f) {
		WIN win = new WIN();
		try {
			win.read(f.getPath());
		} catch (Exception ex) {
			win = null;
		}
		return win;
	}
	
	private SeisanFile readSEISAN(File f) {
		SeisanFile seisan = new SeisanFile();

		try {
			seisan.read(f.getPath());
			// System.out.println(seisan.toWave());
		} catch (Exception ex) {
			ex.printStackTrace();
			seisan = null;
		}
		return seisan;
	}
	
	public WaveViewPanel getWave(String filePath, int fileIndex) {
		for (WaveViewPanel wvp : waves) {
			if (wvp.getFilePath().equals(filePath)
					&& wvp.getFileIndex() == fileIndex) {
				return wvp;
			}
		}
		return null;
	}


	public void openFile(File f) {
		wvd = new WaveLabelDialog();
		SAC sac = null;
		Wave sw = null;
		SeisanFile seisan = null;
		WIN win = null;
		String channel = f.getName();
		FileType ft = FileType.fromFileExtension(f);
		switch (ft) {
		case SAC:
			sac = readSAC(f);
			break;
		case WIN:
			win = readWIN(f);
			break;
		case SEISAN:
			seisan = readSEISAN(f);
			break;
		case TEXT:
			sw = Wave.importFromText(f.getPath());
			break;
		case UNKNOWN:
			// try SAC
			sac = readSAC(f);
			// try text
			if (sac == null)
				sw = Wave.importFromText(f.getPath());
			break;
		}

		ArrayList<Component> components = new ArrayList<Component>();
		if (sac != null) {
			isSeisanFile = false;
			sw = sac.toWave();
			channel = sac.getWinstonChannel().replace('$', ' ') + "_index_1";
			WaveViewPanel po = getWave(f.getAbsolutePath(), 1);
			if (po == null) {
				ArrayList<FileSpec> fss = getRelatedFileSpecs(1);
				WaveViewPanel wvp = new WaveViewPanel();

				wvp.setWave(sw, sw.getStartTime(), sw.getEndTime());
				wvp.setFileName(f.getName());
				wvp.setFileType(ft.name());
				wvp.setFileIndex(1);
				wvp.setFilePath(f.getAbsolutePath());
				WaveViewPanel p = new WaveViewPanel(wvp);
				if (!isChannelInfoValid(p)) {
					editLabels(p, fss);
					if (wvd.getSelectedFileSpec() == null) {
						p.setStationInfo(wvd.getStation(),
								wvd.getFirstTwoComponent(), wvd.getNetwork(),
								wvd.getLastComponentCode());
					} else {
						Component comp = wvd.getSelectedFileSpec()
								.getComponent(p.getFileIndex());

						p.setStationInfo(comp.getStationCode(),
								comp.getComponentCode(), comp.getNetworkCode(),
								comp.getLastComponentCode());
					}

				}
				
				Component comp = new Component();
				comp.setIndex(1);
				comp.setComponentCode(p.getChannel().firstTwoComponentCode);
				comp.setLastComponentCode(p.getChannel().lastComponentCode);
				comp.setNetworkCode(p.getChannel().networkName);
				comp.setStationCode(p.getChannel().stationCode);
				components.add(comp);

				channel = p.getChannel().toString;
				CachedDataSource cache = CachedDataSource.getInstance();
				cache.putWave(channel, sw);
				p.setDataSource(cache);
				select(p);
				WaveClipboardFrame.this.addWave(p);
			}else{
				Component comp = new Component();
				comp.setIndex(1);
				comp.setComponentCode(po.getChannel().firstTwoComponentCode);
				comp.setLastComponentCode(po.getChannel().lastComponentCode);
				comp.setNetworkCode(po.getChannel().networkName);
				comp.setStationCode(po.getChannel().stationCode);
				components.add(comp);
			}
			saveDetailstoFileSpec(f.getName(), components);

		} else if (win != null) {
			isSeisanFile = false;
			Wave[] waves = win.toWave();
			ArrayList<FileSpec> fss = getRelatedFileSpecs(waves.length);
			
			String prevStation = "";
			String prevNetwork = "";
			String prevComp = "";
			
			for (int i = 0; i < waves.length; i++) {
				WaveViewPanel po = getWave(f.getAbsolutePath(), (i + 1));
				if (po == null) {
					sw = waves[i];
					WaveViewPanel wvp = new WaveViewPanel();

					wvp.setWave(sw, sw.getStartTime(), sw.getEndTime());
					wvp.setFileName(f.getName());
					wvp.setFileType(ft.name());
					wvp.setFileIndex(i + 1);
					wvp.setFilePath(f.getAbsolutePath());
					WaveViewPanel p = new WaveViewPanel(wvp);
					if (!isChannelInfoValid(p)) {
						
						p.setStationInfo(prevStation,
								prevComp,
								prevNetwork,
								"");
						
						if (wvd.getSelectedFileSpec() == null) {
							editLabels(p, fss);
							p.setStationInfo(wvd.getStation(),
									wvd.getFirstTwoComponent(),
									wvd.getNetwork(),
									wvd.getLastComponentCode());
						} else {
							Component comp = wvd.getSelectedFileSpec()
									.getComponent(p.getFileIndex());

							p.setStationInfo(comp.getStationCode(),
									comp.getComponentCode(),
									comp.getNetworkCode(),
									comp.getLastComponentCode());
						}
					}else{
						Component comp = wvd.getSelectedFileSpec()
						.getComponent(p.getFileIndex());

						p.setStationInfo(comp.getStationCode(),
								comp.getComponentCode(),
								comp.getNetworkCode(),
								comp.getLastComponentCode());
					}
					
					
					prevStation = p.getStationCode();
					prevNetwork = p.getNetwork();
					prevComp = p.getFirstComp();
					
					Component comp = new Component();
					comp.setIndex(i + 1);
					comp.setComponentCode(p.getChannel().firstTwoComponentCode);
					comp.setLastComponentCode(p.getChannel().lastComponentCode);
					comp.setNetworkCode(p.getChannel().networkName);
					comp.setStationCode(p.getChannel().stationCode);
					components.add(comp);

					channel = p.getChannel().toString;
					CachedDataSource cache = CachedDataSource.getInstance();
					cache.putWave(channel, sw);
					p.setDataSource(cache);
					select(p);
					WaveClipboardFrame.this.addWave(p);
				}
				else{
					Component comp = new Component();
					comp.setIndex(i + 1);
					comp.setComponentCode(po.getChannel().firstTwoComponentCode);
					comp.setLastComponentCode(po.getChannel().lastComponentCode);
					comp.setNetworkCode(po.getChannel().networkName);
					comp.setStationCode(po.getChannel().stationCode);
					components.add(comp);
				}
			}
			saveDetailstoFileSpec(f.getName(), components);
		}

		else if (seisan != null) {
			isSeisanFile = true;
			sw = new Wave();
			ArrayList<FileSpec> fss = getRelatedFileSpecs(seisan.getChannels()
					.size());
			
			String prevStation = "";
			String prevNetwork = "";
			String prevComp = "";
			
			for (int i = 0; i < seisan.getChannels().size(); i++) {
				WaveViewPanel po = getWave(f.getAbsolutePath(), (i + 1));
				if (po == null) {
					SeisanChannel c = seisan.getChannels().get(i);
					
					sw = c.toWave();
					WaveViewPanel wvp = new WaveViewPanel();

					wvp.setWave(sw, sw.getStartTime(), sw.getEndTime());
					wvp.setFileName(f.getName());
					wvp.setFilePath(f.getAbsolutePath());
					wvp.setFileType(ft.name());
					wvp.setFileIndex(i + 1);
					WaveViewPanel p = new WaveViewPanel(wvp);
					
					p.setStationInfo(c.channel.stationCode,
								c.channel.firstTwoComponentCode,
								c.channel.networkName,
								c.channel.lastComponentCode);
					
					if (!isChannelInfoValid(p)) {
						
						String st = c.channel.stationCode;
						String nt = c.channel.networkName ;
						String fc = c.channel.firstTwoComponentCode;
						
						if(st == null || st.isEmpty() || st.trim().length() == 0){
							st = prevStation;
						}
						
						if(nt == null || nt.isEmpty() || nt.trim().length()==0){
							nt = prevNetwork;
						}	
						
						
						
						if(fc == null || fc.isEmpty() || fc.trim().length()==0){
							fc = prevComp;
						}	
						
						p.setStationInfo(st,
								fc,
								nt,
								c.channel.lastComponentCode);
						
						if (wvd.getSelectedFileSpec() == null) {
							editLabels(p, fss);
							if (wvd.getSelectedFileSpec() == null) {
								p.setStationInfo(wvd.getStation(),
										wvd.getFirstTwoComponent(),
										wvd.getNetwork(),
										wvd.getLastComponentCode());
							} else {
								Component comp = wvd.getSelectedFileSpec()
										.getComponent(p.getFileIndex());

								p.setStationInfo(comp.getStationCode(),
										comp.getComponentCode(),
										comp.getNetworkCode(),
										comp.getLastComponentCode());
							}
						}else{
							Component comp = wvd.getSelectedFileSpec()
							.getComponent(p.getFileIndex());

							p.setStationInfo(comp.getStationCode(),
									comp.getComponentCode(),
									comp.getNetworkCode(),
									comp.getLastComponentCode());
						}
					}

					prevStation = p.getStationCode();
					prevNetwork = p.getNetwork();
					prevComp = p.getFirstComp();
					
					
					Component comp = new Component();
					comp.setIndex(i + 1);
					comp.setComponentCode(p.getChannel().firstTwoComponentCode);
					comp.setLastComponentCode(p.getChannel().lastComponentCode);
					comp.setNetworkCode(p.getChannel().networkName);
					comp.setStationCode(p.getChannel().stationCode);
					components.add(comp);

					channel = p.getChannel().toString;
					CachedDataSource cache = CachedDataSource.getInstance();
					cache.putWave(channel, sw);
					p.setDataSource(cache);
					select(p);
					WaveClipboardFrame.this.addWave(p);
				}
				else{
					Component comp = new Component();
					comp.setIndex(i + 1);
					comp.setComponentCode(po.getChannel().firstTwoComponentCode);
					comp.setLastComponentCode(po.getChannel().lastComponentCode);
					comp.setNetworkCode(po.getChannel().networkName);
					comp.setStationCode(po.getChannel().stationCode);
					components.add(comp);
				}
			}
			saveDetailstoFileSpec(f.getName(), components);
		} else {
			JOptionPane.showMessageDialog(Swarm.getApplication(),
					"There was an error opening the file, '" + f.getName()
							+ "'.", "Error", JOptionPane.ERROR_MESSAGE);
			isSeisanFile = false;
		}

	}
	
	private void saveDetailstoFileSpec(String fileName,
			ArrayList<Component> components) {
		try {
			FileSpec fileSpec = Swarm.getApplication().getWaveClipboard()
					.getWaveFileSpec().getFileSpec(fileName);
			if (fileSpec == null) {
				fileSpec = new FileSpec();
				fileSpec.setFileName(fileName);
				Swarm.getApplication().getWaveClipboard().getWaveFileSpec()
						.getSpecs().add(fileSpec);
				fileSpec.setComponents(components);
			}else{
				fileSpec.setComponents(components);
			}
			Swarm.getApplication().getWaveClipboard().getWaveFileSpec()
					.saveFileSpec();
		} catch (JAXBException e) {
			e.printStackTrace();
		}
	}
	
	private boolean editLabels(final WaveViewPanel wvp, ArrayList<FileSpec> fss) {
		if (wvd == null) {
			wvd = new WaveLabelDialog();
		}
		wvd.setWaveViewPanel(wvp.getChannel(), wvp.getFileName(),
				wvp.getFileIndex());
		wvd.setActionAfterFinish(new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				return null;
			}
		});
		wvd.setFileSpecs(fss);
		wvd.setVisible(true);
		return wvd.isOK;
	}

	private void doButtonEnables() {
		boolean enable = (waves == null || waves.size() == 0);
		saveButton.setEnabled(!enable);
		sortButton.setEnabled(!enable);
		saveAllButton.setEnabled(!enable);
		syncButton.setEnabled(!enable);
		removeAllButton.setEnabled(!enable);
		saveAllButton.setEnabled(!enable);

		boolean allowSingle = (selectedSet.size() == 1);
		upButton.setEnabled(allowSingle);
		downButton.setEnabled(allowSingle);
		sortButton.setEnabled(allowSingle);
		syncButton.setEnabled(allowSingle);
		saveButton.setEnabled(allowSingle);

		boolean allowMulti = (selectedSet.size() > 0);
		backButton.setEnabled(allowMulti);
		expXButton.setEnabled(allowMulti);
		compXButton.setEnabled(allowMulti);
		backButton.setEnabled(allowMulti);
		forwardButton.setEnabled(allowMulti);
		histButton.setEnabled(allowMulti);
		removeButton.setEnabled(allowMulti);
		gotoButton.setEnabled(allowMulti);
	}

	public synchronized void sortChannelsByNearest() {
		WaveViewPanel p = getSingleSelected();
		if (p == null)
			return;

		ArrayList<WaveViewPanel> sorted = new ArrayList<WaveViewPanel>(
				waves.size());
		for (WaveViewPanel wave : waves)
			sorted.add(wave);

		final Metadata smd = Swarm.config.getMetadata(p.getChannel().toString);
		if (smd == null || Double.isNaN(smd.getLongitude())
				|| Double.isNaN(smd.getLatitude()))
			return;

		Collections.sort(sorted, new Comparator<WaveViewPanel>() {
			public int compare(WaveViewPanel wvp1, WaveViewPanel wvp2) {
				Metadata md = Swarm.config.getMetadata(wvp1.getChannel().toString);
				double d1 = smd.distanceTo(md);
				md = Swarm.config.getMetadata(wvp2.getChannel().toString);
				double d2 = smd.distanceTo(md);
				return Double.compare(d1, d2);
			}
		});

		removeWaves();
		for (WaveViewPanel wave : sorted)
			addWave(wave);
		select(p);
	}

	public synchronized WaveViewPanel getSingleSelected() {
		if (selectedSet.size() != 1)
			return null;

		WaveViewPanel p = null;
		for (WaveViewPanel panel : selectedSet)
			p = panel;

		return p;
	}

	public synchronized void syncChannels() {
		final WaveViewPanel p = getSingleSelected();
		if (p == null)
			return;

		final double st = p.getStartTime();
		final double et = p.getEndTime();

		// TODO: thread bug here. must synch iterator below
		final SwingWorker worker = new SwingWorker() {
			public Object construct() {
				List<WaveViewPanel> copy = null;
				synchronized (WaveClipboardFrame.this) {
					copy = new ArrayList<WaveViewPanel>(waves);
				}
				for (WaveViewPanel wvp : copy) {
					if (wvp != p) {
						if (wvp.getDataSource() != null) {
							addHistory(wvp, new double[] { wvp.getStartTime(),
									wvp.getEndTime() });
							Wave sw = wvp.getDataSource().getWave(wvp.getChannel().toString, st, et);
							wvp.setWave(sw, st, et);
						}
					}
				}
				return null;
			}

			public void finished() {
				repaint();
			}
		};
		worker.start();
	}

	public void setStatusText(final String s) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				statusLabel.setText(s);
			}
		});
	}

	public WaveViewPanel getSelected() {
		return null;
		// return selected;
	}

	public synchronized void addWave(final WaveViewPanel p) {
		p.addListener(selectListener);
		p.setOffsets(54, 8, 21, 19);
		p.setAllowClose(true);
		p.setStatusLabel(statusLabel);
		p.setAllowDragging(true);
		p.setDisplayTitle(true);
		int w = scrollPane.getViewport().getSize().width;
		p.setSize(w, calculateWaveHeight());
		p.setBottomBorderColor(Color.GRAY);
		p.createImage();
		waveBox.add(p);
		waves.add(p);
		doButtonEnables();
		waveBox.validate();
	}
	
	public boolean isChannelInfoValid(WaveViewPanel p) {
		if (p.getChannel() == null || !p.getChannel().isPopulated()) {
			return false;
		} else {
			return true;
		}
	}
	
	public FileSpec getFileSpec(String filename) {
		FileSpec selectedFileSpec = waveFileSpec.getFileSpec(filename);
		return selectedFileSpec;
	}

	public ArrayList<FileSpec> getRelatedFileSpecs(int channelCount) {
		ArrayList<FileSpec> relatedFileSpecs = waveFileSpec
				.getFileSpecs(channelCount);
		return relatedFileSpecs;
	}

	private synchronized void deselect(final WaveViewPanel p) {
		selectedSet.remove(p);
		waveToolbar.removeSettings(p.getSettings());
		p.setBackgroundColor(BACKGROUND_COLOR);
		p.createImage();
		doButtonEnables();
	}

	private synchronized void deselectAll() {
		WaveViewPanel[] panels = selectedSet.toArray(new WaveViewPanel[0]);
		for (WaveViewPanel p : panels)
			deselect(p);
	}

	private synchronized void select(final WaveViewPanel p) {
		if (p == null || selectedSet.contains(p))
			return;

		selectedSet.add(p);
		doButtonEnables();
		p.setBackgroundColor(SELECT_COLOR);
		Swarm.getApplication().getDataChooser().setNearest(p.getChannel().toString);
		p.createImage();
		waveToolbar.addSettings(p.getSettings());
	}

	private synchronized void remove(WaveViewPanel p) {
		int i = 0;
		for (i = 0; i < waveBox.getComponentCount(); i++) {
			if (p == waveBox.getComponent(i))
				break;
		}

		p.removeListener(selectListener);
		p.getDataSource().close();
		setStatusText(" ");
		waveBox.remove(i);
		waves.remove(p);
		histories.remove(p);
		doButtonEnables();
		waveBox.validate();
		selectedSet.remove(p);
		lastClickedIndex = Math.min(lastClickedIndex,
				waveBox.getComponentCount() - 1);
		waveToolbar.removeSettings(p.getSettings());
		repaint();
	}

	protected int getWaveIndex(WaveViewPanel p) {
		int i = 0;
		for (i = 0; i < waveBox.getComponentCount(); i++) {
			if (p == waveBox.getComponent(i))
				break;
		}
		return i;
	}

	public synchronized void remove() {
		WaveViewPanel[] panels = selectedSet.toArray(new WaveViewPanel[0]);
		for (WaveViewPanel p : panels)
			remove(p);
	}

	public synchronized void moveDown() {
		WaveViewPanel p = getSingleSelected();
		if (p == null)
			return;

		int i = waves.indexOf(p);
		if (i == waves.size() - 1)
			return;

		waves.remove(i);
		waves.add(i + 1, p);
		waveBox.remove(p);
		waveBox.add(p, i + 1);
		waveBox.validate();
		repaint();
	}

	public synchronized void moveUp() {
		WaveViewPanel p = getSingleSelected();
		if (p == null)
			return;

		int i = waves.indexOf(p);
		if (i == 0)
			return;

		waves.remove(i);
		waves.add(i - 1, p);
		waveBox.remove(p);
		waveBox.add(p, i - 1);
		waveBox.validate();
		repaint();
	}

	public void resizeWaves() {
		SwingWorker worker = new SwingWorker() {
			public Object construct() {
				int w = scrollPane.getViewport().getSize().width;
				for (WaveViewPanel wave : waves) {
					wave.setSize(w, calculateWaveHeight());
					wave.createImage();
				}
				return null;
			}

			public void finished() {
				waveBox.validate();
				validate();
				repaint();
			}
		};
		worker.start();
	}

	public void removeWaves() {
		while (waves.size() > 0)
			remove(waves.get(0));

		waveBox.validate();
		scrollPane.validate();
		doButtonEnables();
		repaint();
	}

	private void addHistory(WaveViewPanel wvp, double[] t) {
		Stack<double[]> history = histories.get(wvp);
		if (history == null) {
			history = new Stack<double[]>();
			histories.put(wvp, history);
		}
		history.push(t);
	}

	public void gotoTime(WaveViewPanel wvp, String t) {
		double j2k = Double.NaN;
		try {
			if (t.length() == 12)
				t = t + "30";

			j2k = Time.parse("yyyyMMddHHmmss", t);
		} catch (Exception e) {
			JOptionPane.showMessageDialog(Swarm.getApplication(),
					"Illegal time value.", "Error", JOptionPane.ERROR_MESSAGE);
		}

		if (!Double.isNaN(j2k)) {
			double dt = 60;
			if (wvp.getWave() != null) {
				double st = wvp.getStartTime();
				double et = wvp.getEndTime();
				double[] ts = new double[] { st, et };
				addHistory(wvp, ts);
				dt = (et - st);
			}

			double tzo = Time.getTimeZoneOffset(Swarm.config.getTimeZone(wvp
					.getChannel().toString));
			double nst = j2k - tzo - dt / 2;
			double net = nst + dt;

			fetchNewWave(wvp, nst, net);
		}
	}

	public void gotoTime(String t) {
		for (WaveViewPanel p : selectedSet)
			gotoTime(p, t);
	}

	public void scaleTime(WaveViewPanel wvp, double pct) {
		double st = wvp.getStartTime();
		double et = wvp.getEndTime();
		double[] t = new double[] { st, et };
		addHistory(wvp, t);
		double dt = (et - st) * (1 - pct);
		double mt = (et - st) / 2 + st;
		double nst = mt - dt / 2;
		double net = mt + dt / 2;
		fetchNewWave(wvp, nst, net);
	}

	public void scaleTime(double pct) {
		for (WaveViewPanel p : selectedSet)
			scaleTime(p, pct);
	}

	public void back(WaveViewPanel wvp) {
		Stack<double[]> history = histories.get(wvp);
		if (history == null || history.empty())
			return;

		final double[] t = history.pop();
		fetchNewWave(wvp, t[0], t[1]);
	}

	public void back() {
		for (WaveViewPanel p : selectedSet)
			back(p);
	}

	private void shiftTime(WaveViewPanel wvp, double pct) {
		double st = wvp.getStartTime();
		double et = wvp.getEndTime();
		double[] t = new double[] { st, et };
		addHistory(wvp, t);
		double dt = (et - st) * pct;
		double nst = st + dt;
		double net = et + dt;
		fetchNewWave(wvp, nst, net);
	}

	public void shiftTime(double pct) {
		for (WaveViewPanel p : selectedSet)
			shiftTime(p, pct);
	}

	public void repositionWaves(double st, double et) {
		for (WaveViewPanel wave : waves) {
			fetchNewWave(wave, st, et);
		}
	}

	public Throbber getThrobber() {
		return throbber;
	}

	// TODO: This isn't right, this should be a method of waveviewpanel
	private void fetchNewWave(final WaveViewPanel wvp, final double nst, final double net) {
		final SwingWorker worker = new SwingWorker() {
			public Object construct() {
				throbber.increment();
				SeismicDataSource sds = wvp.getDataSource();
				// Hacky fix for bug #84
				Wave sw = null;
				if (sds instanceof CachedDataSource)
					sw = ((CachedDataSource) sds).getBestWave(wvp.getChannel().toString, nst, net);
				else
					sw = sds.getWave(wvp.getChannel().toString, nst, net);
				wvp.setWave(sw, nst, net);
				wvp.repaint();
				return null;
			}

			public void finished() {
				throbber.decrement();
				repaint();
			}
		};
		worker.start();
	}

	public void setMaximum(boolean max) throws PropertyVetoException {
		if (max) {
			Swarm.config.clipboardX = getX();
			Swarm.config.clipboardY = getY();
		}
		super.setMaximum(max);
	}

	public void paint(Graphics g) {
		super.paint(g);
		if (waves.size() == 0) {
			Dimension dim = this.getSize();
			g.setColor(Color.black);
			g.drawString("Clipboard empty.", dim.width / 2 - 40, dim.height / 2);
		}
	}
	
	public WaveFileSpec getWaveFileSpec() {
		return waveFileSpec;
	}
}