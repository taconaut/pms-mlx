package net.pms.medialibrary.scanner;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;


import javax.inject.Qualifier;

import net.pms.medialibrary.scanner.impl.ConcurrentImportScannerService;

/**
 * Qualifier to identify the import queue of the {@link ConcurrentImportScannerService}
 * @author leonard84
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({ METHOD, CONSTRUCTOR, FIELD })
public @interface ImportQueue {

}
