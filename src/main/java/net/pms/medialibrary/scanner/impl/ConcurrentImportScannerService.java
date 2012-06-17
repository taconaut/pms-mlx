package net.pms.medialibrary.scanner.impl;

import java.util.Queue;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.util.concurrent.AbstractExecutionThreadService;

import net.pms.di.qualifiers.Concurrent;
import net.pms.medialibrary.commons.dataobjects.DOManagedFile;
import net.pms.medialibrary.scanner.ImportQueue;
import net.pms.medialibrary.scanner.ImportScannerService;

@Singleton
public class ConcurrentImportScannerService extends AbstractExecutionThreadService implements ImportScannerService {
	
	@Inject 
	@Concurrent @ImportQueue
	private Queue<DOManagedFile> importQueue;
	
	@Inject
	private ExecutorService executor;
	
	@Override
	public void scan(DOManagedFile mFolder) {
		importQueue.add(mFolder);
	}
	
	@Override
	protected void run() throws Exception {
		// TODO Auto-generated method stub
		
	}
}
