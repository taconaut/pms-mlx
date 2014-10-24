package net.pms.medialibrary.scanner;

import net.pms.medialibrary.commons.dataobjects.DOFileImportTemplate;

public class FileImportConfiguration {
	private String path;
	private DOFileImportTemplate fileImportTemplate;
	private boolean isForceUpdate;
	private boolean isFilePropertiesImportEnabled;
	private boolean isPluginImportEnabled;
	private boolean isVideoImportEnabled;
	private boolean isAudioImportEnabled;
	private boolean isPictureImportEnabled;
	
	public FileImportConfiguration() {
	}
	
	public FileImportConfiguration(String path, DOFileImportTemplate fileImportPlugin, boolean isForceUpdate, boolean isFilePropertiesImportEnabled,
			boolean isPluginImportEnabled, boolean isVideoImportEnabled, boolean isAudioImportEnabled, boolean isPictureImportEnabled) {
		this.path = path;
		this.fileImportTemplate = fileImportPlugin;
		this.isForceUpdate = isForceUpdate;
		this.isFilePropertiesImportEnabled = isFilePropertiesImportEnabled;
		this.isPluginImportEnabled = isPluginImportEnabled;
		this.isVideoImportEnabled = isVideoImportEnabled;
		this.isAudioImportEnabled = isAudioImportEnabled;
		this.isPictureImportEnabled = isPictureImportEnabled;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public DOFileImportTemplate getFileImportTemplate() {
		return fileImportTemplate;
	}

	public void setFileImportTemplate(DOFileImportTemplate fileImportTemplate) {
		this.fileImportTemplate = fileImportTemplate;
	}

	public boolean isForceUpdate() {
		return isForceUpdate;
	}

	public void setForceUpdate(boolean isForceUpdate) {
		this.isForceUpdate = isForceUpdate;
	}

	public boolean isFilePropertiesImportEnabled() {
		return isFilePropertiesImportEnabled;
	}

	public void setFilePropertiesImportEnabled(boolean isFilePropertiesImportEnabled) {
		this.isFilePropertiesImportEnabled = isFilePropertiesImportEnabled;
	}

	public boolean isPluginImportEnabled() {
		return isPluginImportEnabled;
	}

	public void setPluginImportEnabled(boolean isPluginImportEnabled) {
		this.isPluginImportEnabled = isPluginImportEnabled;
	}

	public boolean isVideoImportEnabled() {
		return isVideoImportEnabled;
	}

	public void setVideoImportEnabled(boolean isVideoImportEnabled) {
		this.isVideoImportEnabled = isVideoImportEnabled;
	}

	public boolean isAudioImportEnabled() {
		return isAudioImportEnabled;
	}

	public void setAudioImportEnabled(boolean isAudioImportEnabled) {
		this.isAudioImportEnabled = isAudioImportEnabled;
	}

	public boolean isPictureImportEnabled() {
		return isPictureImportEnabled;
	}

	public void setPictureImportEnabled(boolean isPictureImportEnabled) {
		this.isPictureImportEnabled = isPictureImportEnabled;
	}
}
