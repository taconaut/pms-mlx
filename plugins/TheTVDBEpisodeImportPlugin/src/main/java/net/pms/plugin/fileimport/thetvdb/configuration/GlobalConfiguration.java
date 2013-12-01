/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.pms.plugin.fileimport.thetvdb.configuration;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import net.pms.PMS;
import net.pms.configuration.BaseConfiguration;

/**
 *
 * @author Corey
 */
public class GlobalConfiguration extends BaseConfiguration {
	
	private static final String KEY_importLanguage = "importLanguage";
	
	/** The properties file path. */
	private String propertiesFilePath;
	
	/**
	 * Instantiates a new global configuration.
	 */
	public GlobalConfiguration() {
		propertiesFilePath = getGlobalConfigurationDirectory() + "TheTVDBImportPlugin.conf";
	}

	/**
	 * Save the current configuration.
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public void save() throws IOException {
			save(propertiesFilePath);
	}

	/**
	 * Load the last saved configuration.
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public void load() throws IOException {
		load(propertiesFilePath);
	}
	
	/**
	 * Gets the language to use when importing data
	 * @return the language. e.g. en, de, fr
	 */
	public String getImportLanguage() {
		String defaultLanguage = PMS.getConfiguration().getLanguage();
		if(!getSupportedLanguages().contains(defaultLanguage)) {
			defaultLanguage = "en";
		}
		return getValue(KEY_importLanguage, defaultLanguage);
	}
	
	/**
	 * Sets the language to use when importing data
	 */
	public void setImportLanguage(String importLanguage) {
		setValue(KEY_importLanguage, importLanguage);
	}
	
	/**
	 * Gets the list of supported languages for TheTVDB.com
	 * @return the list of supported languages
	 */
	public static List<String> getSupportedLanguages() {
		return Arrays.asList(new String[] {"en", "sv", "no", "da", "fi", "nl", "de", "it", "es", "fr", "pl", "hu", 
											"el", "tr", "ru", "he", "ja", "pt", "zh", "cs", "sl", "hr", "ko"});
	}
}
