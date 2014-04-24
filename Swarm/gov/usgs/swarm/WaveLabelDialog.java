package gov.usgs.swarm;

import gov.usgs.swarm.FileSpec.Component;
import gov.usgs.swarm.data.ComboTableCellRenderer;
import gov.usgs.vdx.data.wave.SeisanChannel.SimpleChannel;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.concurrent.Callable;

import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * A dialog that is used to reset the station, network and component of a wave
 * view
 * 
 * @author Chirag Patel
 * 
 */
@SuppressWarnings("serial")
public class WaveLabelDialog extends SwarmDialog {
	//private JLabel channelLabelValue;
	private JLabel fileNameValue;

	//private JTextField stationText;
	//private JTextField networkText;
	//private JTextField componentText;

	private JCheckBox fileSpecCheckBox;
	private JComboBox fileSpecCombo;

	private JComboBox lastCompCombo;
	
	private JTable table;
	
	private DefaultTableModel model;
	
	public static int rows;
	
	private int fileIndex;
	private String fileName;
	private SimpleChannel resultChannel;
	private Callable<Object> actionAfterFinish;
	private Object[][] tableData;
	
	FileSpec selectedFileSpec;

	public boolean isOK = true;
	
	public Object[][] getTableData () {
	    DefaultTableModel dtm = (DefaultTableModel) table.getModel();
	    //int nRow = dtm.getRowCount(), nCol = dtm.getColumnCount();
	    int nRow = rows, nCol =5;
	    Object[][] tableData = new Object[nRow][nCol];
	    for (int i = 0 ; i < nRow ; i++)
	        for (int j = 0 ; j < nCol ; j++)
	            tableData[i][j] = dtm.getValueAt(i,j);
	    return tableData;
	}

	private ArrayList<FileSpec> fileSpecs = new ArrayList<FileSpec>();
	public SimpleChannel getResultChannel() {
		return resultChannel;
	}

	public WaveLabelDialog() {
		super(Swarm.getApplication(), "Label Properties", true);
		createUI();

		FormLayout layout = new FormLayout(
				"pref, 1dlu, pref:grow, 1dlu, pref:grow",
				"pref, 3dlu, pref, 3dlu,pref,3dlu,pref, 3dlu, pref, 3dlu, pref, 3dlu, pref");

		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();

		fileSpecCheckBox = new JCheckBox("Use Existing Spec:");
		builder.add(fileSpecCheckBox, "3,1,3,1,FILL,FILL");

		fileSpecCombo = new JComboBox();
		builder.add(fileSpecCombo, "3,3,3,1,FILL,FILL");

		
		fileNameValue = new JLabel("", JLabel.LEFT);
		builder.addLabel("File :", "1,5,1,1,FILL,FILL");
		builder.add(fileNameValue, "3,5,3,1,FILL,FILL");

		
		/*channelLabelValue = new JLabel("", JLabel.LEFT);
		builder.addLabel("Wave Channel index:", "1,7,1,1,FILL,FILL");
		builder.add(channelLabelValue, "3,7,3,1,FILL,FILL");

		stationText = new JTextField();
		builder.addLabel("Station :", "1,9,1,1,FILL,FILL");
		builder.add(stationText, "3,9,3,1,FILL,FILL");

		networkText = new JTextField();
		builder.addLabel("Network :", "1,11,1,1,FILL,FILL");
		builder.add(networkText, "3,11,3,1,FILL,FILL");

		componentText = new JTextField();
		builder.addLabel("Component :", "1,13,1,1,FILL,FILL");
		builder.add(componentText, "3,13,1,1,FILL,FILL");

		String[] specifications = { "Z", "E", "N" };
		lastCompCombo = new JComboBox(specifications);
		builder.add(lastCompCombo, "5,13,1,1,FILL,FILL");*/
		
		
		ComboTableCellRenderer renderer = new ComboTableCellRenderer();
		String[] specifications = { "SELECT", "Z", "E", "N" };
		lastCompCombo = new JComboBox(specifications);
		lastCompCombo.setRenderer(renderer);
	    TableCellEditor editor = new DefaultCellEditor(lastCompCombo);
		
		
		  String[] columnNames = { "Wave Channel Index", "Station", "Network", "Component", "" };
		  //model = new DefaultTableModel(data, columnNames);
		  model = new DefaultTableModel(new Object[0][], columnNames)
		  {
			public boolean isCellEditable(int row, int column)
		    {
				return (!(0==column));
		    }
		  };
		  table = new JTable(model);
		  table.setRowHeight(30);
		  table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
		  TableColumn column = table.getColumnModel().getColumn(4);
		  column.setCellRenderer(renderer);
		  column.setCellEditor(editor);

		  //lastCompCombo.setRenderer(renderer);
		  //lastCompCombo.setCellEditor(editor);
		  for(int i=0;i<rows;i++){
			  model.addRow(createRow());
		  }
		  /*model.addRow(createRow());
		  model.addRow(createRow());
		  model.addRow(createRow());*/
		  // Add table and a Button panel to the frame

		  JScrollPane scrollPane = new JScrollPane(table);
		  getContentPane().add(scrollPane);
		  builder.add(scrollPane,"3,7,3,1,FILL,FILL");

		fileSpecCheckBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (fileSpecCheckBox.isSelected()) {
					fileSpecCombo.setEnabled(true);
					table.setEnabled(false);
					//stationText.setEnabled(false);
					//networkText.setEnabled(false);
					//componentText.setEnabled(false);
					//lastCompCombo.setEnabled(false);
				} else {
					fileSpecCombo.setEnabled(false);
					table.setEnabled(true);
					//stationText.setEnabled(true);
					//networkText.setEnabled(true);
					//componentText.setEnabled(true);
					//lastCompCombo.setEnabled(true);
				}
			}

		});

		mainPanel.add(builder.getPanel(), BorderLayout.CENTER);
		setSizeAndLocation();
	}
	
	private Object[] createRow() {
		 Object[] newRow = new Object[5];
		  int row = table.getRowCount() + 1;
		  newRow[0] = Integer.toString(row);
		  newRow[4] = "SELECT";
		  return newRow;
	}

	
	/**
	 * This associate the selected WaveViewPanel(including fileName for the loaded wave and channel index for that 
	 * the channel the WaveView Panel corresponds to) with this Label Dialog
	 * 
	 * @param channel
	 * @param fileName
	 * @param fileIndex
	 */
	public void setWaveViewPanel(SimpleChannel channel, String fileName, int fileIndex) {
		//TODO: I am confused about this part. How to set these texts as now it's in tabular form
		this.fileIndex = fileIndex;
		this.fileName = fileName;
		//channelLabelValue.setText("" + fileIndex);
		fileNameValue.setText(fileName);
        //stationText.setText(channel==null?"":channel.showStationCode());
		//networkText.setText(channel==null?"":channel.showNetworkName());
		//componentText.setText(channel==null?"":channel.showFirstTwoComponent());
        String lastComponentCode = channel==null?"":channel.lastComponentCode;
		if (lastComponentCode != null && (!lastComponentCode.isEmpty())) {
			if (lastComponentCode.equalsIgnoreCase("Z") || lastComponentCode.equalsIgnoreCase("E") || lastComponentCode.equalsIgnoreCase("N")) {
				//lastCompCombo.setSelectedItem(lastComponentCode);
			}
		}
	}
	
	
	/*public String getStation(){
		return stationText.getText();
	} 
	
	public String getFirstTwoComponent(){
		return componentText.getText();
	}
	
	public String getNetwork(){
		return networkText.getText();
	}*/
	
	/*public String getLastComponentCode(){
		return lastCompCombo.getSelectedItem().toString();
	}*/
	
	
	public void setActionAfterFinish(Callable<Object> actionAfterFinish) {
		this.actionAfterFinish = actionAfterFinish;
	}

	public void wasCancelled(){
		selectedFileSpec = null;
		isOK = false;
		//this.setVisible(false);
		hide();
	}
	
	public void wasOK() {
		if (fileSpecCheckBox.isVisible()) {
			if (fileSpecCheckBox.isSelected()) {
				FileSpec fs = (FileSpec) fileSpecCombo.getSelectedItem();
				selectedFileSpec = fs;
				Component comp = fs.getComponent(fileIndex);
                resultChannel = new SimpleChannel(null, comp.getNetworkCode(), comp.getStationCode(), comp.getComponentCode(), comp.getLastComponentCode());

			} else {
				selectedFileSpec = null;
			}
		} else {
			selectedFileSpec = null;
		}
		try {
			actionAfterFinish.call();
			getTableData();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		isOK = true;
		hide();
		
	}
	
	
	

	public FileSpec getSelectedFileSpec() {
		return selectedFileSpec;
	}

	

	public void setSelectedFileSpec(FileSpec selectedFileSpec) {
		this.selectedFileSpec = selectedFileSpec;
	}

	

	@Override
	public void show() {
		//stationText.setEnabled(true);
		//networkText.setEnabled(true);
		//componentText.setEnabled(true);
		//lastCompCombo.setEnabled(true);
		fileSpecCheckBox.setSelected(false);
		fileSpecCombo.setEnabled(false);

		if (fileSpecs == null || fileSpecs.size() == 0) {
			fileSpecCombo.setVisible(true);
			fileSpecCheckBox.setVisible(true);
			fileSpecCombo.setEnabled(false);
			fileSpecCheckBox.setEnabled(false);
		} else {
			DefaultComboBoxModel model = (DefaultComboBoxModel) fileSpecCombo
					.getModel();
			model.removeAllElements();
			for (FileSpec f : fileSpecs) {
				model.addElement(f);
			}
			fileSpecCombo.setVisible(true);
			fileSpecCheckBox.setVisible(true);
		}
		super.show();

	}

	public ArrayList<FileSpec> getFileSpecs() {
		return fileSpecs;
	}

	public void setFileSpecs(ArrayList<FileSpec> fileSpecs) {
		this.fileSpecs = fileSpecs;
	}

}