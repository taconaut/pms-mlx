/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2012  Ph.Waeber
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
package net.pms.medialibrary.scanner;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.pms.Messages;
import net.pms.medialibrary.commons.dataobjects.DOFileInfo;
import net.pms.medialibrary.commons.dataobjects.DOManagedFile;
import net.pms.medialibrary.commons.dataobjects.DOScanReport;
import net.pms.medialibrary.commons.enumarations.FileImportResult;
import net.pms.medialibrary.commons.enumarations.FileType;
import net.pms.medialibrary.commons.enumarations.ScanState;
import net.pms.medialibrary.commons.exceptions.ScanStateException;
import net.pms.medialibrary.commons.interfaces.IFileScannerEventListener;
import net.pms.medialibrary.commons.interfaces.IMediaLibraryStorage;
import net.pms.medialibrary.storage.MediaLibraryStorage;

public class FileScanner implements Runnable{
	private static FileScanner instance;

	private static final Logger log = LoggerFactory.getLogger(FileScanner.class);
	private static int nbScans = 0;
	
	private Queue<FileImportConfiguration> directoryPaths;
	private Thread scanThread;
	private ScanState scanState = ScanState.IDLE;
	private int nbScannedItems;
	private int nbItemsToScan;
	private FullDataCollector dataCollector;
	private IMediaLibraryStorage mediaLibraryStorage;
	private Object scanThreadPause;
	private List<IFileScannerEventListener> fileScannerEventListeners;
	
	public int updateIntervalDays = 50000;
	
	private FileScanner() {
		directoryPaths = new ConcurrentLinkedQueue<FileImportConfiguration>();
		scanThread = new Thread(this);
		nbScannedItems = 0;
		nbItemsToScan = 0;
		dataCollector = FullDataCollector.getInstance();
		mediaLibraryStorage = MediaLibraryStorage.getInstance();
		fileScannerEventListeners = new ArrayList<IFileScannerEventListener>();
		scanThreadPause = new Object();
	}
	
	private synchronized void enqueueManagedFile(FileImportConfiguration mf){
		directoryPaths.add(mf);
	}
	
	private synchronized FileImportConfiguration dequeueImportFile(){
		return directoryPaths.poll();
	}

	public static synchronized FileScanner getInstance() {
	    if(instance == null){
	    	instance = new FileScanner();
	    }
	    return instance;
    }
	
	public DOScanReport getScanState(){
		return new DOScanReport(scanState, nbScannedItems, nbItemsToScan);
	}

	public void scanFolder(DOManagedFile mFolder) {
		File folderToScan = new File(mFolder.getPath());
		if (folderToScan.isDirectory()) {
			net.pms.PMS.get().getFrame().setStatusLine(String.format(Messages.getString("ML.FileScanner.ScanFolder"), folderToScan.getAbsoluteFile()));

			File[] childPaths = folderToScan.listFiles();
			if(childPaths != null) {
				for (int i = 0; i < childPaths.length; i++) {
					
					File currentFile = childPaths[i];
					
					if(currentFile.isHidden()) {
						continue;
					}
					
					if (currentFile.isFile()) {
						// Enqueue the file
						FileImportConfiguration fileImportConfiguration = new FileImportConfiguration(mFolder.getPath(), mFolder.getFileImportTemplate(), false, true,
								mFolder.isPluginImportEnabled(), mFolder.isVideoEnabled(), mFolder.isAudioEnabled(), mFolder.isPicturesEnabled());
						enqueueManagedFile(fileImportConfiguration);
						startScan();
					} else if (currentFile.isDirectory() && mFolder.isSubFoldersEnabled()) {
						// Scan sub-folders
						DOManagedFile tmpFile = new DOManagedFile(mFolder.isWatchEnabled(), currentFile.toString(), 
								mFolder.isVideoEnabled(), mFolder.isAudioEnabled(), mFolder.isPicturesEnabled(),
								mFolder.isSubFoldersEnabled(), mFolder.isPluginImportEnabled(), mFolder.getFileImportTemplate());
						scanFolder(tmpFile);
					}
				}
			} else {
				log.debug("No children found for folder " + folderToScan.getAbsolutePath());
			}
		}
	}
	
	public FileImportResult scanFile(FileImportConfiguration importFile) {
		FileImportResult fileImportResult = FileImportResult.Unknown;

		File f = new File(importFile.getPath());
		if (f.isFile()) {
			// Only update files if they're older then the configured value
			Date dateLastUpdate = mediaLibraryStorage.getFileInfoLastUpdated(f.getAbsolutePath());
			Calendar comp = Calendar.getInstance();
			comp.add(Calendar.DATE, -updateIntervalDays);
			if (importFile.isForceUpdate() || dateLastUpdate.before(comp.getTime())) {
				// retrieve file info
				DOFileInfo fileInfo = null;
				try {
					fileInfo = dataCollector.get(importFile);
				} catch (Throwable t) {
					log.error("Failed to collect info for " + importFile.getPath(), t);
				}

				// insert file info if we were able to retrieve it
				if (fileInfo != null) {
					if(mediaLibraryStorage.isFileImported(fileInfo.getFilePath())) {
						// The file has been previously imported.
						// Get the existing file and set the new properties
						DOFileInfo currentFileInfo = mediaLibraryStorage.getFileInfo(fileInfo.getFilePath());
						currentFileInfo.copySetSystemPropertiesFrom(fileInfo);
						
						mediaLibraryStorage.updateFileInfo(currentFileInfo);
						fileImportResult = FileImportResult.Updated;
					} else {
						mediaLibraryStorage.insertFileInfo(fileInfo);
						fileImportResult = FileImportResult.Imported;
					}

					for (IFileScannerEventListener l : fileScannerEventListeners) {
						l.itemInserted(FileType.VIDEO);
					}
				} else {
					log.debug("Couldn't read " + f);
					fileImportResult = FileImportResult.Failed;
				}
			}
		}

		return fileImportResult;
	}
	
	public void updateFilesRequiringFileUpdate(List<FileType> fileTypesToUpdate) {
		List<String> filePaths = mediaLibraryStorage.getFilePathsRequiringUpdate(fileTypesToUpdate);
		for(String filePath : filePaths) {
			
			File f = new File(filePath);
			if (!f.isFile()) {
				log.warn(String.format("File '%s' won't be updated because it couldn't be found", filePath));
			} else {
				FileImportConfiguration fileImportConfiguration = new FileImportConfiguration(filePath, null, true, true, false, true, true, true);
				enqueueManagedFile(fileImportConfiguration);
			}
		}
		
		startScan();
	}
	
	public void pause() throws ScanStateException{	
		if(log.isDebugEnabled()) log.debug("pause() called.");
		if(scanState != ScanState.RUNNING){
			throw new ScanStateException(ScanState.RUNNING, scanState, "The pause() method can only be called when the ScanState==RUNNING. current state="
					+ scanState); 
		}
		changeScanState(ScanState.PAUSING);
		if(log.isDebugEnabled()) log.debug("Pausing set. Waiting for scan thread to pause.");
	}
	
	public void unPause() throws ScanStateException{	
		if(log.isDebugEnabled()) log.debug("pause() called.");
		if(scanState != ScanState.PAUSED){
			throw new ScanStateException(ScanState.PAUSED, scanState, "The pause() method can only be called when the ScanState==PAUSED. current state="
					+ scanState); 
		}
		synchronized (scanThreadPause) {
			scanThreadPause.notify();
        }
		if(log.isDebugEnabled()) log.debug("Pausing set. Waiting for scan thread to pause.");
	}
	
	
	public void stop(){	
		if(log.isDebugEnabled()) log.debug("stop() called.");
		changeScanState(ScanState.STOPPING);
		if(log.isDebugEnabled()) log.debug("Stopping set. Waiting for scan thread to terminate.");
		synchronized (scanThreadPause) {
			scanThreadPause.notifyAll();
        }
		try{
			scanThread.join();
			if(log.isDebugEnabled()) log.debug("Stopped! Scan thread terminated properly.");		
		}catch(InterruptedException ex){
			if(log.isDebugEnabled()) log.debug("Stopped! Terminated by a InterruptedException.");								
		}
	}
	
	@Override
	public void run() {
		changeScanState(ScanState.RUNNING);
		// Handle each directory in the list
		FileImportConfiguration importFile;
		int nbFilesAdded = 0;
		int nbFilesUpdated = 0;
		//Calendar lastGetDate = Calendar.getInstance();
		while ((importFile = dequeueImportFile()) != null) {
			// check if we have to pause or stop the thread
			if (scanState == ScanState.PAUSING) {
				try {
					if(log.isInfoEnabled()) log.info("Scan paused");
					net.pms.PMS.get().getFrame().setStatusLine("Scan paused");
					changeScanState(ScanState.PAUSED);
					synchronized (scanThreadPause) {
						scanThreadPause.wait();
					}
					net.pms.PMS.get().getFrame().setStatusLine("Restarted scan");
					
					if (scanState == ScanState.STOPPING) {
						break;
					} else {
						changeScanState(ScanState.RUNNING);
						if(log.isInfoEnabled()) log.info("Scan started after pause");
					}
				} catch (InterruptedException ex) {
					log.error("Scan stopped because pause has been interrupted by a Interrupt.", ex);
					break;
				}
			} else if (scanState == ScanState.STOPPING) {
				break;
			}
			
			// Scan the file
			switch(scanFile(importFile)) {
			case Imported:
				nbFilesAdded++;
				break;
			case Updated:
				nbFilesUpdated++;
				break;
			default:
				log.warn(String.format("Failed to scan file '%s'", importFile.getPath()));
				break;
			}
		}

		net.pms.PMS.get().getFrame().setStatusLine(String.format(Messages.getString("ML.Messages.ScanFinished"), String.valueOf(nbFilesAdded), String.valueOf(nbFilesUpdated)));

		log.info(String.format("Scanning finished. Result: %s added, %s updated", nbFilesAdded, nbFilesUpdated));
		changeScanState(ScanState.IDLE);
	}

	public void addFileScannerEventListener(IFileScannerEventListener listener) {
		fileScannerEventListeners.add(listener);
    }
	
	private void changeScanState(ScanState state){
		synchronized (scanState) {
			scanState = state;
			for(IFileScannerEventListener l : fileScannerEventListeners) {
				l.scanStateChanged(scanState);
			}
		}
	}
	
	private void startScan() {
		synchronized (scanState) {
			if (scanState != ScanState.STARTING && scanState != ScanState.RUNNING) {
				changeScanState(ScanState.STARTING);
				scanThread = new Thread(this);
				scanThread.setName("scan" + nbScans++);
				scanThread.start();
			}
		}		
	}
}