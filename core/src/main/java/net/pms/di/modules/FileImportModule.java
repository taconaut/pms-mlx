package net.pms.di.modules;

import net.pms.job.Job;
import net.pms.medialibrary.scanner.ImportScannerService;
import net.pms.medialibrary.scanner.MediaInfo;
import net.pms.medialibrary.scanner.impl.DispatchingImportScannerService;
import net.pms.medialibrary.scanner.impl.ImportJob;
import net.pms.medialibrary.scanner.impl.ImportJobFactory;
import net.pms.medialibrary.scanner.impl.MediaInfoAnalyzer;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;

public class FileImportModule extends AbstractModule {

	@Override
	protected void configure() {
		install(new FactoryModuleBuilder()
				.implement(Job.class, ImportJob.class).build(
						ImportJobFactory.class));

		bind(MediaInfo.class).to(MediaInfoAnalyzer.class);

		bind(ImportScannerService.class).to(
				DispatchingImportScannerService.class);
	}

}
