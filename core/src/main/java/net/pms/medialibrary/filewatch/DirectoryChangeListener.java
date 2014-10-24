/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2013  Ph.Waeber
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.pms.medialibrary.filewatch;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.contentobjects.jnotify.JNotifyListener;
import net.pms.medialibrary.commons.dataobjects.DOManagedFile;
import net.pms.medialibrary.commons.helpers.FileHelper;
import net.pms.medialibrary.scanner.FileImportConfiguration;
import net.pms.medialibrary.scanner.FileScanner;
import net.pms.medialibrary.storage.MediaLibraryStorage;


/**
 * Private class handling file and directory change notifications
 */
public class DirectoryChangeListener implements JNotifyListener {
	private static final Logger logger = LoggerFactory.getLogger(MediaLibraryStorage.class);
	
	@Override
	public void fileRenamed(int wd, String rootPath, String oldName, String newName) {
		if(logger.isDebugEnabled()) logger.debug(String.format("File renamed event receicved for directory='%s'. OldName='%s', NewName='%s'", rootPath, oldName, newName));
		
		if(!rootPath.endsWith(String.valueOf(File.separatorChar))) {
			// Append a path separator as the paths are stored like this in the DB
			rootPath += File.separatorChar;
		}

		// Update the file name for the given path
		MediaLibraryStorage.getInstance().updateFilePath(rootPath, oldName, newName);
	}

	@Override
	public void fileModified(int wd, String rootPath, String name) {
		String filePath = FileHelper.combine(rootPath, name);
		if(logger.isDebugEnabled()) logger.debug(String.format("File deleted event receicved for '%s' ", filePath));
		
		// Do nothing (yet)
	}

	@Override
	public void fileDeleted(int wd, String rootPath, String name) {
		String filePath = FileHelper.combine(rootPath, name);
		if(logger.isDebugEnabled()) logger.debug(String.format("File deleted event receicved for '%s' ", filePath));
		
		// Delete the file
		MediaLibraryStorage.getInstance().deleteFileInfoByFilePath(filePath);
	}

	@Override
	public void fileCreated(int wd, String rootPath, String name) {
		String filePath = FileHelper.combine(rootPath, name);
		if(logger.isDebugEnabled()) logger.debug(String.format("File created event receicved for '%s' ", filePath));
		
		DOManagedFile managedFolder = getManagedFileByFolderPath(rootPath);
		if(managedFolder == null) {
			logger.warn("Failed to import file '%s' because its managed folder could not be found.");
		}else {
			// Create the managed file
			FileImportConfiguration fileImportConfiguration = new FileImportConfiguration(filePath, managedFolder.getFileImportTemplate(), true, true, 
					managedFolder.isPluginImportEnabled(), managedFolder.isVideoEnabled(), managedFolder.isAudioEnabled(), managedFolder.isPicturesEnabled());
			
			// Wait for the file to be fully written to disk
			waitForFileBeingCreated(filePath);
			
			// Import the file
			FileScanner.getInstance().scanFile(fileImportConfiguration);
		}
	}
	
	/**
	 * Gets the managed file by folder path.
	 *
	 * @param folderPath the folder path
	 * @return the managed file corresponding to the folder path
	 */
	private DOManagedFile getManagedFileByFolderPath(String folderPath) {
		for(DOManagedFile managedFolder : MediaLibraryStorage.getInstance().getManagedFolders()){
			if(managedFolder.getPath().equals(folderPath)) {
				return managedFolder;
			}
		}
		
		return null;
	}
	
	private void waitForFileBeingCreated(String filePath) {
		File file = new File(filePath);
		RandomAccessFile stream = null;
		try {
			// The RandomAccessFile will block the thread until the file has been fully written
			// and will raise an exception when it becomes accessible.
			stream = new RandomAccessFile(file, "rw");
		} catch (Exception e) {
			// Do nothing, the exception is expected (see above comment)
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) {
					logger.error(String.format("Exception during closing file '%s' while waiting for file being created", file.getName()), e);
				}
			}
		}
	}
}
