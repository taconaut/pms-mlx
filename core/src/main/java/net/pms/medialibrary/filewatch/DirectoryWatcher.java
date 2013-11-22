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

import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.contentobjects.jnotify.JNotify;
import net.contentobjects.jnotify.JNotifyListener;
import net.pms.medialibrary.commons.dataobjects.DOManagedFile;
import net.pms.medialibrary.storage.MediaLibraryStorage;
import net.pms.notifications.NotificationCenter;
import net.pms.notifications.NotificationSubscriber;
import net.pms.notifications.types.ManagedFoldersChangedEvent;

/**
 * The DirectoryWatcher will start watching all configured managed folders when startWatch() is being called.
 * When the managed folder configuration changes, the watched directories will be updated accordingly.
 */
public class DirectoryWatcher {
	private static final Logger logger = LoggerFactory.getLogger(DirectoryWatcher.class);
	private static DirectoryWatcher instance;
	
	private HashMap<String, Integer> watchIdsByWatchedDirectory;
	private ManagedFoldersChangedNotificationSubscriber managedFoldersChangedNotificationSubscriber;

	/**
	 * Private constructor to use class as a singleton
	 */
	private DirectoryWatcher() {
		watchIdsByWatchedDirectory = new HashMap<String, Integer>();
	}

	/**
	 * Gets the single instance of DirectoryWatcher.
	 *
	 * @return single instance of DirectoryWatcher
	 */
	public static DirectoryWatcher getInstance() {
		if (instance == null) {
			instance = new DirectoryWatcher();
		}

		return instance;
	}

	/**
	 * Starts watching managed folders being configured for watching.
	 * The sub-folder flag specifies if sub-folders have to be watched as well.
	 * When the configuration of managed folders changes, the watched folders will be updated accordingly.
	 */
	public void startWatch() {
		// Start watching the directories
		startWatchAll();
		
		// Subscribe to changes in managed folder configuration
		managedFoldersChangedNotificationSubscriber = new ManagedFoldersChangedNotificationSubscriber();
		NotificationCenter.getInstance(ManagedFoldersChangedEvent.class).subscribe(managedFoldersChangedNotificationSubscriber);
	}

	/**
	 * Stops watching the currently watched directories and stops the subscription of managed folder changes.
	 */
	public void stopWatch() {
		// Unsubscribe to changes in managed folder configuration
		NotificationCenter.getInstance(ManagedFoldersChangedEvent.class).unsubscribe(managedFoldersChangedNotificationSubscriber);
		managedFoldersChangedNotificationSubscriber = null;
		
		// Stop watching the directories
		stopWatchAll();
	}

	/**
	 * Starts watching all managed folders configured for watch.
	 */
	private void startWatchAll() {
		int mask = JNotify.FILE_CREATED 
				| JNotify.FILE_DELETED
				| JNotify.FILE_MODIFIED 
				| JNotify.FILE_RENAMED;

		List<DOManagedFile> managedFolders = MediaLibraryStorage.getInstance().getManagedFolders();
		int nbFolders = 0;
		for (DOManagedFile managedFolder : managedFolders) {
			if(managedFolder.isWatchEnabled()) {
				try {
					String directoryToWatch = managedFolder.getPath();
					boolean watchSubFolders = managedFolder.isSubFoldersEnabled();
					
					int watchId = JNotify.addWatch(directoryToWatch, mask, watchSubFolders, new DirectoryChangeListener());
					watchIdsByWatchedDirectory.put(directoryToWatch, watchId);					
					nbFolders++;
					
					if(logger.isDebugEnabled()) logger.debug(String.format("Started watching folder='%s'. Watch subfolders=%s", directoryToWatch, watchSubFolders));
				} catch (Throwable t) {
					logger.error(String.format("Failed to start watching directory %s", managedFolder.getPath()), t);
				}
			}
		}
		
		logger.info(String.format("Started watching %s folders", nbFolders));
	}
	
	/**
	 * Stops watching all folders currently being watched.
	 */
	private void stopWatchAll() {
		if(watchIdsByWatchedDirectory.size() == 0){
			// No folders are currently being watched, return!
			return;
		}
		
		int nbFolders = 0;
		for(String folderPath : watchIdsByWatchedDirectory.keySet()) {
			int watchId = watchIdsByWatchedDirectory.get(folderPath);
			try {
				JNotify.removeWatch(watchId);
				nbFolders++;
				
				if(logger.isDebugEnabled()) logger.debug(String.format("Stopped watching folder='%s'", folderPath));
			} catch (Throwable t) {
				logger.error(String.format("Failed to stop watching directory %s", folderPath), t);
			}
		}

		watchIdsByWatchedDirectory.clear();
		
		logger.info(String.format("Stopped watching %s folders", nbFolders));
	}
	
	/**
	 * The ManagedFoldersChangedNotificationSubscriber class handles managed folder change notifications
	 * and updates the folders to be watched accordingly.
	 */
	private class ManagedFoldersChangedNotificationSubscriber implements NotificationSubscriber<ManagedFoldersChangedEvent>{

		@Override
		public void onMessage(ManagedFoldersChangedEvent obj) {
			stopWatchAll();
			startWatchAll();
		}		
	}

	/**
	 * Private class handling file and directory change notifications
	 */
	private class DirectoryChangeListener implements JNotifyListener {
		public void fileRenamed(int wd, String rootPath, String oldName, String newName) {
			if(logger.isDebugEnabled()) logger.debug("renamed " + rootPath + " : " + oldName + " -> " + newName);
		}

		public void fileModified(int wd, String rootPath, String name) {
			if(logger.isDebugEnabled()) logger.debug("modified " + rootPath + " : " + name);
		}

		public void fileDeleted(int wd, String rootPath, String name) {
			if(logger.isDebugEnabled()) logger.debug("deleted " + rootPath + " : " + name);
		}

		public void fileCreated(int wd, String rootPath, String name) {
			if(logger.isDebugEnabled()) logger.debug("created " + rootPath + " : " + name);
		}
	}
}
