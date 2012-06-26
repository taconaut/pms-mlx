package net.pms.medialibrary.scanner.impl;

import net.pms.job.Job;
import net.pms.medialibrary.commons.dataobjects.DOManagedFile;

/**
 * Interface for a factory to be generated be Guice, using <a
 * href="https://code.google.com/p/google-guice/wiki/AssistedInject">
 * AssistedInject</a>.
 *
 * @author leonard84
 *
 */
public interface ImportJobFactory {

	/**
	 * Creates a new import job
	 * 
	 * @param file
	 *            the starting point
	 * @return the Job
	 */
	Job create(DOManagedFile file);
}
