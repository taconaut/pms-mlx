package net.pms.medialibrary.scanner;

import net.pms.medialibrary.commons.dataobjects.DOFileInfo;
import net.pms.medialibrary.commons.dataobjects.DOManagedFile;

import com.google.common.base.Optional;

public interface FileInfoCollector {

	public Optional<DOFileInfo> analyze(DOManagedFile mf);
}
