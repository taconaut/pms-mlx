package net.pms.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class that handles all the threading and state related operations
 * for its implementors.
 * 
 * @author leonard84
 * 
 */
public abstract class AbstractJob implements Job {

	private static final Logger log = LoggerFactory.getLogger(Job.class);

	private volatile JobState state;
	private volatile JobAction action;

	/**
	 * Default c'tor.
	 */
	public AbstractJob() {
		state = JobState.New;
		action = JobAction.Run;
	}


	@Override
	public final JobState call() throws Exception {
		if (action == JobAction.Resume) {
			action = JobAction.Run;
			onResume();
		} else {
			onStart();
		}

		state = JobState.Running;

		while (action == JobAction.Run) {
			try {
				performUnitOfWork();
			} catch (final InterruptedException e) {
				if (log.isDebugEnabled()) {
					log.debug("Interrupted", e);
				}
			}
		}
		switch (action) {
		case Abort:
			state = JobState.Aborted;
			break;

		case Pause:
			state = JobState.Paused;
			break;

		case Finish:
			state = JobState.Finished;
			break;
		}
		return state;
	}

	@Override
	public final JobState getState() {
		return state;
	}

	@Override
	public final void abort() {
		if (state == JobState.New || state == JobState.Running
				|| state == JobState.Paused) {
			onAbort();
			action = JobAction.Abort;
		}
	}

	/**
	 * If the job is done it should call finished() to indicate this.
	 */
	protected final void finished() {
		if (state == JobState.Running) {
			onFinish();
			action = JobAction.Finish;
		}
	}

	@Override
	public final void pause() {
		if (state == JobState.New || state == JobState.Running) {
			onPause();
			action = JobAction.Pause;
		}
	}

	@Override
	public final void resume() {
		if (state == JobState.Paused) {
			action = JobAction.Resume;
		}
	}

	/**
	 * Is called when the {@link Job#abort()} is invoked.
	 */
	protected void onAbort() {
	}

	/**
	 * Is called when the {@link AbstractJob#finished()} is invoked.
	 */
	protected void onFinish() {
	}

	/**
	 * Is called when the {@link Job#pause()} is invoked.
	 */
	protected void onPause() {
	}

	/**
	 * Is called when the processing starts for the first time.
	 * 
	 * May be skipped if the Job was paused before it started, then only
	 * {@link AbstractJob#onResume()} will be called.
	 * 
	 */
	protected void onStart() {
	}

	/**
	 * Is called when the processing resumes after being paused before.
	 */
	protected void onResume() {
	}

	/**
	 * Implementors should put all of the work performing code here. If a
	 * blocking operation is performed, it should be done with a reasonable
	 * timeout, so that the job stays responsive to pause and abort requests.
	 * 
	 * @throws InterruptedException
	 *             if a blocking call was interrupted
	 */
	protected abstract void performUnitOfWork() throws InterruptedException;



}
