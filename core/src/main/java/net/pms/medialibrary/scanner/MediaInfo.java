package net.pms.medialibrary.scanner;

import java.io.File;

import net.pms.medialibrary.commons.enumarations.FileType;

/**
 * Provides information about the File
 *
 * @author leonard84
 * 
 */
public interface MediaInfo {

	/**
	 * Tries to determine the {@link FileType} based on the extension
	 * 
	 * @param fileName
	 *            the filename
	 * @return NotNull - the {@link FileType}
	 */
	FileType analyzeMediaType(String fileName);

	/**
	 * Tries to determine the {@link FileType} based on the extension
	 * 
	 * @param file
	 *            the file to analyze
	 * @return NotNull - the {@link FileType}
	 */
	FileType analyzeMediaType(File file);

}
