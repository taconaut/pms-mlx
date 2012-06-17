package net.pms.di;

import javax.inject.Inject;

import com.google.inject.Injector;

/**
 * A helper class to provide access to post construct injections
 * to make the transition to complete dependency injection easier. 
 * @author leonard84
 *
 */
public final class InjectionHelper {
	
	private static Injector injector;
	
	/**
	 * Sets the injector to use for inject members
	 * @param injector {@link Injector} to be used
	 */
	static void setInjector(Injector injector) {
		InjectionHelper.injector = injector;
	}
		
	/**
	 * Uses {@link Injector#injectMembers(Object)} to inject all {@link Inject} 
	 * annotated properties into the target.
	 * @param target the target of the injection
	 */
	public static void injectMembers(Object target) {
		injector.injectMembers(target);
	}

}
