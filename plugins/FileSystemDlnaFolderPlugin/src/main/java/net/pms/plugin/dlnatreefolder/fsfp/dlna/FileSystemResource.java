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
package net.pms.plugin.dlnatreefolder.fsfp.dlna;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.pms.PMS;
import net.pms.dlna.CueFolder;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.DVDISOFile;
import net.pms.dlna.PlaylistFolder;
import net.pms.dlna.RarredFile;
import net.pms.dlna.RealFile;
import net.pms.dlna.ZippedFile;
import net.pms.dlna.virtual.TranscodeVirtualFolder;
import net.pms.dlna.virtual.VirtualFolder;
import net.pms.formats.FormatFactory;

/**
 * The FileSystemResource contains 0-n shared folders.<br>
 * If nbFolders == 0 all drives will be shared<br>
 * If nbFolders == 1 this folder will be shared<br>
 * If nbFolders > 1 all folders will be merged into one
 */
public class FileSystemResource extends VirtualFolder {
	private static final Logger logger = LoggerFactory.getLogger(FileSystemResource.class);
	private List<File> discoverable;
	private List<String> folderPaths = new ArrayList<String>();
	private boolean isRefreshing = false;

	/**
	 * Instantiates a new file system resource.
	 *
	 * @param name the name that will show up on the renderer
	 * @param folderPaths the paths of the folders to share
	 */
	public FileSystemResource(String name, List<String> folderPaths) {
	    super(name, null);
	    setFolderPaths(folderPaths);
    }
	
	/**
	 * Gets the folder paths.
	 *
	 * @return the folder paths
	 */
	public List<String> getFolderPaths(){
		return folderPaths;
	}
	
	/**
	 * Adds the folder path.
	 *
	 * @param path the path
	 */
	public void addFolderPath(String path){
		if(!folderPaths.contains(path)){
			folderPaths.add(path);
		}
	}
	
	/**
	 * Sets the folder paths.
	 *
	 * @param folderPaths the new folder paths
	 */
	public void setFolderPaths(List<String> folderPaths){
		this.folderPaths = folderPaths;
	}
	
	/* (non-Javadoc)
	 * @see net.pms.dlna.DLNAResource#discoverChildren()
	 */
	@Override
	public void discoverChildren() {
		if (discoverable == null){
			discoverable = new ArrayList<File>();			
		}
		else{
			return;			
		}
		
		refreshChildren(true);
	}
	
	/* (non-Javadoc)
	 * @see net.pms.dlna.DLNAResource#isRefreshNeeded()
	 */
	@Override
	public boolean isRefreshNeeded() {
		return true;
	}

	/* (non-Javadoc)
	 * @see net.pms.dlna.DLNAResource#refreshChildren()
	 */
	@Override
	public boolean refreshChildren() {
		return refreshChildren(false);
	}

	/**
	 * Refresh children. For the first use, the shared files and
	 * folders found will be added to the list discoverable to
	 * speed up the process and read the files later. When
	 * refreshing a {@link FileSystemResource}, files which have
	 * been removed from the file system will disappear and added
	 * ones show up at the bottom of the list
	 *
	 * @param isFirstUse true if it is the first use
	 * @return true, if children have been refreshed
	 */
	private boolean refreshChildren(boolean isFirstUse) {
		synchronized (this) {
			if(isRefreshing) return false;
			isRefreshing = true;			
		}

		if(folderPaths == null){
			folderPaths = new ArrayList<String>();
		}

		List<File> rootFolders = Arrays.asList(File.listRoots());

		//Get the list of files and folders contained in the configured folders
		Map<File, String> mergeFolders = new HashMap<File, String>(); // value=file, key=Name
		Map<File, String> mergeFiles = new HashMap<File, String>(); // value=file, key=Name
		
		if(folderPaths.size() == 0){
			//add all disks if no folder has been configured
			for(File f : rootFolders){
				mergeFolders.put(f, f.getAbsolutePath());
			}
			logger.info(String.format("Added all disks (%s) because no folders were configured for file system folder %s", mergeFolders.size(), getName()));
		} else {
			//add the configured folder(s)
			for (String folderPath : folderPaths) {
				File dir = new File(folderPath);
				if (dir.isDirectory()) {
					for (String s : dir.list()) {
						File child = new File(dir.getAbsolutePath() + File.separatorChar + s);
						if (child.isDirectory()) {
							mergeFolders.put(child, child.getName());
						} else if (child.isFile()) {
							mergeFiles.put(child, child.getName()); 
						}
					}
				}
			}
		}
		
		//merge the sorted lists
		List<File> allFiles = new ArrayList<File>(getSortedPaths(mergeFolders));
		allFiles.addAll(getSortedPaths(mergeFiles));
		
		//Use the same algo as in RealFile
		ArrayList<DLNAResource> removedFiles = new ArrayList<DLNAResource>();
		ArrayList<File> addedFiles = new ArrayList<File>();

		for (DLNAResource d : getChildren()) {
			boolean isNeedMatching = !(d.getClass() == FileSystemResource.class || (d instanceof VirtualFolder && !(d instanceof DVDISOFile)));
			if (isNeedMatching && !foundInList(allFiles, d)) {
				removedFiles.add(d);
			}
		}

		for (File f : allFiles) {
			if (!f.isHidden() && (f.isDirectory() || FormatFactory.getAssociatedFormat(f.getName()) != null)) {
				addedFiles.add(f);
			}
		}

		for (DLNAResource f : removedFiles) {
			logger.debug("File automatically removed: " + f.getName());
		}

		for (File f : addedFiles) {
			logger.debug("File automatically added: " + f.getName());
		}

		// false: don't create the folder if it doesn't exist i.e. find the folder
		TranscodeVirtualFolder transcodeFolder = getTranscodeFolder(false);

		for (DLNAResource f : removedFiles) {
			getChildren().remove(f);

			if (transcodeFolder != null) {
				for (int j = transcodeFolder.getChildren().size() - 1; j >= 0; j--) {
					if (transcodeFolder.getChildren().get(j).getName().equals(f.getName())) {
						transcodeFolder.getChildren().remove(j);
					}
				}
			}
		}

		for (File f : addedFiles) {
			manageFile(f);
		}
		for(File f : addedFiles) {
			if(isFirstUse){
				discoverable.add(f);
			} else {
				addChild(new RealFile(f));
			}
		}

		synchronized (this) {
			isRefreshing = false;
		}
		
		return isFirstUse ? false : removedFiles.size() > 0 || addedFiles.size() > 0;
	}
	
	
	/* (non-Javadoc)
	 * @see net.pms.dlna.DLNAResource#analyzeChildren(int)
	 */
	@Override
	public boolean analyzeChildren(int count) {
		int currentChildrenCount = getChildren().size();
		while ((getChildren().size() - currentChildrenCount) < count || count == -1) {
			if (discoverable.size() == 0) {
				break;
			}
			manageFile(discoverable.remove(0));
		}
		return discoverable.size() == 0;
	}

	/**
	 * Gets the paths sorted by folder or file name
	 *
	 * @param mergePaths the merge paths
	 * @return the sorted paths
	 */
	private Collection<File> getSortedPaths(Map<File, String> mergePaths) {
		List<File> res = new ArrayList<File>();
		
		//Sort the lists by folder or file name
		Entry<File, String>[] sortedFolders = getSortedHashtableEntries(mergePaths);
		for(Entry<File, String> entry : sortedFolders){
			res.add(entry.getKey());
		}
		
	    return res;
    }
	
	/**
	 * Loads a specific resource if the file is of type
	 * .zip, .cbz, .rar, .cbr, .iso, .img etc.
	 *
	 * @param f the file
	 */
	private void manageFile(File f) {
		List<File> rootFolders = Arrays.asList(File.listRoots());
		if ((f.isFile() || f.isDirectory()) && (!f.isHidden() || rootFolders.contains(f))) {
			if (f.getName().toLowerCase().endsWith(".zip") || f.getName().toLowerCase().endsWith(".cbz")) {
				addChild(new ZippedFile(f));
			} else if (f.getName().toLowerCase().endsWith(".rar") || f.getName().toLowerCase().endsWith(".cbr")) {
				addChild(new RarredFile(f));
			} else if ((f.getName().toLowerCase().endsWith(".iso") || f.getName().toLowerCase().endsWith(".img")) || (f.isDirectory() && f.getName().toUpperCase().equals("VIDEO_TS"))) {
				addChild(new DVDISOFile(f));
			} else if (f.getName().toLowerCase().endsWith(".m3u") || f.getName().toLowerCase().endsWith(".m3u8") || f.getName().toLowerCase().endsWith(".pls")) {
				addChild(new PlaylistFolder(f));
			} else if (f.getName().toLowerCase().endsWith(".cue")) {
				addChild(new CueFolder(f));
			} else {
				
				/* Optionally ignore empty directories */
				if (f.isDirectory() && PMS.getConfiguration().isHideEmptyFolders() && !isFolderRelevant(f)) {					
					if(logger.isInfoEnabled()) logger.info("Ignoring empty/non relevant directory: " + f.getName());
				}
				
				/* Otherwise add the file */
				else {
					RealFile file = new RealFile(f);
					addChild(file);
				}
			}
		}
	}
	
	/**
	 * Checks if a folder is relevant.
	 *
	 * @param f the f
	 * @return true, if f is a folder containing playable files
	 */
	private boolean isFolderRelevant(File f) {
		boolean isRelevant = false;

		if (f.isDirectory() && PMS.getConfiguration().isHideEmptyFolders()) {
			File[] children = f.listFiles();

			// listFiles() returns null if "this abstract pathname does not denote a directory, or if an I/O error occurs".
			// in this case (since we've already confirmed that it's a directory), this seems to mean the directory is non-readable
			// http://www.ps3mediaserver.org/forum/viewtopic.php?f=6&t=15135
			// http://stackoverflow.com/questions/3228147/retrieving-the-underlying-error-when-file-listfiles-return-null
			if (children == null) {
				logger.warn("Can't list files in non-readable directory: {}", f.getAbsolutePath());
			} else {
				for (File child : children) {
					if (child.isFile()) {
						if (FormatFactory.getAssociatedFormat(child.getName()) != null || isFileRelevant(child)) {
							isRelevant = true;
							break;
						}
					} else {
						if (isFolderRelevant(child)) {
							isRelevant = true;
							break;
						}
					}
				}
			}
		}
		return isRelevant;
	}
	
	/**
	 * Checks if is file relevant.
	 *
	 * @param f the file to check
	 * @return true, if is file relevant
	 */
	private boolean isFileRelevant(File f) {
		String fileName = f.getName().toLowerCase();
		return (PMS.getConfiguration().isArchiveBrowsing() && (fileName.endsWith(".zip") || fileName.endsWith(".cbz") ||
			fileName.endsWith(".rar") || fileName.endsWith(".cbr"))) ||
			fileName.endsWith(".iso") || fileName.endsWith(".img") || 
			fileName.endsWith(".m3u") || fileName.endsWith(".m3u8") || fileName.endsWith(".pls") || fileName.endsWith(".cue");
	}

	/**
	 * Helper method used to sort paths
	 *
	 * @param h the h
	 * @return the sorted hashtable entries
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Entry<File, String>[] getSortedHashtableEntries(Map<File, String> h) {
		Set<?> set = h.entrySet();
		Map.Entry[] entries = (Map.Entry[]) set.toArray(new Map.Entry[set.size()]);
		Arrays.sort(entries, new Comparator<Object>() {
			public int compare(Object o1, Object o2) {
				Object v1 = ((Map.Entry) o1).getValue();
				Object v2 = ((Map.Entry) o2).getValue();
				return ((Comparable<Object>) v1).compareTo(v2);
			}
		});
		return entries;
	}

	private boolean foundInList(List<File> files, DLNAResource dlna) {
		for (File file: files) {
			if (!file.isHidden() && isNameMatch(dlna, file) && (isRealFolder(dlna) || isSameLastModified(dlna, file))) {
				files.remove(file);
				return true;
			}
		}
		return false;
	}

	private boolean isSameLastModified(DLNAResource dlna, File file) {
		return dlna.getLastModified() == file.lastModified();
	}

	private boolean isRealFolder(DLNAResource dlna) {
		return dlna instanceof RealFile && dlna.isFolder();
	}

	private boolean isNameMatch(DLNAResource dlna, File file) {
		return (dlna.getName().equals(file.getName()) || isDVDIsoMatch(dlna, file));
	}

	private boolean isDVDIsoMatch(DLNAResource dlna, File file) {
		if (dlna instanceof DVDISOFile) {
			DVDISOFile dvdISOFile = (DVDISOFile) dlna;
			return dvdISOFile.getFilename().equals(file.getName());
		} else {
			return false;
		}
	}
}
