package net.pms.job.executor;

import java.util.concurrent.Executors;

import javax.inject.Singleton;

import net.pms.job.JobExecutorService;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;

public class JobModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(JobExecutorService.class).to(JobExecutor.class);

	}

	@Provides
	@Singleton
	ListeningScheduledExecutorService provideListeningExecutorService() {

		final int minThreads = Math.min(4, Runtime.getRuntime()
				.availableProcessors());
		return MoreExecutors.listeningDecorator(Executors
				.newScheduledThreadPool(minThreads, new ThreadFactoryBuilder()
						.setDaemon(true)
						.build()));
	}

}
