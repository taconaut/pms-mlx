package net.pms.job;

import java.util.concurrent.Callable;

/**
 * Interface that all jobs must implement.
 * 
 * The result that {@link Callable#call()} returns must be the {@link JobState}
 * with which the job exited.
 * 
 * @author leonard84
 * 
 */
public interface Job extends Callable<JobState> {

	/**
	 * Requests that the job should be aborted.
	 * 
	 * Only applicable when {@link Job#getState()} is {@link JobState#New},
	 * {@link JobState#Running}, {@link JobState#Paused}.
	 */
	void abort();

	/**
	 * Requests that the job should be paused.
	 * 
	 * Only applicable when {@link Job#getState()} is {@link JobState#New},
	 * {@link JobState#Running}.
	 */
	void pause();

	/**
	 * Requests that the job should be resumed.
	 * 
	 * Only applicable when {@link JobState#Paused}.
	 */
	void resume();

	/**
	 * @return the current state of the job
	 */
	JobState getState();
}
