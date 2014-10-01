package net.pms.plugin.dlnatreefolder;

import java.io.File;
import java.io.IOException;
import java.util.ResourceBundle;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.pms.PMS;
import net.pms.dlna.DLNAResource;
import net.pms.plugin.dlnatreefolder.web.configuration.InstanceConfiguration;
import net.pms.plugin.dlnatreefolder.web.dlna.WebFolderResource;
import net.pms.plugin.dlnatreefolder.web.gui.InstanceConfigurationPanel;
import net.pms.plugins.DlnaTreeFolderPlugin;
import net.pms.util.PmsProperties;

public class WebFolderPlugin implements DlnaTreeFolderPlugin {
	private static final Logger log = LoggerFactory.getLogger(WebFolderPlugin.class);
	public static final ResourceBundle messages = ResourceBundle.getBundle("net.pms.plugin.dlnatreefolder.web.lang.messages");
	private String rootFolderName = "root";
	private WebFolderResource resource;

	/** Holds only the project version. It's used to always use the maven build number in code */
	private static final PmsProperties properties = new PmsProperties();
	static {
		try {
			properties.loadFromResourceFile("/webfolderplugin.properties", WebFolderPlugin.class);
		} catch (IOException e) {
			log.error("Could not load itunesfolderplugin.properties", e);
		}
	}
	
	/** The instance configuration is shared amongst all plugin instances. */
	private InstanceConfiguration instanceConfig;

	/** GUI */
	private InstanceConfigurationPanel pInstanceConfiguration;
	
	@Override
	public JPanel getInstanceConfigurationPanel() {
		//make sure the instance configuration has been initialized;
		if(instanceConfig == null) {
			instanceConfig = new InstanceConfiguration();
		}
		
		//lazy initialize the configuration panel
		if(pInstanceConfiguration == null ) {
			if(instanceConfig.getFilePath() == null || !new File(instanceConfig.getFilePath()).exists()) {
				String profileDir = PMS.getConfiguration().getProfileDirectory();
				String defaultWebConf = profileDir + File.separatorChar + "web.conf";
				if(new File(defaultWebConf).exists()) {
					instanceConfig.setFilePath(defaultWebConf);
				}			
			}
			
			pInstanceConfiguration = new InstanceConfigurationPanel(instanceConfig.getFilePath());
		}
		pInstanceConfiguration.applyConfig();
		
		return pInstanceConfiguration;
	}

	@Override
	public DLNAResource getDLNAResource() {
		if( instanceConfig == null) {
			return null;
		}
		
		if(resource == null){
			resource = new WebFolderResource(rootFolderName, instanceConfig.getFilePath());
			resource.discoverChildren();
		}
		
		return resource;
	}

	@Override
	public Icon getTreeNodeIcon() {
		return new ImageIcon(getClass().getResource("/webfolder-16.png"));
	}

	@Override
	public String getName() {
		return messages.getString("WebFolderPlugin.Name");
	}

	@Override
	public void setDisplayName(String name) {
		rootFolderName = name;
	}

	@Override
	public void loadInstanceConfiguration(String configFilePath) throws IOException {
		instanceConfig = new InstanceConfiguration();
		instanceConfig.load(configFilePath);
	}

	@Override
	public void saveInstanceConfiguration(String configFilePath) throws IOException {
		if(pInstanceConfiguration != null) {
			pInstanceConfiguration.updateConfiguration(instanceConfig);
			instanceConfig.save(configFilePath);
		}
	}

	@Override
	public String toString() {
		return getName();
	}

	@Override
    public boolean isInstanceAvailable() {
	    return true;
    }

	@Override
	public String getVersion() {
		return properties.get("project.version");
	}

	@Override
	public String getShortDescription() {
		return messages.getString("WebFolderPlugin.ShortDescription");
	}

	@Override
	public String getLongDescription() {
		return messages.getString("WebFolderPlugin.LongDescription");
	}

	@Override
	public void shutdown() {
		// do nothing
	}

	@Override
	public JComponent getGlobalConfigurationPanel() {
		return null;
	}

	@Override
	public Icon getPluginIcon() {
		return new ImageIcon(getClass().getResource("/webfolder-32.png"));
	}

	@Override
	public String getUpdateUrl() {
		return null;
	}

	@Override
	public String getWebSiteUrl() {
		return "http://www.ps3mediaserver.org/";
	}

	@Override
	public void initialize() {
	}

	@Override
	public void saveConfiguration() {
	}

	@Override
	public boolean isPluginAvailable() {
		return true;
	}
}
