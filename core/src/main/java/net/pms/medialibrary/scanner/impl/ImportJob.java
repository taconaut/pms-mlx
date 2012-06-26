package net.pms.medialibrary.scanner.impl;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import javax.inject.Inject;

import net.pms.job.AbstractJob;
import net.pms.medialibrary.commons.dataobjects.DOManagedFile;

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
	private ImportJob(@Assisted final DOManagedFile start) {
		importQueue.add(start);
	}


	@Override
	protected void performUnitOfWork() throws InterruptedException {
		final DOManagedFile parent = importQueue.poll();

		if (parent == null) {
			finished();
			return;
		}

		final File dir = new File(parent.getPath());

		final List<DOManagedFile> filesToAdd = new ArrayList<DOManagedFile>();

		for (final File f : dir.listFiles()) {
			if (f.isHidden()) {
				continue;
			}

			if (f.isDirectory()) {
				if (parent.isSubFoldersEnabled()) {
					importQueue.add(new DOManagedFile(parent, f));
				}
			} else if (f.isFile() && parent.isFileImportEnabled()) {
				filesToAdd.add(new DOManagedFile(parent, f));
			}
		}

		// perform batch insert action

	}

}
