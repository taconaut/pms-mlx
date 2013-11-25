package net.pms.medialibrary.filewatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.contentobjects.jnotify.JNotifyListener;
import net.pms.medialibrary.storage.MediaLibraryStorage;


/**
 * Private class handling file and directory change notifications
 */
public class DirectoryChangeListener implements JNotifyListener {
	private static final Logger logger = LoggerFactory.getLogger(MediaLibraryStorage.class);
	
	@Override
	public void fileRenamed(int wd, String rootPath, String oldName, String newName) {
		if(logger.isDebugEnabled()) logger.debug("renamed " + rootPath + " : " + oldName + " -> " + newName);
	}

	@Override
	public void fileModified(int wd, String rootPath, String name) {
		if(logger.isDebugEnabled()) logger.debug("modified " + rootPath + " : " + name);
	}

	@Override
	public void fileDeleted(int wd, String rootPath, String name) {
		if(logger.isDebugEnabled()) logger.debug("deleted " + rootPath + " : " + name);
	}

	@Override
	public void fileCreated(int wd, String rootPath, String name) {
		if(logger.isDebugEnabled()) logger.debug("created " + rootPath + " : " + name);
	}
}
