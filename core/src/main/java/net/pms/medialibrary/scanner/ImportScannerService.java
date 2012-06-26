package net.pms.medialibrary.scanner;

import net.pms.medialibrary.commons.dataobjects.DOManagedFile;

/**
 * A service that provides import functionality.
 * 
 * @author leonard84
 *
 */
public interface ImportScannerService {

	/**
	 * Enqueues the folder in the scan queue and returns.
	 * @param notnull - mFolder the folder to scan
	 */
	public void scan(DOManagedFile mFolder);
	
}
