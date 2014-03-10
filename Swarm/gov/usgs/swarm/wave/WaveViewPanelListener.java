package gov.usgs.swarm.wave;

import java.awt.event.MouseEvent;
import java.util.EventListener;

/**
 * 
 * $Log: not supported by cvs2svn $
 * Revision 1.1  2006/08/01 23:45:23  cervelli
 * Moved package.
 *
 * Revision 1.1  2006/06/05 18:06:49  dcervelli
 * Major 1.3 changes.
 *
 * @author Dan Cervelli
 */
public interface WaveViewPanelListener extends EventListener
{
	public void waveZoomed(WaveViewPanel src, double oldST, double oldET, double newST, double newET);
	public void mousePressed(WaveViewPanel src, MouseEvent e, boolean dragging);
	public void waveClosed(WaveViewPanel src);
	public void waveTimePressed(WaveViewPanel src, MouseEvent e, double j2k);
}
