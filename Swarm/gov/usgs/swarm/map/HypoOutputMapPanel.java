package gov.usgs.swarm.map;

import gov.usgs.plot.Plot;
import gov.usgs.plot.TextRenderer;
import gov.usgs.plot.map.GeoImageSet;
import gov.usgs.plot.map.GeoLabelSet;
import gov.usgs.plot.map.MapRenderer;
import gov.usgs.plot.map.WMSGeoImageSet;
import gov.usgs.proj.GeoRange;
import gov.usgs.proj.Mercator;
import gov.usgs.proj.Projection;
import gov.usgs.proj.TransverseMercator;
import gov.usgs.swarm.Icons;
import gov.usgs.swarm.Metadata;
import gov.usgs.swarm.Swarm;
import gov.usgs.swarm.SwingWorker;
import gov.usgs.swarm.TimeListener;
import gov.usgs.swarm.data.SeismicDataSource;
import gov.usgs.swarm.database.model.HypoResults;
import gov.usgs.swarm.map.MapMiniPanel.Position;
import gov.usgs.swarm.wave.WaveClipboardFrame;
import gov.usgs.swarm.wave.WaveViewPanel;
import gov.usgs.util.CodeTimer;
import gov.usgs.util.ConfigFile;
import gov.usgs.util.Pair;
import gov.usgs.vdx.calc.data.Hypocenter;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
/**
 * A map panel that displays the output of an hypo calculation
 * 
 * @author Chirag Patel
 */
public class HypoOutputMapPanel extends JPanel{
	public enum DragMode
	{
		DRAG_MAP, BOX, RULER;
	}

	public enum LabelSetting
	{
		NONE("N", Icons.label_none), SOME("S", Icons.label_some), ALL("A", Icons.label_all);

		public String code;
		public ImageIcon image;

		LabelSetting(String s, ImageIcon i)
		{
			code = s;
			image = i;
		}

		public LabelSetting next()
		{
			switch (this) {
			case SOME:
				return LabelSetting.ALL;
			case ALL:
				return LabelSetting.NONE;
			case NONE:
			default:
				return LabelSetting.SOME;
			}
		}

		public Icon getIcon()
		{
			return image;
		}

		public static LabelSetting fromString(String s)
		{
			if (s == null)
				return SOME;

			if (s.equals("N"))
				return NONE;
			else if (s.equals("A"))
				return ALL;
			else if (s.equals("S"))
				return SOME;
			else
				return SOME;
		}

	}

	private static final long serialVersionUID = 1L;

	private static final int INSET = 30;

	private List<Line2D.Double> lines;

	private GeoImageSet images;
	private GeoLabelSet labels;
	private GeoRange range;
	private Projection projection;
	private RenderedImage image;

	private Point2D.Double center;
	private double scale = 100000;

	private JLayeredPane pane;
	private MapImagePanel mapImagePanel;
	private BufferedImage mapImage;

	private MapRenderer renderer;

	private Map<Double, MapMiniPanel> miniPanels;
	private Map<Double, ConfigFile> layouts;
	private List<MapMiniPanel> visiblePanels;

	private DragMode dragMode = DragMode.DRAG_MAP;
	private Point mouseDown;
	private Point mouseNow;
	private Rectangle dragRectangle;

	
	
    private JLabel rmsLabel;
	
	private JLabel erhoutLabel;
	
	private JLabel se3outLabel;

	private Stack<double[]> mapHistory;
	private Stack<double[]> timeHistory;

	private int missing;

	private Set<MapMiniPanel> selectedPanels;
	private boolean allowMultiSelection = false;

	private double startTime;
	private double endTime;

	private int dragDX = Integer.MAX_VALUE;
	private int dragDY = Integer.MAX_VALUE;
	
	private Integer moveX;
	private Integer moveY;
	
	private Integer pressX;
	private Integer pressY;
	

	private LabelSetting labelSetting = LabelSetting.SOME;

	private List<? extends ClickableGeoLabel> clickableLabels;

	List<Hypocenter> hypoCenters;
	

	
	
	
	public JLabel getRmsLabel() {
		return rmsLabel;
	}

	public void setRmsLabel(JLabel rmsLabel) {
		this.rmsLabel = rmsLabel;
	}

	public JLabel getErhoutLabel() {
		return erhoutLabel;
	}

	public void setErhoutLabel(JLabel erhoutLabel) {
		this.erhoutLabel = erhoutLabel;
	}

	public JLabel getSe3outLabel() {
		return se3outLabel;
	}

	public void setSe3outLabel(JLabel se3outLabel) {
		this.se3outLabel = se3outLabel;
	}

	public HypoOutputMapPanel()
	{
		mapHistory = new Stack<double[]>();
		timeHistory = new Stack<double[]>();
		lines = new ArrayList<Line2D.Double>();
		miniPanels = Collections.synchronizedMap(new HashMap<Double, MapMiniPanel>());
		layouts = Collections.synchronizedMap(new HashMap<Double, ConfigFile>());
		visiblePanels = Collections.synchronizedList(new ArrayList<MapMiniPanel>());
		selectedPanels = new HashSet<MapMiniPanel>();

		Cursor crosshair = new Cursor(Cursor.CROSSHAIR_CURSOR);
		this.setCursor(crosshair);

		createUI();
	}

	public void saveLayout(ConfigFile cf, String prefix)
	{
		cf.put(prefix + ".longitude", Double.toString(center.x));
		cf.put(prefix + ".latitude", Double.toString(center.y));
		cf.put(prefix + ".scale", Double.toString(scale));
		cf.put(prefix + ".labelSetting", labelSetting.code);
		synchronized (visiblePanels) {
			int waves = 0;
			for (MapMiniPanel panel : visiblePanels) {
				if (panel.isWaveVisible()) {
					cf.put(prefix + ".wave-" + waves + ".hash",
							Double.toString(panel.getActiveMetadata().getLocationHashCode()));
					panel.saveLayout(cf, prefix + ".wave-" + waves++);
				}
			}
			cf.put(prefix + ".waves", Integer.toString(waves));
		}
	}

	public void processLayout(ConfigFile cf)
	{
		int waves = Integer.parseInt(cf.getString("waves"));
		for (int i = 0; i < waves; i++) {
			String w = "wave-" + i;
			double hash = Double.parseDouble(cf.getString(w + ".hash"));
			ConfigFile scf = cf.getSubConfig(w);
			layouts.put(hash, scf);
		}

		labelSetting = LabelSetting.fromString(cf.getString("labelSetting"));
		double lon = Double.parseDouble(cf.getString("longitude"));
		double lat = Double.parseDouble(cf.getString("latitude"));
		Point2D.Double c = new Point2D.Double(lon, lat);
		double sc = Double.parseDouble(cf.getString("scale"));
		setCenterAndScale(c, sc);
	}

	public void loadMaps(boolean redraw)
	{
		Pair<GeoImageSet, GeoLabelSet> pair;
		if (Swarm.config.useWMS) {
			// TODO: what about GeoLabelSet?
			WMSGeoImageSet wms = new WMSGeoImageSet();
			wms.setServer(Swarm.config.wmsServer);
			wms.setLayer(Swarm.config.wmsLayer);
			wms.setStyle(Swarm.config.wmsStyles);
			pair = new Pair<GeoImageSet, GeoLabelSet>(wms, new GeoLabelSet());
		} else {
			pair = GeoImageSet.loadMapPacks(Swarm.config.mapPath);
		}
		if (pair != null) {
			images = pair.item1;
			labels = pair.item2;
		}

		if (images == null) {
			Swarm.logger.warning("No map images found in " + Swarm.config.mapPath + ".");
			images = new GeoImageSet();
		}
		images.setArealCacheSort(false);
		int mp = (int) Math.round(Runtime.getRuntime().maxMemory() / 1024.0 / 1024.0 / 8.0);
		images.setMaxLoadedImagesSize(mp);

		if (redraw)
			resetImage(true);
	}

	private void createUI()
	{
		loadMaps(false);

		center = new Point2D.Double(Swarm.config.mapLongitude, Swarm.config.mapLatitude);
		scale = Swarm.config.mapScale;

		setLayout(new BorderLayout());
		pane = new JLayeredPane();
		mapImagePanel = new MapImagePanel();
		addMouseWheelListener(new MouseWheelListener()
		{
			@Override
			public void mouseWheelMoved(MouseWheelEvent e)
			{
				if (e.isControlDown()) {
					int cnt = -e.getWheelRotation();
					for (MapMiniPanel panel : miniPanels.values())
						panel.changeSize(cnt);
				}
			}
		});

		addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseExited(MouseEvent e)
			{
			}

			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (clickableLabels != null) {
					for (ClickableGeoLabel label : clickableLabels) {
						Rectangle r = label.getClickBox();
						Point2D.Double xy = getXY(label.location.x, label.location.y);
						if (xy != null) {
							r.translate((int) xy.x, (int) xy.y);
							if (r.contains(e.getPoint()))
								label.mouseClicked(e);
						}
					}
				}
			}

			@Override
			public void mousePressed(MouseEvent e)
			{
				pressX = e.getX();
				pressY = e.getY();
				
				requestFocusInWindow();
				if (SwingUtilities.isRightMouseButton(e)) {
					mapPush();
					center = getLonLat(e.getX(), e.getY());
					resetImage();
				} else {
					if (dragMode == DragMode.DRAG_MAP)
						setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
					mouseDown = e.getPoint();
					dragRectangle = new Rectangle();
					dragRectangle.setFrameFromDiagonal(mouseDown, mouseDown);
				}
				repaint();
			}

			@Override
			public void mouseReleased(MouseEvent e)
			{
				setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
				if (dragMode == DragMode.DRAG_MAP && mouseDown != null && mouseNow != null) {
					mapPush();
					dragDX = mouseDown.x - mouseNow.x;
					dragDY = mouseDown.y - mouseNow.y;
					center = getLonLat(getWidth() / 2 + dragDX, getHeight() / 2 + dragDY);
					resetImage();
				}
				if (dragMode == DragMode.BOX && dragRectangle != null) {
					Point mouseUp = e.getPoint();
					int x1 = Math.min(mouseUp.x, mouseDown.x);
					int x2 = Math.max(mouseUp.x, mouseDown.x);
					int y1 = Math.min(mouseUp.y, mouseDown.y);
					int y2 = Math.max(mouseUp.y, mouseDown.y);
					int dx = x2 - x1;
					int dy = y2 - y1;
					if (dx > 3 && dy > 3) {
						mapPush();
						double xs = (double) dx / (double) (getWidth() - INSET * 2);
						double ys = (double) dy / (double) (getHeight() - INSET * 2);
						scale = scale * Math.max(xs, ys);
						center = getLonLat((int) Math.round(dragRectangle.getCenterX()),
								(int) Math.round(dragRectangle.getCenterY()));
						resetImage();
					}
					dragRectangle = null;

				}
				mouseDown = null;
				mouseNow = null;
				repaint();
				Swarm.getSelectedAttempt().persist();
			}
		});

		addMouseMotionListener(new MouseMotionListener()
		{
			@Override
			public void mouseMoved(MouseEvent e)
			{
				moveX = e.getX();
				moveY = e.getY();
				repaint();
			}

			@Override
			public void mouseDragged(MouseEvent e)
			{
				mouseNow = e.getPoint();
				if (dragMode == DragMode.DRAG_MAP) {
					if (SwingUtilities.isLeftMouseButton(e) && pane.getComponentCount() != 1) {
						lines.clear();
						pane.removeAll();
						pane.add(mapImagePanel, new Integer(10));
						repaint();
					}
				} 
				repaint();
			}
		});

		addComponentListener(new ComponentAdapter()
		{
			@Override
			public void componentResized(ComponentEvent e)
			{
				mapImagePanel.setSize(pane.getSize());
				resetImage();
				repaint();
			}
		});

		Swarm.getApplication().addTimeListener(new TimeListener()
		{
			@Override
			public void timeChanged(double j2k)
			{
				for (MapMiniPanel panel : miniPanels.values()) {
					if (panel != null && panel.getWaveViewPanel() != null)
						panel.getWaveViewPanel().setCursorMark(j2k);
				}
			}
		});

		addKeyListener(new KeyListener()
		{
			@Override
			public void keyPressed(KeyEvent e)
			{
				if (allowMultiSelection && e.isControlDown() && e.getKeyCode() == KeyEvent.VK_A) {
					deselectAllPanels();
					synchronized (visiblePanels) {
						for (MapMiniPanel panel : visiblePanels) {
							if (panel.isWaveVisible())
								addSelectedPanel(panel);
						}
					}
				}
				if (e.isShiftDown() && e.getKeyCode() == KeyEvent.VK_R) {
					resetAllAutoScaleMemory();
				}
			}

			@Override
			public void keyReleased(KeyEvent e)
			{}

			@Override
			public void keyTyped(KeyEvent e)
			{}
		});

		pane.add(mapImagePanel, new Integer(10));
		add(pane, BorderLayout.CENTER);
		loadLabels();
	}

	public void loadLabels()
	{
		try {
			Class<?> cl = Class.forName(Swarm.config.labelSource);
			LabelSource src = (LabelSource) cl.newInstance();
			clickableLabels = src.getLabels();
			repaint();
		} catch (Exception e) {
			Swarm.logger.log(Level.WARNING, e.getMessage());
		}
	}

//	public Throbber getThrobber()
//	{
////		return parent.getThrobber();
//	}

	public void setStatusText(String t)
	{
//		parent.setStatusText(t);
	}

	public void wavesToClipboard()
	{
		synchronized (visiblePanels) {
			WaveClipboardFrame cb = Swarm.getApplication().getWaveClipboard();
			int cnt = 0;
			for (MapMiniPanel panel : visiblePanels) {
				if (panel.isWaveVisible()) {
					cnt++;
					WaveViewPanel p = new WaveViewPanel(panel.getWaveViewPanel());
					SeismicDataSource src = panel.getWaveViewPanel().getDataSource();
					if (src != null)
						p.setDataSource(src.getCopy());

					cb.addWave(p);
				}
			}
			if (cnt > 0) {
				cb.setVisible(true);
				requestFocus();
			}
		}
	}

	public synchronized void deselectAllPanels()
	{
		for (MapMiniPanel panel : selectedPanels)
			panel.setSelected(false);
		selectedPanels.clear();
	}

	public synchronized void deselectPanel(MapMiniPanel p)
	{
		if (selectedPanels.contains(p)) {
			p.setSelected(false);
			selectedPanels.remove(p);
		}
	}

	public synchronized void addSelectedPanel(MapMiniPanel p)
	{
		if (allowMultiSelection) {
			p.setSelected(true);
			selectedPanels.add(p);
		} else
			setSelectedPanel(p);
	}

	public synchronized void setSelectedPanel(MapMiniPanel p)
	{
		deselectAllPanels();
		p.setSelected(true);
//		parent.setSelectedWave(p.getWaveViewPanel());
		selectedPanels.add(p);
	}

	public void setDragMode(DragMode mode)
	{
		dragMode = mode;
	}

	public LabelSetting getLabelSetting()
	{
		return labelSetting;
	}

	public void setLabelSetting(LabelSetting ls)
	{
		labelSetting = ls;
		resetImage(false);
	}

	public void mapPush()
	{
		if (center != null)
			mapHistory.push(new double[] { center.x, center.y, scale });
	}

	public boolean mapPop()
	{
		if (!mapHistory.isEmpty()) {
			double[] last = mapHistory.pop();
			center = new Point2D.Double(last[0], last[1]);
			scale = last[2];
			resetImage();
			return true;
		} else
			return false;
	}

	public void timePush()
	{
		timeHistory.push(new double[] { startTime, endTime });
	}

	public boolean timePop()
	{
		if (!timeHistory.isEmpty()) {
			double[] t = timeHistory.pop();
			setTimes(t[0], t[1]);
			return true;
		} else
			return false;
	}

	public void zoom(double f)
	{
		mapPush();
		scale *= f;
		resetImage();
	}

	public double getStartTime()
	{
		return startTime;
	}

	public double getEndTime()
	{
		return endTime;
	}

	public void scaleTime(double pct)
	{
		timePush();
		double dt = (endTime - startTime) * (1 - pct);
		double mt = (endTime - startTime) / 2 + startTime;
		startTime = mt - dt / 2;
		endTime = mt + dt / 2;
		setTimes(startTime, endTime, true);
	}

	public void shiftTime(double pct)
	{
		timePush();
		double dt = (endTime - startTime) * pct;
		startTime += dt;
		endTime += dt;
		setTimes(startTime, endTime, true);
	}

	public void gotoTime(double j2k)
	{
		timePush();
		double dt = (endTime - startTime);
		startTime = j2k - dt / 2;
		endTime = j2k + dt / 2;
		setTimes(startTime, endTime, true);
	}

	public Point2D.Double getXY(double lon, double lat)
	{
		if (range == null || projection == null || image == null || renderer == null)
			return null;
		Point2D.Double xy = projection.forward(new Point2D.Double(lon, lat));
		double[] ext = range.getProjectedExtents(projection);
		double dx = (ext[1] - ext[0]);
		double dy = (ext[3] - ext[2]);
		Point2D.Double res = new Point2D.Double();
		res.x = (((xy.x - ext[0]) / dx) * renderer.getGraphWidth() + INSET);
		res.y = ((1 - (xy.y - ext[2]) / dy) * renderer.getGraphHeight() + INSET);
		return res;
	}

	public Point2D.Double getLonLat(int x, int y)
	{
		if (range == null || projection == null || renderer == null)
			return null;

		int tx = x - INSET;
		int ty = y - INSET;
		double[] ext = range.getProjectedExtents(projection);
		double dx = (ext[1] - ext[0]) / renderer.getGraphWidth();
		double dy = (ext[3] - ext[2]) / renderer.getGraphHeight();
		double px = tx * dx + ext[0];
		double py = ext[3] - ty * dy;

		Point2D.Double pt = projection.inverse(new Point2D.Double(px, py));
		pt.x = pt.x % 360;
		if (pt.x > 180)
			pt.x -= 360;
		if (pt.x < -180)
			pt.x += 360;
		return pt;
	}

	public void setTimes(double st, double et)
	{
		setTimes(st, et, false);
	}

	public void setTimes(double st, double et, boolean repaint)
	{
		startTime = st;
		endTime = et;
		boolean updated = false;
		synchronized (visiblePanels) {
			for (MapMiniPanel panel : visiblePanels) {
				if (panel.isWaveVisible()) {
					updated = true;
					panel.updateWave(startTime, endTime, false, repaint);
				}
			}
			if (updated)
				repaint();
		}
	}

	public Point2D.Double getCenter()
	{
		return center;
	}

	public double getScale()
	{
		return scale;
	}

	public void setCenterAndScale(Point2D.Double c, double s)
	{
		center = c;
		scale = s;
		resetImage();
	}

	public void setCenterAndScale(GeoRange gr)
	{
		mapPush();
		int width = mapImagePanel.getWidth() - (INSET * 2);
		int height = mapImagePanel.getHeight() - (INSET * 2);
		center = gr.getCenter();
		TransverseMercator tm = new TransverseMercator();
		tm.setOrigin(center);
		scale = gr.getScale(tm, width, height) * 1.1;
		if (scale > 6000) {
			Mercator merc = new Mercator();
			merc.setOrigin(center);
			scale = gr.getScale(merc, width, height) * 1.1;
		}
		resetImage();
	}

	public void pickMapParameters(int width, int height)
	{
		double xm = scale * width;
		double ym = scale * height;

		if (xm > 3000000) {
			// use Mercator
			projection = new Mercator();
			projection.setOrigin(center);
			if (xm > Mercator.getMaxWidth()) {
				xm = Mercator.getMaxWidth() * 0.999999;
				scale = xm / width;
				ym = scale * height;
			}
			range = projection.getGeoRange(center, xm, ym);
		} else {
			// use Transverse Mercator
			TransverseMercator tm = new TransverseMercator();
			tm.setOrigin(center);
			projection = tm;
			range = projection.getGeoRange(center, xm, ym);
		}
	}

	private Point getLabelPosition(GeneralPath boxes, int x, int y, int w, int h)
	{
		int[] dxy = new int[] { x + 5, y - 5, x + 5, y, x + 5, y - 10, x - w - 5, y - 5, x - w - 5, y, x - w - 5,
				y - 10, x + 5, y - 15, x + 5, y + 5, x, y - 15, x, y + 5, x, y - 20, x, y + 10, x - w - 5, y - 15,
				x - w - 5, y + 5, x, y - 20, x + 40, y + 10, x - w - 40, y - 15, };

		for (int i = 0; i < dxy.length / 2; i++) {
			int px = dxy[i * 2];
			int py = dxy[i * 2 + 1];
			if (px < 0 || py < 0)
				continue;
			Rectangle rect = new Rectangle(px, py, w, h);
			if (!boxes.intersects(rect))
				return new Point(px, py);
		}

		return null;
	}

	public int getMissing()
	{
		return missing;
	}

	public boolean imageValid()
	{
		return mapImage != null;
	}

	public void resetAllAutoScaleMemory()
	{
		for (MapMiniPanel panel : visiblePanels)
			panel.getWaveViewPanel().resetAutoScaleMemory();
	}

	public void resetImage()
	{
		resetImage(true);
	}

	private void checkLayouts()
	{
		if (layouts.size() == 0)
			return;

		Set<Double> ks = layouts.keySet();
		Set<Double> toRemove = new HashSet<Double>();
		for (double hash : ks) {
			MapMiniPanel mmp = miniPanels.get(hash);
			if (mmp != null) {
				mmp.processLayout(layouts.get(hash));
				toRemove.add(hash);
			}
		}
		for (double hash : toRemove)
			layouts.remove(hash);
	}

	// thoughts:
	// here's what should happen when someone wants to redraw the map
	// -- if the underlying map is being redrawn then that happens first
	// -- all non-event thread processing should be done in one method
	// -- all component adjustments should be done on the event thread
	// after the above is done

	private BufferedImage updateMapRenderer()
	{
		BufferedImage mi = null;
		CodeTimer ct = new CodeTimer("whole map");
		try {
			Swarm.config.mapScale = scale;
			Swarm.config.mapLongitude = center.x;
			Swarm.config.mapLatitude = center.y;

//			parent.getThrobber().increment();

			int width = mapImagePanel.getWidth() - (INSET * 2);
			int height = mapImagePanel.getHeight() - (INSET * 2);

			pickMapParameters(width, height);

			Swarm.logger.finest("map scale: " + scale);
			Swarm.logger.finest("center: " + center.x + " " + center.y);
			MapRenderer mr = new MapRenderer(range, projection);
			ct.mark("pre bg");
			image = images.getMapBackground(projection, range, width, scale);
			ct.mark("bg");
			mr.setLocation(INSET, INSET, width);
			mr.setMapImage(image);
			mr.setGeoLabelSet(labels);
			mr.createGraticule(6, true);
			mr.createBox(6); // The black outline of the map

			File linedir = new File("mapdata/Lines"); // DCK : deal with missing
														// Lines directory

			if (linedir != null) {
				File[] files = linedir.listFiles();
				if (files != null)
					for (File f : files)
						if (f.isFile())
							mr.createLine(f.toString());
			}
			mr.createScaleRenderer(1 / projection.getScale(center), INSET, 14);
			TextRenderer tr = new TextRenderer(mapImagePanel.getWidth() - INSET, 14, projection.getName()
					+ " Projection");
			tr.antiAlias = false;
			tr.font = new Font("Arial", Font.PLAIN, 10);
			tr.horizJustification = TextRenderer.RIGHT;
			mr.addRenderer(tr);
			renderer = mr;

			Plot plot = new Plot();
			plot.setSize(mapImagePanel.getWidth(), mapImagePanel.getHeight());
			plot.addRenderer(renderer);
			ct.mark("pre plot");
			mi = plot.getAsBufferedImage(false);
			ct.mark("plot");
			dragDX = Integer.MAX_VALUE;
			dragDY = Integer.MAX_VALUE;
			ct.stop();
		} catch (Exception e) {
			Swarm.logger.log(Level.SEVERE, "Exception during map creation.", e);
		} finally {
//			parent.getThrobber().decrement();
		}
		return mi;
	}

	private Pair<List<JComponent>, List<Line2D.Double>> updateMiniPanels()
	{
		List<JComponent> compsToAdd = new ArrayList<JComponent>();
		List<Line2D.Double> linesToAdd = new ArrayList<Line2D.Double>();

		FontRenderContext frc = new FontRenderContext(new AffineTransform(), false, false);

		GeneralPath boxes = new GeneralPath();
		missing = 0;

		Map<String, Metadata> allMetadata = Swarm.config.getMetadata();
		synchronized (allMetadata) {
			for (MapMiniPanel panel : miniPanels.values()) {
				if (panel.getPosition() == MapMiniPanel.Position.MANUAL_SET)
					panel.setPosition(Position.MANUAL_UNSET);
				else
					panel.setPosition(Position.UNSET);
			}
			for (Metadata md : allMetadata.values()) {
				if (!range.contains(new Point2D.Double(md.getLongitude(), md.getLatitude()))) {
					MapMiniPanel mmp = miniPanels.get(md.getLocationHashCode());
					if (mmp != null) {
						miniPanels.remove(md.getLocationHashCode());
						deselectPanel(mmp);
					}
				} else {
					MapMiniPanel cmp = miniPanels.get(md.getLocationHashCode());
					Point2D.Double xy = getXY(md.getLongitude(), md.getLatitude());
					if (xy == null)
						continue;
					int iconX = (int) xy.x - 8;
					int iconY = (int) xy.y - 8;
					if (cmp == null || cmp.getPosition() == Position.UNSET
							|| cmp.getPosition() == Position.MANUAL_UNSET) {
						JLabel icon = new JLabel(Icons.bullet);
						icon.setBounds(iconX, iconY, 16, 16);
						compsToAdd.add(icon);
//						if (cmp == null)
//							cmp = new MapMiniPanel(MapPanel.this);
					}

					if (cmp.getPosition() == Position.UNSET || cmp.getPosition() == Position.MANUAL_UNSET) {
						if (labelSetting == LabelSetting.NONE && !layouts.containsKey(md.getLocationHashCode())) {
							if (cmp.getPosition() == Position.UNSET)
								continue;
							if (cmp.getPosition() == Position.MANUAL_UNSET && !cmp.isWaveVisible())
								continue;
						}

						int w = (int) Math.round(MapMiniPanel.FONT.getStringBounds(md.getSCNL().station + 6, frc)
								.getWidth());
						int locX = (int) xy.x;
						int locY = (int) xy.y;
						Point pt = null;
						if (cmp.getPosition() == Position.MANUAL_UNSET) {
							Point2D.Double mp = cmp.getManualPosition();
							Point2D.Double xy2 = mp;// getXY(mp.x, mp.y);
							locX = (int) xy2.x;
							locY = (int) xy2.y;
							cmp.setPosition(Position.MANUAL_SET);
							pt = new Point(locX, locY);
						} else
							pt = getLabelPosition(boxes, locX, locY, w, 13);

						if (pt == null && labelSetting == LabelSetting.ALL)
							pt = new Point(locX, locY);

						if (pt != null) {
							locX = pt.x;
							locY = pt.y;
							boxes.append(new Rectangle(locX, locY, w, 13), false);
							cmp.setLocation(locX, locY);
							if (cmp.getPosition() == Position.UNSET)
								cmp.setPosition(Position.AUTOMATIC);

							Line2D.Double line = new Line2D.Double(locX, locY, iconX + 8, iconY + 8);
							cmp.setLine(line);
							cmp.adjustLine();
							linesToAdd.add(line);

							compsToAdd.add(cmp);
							miniPanels.put(md.getLocationHashCode(), cmp);
						} else {
							missing++;
							cmp.setPosition(Position.HIDDEN);
						}
					}
					cmp.addMetadata(md);
				}
			}
		}
		return new Pair<List<JComponent>, List<Line2D.Double>>(compsToAdd, linesToAdd);
	}

	private Semaphore lock = new Semaphore(1);

	// this function should not allow reentrancy
	public void resetImage(final boolean doMap)
	{
		// if there's any problem with the container holding the panel, just
		// forget it.
//		if (!parent.isVisible() || mapImagePanel.getHeight() == 0 || mapImagePanel.getWidth() == 0)
//			return;
		
		if (mapImagePanel.getHeight() == 0 || mapImagePanel.getWidth() == 0)
			return;

		// first, get the map renderer up and running.
		// this occurs in the construct() method below which does NOT occur
		// on the event thread.
		final SwingWorker worker = new SwingWorker()
		{
			private List<JComponent> compsToAdd;
			private List<Line2D.Double> linesToAdd;
			private BufferedImage tempMapImage;

			@Override
			public Object construct()
			{
				try {
					lock.acquire();
				} catch (InterruptedException ex) {
					return new Boolean(false);
				}

				// if other threads are waiting to update then don't bother
				// continuing
				if (lock.hasQueuedThreads()) {
					return new Boolean(false);
				}

				if (doMap) {
					tempMapImage = updateMapRenderer();
				}

				return new Boolean(true);
			}

			@Override
			public void finished()
			{
				// if we abort due to queueing, or, in the meantime, another
				// thread has queued then don't bother finishing, the next
				// thread
				// will
				if (tempMapImage != null)
					mapImage = tempMapImage;

				if (((Boolean) this.get()).booleanValue() && !lock.hasQueuedThreads()) {
					// ideally you'd call updateMiniPanels in construct()
					// however they set the position of the panels so has to be
					// done in the event thread
					// Make changes in updateMiniPanels run in non-event
					// thread
					Pair<List<JComponent>, List<Line2D.Double>> p = updateMiniPanels();
					compsToAdd = p.item1;
					linesToAdd = p.item2;

					if (lines != null)
						lines.clear();
					pane.removeAll();
					pane.add(mapImagePanel, new Integer(10));

					// /
					visiblePanels.clear();
					for (MapMiniPanel mp : miniPanels.values())
						visiblePanels.add(mp);

					pane.removeAll();
					pane.add(mapImagePanel, new Integer(10));
					if (compsToAdd != null) {
						for (JComponent comp : compsToAdd) {
							if (comp instanceof JLabel)
								pane.add(comp, new Integer(15));
							else
								pane.add(comp, new Integer(20));
						}
					}
					lines = linesToAdd;
//					parent.setStatusText(" ");
					checkLayouts();
					// /
					repaint();
				}
				lock.release();
			}
		};
		worker.start();
	}

	public MapImagePanel getMapImagePanel()
	{
		return mapImagePanel;
	}

	public class MapImagePanel extends JPanel
	{
		private static final long serialVersionUID = 1L;

		private void paintRadius(Graphics2D g2)
		{
			Point2D.Double lonLat = getLonLat(mouseNow.x, mouseNow.y);
			Point2D.Double origin = getLonLat(mouseDown.x, mouseDown.y);
			double d = Projection.distanceBetween(origin, lonLat);
			int n = 720;
			Point2D.Double[] pts = Projection.getPointsFrom(origin, d, n);
			GeneralPath gp = new GeneralPath();
			Point2D.Double xy = getXY(pts[0].x, pts[0].y);
			Point lastXY = new Point();
			lastXY.x = (int) Math.round(xy.x);
			lastXY.y = (int) Math.round(xy.y);
			gp.moveTo(lastXY.x - 2, lastXY.y - 1);
			for (int i = 1; i <= pts.length; i++) {
				xy = getXY(pts[i % n].x, pts[i % n].y);
				Point thisXY = new Point();
				thisXY.x = (int) Math.round(xy.x);
				thisXY.y = (int) Math.round(xy.y);
				double a = thisXY.x - lastXY.x;
				double b = thisXY.y - lastXY.y;
				double dist = Math.sqrt(a * a + b * b);
				if (dist > 100)
					gp.moveTo(thisXY.x - 2, thisXY.y - 1);
				else
					gp.lineTo(thisXY.x - 2, thisXY.y - 1);
				lastXY = thisXY;
			}
			g2.setColor(Color.YELLOW);
			g2.draw(gp);
		}

		private void paintGreatCircleRoute(Graphics2D g2)
		{
			Point2D.Double lonLat = getLonLat(mouseNow.x, mouseNow.y);
			Point2D.Double origin = getLonLat(mouseDown.x, mouseDown.y);
			GeneralPath gp = new GeneralPath();
			Point2D.Double xy = getXY(origin.x, origin.y);
			Point lastXY = new Point();
			lastXY.x = (int) Math.round(xy.x);
			lastXY.y = (int) Math.round(xy.y);
			gp.moveTo(lastXY.x - 2, lastXY.y - 1);
			double d = Projection.distanceBetween(origin, lonLat);
			while (d > 20 * 1000) {
				double az = Projection.azimuthTo(origin, lonLat);
				Point2D.Double p0 = Projection.getPointFrom(origin, 20 * 1000, az);

				xy = getXY(p0.x, p0.y);
				Point thisXY = new Point();
				thisXY.x = (int) Math.round(xy.x);
				thisXY.y = (int) Math.round(xy.y);
				double a = thisXY.x - lastXY.x;
				double b = thisXY.y - lastXY.y;
				double dist = Math.sqrt(a * a + b * b);
				if (dist > 100)
					gp.moveTo(thisXY.x - 2, thisXY.y - 1);
				else
					gp.lineTo(thisXY.x - 2, thisXY.y - 1);
				lastXY = thisXY;

				origin = p0;
				d = Projection.distanceBetween(origin, lonLat);
			}
			g2.setColor(Color.GREEN);
			g2.draw(gp);
		}


		
		public void paintHypoOuputLocations(Graphics2D g2, Double lon, Double lat, Color mainColor){
			BasicStroke strk = new BasicStroke(1.5f);
			g2.setStroke(strk);
			Point2D.Double xy = getXY(lon,  lat);
			int or = 2;
			int r = 10;
			g2.drawOval((int)(xy.x)-r, (int)(xy.y)-r, 2*r, 2*r);
			if(		(moveX != null && moveY != null) &&
					
					((
					(moveX >=(int)(xy.x) && moveX <=((int)(xy.x)+r))
					||
					(moveX <=(int)(xy.x) && moveX >=((int)(xy.x)-r))
					)
					&&
					(
					(moveY >=(int)(xy.y) && moveY <=((int)(xy.y)+r))
					||
					(moveY <=(int)(xy.y) && moveY >=((int)(xy.y)-r))
					))
					
				){
				g2.setColor(Color.GREEN);
			}else{
				g2.setColor(mainColor);
			}
			g2.fillOval((int)(xy.x)-or, (int)(xy.y)-or, 2*or, 2*or);
			g2.drawOval((int)(xy.x)-r, (int)(xy.y)-r, 2*r, 2*r);
		}
		
		
		
		public void checkAndSetSelectedHypoOuputLocation(Graphics2D g2, Hypocenter c){
			BasicStroke strk = new BasicStroke(1.5f);
			g2.setStroke(strk);
			Point2D.Double xy = getXY(c.getLON1(),  c.getLAT1());
			int r = 10;
			g2.drawOval((int)(xy.x)-r, (int)(xy.y)-r, 2*r, 2*r);
			if(	(pressX != null && pressY != null) &&
					
					((
					(pressX >=(int)(xy.x) && pressX <=((int)(xy.x)+r))
					||
					(pressX <=(int)(xy.x) && pressX >=((int)(xy.x)-r))
					)
					&&
					(
					(pressY >=(int)(xy.y) && pressY <=((int)(xy.y)+r))
					||
					(pressY <=(int)(xy.y) && pressY >=((int)(xy.y)-r))
					))
					
				){
				Swarm.getSelectedAttempt().setLatitude((double)c.getLAT1());
				Swarm.getSelectedAttempt().setLongitude((double)c.getLON1());
				Swarm.getSelectedAttempt().setDepth(c.getZ());
			}
			
		}
		
		@Override
		public void paintComponent(Graphics g)
		{
			if (renderer == null || mapImage == null) {
				Dimension d = getSize();
				g.drawString("Loading map...", d.width / 2 - 50, d.height / 2);
			} else {
				Graphics2D g2 = (Graphics2D) g;
				int dx = 0;
				int dy = 0;
				if (dragMode == DragMode.DRAG_MAP && mouseDown != null && mouseNow != null) {
					dx = mouseDown.x - mouseNow.x;
					dy = mouseDown.y - mouseNow.y;
					g2.drawImage(mapImage, -dx, -dy, null);
					if (Swarm.getSelectedAttempt() != null && (Swarm.getSelectedAttempt().getLatitude() !=null && (Swarm.getSelectedAttempt().getLongitude() !=null))) {
//						checkAndSetSelectedHypoOuputLocation(g2, Swarm.getSelectedAttempt().getLongitude(), Swarm.getSelectedAttempt().getLatitude());
//						paintHypoOuputLocations(g2, Swarm.getSelectedAttempt().getLongitude(), Swarm.getSelectedAttempt().getLatitude(), Color.WHITE);
//						System.out.println("main  : " + Swarm.getSelectedAttempt().getLatitude() +   "  " +Swarm.getSelectedAttempt().getLongitude());

						try {
							HypoResults results = Swarm.getSelectedAttempt().getHypoResultsAsObject();
							List<Hypocenter> hypoCenters = results.getHypocenterOuput();
							if(hypoCenters != null && hypoCenters.size() > 0){
								for(Hypocenter h : hypoCenters){
									if(h.getLON1() != Swarm.getSelectedAttempt().getLongitude().doubleValue() && 
											h.getLAT1() != Swarm.getSelectedAttempt().getLatitude().doubleValue()){
										paintHypoOuputLocations(g2, (double)h.getLON1(),(double)h.getLAT1(), Color.RED);
										
									}else{
										paintHypoOuputLocations(g2, (double)h.getLON1(),(double)h.getLAT1(), Color.WHITE);
										rmsLabel.setText("<html><b>RMS :</b> <i>" + h.getRMS() +"</i></html>");
										erhoutLabel.setText("<html><b>ERHOUT :</b> <i>" + h.getERHOUT() +"</i></html>");
										se3outLabel.setText("<html><b>SE3OUT :</b> <i>" + h.getSE3OUT() +"</i></html>");
										checkAndSetSelectedHypoOuputLocation(g2,  h);
									}
									

								}
							}
						} catch (IOException e) {
							e.printStackTrace();
						} catch (ClassNotFoundException e) {
							e.printStackTrace();
						}
						
					}
				} else if (dragDX != Integer.MAX_VALUE && dragDY != Integer.MAX_VALUE) {
					g2.drawImage(mapImage, -dragDX, -dragDY, null);
				} else {
					g2.drawImage(mapImage, 0, 0, null);
					if (Swarm.getSelectedAttempt() != null && (Swarm.getSelectedAttempt().getLatitude() !=null && (Swarm.getSelectedAttempt().getLongitude() !=null))) {
//						checkAndSetSelectedHypoOuputLocation(g2, Swarm.getSelectedAttempt().getLongitude(), Swarm.getSelectedAttempt().getLatitude());
//						paintHypoOuputLocations(g2, Swarm.getSelectedAttempt().getLongitude(), Swarm.getSelectedAttempt().getLatitude(), Color.WHITE);
//						System.out.println("main  : " + Swarm.getSelectedAttempt().getLatitude() +   "  " +Swarm.getSelectedAttempt().getLongitude());
						try {
							HypoResults results = Swarm.getSelectedAttempt().getHypoResultsAsObject();
							List<Hypocenter> hypoCenters = results.getHypocenterOuput();
							if(hypoCenters != null && hypoCenters.size() > 0){
								for(Hypocenter h : hypoCenters){
									if(h.getLON1() != Swarm.getSelectedAttempt().getLongitude().doubleValue() && 
											h.getLAT1() != Swarm.getSelectedAttempt().getLatitude().doubleValue()){
										paintHypoOuputLocations(g2, (double)h.getLON1(),(double)h.getLAT1(), Color.RED);
										
									}else{
										paintHypoOuputLocations(g2, (double)h.getLON1(),(double)h.getLAT1(), Color.WHITE);
										rmsLabel.setText("<html><b>RMS :</b> <i>" + h.getRMS() +"</i></html>");
										erhoutLabel.setText("<html><b>ERHOUT :</b> <i>" + h.getERHOUT() +"</i></html>");
										se3outLabel.setText("<html><b>SE3OUT :</b> <i>" + h.getSE3OUT() +"</i></html>");
										checkAndSetSelectedHypoOuputLocation(g2,  h);
									}
								}
							}
						} catch (IOException e) {
							e.printStackTrace();
						} catch (ClassNotFoundException e) {
							e.printStackTrace();
						}
						
						
					}
				}

				g.setXORMode(Color.WHITE);
				if (lines != null) {
					for (Line2D.Double line : lines) {
						g2.draw(line);
					}
				}
				g.setPaintMode();

				g.setColor(Color.RED);
				if (dragRectangle != null) {
					if (dragMode == DragMode.BOX && mouseNow != null) {
						dragRectangle.setFrameFromDiagonal(mouseDown, mouseNow);
						g2.draw(dragRectangle);
					} else if (dragMode == DragMode.RULER && mouseDown != null && mouseNow != null) {
						g2.drawLine(mouseDown.x - HypoOutputMapPanel.this.getInsets().left,
								mouseDown.y - HypoOutputMapPanel.this.getInsets().top, mouseNow.x
										- HypoOutputMapPanel.this.getInsets().left, mouseNow.y - HypoOutputMapPanel.this.getInsets().top);

						paintRadius(g2);
						paintGreatCircleRoute(g2);
					}
				}

				Object oldaa = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				AffineTransform at = g2.getTransform();
				g2.setFont(Font.decode("dialog-plain-12"));
				if (clickableLabels != null) {
					for (ClickableGeoLabel label : clickableLabels) {
						Point2D.Double xy = getXY(label.location.x, label.location.y);
						if (xy != null) {
							g2.translate(xy.x - dx, xy.y - dy);
							label.draw(g2);
							g2.translate(-xy.x + dx, -xy.y + dy);
						}
					}
				}
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldaa);
				g2.setTransform(at);

			}
		}

		
	}
	
}
