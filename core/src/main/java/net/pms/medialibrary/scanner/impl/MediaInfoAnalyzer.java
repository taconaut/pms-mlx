package net.pms.medialibrary.scanner.impl;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import net.pms.formats.FLAC;
import net.pms.formats.GIF;
import net.pms.formats.ISO;
import net.pms.formats.JPG;
import net.pms.formats.M4A;
import net.pms.formats.MKV;
import net.pms.formats.MP3;
import net.pms.formats.MPG;
import net.pms.formats.OGG;
import net.pms.formats.PNG;
import net.pms.formats.RAW;
import net.pms.formats.TIF;
import net.pms.medialibrary.commons.enumarations.FileType;
import net.pms.medialibrary.scanner.MediaInfo;

import com.google.common.collect.ImmutableSet;
import com.google.common.io.Files;

public class MediaInfoAnalyzer implements MediaInfo {

	private final Set<String> audioFileExtensions;
	private final Set<String> videoFileExtensions;
	private final Set<String> imageFileExtensions;

	MediaInfoAnalyzer() {
		final Set<String> tmp = new HashSet<String>();
		tmp.addAll(Arrays.asList(new M4A().getId()));
		tmp.addAll(Arrays.asList(new MP3().getId()));
		tmp.addAll(Arrays.asList(new OGG().getId()));
		tmp.addAll(Arrays.asList(new FLAC().getId()));
		audioFileExtensions = ImmutableSet.copyOf(tmp);

		tmp.clear();
		tmp.addAll(Arrays.asList(new MKV().getId()));
		tmp.addAll(Arrays.asList(new ISO().getId()));
		tmp.addAll(Arrays.asList(new MPG().getId()));
		videoFileExtensions = ImmutableSet.copyOf(tmp);

		tmp.clear();
		tmp.addAll(Arrays.asList(new JPG().getId()));
		tmp.addAll(Arrays.asList(new PNG().getId()));
		tmp.addAll(Arrays.asList(new GIF().getId()));
		tmp.addAll(Arrays.asList(new TIF().getId()));
		tmp.addAll(Arrays.asList(new RAW().getId()));
		imageFileExtensions = ImmutableSet.copyOf(tmp);
	}

	@Override
	public FileType analyzeMediaType(final String fileName) {
		FileType retVal = FileType.UNKNOWN;

		final String extension = Files.getFileExtension(fileName).toLowerCase();
		if (this.videoFileExtensions.contains(extension)) {
			retVal = FileType.VIDEO;
		} else if (this.audioFileExtensions.contains(extension)) {
			retVal = FileType.AUDIO;
		} else if (this.imageFileExtensions.contains(extension)) {
			retVal = FileType.PICTURES;
		}

		return retVal;
	}

	@Override
	public FileType analyzeMediaType(final File file) {

		FileType retVal = FileType.UNKNOWN;

		if (file.isFile()) {
			retVal = analyzeMediaType(file.getName());
		}

		return retVal;
	}

}
