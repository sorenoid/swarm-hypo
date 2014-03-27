package gov.usgs.swarm.database.util;

import java.awt.Component;

import javax.swing.JOptionPane;

public class MessageManager
{

	public static void popupErrorMessage(String message, Component parent)
	{
		JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE, null);
	}

	public static void popupInfoMessage(String message, Component parent)
	{
		JOptionPane.showMessageDialog(parent, message, "Info", JOptionPane.INFORMATION_MESSAGE);
	}

	// public static void popupXErrorMessage(Component parent, String
	// errorMessage, Throwable exception)
	// {
	// Exception cause = new Exception("This is the cause :",
	// exception.getCause());
	// JXErrorPane.showDialog(parent, new ErrorInfo("Your exception",
	// exception.getMessage(), errorMessage, null,
	// null, ErrorLevel.SEVERE, null));
	// }
}