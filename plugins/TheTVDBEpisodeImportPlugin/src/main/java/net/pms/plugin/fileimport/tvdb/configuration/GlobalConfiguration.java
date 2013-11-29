/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.pms.plugin.fileimport.tvdb.configuration;

import java.io.IOException;
import net.pms.configuration.BaseConfiguration;

/**
 *
 * @author Corey
 */
public class GlobalConfiguration extends BaseConfiguration {

    /**
     * The properties file path.
     */
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
}
