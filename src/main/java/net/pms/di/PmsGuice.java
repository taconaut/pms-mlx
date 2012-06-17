package net.pms.di;

import net.pms.job.executor.JobModule;
import net.pms.modules.FileImportModule;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

/**
 * Takes care of initializing {@link Guice} with all {@link Module}s used
 * by PMS
 * 
 * @author leonard84
 *
 */
public class PmsGuice {
	
	private Injector injector;

	/**
	 * c'tor
	 */
	public PmsGuice() {
		injector = Guice.createInjector(new FileImportModule(), new JobModule());
		InjectionHelper.setInjector(injector);
	}
	
	/**
	 * Gives access to the configured {@link Injector}
	 * @return {@link Injector}
	 */
	public Injector getInjector() {
		return injector;
	}
}
