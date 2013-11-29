package net.pms.plugin.fileimport.tvdb.gui;

import java.awt.GridLayout;
import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.pms.plugin.fileimport.tvdb.configuration.GlobalConfiguration;

/**
 *
 * @author Corey
 */
public class GlobalConfigurationPanel extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(GlobalConfigurationPanel.class);
    private final GlobalConfiguration globalConfig;

    /**
     * Instantiates a new global configuration panel.
     *
     * @param globalConfig the global configuration
     */
    public GlobalConfigurationPanel(GlobalConfiguration globalConfig) {
        setLayout(new GridLayout());
        this.globalConfig = globalConfig;
//		init();
//		build();
    }

    public void applyConfig() {
    }
}
