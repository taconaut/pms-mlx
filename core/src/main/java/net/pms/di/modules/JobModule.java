package net.pms.di.modules;

import java.util.concurrent.Executors;

import javax.inject.Singleton;

import net.pms.job.JobExecutorService;
import net.pms.job.executor.JobExecutor;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;

@SuppressWarnings("unused")
public class JobModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(JobExecutorService.class).to(JobExecutor.class);

	}

	@Provides
	@Singleton
	private ListeningScheduledExecutorService provideListeningExecutorService() {

		final int minThreads = Math.min(4, Runtime.getRuntime()
				.availableProcessors());
		return MoreExecutors.listeningDecorator(Executors
				.newScheduledThreadPool(minThreads, new ThreadFactoryBuilder()
						.setDaemon(true)
						.build()));
	}

}
