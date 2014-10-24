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
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.RealFile;
import net.pms.medialibrary.commons.MediaLibraryConfiguration;
import net.pms.medialibrary.commons.VersionConstants;
import net.pms.medialibrary.commons.dataobjects.DOFileInfo;
import net.pms.medialibrary.commons.dataobjects.DOAudioFileInfo;
import net.pms.medialibrary.commons.dataobjects.DOImageFileInfo;
import net.pms.medialibrary.commons.dataobjects.DOVideoFileInfo;
import net.pms.medialibrary.commons.enumarations.FileType;
import net.pms.medialibrary.commons.helpers.FileImportHelper;

public class FullDataCollector {	
	private static final Logger log = LoggerFactory.getLogger(FullDataCollector.class);
	private static FullDataCollector instance;

	private String videoCoverSaveFolderPath = "";
	private static FileScannerDlnaResource dummyParent = new FileScannerDlnaResource();
	
	private FullDataCollector(String videoCoverSaveFolderPath){
		if(videoCoverSaveFolderPath.endsWith(String.valueOf(File.separatorChar))){
			this.videoCoverSaveFolderPath = videoCoverSaveFolderPath;
		}else{
			this.videoCoverSaveFolderPath = videoCoverSaveFolderPath + File.separatorChar;
		}
		
		try{
			File saveFolder = new File(this.videoCoverSaveFolderPath);
			if(!saveFolder.isDirectory()){
				saveFolder.mkdirs();
			}
		}catch(Exception ex){
			log.error("Failed to create directory " + this.videoCoverSaveFolderPath 
					+ "for video cover storage", ex);
		}
	}

	public static FullDataCollector getInstance() {
	    return instance;
    }

	public static void configure(String videoCoverSaveFolderPath) {
	    instance = new FullDataCollector(videoCoverSaveFolderPath);
    }
	
	public DOFileInfo get(FileImportConfiguration importFile) {
		DOFileInfo retVal = null;
		int sep = importFile.getPath().lastIndexOf(java.io.File.separator) + 1;
		String folderPath = importFile.getPath().substring(0, sep);
		String fileName = importFile.getPath().substring(sep);
		switch(FileImportHelper.getFileType(new File(importFile.getPath()))){
			case VIDEO:
				if(importFile.isVideoImportEnabled()){
    				DOVideoFileInfo tmpVideoFileInfo = new DOVideoFileInfo();
    				tmpVideoFileInfo.setFolderPath(folderPath);
    				tmpVideoFileInfo.setFileName(fileName);
    				tmpVideoFileInfo.setType(FileType.VIDEO);
    				
    				if(importFile.isFilePropertiesImportEnabled()) {
	    				//get the information from pms internal util (mediainfo or ffmpeg)
	    				populateMovieInfo(tmpVideoFileInfo);
	    				tmpVideoFileInfo.setFileImportVersion(VersionConstants.VIDEO_FILE_VERSION);
    				}
    				
    				//import the info with configured plugins
    				if(importFile.isPluginImportEnabled()) {
    					FileImportHelper.updateFileInfo(importFile.getFileImportTemplate(), tmpVideoFileInfo);
    				}
    				
    				if(tmpVideoFileInfo.getName().equals("")) {
    					tmpVideoFileInfo.setName(tmpVideoFileInfo.getFileName(false));
    				}
    				if(tmpVideoFileInfo.getSortName().equals("")) {
    					tmpVideoFileInfo.setSortName(tmpVideoFileInfo.getName());
    				}
    				
    				retVal = tmpVideoFileInfo;
				}
				break;
			case AUDIO:
				if(importFile.isAudioImportEnabled()){
    				DOAudioFileInfo tmpAudioFileInfo = new DOAudioFileInfo();
    				tmpAudioFileInfo.setFolderPath(folderPath);
    				tmpAudioFileInfo.setFileName(fileName);
    				tmpAudioFileInfo.setType(FileType.AUDIO);
    				tmpAudioFileInfo.setFileImportVersion(VersionConstants.AUDIO_FILE_VERSION);
    				//TODO: Implement
    				retVal = tmpAudioFileInfo;
				}
				break;
			case PICTURES:
				if(importFile.isPictureImportEnabled()){
    				DOImageFileInfo tmpImageFileInfo = new DOImageFileInfo();
    				tmpImageFileInfo.setFolderPath(folderPath);
    				tmpImageFileInfo.setFileName(fileName);
    				tmpImageFileInfo.setType(FileType.PICTURES);
    				tmpImageFileInfo.setFileImportVersion(VersionConstants.PICTURE_FILE_VERSION);
    				//TODO: Implement
    				retVal = tmpImageFileInfo;
				}
				break;
			default:
				break;
		}

		File f = new File(importFile.getPath());
		if (retVal != null) {
			retVal.setActive(true);
			if (f != null && f.exists()) {
				retVal.setSize(f.length());
				if (f.exists()) {
					retVal.setDateModifiedOs(new Date(f.lastModified()));
				}
			}
		}
		return retVal;
	}
	
	private void populateMovieInfo(DOVideoFileInfo fi) {
		if(fi.getType() == FileType.VIDEO){
			
			File inFile = new File(fi.getFilePath());
			if(!inFile.exists() && !inFile.canRead()){
				log.error("File " + fi.getFilePath() + " doesn't exist or couldn't be opened as a file for reading");
				return;
			}
			RealFile rf = new RealFile(inFile);
			//add the parent to avoid a null pointer exception when calling isValid
			rf.setParent(dummyParent);
			if(!rf.isValid()){
				return;
			}
			rf.resolve();
			DLNAMediaInfo mi = rf.getMedia();
			
			try{
				fi.setAspectRatio(mi.getAspect());
				fi.setBitrate(mi.getBitrate());
				fi.setBitsPerPixel(mi.getBitsPerPixel());
				if(mi.getCodecV() != null) fi.setCodecV(mi.getCodecV());
				if(mi.getContainer() != null) fi.setContainer(mi.getContainer());
				fi.setDurationSec(mi.getDurationInSeconds());
				fi.setDvdtrack(mi.getDvdtrack());
				if(mi.getFrameRate() != null) fi.setFrameRate(mi.getFrameRate());
				if(mi.getH264AnnexB() != null) fi.setH264_annexB(mi.getH264AnnexB());
				fi.setHeight(mi.getHeight());
				if(mi.getMimeType() != null) fi.setMimeType(mi.getMimeType());
				if(mi.getModel() != null) fi.setModel(mi.getModel());
				fi.setSize(mi.getSize());
				fi.setWidth(mi.getWidth());
				fi.setMuxingMode(mi.getMuxingMode());
				fi.setFrameRateMode(mi.getFrameRateMode());
				fi.setAspectRatioContainer(mi.getAspectRatioContainer());
				fi.setAspectRatioVideoTrack(mi.getAspectRatioVideoTrack());
				fi.setReferenceFrameCount(mi.getReferenceFrameCount());
				fi.setAvcLevel(mi.getAvcLevel());
				if(mi.getSubtitleTracksList() != null) fi.setSubtitlesCodes(mi.getSubtitleTracksList());
				if(mi.getAudioTracksList() != null) fi.setAudioCodes(mi.getAudioTracksList());
			}catch(Exception ex){
				log.error("Failed to parse file info", ex);
			}
		}
	}
}
