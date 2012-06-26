package net.pms.job.executor;

import java.util.concurrent.Executors;

import net.pms.job.JobExecutorService;

import com.google.common.util.concurrent.ListeningExecutorService;
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
	ListeningExecutorService provideListeningExecutorService() {
		return MoreExecutors.listeningDecorator(Executors
				.newCachedThreadPool(new ThreadFactoryBuilder().setDaemon(true)
						.build()));
	}

}
