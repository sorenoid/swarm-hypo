package gov.usgs.util.persist;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotated interface to mark fields for use as persistence id, 
 * unique id for object instance
 * 
 * $Log: not supported by cvs2svn $
 * @author Dan Cervelli
 * @version $Id: PersistentID.java,v 1.1 2007-04-21 10:11:54 dcervelli Exp $
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface PersistentID {}
