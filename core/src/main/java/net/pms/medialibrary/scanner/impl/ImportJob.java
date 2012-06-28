package net.pms.medialibrary.scanner.impl;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Queue;

import javax.inject.Inject;

import net.pms.job.AbstractJob;
import net.pms.medialibrary.commons.dataobjects.DOFileInfo;
import net.pms.medialibrary.commons.dataobjects.DOManagedFile;
import net.pms.medialibrary.commons.interfaces.IMediaLibraryStorage;
import net.pms.medialibrary.scanner.FileInfoCollector;

import com.google.common.base.Optional;
import com.google.inject.assistedinject.Assisted;

/**
 * Simple import job for new files.
 *
 * @author leonard84
 *
 */
public class ImportJob extends AbstractJob {

	private final Queue<DOManagedFile> importQueue = new ArrayDeque<DOManagedFile>();

	@Inject
	private FileInfoCollector fileInfoCollector;
	@Inject
	private IMediaLibraryStorage mediaLibraryStorage;

	@Inject
	private ImportJob(@Assisted final DOManagedFile start) {
		importQueue.add(start);
	}

	@Override
	protected void performUnitOfWork() throws InterruptedException {
		final DOManagedFile current = importQueue.poll();

		if (current == null) {
			finished();
			return;
		}

		final File currentFile = new File(current.getPath());

		final List<DOManagedFile> filesToAdd = new ArrayList<DOManagedFile>();
		if (currentFile.isDirectory()) {
			for (final File f : currentFile.listFiles()) {
				if (f.isHidden()) {
					continue;
				}

				if (f.isDirectory()) {
					if (current.isSubFoldersEnabled()) {
						importQueue.add(new DOManagedFile(current, f));
					}
				} else if (f.isFile()) {
					filesToAdd.add(new DOManagedFile(current, f));
				}
			}
		} else {
			// if we just want to import a single file
			filesToAdd.add(current);
		}

		// This could be handed off to a separate Job
		for (final DOManagedFile doManagedFile : filesToAdd) {
			final Date lastUpdated = mediaLibraryStorage
					.getFileInfoLastUpdated(doManagedFile.getPath());

			if (lastUpdated == null || lastUpdated.getTime() == 0) {

				final Optional<DOFileInfo> fileInfo = fileInfoCollector
						.analyze(doManagedFile);

				if (fileInfo.isPresent()) {
					mediaLibraryStorage.insertFileInfo(fileInfo.get());
				}
			}
		}
	}
}
