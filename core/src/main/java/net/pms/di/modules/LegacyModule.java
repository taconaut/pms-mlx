package net.pms.di.modules;

import javax.inject.Singleton;

import net.pms.medialibrary.commons.interfaces.IMediaLibraryStorage;
import net.pms.medialibrary.storage.MediaLibraryStorage;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

/**
 * Support Module for not DI enabled code
 *
 * @author leonard84
 * 
 */
@SuppressWarnings("unused")
public class LegacyModule extends AbstractModule {

	@Override
	protected void configure() {

	}

	@Provides
	@Singleton
	private IMediaLibraryStorage getMediaLibrary() {
		return MediaLibraryStorage.getInstance();
	}

}
