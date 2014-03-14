package gov.usgs.util.ui;

import java.awt.Component;
import java.awt.Container;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * @author richersoon
 * Customization in UI should be put here 
 */
public class ComponentCustomization {
	
	/** 
	 * @param Container - can be any swing component like JFileChooser, JPanel, JButton, etc
	 * @param Container - 
	 * @return Has applied customization? true else false
	 */
	public boolean customizeComponentInternally(Container c, ComponentList key) {
		Component[] cmps = c.getComponents();
		
		for (Component cmp : cmps) {
			switch (key) {
			case CHOOSER_SAVES_DIRECTORY:
				if (cmp instanceof JLabel) {
					if (((JLabel) cmp).getText().trim().equalsIgnoreCase("File Name:")) {
						((JLabel) cmp).setText("Directory: ");
					}
				}
				if (cmp instanceof JTextField) {
					((JTextField) cmp).setEnabled(false);
					((JTextField) cmp).setText("");
				}
				break;
			case CHOOSER_SAVES_FILES:
				if (cmp instanceof JTextField) {
					((JTextField) cmp).setEnabled(false);
					((JTextField) cmp).setText("");
				}
				break;
			case BUTTON:
				break;
			case LABEL:
				break;
			default:
				break;
			}

			if (cmp instanceof Container) {
				if (customizeComponentInternally((Container) cmp, key))
					return true;
			}
		}
        return false;
    } 
	
	
	/**
	 * @param chooser - JFileChooser instance
	 * @param key - Operation 
	 * @param defaultDirectory - Default location when first opened
	 */
	public void designFileChooser(JFileChooser chooser, ComponentList key, File defaultDirectory){
		chooser.resetChoosableFileFilters();
		chooser.setCurrentDirectory(defaultDirectory);
		
		switch(key){
		case BUTTON:
			break;
		case CHOOSER_OPEN_DIRECTORY:
		case CHOOSER_SAVES_DIRECTORY:
			chooser.setMultiSelectionEnabled(false);
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			chooser.setDialogTitle("Save SAC File Directory");
			customizeFileExtension(chooser, FileExtensionList.SAC);
			break;
		case CHOOSER_OPEN_FILES:
			chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			chooser.setMultiSelectionEnabled(true);
			chooser.setDialogTitle("Open Wave");
			customizeFileExtension(chooser, FileExtensionList.KNOWNFILES);
			break;
		case CHOOSER_SAVES_FILES:	
			chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			chooser.setMultiSelectionEnabled(false);
			chooser.setDialogTitle("Save Wave");
			customizeFileExtension(chooser, FileExtensionList.SAC);
			break;
		
		case LABEL:
			break;
		default:
			break;
		}
		
		
	}
	
	private void customizeFileExtension(JFileChooser chooser, FileExtensionList key){
		String[] fileExtension = {"SAC","SEED","SEISAN","WIN"};
		FileNameExtensionFilter filter = null;
		
		switch(key){
		case SAC:
			filter = new FileNameExtensionFilter("SAC FILES", fileExtension[0]);
			break;
			
		case SEED:
			filter = new FileNameExtensionFilter("SEED FILES", fileExtension[1]);
			break;
			
		case SEISAN:
			filter = new FileNameExtensionFilter("SEISAN FILES", fileExtension[2]);
			break;
					
		case WIN:
			filter = new FileNameExtensionFilter("WIN FILES", fileExtension[3]);
			break;
		
		case KNOWNFILES:
			filter = new FileNameExtensionFilter("SAC/SEED/SEISAN/WIN FILES", fileExtension);
			break;
			
		default:
			break;
		}
		
		chooser.setFileFilter(filter);
		chooser.setAcceptAllFileFilterUsed(false);
	}
	
	
	public enum ComponentList {
		CHOOSER_SAVES_DIRECTORY, CHOOSER_SAVES_FILES, 
		CHOOSER_OPEN_DIRECTORY, CHOOSER_OPEN_FILES,
		BUTTON, LABEL;
	}
	
	public enum FileExtensionList {
		WIN, SEED, SEISAN, SAC, TEXT, KNOWNFILES
	}
}
