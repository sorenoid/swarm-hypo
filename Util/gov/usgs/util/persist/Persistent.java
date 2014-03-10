package gov.usgs.util.persist;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotated interface to mark fields as persistent, we should
 * process such fields while serializing/deserializing
 * 
 * $Log: not supported by cvs2svn $
 * @author Dan Cervelli
 * @version $Id: Persistent.java,v 1.1 2007-04-21 10:11:53 dcervelli Exp $
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Persistent {}
