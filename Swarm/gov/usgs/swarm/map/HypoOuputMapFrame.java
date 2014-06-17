package gov.usgs.swarm.map;


import gov.usgs.swarm.Swarm;
import gov.usgs.swarm.Throbber;
import gov.usgs.util.ui.ExtensionFileFilter;
import gov.usgs.vdx.calc.data.HypoArchiveOutput;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.plaf.SplitPaneUI;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * A Jframe that holds the Hypooutput Map panel
 * 
 * @author Chirag Patel
 */
public class HypoOuputMapFrame extends JFrame{
	
	private JPanel mainPanel;
	

	private HypoOutputMapPanel mapPanel;

	private Throbber throbber;

	JTextPane dataArea;
	
	private Border border;
	
	private JTextField hypoOutput;
	private JButton hypoSaveButton;
	
	
	private JLabel rmsLabel;
	
	private JLabel erhoutLabel;
	
	private JLabel se3outLabel;
	
	
	HypoArchiveOutput hy;
	
	
	

	public HypoOuputMapFrame() {
		setTitle("Hypo Output");
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		createUI();
	}
	
	
	private void createUI() {
		setSize(550, 530);
		throbber = new Throbber();

		mainPanel = new JPanel(new BorderLayout());

		mapPanel = new HypoOutputMapPanel();
		
		dataArea = new JTextPane();
		dataArea.setText("");
		dataArea.setEditable(false);
		dataArea.setFont(Font.getFont(Font.MONOSPACED));
		JScrollPane scrollPane = new JScrollPane(dataArea);
		
		
		JPanel jp = new JPanel();
		
		jp.setSize(mainPanel.getWidth(), 30);
		
		hypoSaveButton = new JButton("Save All Hypo Inputs To A file");
		hypoOutput = new JTextField(12);
		jp.add(hypoOutput);
		jp.add(hypoSaveButton);

		border = BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder(0, 2, 0, 3),
				LineBorder.createGrayLineBorder());
		mapPanel.setBorder(border);
		
		
		rmsLabel = new JLabel("<html><b>RMS :</b></html> ");
		erhoutLabel = new JLabel("<html><b>ERHOUT :</b></html> ");
		se3outLabel = new JLabel("<html><b>SE3OUT :</b></html> ");
		
		mapPanel.setRmsLabel(rmsLabel);
		mapPanel.setErhoutLabel(erhoutLabel);
		mapPanel.setSe3outLabel(se3outLabel);
		
		FormLayout labellayout = new FormLayout(
				"80dlu, 3dlu,  80dlu, 3dlu, 80dlu",
				"pref");

		DefaultFormBuilder builder = new DefaultFormBuilder(labellayout);
		builder.add(rmsLabel, "1,1,1,1,FILL,FILL");
		builder.add(erhoutLabel, "3,1,1,1,FILL,FILL");
		builder.add(se3outLabel, "5,1,1,1,FILL,FILL");
		builder.getPanel().setBackground(Color.WHITE);
		
		
		JPanel mapPanelContainer = new JPanel();
		mapPanelContainer.setLayout(new BorderLayout());
		mapPanelContainer.add(mapPanel, BorderLayout.CENTER);
		mapPanelContainer.add(builder.getPanel(), BorderLayout.SOUTH);
		
		JSplitPane splitPane = createSplitPane();

		splitPane.setDividerLocation(300);
		splitPane.setTopComponent(mapPanelContainer);
		
		splitPane.setBottomComponent(scrollPane);
		mainPanel.add(splitPane, BorderLayout.CENTER);
		mainPanel.add(jp, BorderLayout.SOUTH);
		
		
	    setContentPane(mainPanel);
	    
	    
	    hypoSaveButton.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = getCustomFileChooser();
				int result = chooser.showSaveDialog(Swarm.getApplication());
				if (result == JFileChooser.APPROVE_OPTION) {
					File propertiesFile = chooser.getSelectedFile();
					String filePath = propertiesFile.getAbsolutePath();
					if (!filePath.endsWith(".xml")) {
						filePath = filePath + ".xml";
					}

						hypoOutput.setText(filePath);
						if(Swarm.getSelectedAttempt()!= null){
							Swarm.getSelectedAttempt().setHypoInputArchiveFilePath(filePath);
							Swarm.getSelectedAttempt().persist();
						}
						try {
						File file = new File(filePath);
						JAXBContext jaxbContext = JAXBContext.newInstance(HypoArchiveOutput.class);
						Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
						jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
						jaxbMarshaller.marshal(hy, file);
						} catch (PropertyException e1) {
							e1.printStackTrace();
						}catch (JAXBException e1) {
							e1.printStackTrace();
						}
						
					}

//				}
			}

		
			}
	    	
	    );
	    
		this.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentShown(ComponentEvent e) {
				if (!mapPanel.imageValid())
					mapPanel.resetImage();
			}
		});
	}


private JFileChooser getCustomFileChooser() {
	JFileChooser customFileChoser = new JFileChooser() {
		@Override
		public void approveSelection() {
			File f = getSelectedFile();
			if (f.exists() && getDialogType() == SAVE_DIALOG) {

				if (!f.getAbsolutePath().endsWith(".xml")) {
					JOptionPane.showMessageDialog(null,
							"Please select an xml file", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				} else {
					int result = JOptionPane.showConfirmDialog(this,
							"The file exists, overwrite?", "Existing file",
							JOptionPane.YES_NO_CANCEL_OPTION);
					switch (result) {
					case JOptionPane.YES_OPTION:
						super.approveSelection();
						return;
					case JOptionPane.NO_OPTION:
						return;
					case JOptionPane.CLOSED_OPTION:
						return;
					case JOptionPane.CANCEL_OPTION:
						cancelSelection();
						return;
					}
				}

			}
			super.approveSelection();
		}
	};

	customFileChoser.resetChoosableFileFilters();
	ExtensionFileFilter propsExt = new ExtensionFileFilter(".xml",
			"Xml file");
	customFileChoser.addChoosableFileFilter(propsExt);
	customFileChoser.setDialogTitle("Save All Hypo Inputs To xml File");
	customFileChoser.setFileFilter(customFileChoser
			.getAcceptAllFileFilter());
	File lastPath = new File(Swarm.config.lastPath);
	customFileChoser.setCurrentDirectory(lastPath);
	customFileChoser.setFileSelectionMode(JFileChooser.FILES_ONLY);
	return customFileChoser;
}
	
	public void setStatusText(final String t) {
		
	}
	
	
	public void setResultText(String text) {
		dataArea.setText(text);
	}
	
	public Throbber getThrobber() {
		return throbber;
	}
	
	
	private JSplitPane createSplitPane()
	{
		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		splitPane.setDividerSize(10);
		splitPane.setOneTouchExpandable(true);
		final SplitPaneUI ui = splitPane.getUI();
		if (ui instanceof BasicSplitPaneUI) {
			((BasicSplitPaneUI) ui).getDivider().setBorder(null);
			splitPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
					KeyStroke.getKeyStroke(KeyEvent.VK_U, KeyEvent.CTRL_DOWN_MASK), "jumpToTop");
			splitPane.getActionMap().put("jumpToTop", new AbstractAction()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					((JButton) ((BasicSplitPaneUI) ui).getDivider().getComponent(0)).doClick();
				}
			});
			splitPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
					KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.CTRL_DOWN_MASK), "jumpToBottom");
			splitPane.getActionMap().put("jumpToBottom", new AbstractAction()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					((JButton) ((BasicSplitPaneUI) ui).getDivider().getComponent(1)).doClick();
				}
			});
		}
		return splitPane;
	}

	public HypoArchiveOutput getHy() {
		return hy;
	}

	public void setHy(HypoArchiveOutput hy) {
		this.hy = hy;
	}
	
	public void setHypoOutput(String text){
		hypoOutput.setText(text);
	}
	
	public HypoOutputMapPanel getMapPanel() {
		return mapPanel;
	}
	
	
}