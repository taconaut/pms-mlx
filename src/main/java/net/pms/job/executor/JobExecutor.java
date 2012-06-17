package net.pms.job.executor;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import net.pms.job.Job;
import net.pms.job.JobExecutorService;
import net.pms.job.JobState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

@Singleton
class JobExecutor implements JobExecutorService {

	private static final Logger log = LoggerFactory
			.getLogger(JobExecutorService.class);

	@Inject
	private ListeningExecutorService executorService;

	private final List<Job> running = new CopyOnWriteArrayList<Job>();
	private final List<Job> paused = new CopyOnWriteArrayList<Job>();

	@Override
	public void schedule(final Job job) {
		if (paused.contains(job)) {
			paused.remove(job);
		}
		running.add(job);

		final ListenableFuture<JobState> future = executorService.submit(job);
		Futures.addCallback(future, new FutureCallback<JobState>() {

			@Override
			public void onFailure(final Throwable t) {
				log.info("Execution of job failed with exception", t);
				running.remove(job);
				paused.remove(job);
			}

			@Override
			public void onSuccess(final JobState state) {
				switch (state) {
				case Paused:
					running.remove(job);
					paused.add(job);
					break;

				case Finished:
				case Aborted:
					running.remove(job);
					break;

				default:
					log.warn("Job finished with invalid state");
					running.remove(job);
					paused.remove(job);
					break;
				}

			}
		});

	}

	@Override
	public void initiateShutdown() {
		for (final Job job : running) {
			job.abort();
		}
		for (final Job job : paused) {
			job.abort();
		}
		executorService.shutdownNow();
		try {
			executorService.awaitTermination(5, TimeUnit.SECONDS);
		} catch (final InterruptedException e) {
			log.debug("Failed to shutdown all threads in time", e);
		}
	}
}
