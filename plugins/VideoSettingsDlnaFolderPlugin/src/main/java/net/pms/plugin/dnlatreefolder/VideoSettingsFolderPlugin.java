package net.pms.plugin.dnlatreefolder;

import java.io.IOException;
import java.util.ResourceBundle;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.apache.commons.configuration.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.virtual.VirtualFolder;
import net.pms.dlna.virtual.VirtualVideoAction;
import net.pms.plugins.DlnaTreeFolderPlugin;
import net.pms.util.PmsProperties;

public class VideoSettingsFolderPlugin implements DlnaTreeFolderPlugin {
	private static final Logger logger = LoggerFactory.getLogger(VideoSettingsFolderPlugin.class);
	public static final ResourceBundle messages = ResourceBundle.getBundle("net.pms.plugin.dnlatreefolder.vsfp.lang.messages");
	private String rootFolderName = "root";

	/** Holds only the project version. It's used to always use the maven build number in code */
	private static final PmsProperties properties = new PmsProperties();
	static {
		try {
			properties.loadFromResourceFile("/videosettingsfolderplugin.properties", VideoSettingsFolderPlugin.class);
		} catch (IOException e) {
			logger.error("Could not load videosettingsfolderplugin.properties", e);
		}
	}

	@Override
	public JPanel getInstanceConfigurationPanel() {
		return null;
	}

	@Override
	public DLNAResource getDLNAResource() {
		final PmsConfiguration configuration = PMS.getConfiguration();
		
		VirtualFolder vf = new VirtualFolder(rootFolderName, null);
		VirtualFolder vfSub = new VirtualFolder(Messages.getString("PMS.8"), null);
		vf.addChild(vfSub);

		vf.addChild(new VirtualVideoAction(Messages.getString("PMS.3"), configuration.isMencoderNoOutOfSync()) {
			@Override
			public boolean enable() {
				configuration.setMencoderNoOutOfSync(!configuration.isMencoderNoOutOfSync());
				return configuration.isMencoderNoOutOfSync();
			}
		});

		vf.addChild(new VirtualVideoAction("  !!-- Fix 23.976/25fps A/V Mismatch --!!", configuration.isFix25FPSAvMismatch()) {
			@Override
			public boolean enable() {
				configuration.setMencoderForceFps(!configuration.isFix25FPSAvMismatch());
				configuration.setFix25FPSAvMismatch(!configuration.isFix25FPSAvMismatch());
				return configuration.isFix25FPSAvMismatch();
			}
		});

		vf.addChild(new VirtualVideoAction(Messages.getString("PMS.4"), configuration.isMencoderYadif()) {
			@Override
			public boolean enable() {
				configuration.setMencoderYadif(!configuration.isMencoderYadif());

				return configuration.isMencoderYadif();
			}
		});

		vfSub.addChild(new VirtualVideoAction(Messages.getString("TrTab2.51"), configuration.isDisableSubtitles()) {
			@Override
			public boolean enable() {
				boolean oldValue = configuration.isDisableSubtitles();
				boolean newValue = !oldValue;
				configuration.setDisableSubtitles(newValue);
				return newValue;
			}
		});

		vfSub.addChild(new VirtualVideoAction(Messages.getString("PMS.6"), configuration.isAutoloadExternalSubtitles()) {
			@Override
			public boolean enable() {
				boolean oldValue = configuration.isAutoloadExternalSubtitles();
				boolean newValue = !oldValue;
				configuration.setAutoloadExternalSubtitles(newValue);
				return newValue;
			}
		});

		vfSub.addChild(new VirtualVideoAction(Messages.getString("MEncoderVideo.36"), configuration.isMencoderAssDefaultStyle()) {
			@Override
			public boolean enable() {
				boolean oldValue = configuration.isMencoderAssDefaultStyle();
				boolean newValue = !oldValue;
				configuration.setMencoderAssDefaultStyle(newValue);
				return newValue;
			}
		});

		vf.addChild(new VirtualVideoAction(Messages.getString("PMS.7"), configuration.getSkipLoopFilterEnabled()) {
			@Override
			public boolean enable() {
				configuration.setSkipLoopFilterEnabled(!configuration.getSkipLoopFilterEnabled());
				return configuration.getSkipLoopFilterEnabled();
			}
		});

		vf.addChild(new VirtualVideoAction(Messages.getString("TrTab2.28"), configuration.isAudioEmbedDtsInPcm()) {
			@Override
			public boolean enable() {
				configuration.setAudioEmbedDtsInPcm(!configuration.isAudioEmbedDtsInPcm());
				return configuration.isAudioEmbedDtsInPcm();
			}
		});

		vf.addChild(new VirtualVideoAction(Messages.getString("PMS.27"), true) {
			@Override
			public boolean enable() {
				try {
					configuration.save();
				} catch (ConfigurationException e) {
					logger.debug("Caught exception", e);
				}
				return true;
			}
		});

		vf.addChild(new VirtualVideoAction(Messages.getString("LooksFrame.12"), true) {
			@Override
			public boolean enable() {
				PMS.get().reset();
				return true;
			}
		});
		return vf;
	}

	@Override
	public Icon getTreeNodeIcon() {
		return new ImageIcon(getClass().getResource("/videosettingsfolder-16.png"));
	}

	@Override
	public String getName() {
		return messages.getString("VideoSettingsFolderPlugin.Name");
	}
	
	@Override
	public void setDisplayName(String name){
		rootFolderName = name;
	}

	@Override
	public void loadInstanceConfiguration(String configFilePath) throws IOException {
	}

	@Override
	public void saveInstanceConfiguration(String configFilePath) throws IOException {
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
		return messages.getString("VideoSettingsFolderPlugin.ShortDescription");
	}

	@Override
	public String getLongDescription() {
		return messages.getString("VideoSettingsFolderPlugin.LongDescription");
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
		return new ImageIcon(getClass().getResource("/videosettingsfolder-32.png"));
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
