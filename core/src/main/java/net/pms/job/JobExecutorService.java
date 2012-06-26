package net.pms.job;

public interface JobExecutorService {
	
	void schedule(Job job);
	
	void initiateShutdown();

}
