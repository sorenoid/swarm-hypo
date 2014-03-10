package gov.usgs.plot;

import java.awt.Graphics2D;

/**
 * This is an interface for any object that wants to render something on a Plot.

 * @author  Dan Cervelli
 */
public interface Renderer {
	
	/** The function that does the rendering.
	 * @param g the Graphics2D to render to
	 */
    public void render(Graphics2D g);
}
