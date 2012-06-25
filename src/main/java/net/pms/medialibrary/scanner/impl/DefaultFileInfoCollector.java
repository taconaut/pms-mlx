package net.pms.medialibrary.scanner.impl;

import java.io.File;
import java.util.Date;

import javax.inject.Inject;

import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.RealFile;
import net.pms.medialibrary.commons.dataobjects.DOAudioFileInfo;
import net.pms.medialibrary.commons.dataobjects.DOFileInfo;
import net.pms.medialibrary.commons.dataobjects.DOImageFileInfo;
import net.pms.medialibrary.commons.dataobjects.DOManagedFile;
import net.pms.medialibrary.commons.dataobjects.DOVideoFileInfo;
import net.pms.medialibrary.commons.enumarations.FileType;
import net.pms.medialibrary.scanner.FileInfoCollector;
import net.pms.medialibrary.scanner.FileScannerDlnaResource;
import net.pms.medialibrary.scanner.MediaInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class DefaultFileInfoCollector implements FileInfoCollector {

	private static final Logger log = LoggerFactory
			.getLogger(DefaultFileInfoCollector.class);

	private final FileScannerDlnaResource dummyParent = new FileScannerDlnaResource();

	@Inject
	private MediaInfo mediaInfo;

	@Override
	public Optional<DOFileInfo> analyze(final DOManagedFile mf) {
		final File f = new File(mf.getPath());

		if (!f.exists()) {
			return Optional.absent();
		}

		Optional<DOFileInfo> retVal = Optional.absent();

		final FileType ft = mediaInfo.analyzeMediaType(f);
		switch (ft) {
		case VIDEO:
			retVal = analyzeVideo(mf, f);
			break;

		case AUDIO:
			retVal = analyzeAudio(mf, f);
			break;

		case PICTURES:
			retVal = analyzePicture(mf, f);
			break;

		default:
			break;
		}

		if (retVal.isPresent()) {
			final DOFileInfo info = retVal.get();
			info.setActive(true);
			info.setSize(f.length());
			info.setDateModifiedOs(new Date(f.lastModified()));
		}

		return retVal;
	}

	private Optional<DOFileInfo> analyzePicture(final DOManagedFile mf,
			final File f) {
		if (mf.isPicturesEnabled()) {
			final DOFileInfo tmpImageFileInfo = new DOImageFileInfo();
			tmpImageFileInfo.setFolderPath(f.getParent());
			tmpImageFileInfo.setFileName(f.getName());
			tmpImageFileInfo.setType(FileType.PICTURES);
			// TODO: Implement
			return Optional.of(tmpImageFileInfo);
		}
		return Optional.absent();
	}

	private Optional<DOFileInfo> analyzeAudio(final DOManagedFile mf,
			final File f) {
		if (mf.isAudioEnabled()) {
			final DOFileInfo tmpAudioFileInfo = new DOAudioFileInfo();
			tmpAudioFileInfo.setFolderPath(f.getParent());
			tmpAudioFileInfo.setFileName(f.getName());
			tmpAudioFileInfo.setType(FileType.AUDIO);
			// TODO: Implement
			return Optional.of(tmpAudioFileInfo);
		}
		return Optional.absent();
	}

	private Optional<DOFileInfo> analyzeVideo(final DOManagedFile mf,
			final File f) {
		if (mf.isVideoEnabled()) {
			final DOVideoFileInfo tmpVideoFileInfo = new DOVideoFileInfo();
			tmpVideoFileInfo.setFolderPath(f.getParent());
			tmpVideoFileInfo.setFileName(f.getName());
			tmpVideoFileInfo.setType(FileType.VIDEO);
			// get the information from pms internal util (mediainfo or ffmpeg)
			populateMovieInfo(tmpVideoFileInfo);

			if (tmpVideoFileInfo.getName().equals("")) {
				tmpVideoFileInfo.setName(tmpVideoFileInfo.getFileName(false));
			}
			if (tmpVideoFileInfo.getSortName().equals("")) {
				tmpVideoFileInfo.setSortName(tmpVideoFileInfo.getName());
			}

			return Optional.of((DOFileInfo) tmpVideoFileInfo);
		}
		return Optional.absent();
	}

	private void populateMovieInfo(final DOVideoFileInfo fi) {

		final File inFile = new File(fi.getFilePath());
		if (!inFile.exists() && !inFile.canRead()) {
			log.error("File "
					+ fi.getFilePath()
					+ " doesn't exist or couldn't be opened as a file for reading");
			return;
		}
		final RealFile rf = new RealFile(inFile);
		// add the parent to avoid a null pointer exception when calling
		// isValid
		rf.setParent(dummyParent);
		if (!rf.isValid()) {
			return;
		}
		rf.resolve();
		final DLNAMediaInfo mi = rf.getMedia();

		try {
			fi.setAspectRatio(mi.getAspect());
			fi.setBitrate(mi.getBitrate());
			fi.setBitsPerPixel(mi.getBitsPerPixel());
			if (mi.getCodecV() != null) {
				fi.setCodecV(mi.getCodecV());
			}
			if (mi.getContainer() != null) {
				fi.setContainer(mi.getContainer());
			}
			fi.setDurationSec(mi.getDurationInSeconds());
			fi.setDvdtrack(mi.getDvdtrack());
			if (mi.getFrameRate() != null) {
				fi.setFrameRate(mi.getFrameRate());
			}
			if (mi.getH264AnnexB() != null) {
				fi.setH264_annexB(mi.getH264AnnexB());
			}
			fi.setHeight(mi.getHeight());
			if (mi.getMimeType() != null) {
				fi.setMimeType(mi.getMimeType());
			}
			if (mi.getModel() != null) {
				fi.setModel(mi.getModel());
			}
			fi.setSize(mi.getSize());
			fi.setWidth(mi.getWidth());
			fi.setMuxingMode(mi.getMuxingMode());
			if (mi.getSubtitlesCodes() != null) {
				fi.setSubtitlesCodes(mi.getSubtitlesCodes());
			}
			if (mi.getAudioCodes() != null) {
				fi.setAudioCodes(mi.getAudioCodes());
			}
		} catch (final Exception ex) {
			log.error("Failed to parse file info", ex);
		}
	}

}
