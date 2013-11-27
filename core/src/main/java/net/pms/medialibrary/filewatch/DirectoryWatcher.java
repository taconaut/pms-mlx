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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.contentobjects.jnotify.JNotify;
import net.pms.medialibrary.commons.dataobjects.DOManagedFile;
import net.pms.medialibrary.storage.MediaLibraryStorage;
import net.pms.notifications.NotificationCenter;
import net.pms.notifications.NotificationSubscriber;
import net.pms.notifications.types.ManagedFoldersChangedEvent;

/**
 * This class will start watching all configured managed folders when startWatch() is being called.
 * When the managed folder configuration changes, the watched directories will be updated accordingly.
 */
public class DirectoryWatcher {
	private static final Logger logger = LoggerFactory.getLogger(DirectoryWatcher.class);
	private static DirectoryWatcher instance;
	
	private final int JNOTIFY_MASK = JNotify.FILE_CREATED | JNotify.FILE_DELETED | JNotify.FILE_RENAMED;
	
	private HashMap<DOManagedFile, Integer> watchIdsByWatchDirectory;
	private ManagedFoldersChangedNotificationSubscriber managedFoldersChangedNotificationSubscriber;

	/**
	 * Private constructor to use class as a singleton
	 */
	private DirectoryWatcher() {
		watchIdsByWatchDirectory = new HashMap<DOManagedFile, Integer>();
		managedFoldersChangedNotificationSubscriber = new ManagedFoldersChangedNotificationSubscriber();
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
		// Start watching all configured directories
		startWatch(MediaLibraryStorage.getInstance().getManagedFolders());
		
		// Subscribe to changes in managed folder configuration
		NotificationCenter
			.getInstance(ManagedFoldersChangedEvent.class)
			.subscribe(managedFoldersChangedNotificationSubscriber);
	}

	/**
	 * Stops watching the currently watched directories and stops the subscription of managed folder changes.
	 */
	public void stopWatch() {
		// Unsubscribe to changes in managed folder configuration
		NotificationCenter
			.getInstance(ManagedFoldersChangedEvent.class)
			.unsubscribe(managedFoldersChangedNotificationSubscriber);
		
		// Stops watching all directories currently being watched
		List<String> allWatchedDirectoryPaths = new ArrayList<String>();
		for(DOManagedFile managedFolder : watchIdsByWatchDirectory.keySet()) {
			allWatchedDirectoryPaths.add(managedFolder.getPath());
		}
		stopWatch(allWatchedDirectoryPaths);
	}

	/**
	 * Starts watching managed folders.
	 * 
	 * @param managedFolders The managed folders for which to start the watch
	 */
	private void startWatch(List<DOManagedFile> managedFolders) {
		int nbFolders = 0;
		for (DOManagedFile managedFolder : managedFolders) {
			File directoryFile = new File(managedFolder.getPath());
			if(!directoryFile.exists()) {
				logger.warn(String.format("The directory '%s' does not exist. It won't be watched.", managedFolder.getPath()));
				continue;
			}
			if(!directoryFile.isDirectory()) {
				logger.warn(String.format("The path '%s' is not a directory. It won't be watched.", managedFolder.getPath()));
				continue;
			}
			
			if(managedFolder.isWatchEnabled()) {
				try {
					String directoryToWatch = managedFolder.getPath();
					boolean watchSubFolders = managedFolder.isSubFoldersEnabled();
					
					int watchId = JNotify.addWatch(directoryToWatch, JNOTIFY_MASK, watchSubFolders, new DirectoryChangeListener());
					watchIdsByWatchDirectory.put(managedFolder, watchId);
					nbFolders++;
					
					if(logger.isDebugEnabled()) logger.debug(String.format("Started watching directory='%s'. Watch subfolders=%s", directoryToWatch, watchSubFolders));
				} catch (Throwable t) {
					logger.error(String.format("Failed to start watching directory '%s'", managedFolder.getPath()), t);
				}
			}
		}
		
		logger.info(String.format("Started watching %s directories", nbFolders));
	}
	
	/**
	 * Stops watching the specified managed folders.
	 * 
	 * @param managedFolderPaths The managed folder paths for which to start the watch
	 */
	private void stopWatch(List<String> managedFolderPaths) {
		if(managedFolderPaths == null || managedFolderPaths.size() == 0){
			return;
		}
		
		int nbFolders = 0;
		for(String managedFolderPath : managedFolderPaths) {
			
			// Determine the watch ID for the current path
			int watchId = -1;
			for(DOManagedFile managedFolder : watchIdsByWatchDirectory.keySet()) {
				if(managedFolder.getPath().equals(managedFolderPath)) {
					watchId = watchIdsByWatchDirectory.get(managedFolder);
					break;
				}
			}
			
			if(watchId < 0) {
				logger.warn("Failed to stop watch on folder '%s' because it couldn't be found in the list of currently watched folders");
				continue;
			}
			
			// Remove current directory from watch
			try {
				JNotify.removeWatch(watchId);
				nbFolders++;
				
				if(logger.isDebugEnabled()) logger.debug(String.format("Stopped watching directory='%s'", managedFolderPath));
			} catch (Throwable t) {
				logger.error(String.format("Failed to stop watching directory '%s'", managedFolderPath), t);
			}
		}

		// Remove current directory from the map holding the references directoryPath -> watchId
		for(String managedFolderPath : managedFolderPaths) {
			DOManagedFile managedFolderToRemove = null;
			for(DOManagedFile managedFolder : watchIdsByWatchDirectory.keySet()) {
				if(managedFolder.getPath().equals(managedFolderPath)) {
					managedFolderToRemove = managedFolder;
					break;
				}
			}
			
			if(managedFolderToRemove != null) {
				watchIdsByWatchDirectory.remove(managedFolderToRemove);
			}
		}
		
		logger.info(String.format("Stopped watching %s directories", nbFolders));
	}
	
	/**
	 * The ManagedFoldersChangedNotificationSubscriber class handles managed folder change notifications
	 * and updates the folders to be watched accordingly.
	 */
	private class ManagedFoldersChangedNotificationSubscriber implements NotificationSubscriber<ManagedFoldersChangedEvent>{

		@Override
		public void onMessage(ManagedFoldersChangedEvent obj) {
			List<DOManagedFile> configuredManagedFolders = MediaLibraryStorage.getInstance().getManagedFolders();

			// Determine the folders for which watching has to be started. 
			// These are either newly added folders or folders where the sub-directory or watch properties has changed.
			ArrayList<DOManagedFile> foldersToStartWatch = new ArrayList<DOManagedFile>();			
			for(DOManagedFile configuredManagedFolder : configuredManagedFolders){
				if(configuredManagedFolder.isWatchEnabled()) {
					boolean folderFound = false;
					for(DOManagedFile watchedManagedFolder : watchIdsByWatchDirectory.keySet()) {
						if(configuredManagedFolder.getPath().equals(watchedManagedFolder.getPath())) {
							if(configuredManagedFolder.isSubFoldersEnabled() != watchedManagedFolder.isSubFoldersEnabled() ||
									configuredManagedFolder.isWatchEnabled() != watchedManagedFolder.isWatchEnabled()) {
								foldersToStartWatch.add(configuredManagedFolder);							
							}
							folderFound = true;
							break;
						}
					}
					
					if(!folderFound) {
						foldersToStartWatch.add(configuredManagedFolder);
					}
				}
			}

			// Determine the folders for which watching has to be stopped. 
			// These are either removed folders or folders where the sub-directory or watch properties has changed.
			ArrayList<String> foldersToStopWatch = new ArrayList<String>();
			for(DOManagedFile watchedManagedFolder : watchIdsByWatchDirectory.keySet()){
				boolean folderFound = false;
				for(DOManagedFile configuredManagedFolder : configuredManagedFolders) {
					if(configuredManagedFolder.getPath().equals(watchedManagedFolder.getPath())) {
						if(configuredManagedFolder.isSubFoldersEnabled() != watchedManagedFolder.isSubFoldersEnabled() ||
								configuredManagedFolder.isWatchEnabled() != watchedManagedFolder.isWatchEnabled()) {
							foldersToStopWatch.add(configuredManagedFolder.getPath());
						}
						folderFound = true;
						break;
					}
				}
				
				if(!folderFound) {
					foldersToStopWatch.add(watchedManagedFolder.getPath());
				}
			}
			
			if(foldersToStopWatch.size() > 0) {
				stopWatch(foldersToStopWatch);
			}
			
			if(foldersToStartWatch.size() > 0) {
				startWatch(foldersToStartWatch);
			}
		}		
	}
}
