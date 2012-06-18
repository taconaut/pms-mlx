package net.pms.modules;

import net.pms.job.Job;
import net.pms.medialibrary.scanner.impl.ImportJob;
import net.pms.medialibrary.scanner.impl.ImportJobFactory;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;

public class FileImportModule extends AbstractModule {

	@Override
	protected void configure() {
		install(new FactoryModuleBuilder()
				.implement(Job.class, ImportJob.class).build(
						ImportJobFactory.class));
	}

}
