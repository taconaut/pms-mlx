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
package net.pms.medialibrary.commons.dataobjects;

import java.io.File;

public class DOManagedFile {
	private boolean watchEnabled;
	private String  path;
	private boolean videoEnabled;
	private boolean audioEnabled;
	private boolean picturesEnabled;
	private boolean subFoldersEnabled;
	private DOFileImportTemplate importConfig;
	private boolean fileImportEnabled;

	public DOManagedFile() {
		this(false, "", false, false, false, false, false, new DOFileImportTemplate());
	}

	public DOManagedFile(final DOManagedFile other, final File path) {
		this(other, path.getAbsolutePath());
	}

	public DOManagedFile(final DOManagedFile other, final String path) {
		this(other.isWatchEnabled(), path, other.isVideoEnabled(), other
				.isAudioEnabled(), other.isPicturesEnabled(), other
				.isSubFoldersEnabled(), other.isFileImportEnabled(),
				other.getFileImportTemplate());
	}

	public DOManagedFile(final boolean watchEnabled, final String path, final boolean videoEnabled, final boolean audioEnabled, final boolean picturesEnabled, final boolean subFoldersEnabled, final boolean fileImportEnabled, final DOFileImportTemplate importConfig) {
		setWatchEnabled(watchEnabled);
		setPath(path);
		setVideoEnabled(videoEnabled);
		setAudioEnabled(audioEnabled);
		setPicturesEnabled(picturesEnabled);
		setSubFoldersEnabled(subFoldersEnabled);
		setFileImportTemplate(importConfig);
		setFileImportEnabled(fileImportEnabled);
	}

	public void setWatchEnabled(final boolean watchEnabled) {
		this.watchEnabled = watchEnabled;
	}

	public boolean isWatchEnabled() {
		return watchEnabled;
	}

	public void setPath(final String folderPath) {
		this.path = folderPath;
	}

	public String getPath() {
		return path;
	}

	public void setVideoEnabled(final boolean videoEnabled) {
		this.videoEnabled = videoEnabled;
	}

	public boolean isVideoEnabled() {
		return videoEnabled;
	}

	public void setAudioEnabled(final boolean audioEnabled) {
		this.audioEnabled = audioEnabled;
	}

	public boolean isAudioEnabled() {
		return audioEnabled;
	}

	public void setPicturesEnabled(final boolean picturesEnabled) {
		this.picturesEnabled = picturesEnabled;
	}

	public boolean isPicturesEnabled() {
		return picturesEnabled;
	}

	public void setSubFoldersEnabled(final boolean subFoldersEnabled) {
		this.subFoldersEnabled = subFoldersEnabled;
	}

	public boolean isSubFoldersEnabled() {
		return subFoldersEnabled;
	}

	public DOFileImportTemplate getFileImportTemplate() {
		if(importConfig == null) {
			importConfig = new DOFileImportTemplate();
		}
		return importConfig;
	}

	public void setFileImportTemplate(final DOFileImportTemplate importConfig) {
		this.importConfig = importConfig;
	}

	public void setFileImportEnabled(final boolean enabled) {
		fileImportEnabled = enabled;
	}

	public boolean isFileImportEnabled() {
		return fileImportEnabled;
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof DOManagedFile)) {
			return false;
		}

		final DOManagedFile compObj = (DOManagedFile) obj;
		if (isWatchEnabled() == compObj.isWatchEnabled()
				&& getPath().equals(compObj.getPath())
				&& isAudioEnabled() == compObj.isAudioEnabled()
				&& isPicturesEnabled() == compObj.isPicturesEnabled()
				&& isVideoEnabled() == compObj.isVideoEnabled()
				&& isSubFoldersEnabled() == compObj.isSubFoldersEnabled()
				//&& isFileImportEnabled() == compObj.isFileImportEnabled() //don't use this attribute as it isn't part of the primary key in the db
				) {
			return true;
		}

		return false;
	}

	@Override
	public int hashCode(){
		int hashCode = 24 + (isWatchEnabled() ? 1 : 2);
		hashCode *= 24 + getPath().hashCode();
		hashCode *= 24 + (isAudioEnabled() ? 3 : 4);
		hashCode *= 24 + (isPicturesEnabled() ? 5 : 6);
		hashCode *= 24 + (isVideoEnabled() ? 7 : 8);
		hashCode *= 24 + (isSubFoldersEnabled() ? 11 : 12);
		//hashCode *= 24 + (isFileImportEnabled() ? 13 : 14); //don't use this attribute as it isn't part of the primary key in the db
		return hashCode;
	}

	@Override
	public String toString(){
		return String.format("folder=%s, watch=%s, subfolders=%s, video=%s, audio=%s, pictures=%s, fileImport=%s, fileImportTemplate=%s",
				getPath(), isWatchEnabled(), isSubFoldersEnabled(), isVideoEnabled(), isAudioEnabled(), isPicturesEnabled(), isFileImportEnabled(), getFileImportTemplate().getId());
	}
}
