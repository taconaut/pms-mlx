package net.pms.medialibrary.scanner.impl;

import javax.inject.Inject;
import javax.inject.Singleton;

import net.pms.job.JobExecutorService;
import net.pms.medialibrary.commons.dataobjects.DOManagedFile;
import net.pms.medialibrary.scanner.ImportScannerService;

@Singleton
public class DispatchingImportScannerService implements ImportScannerService {

	@Inject
	private JobExecutorService jobService;

	@Inject
	private ImportJobFactory jobFactory;

	@Override
	public void scan(final DOManagedFile mFolder) {
		jobService.schedule(jobFactory.create(mFolder));
	}
}
