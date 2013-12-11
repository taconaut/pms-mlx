package net.pms.plugin.dlnatreefolder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xmlwise.Plist;

import net.pms.dlna.DLNAResource;
import net.pms.dlna.RealFile;
import net.pms.dlna.virtual.VirtualFolder;
import net.pms.plugins.DlnaTreeFolderPlugin;

public class iPhotoFolderPlugin implements DlnaTreeFolderPlugin {
	private static final Logger logger = LoggerFactory.getLogger(iPhotoFolderPlugin.class);
	private Properties properties = new Properties();
	protected static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("net.pms.plugin.dlnatreefolder.iphoto.lang.messages");
	
	private String rootFolderName = "root";

	public iPhotoFolderPlugin() {
		loadProperties();
	}

	@Override
	public JPanel getInstanceConfigurationPanel() {
		return null;
	}

	@Override
	public DLNAResource getDLNAResource() {
		VirtualFolder iPhotoVirtualFolder = null;

			InputStream inputStream = null;

			try {
				// This command will show the XML files for recently opened iPhoto databases
				Process process = Runtime.getRuntime().exec("defaults read com.apple.iApps iPhotoRecentDatabases");
				inputStream = process.getInputStream();
				List<String> lines = IOUtils.readLines(inputStream);
				logger.debug("iPhotoRecentDatabases: {}", lines);

				if (lines.size() >= 2) {
					// we want the 2nd line
					String line = lines.get(1);

					// Remove extra spaces
					line = line.trim();

					// Remove quotes
					line = line.substring(1, line.length() - 1);

					URI uri = new URI(line);
					URL url = uri.toURL();
					File file = FileUtils.toFile(url);
					logger.debug("Resolved URL to file: {} -> {}", url, file.getAbsolutePath());

					// Load the properties XML file.
					Map<String, Object> iPhotoLib = Plist.load(file);

					// The list of all photos
					Map<?, ?> photoList = (Map<?, ?>) iPhotoLib.get("Master Image List");

					// The list of events (rolls)
					@SuppressWarnings("unchecked")
					List<Map<?, ?>> listOfRolls = (List<Map<?, ?>>) iPhotoLib.get("List of Rolls");

					iPhotoVirtualFolder = new VirtualFolder(rootFolderName, null);

					for (Map<?, ?> roll : listOfRolls) {
						Object rollName = roll.get("RollName");

						if (rollName != null) {
							VirtualFolder virtualFolder = new VirtualFolder(rollName.toString(), null);

							// List of photos in an event (roll)
							List<?> rollPhotos = (List<?>) roll.get("KeyList");

							for (Object photo : rollPhotos) {
								Map<?, ?> photoProperties = (Map<?, ?>) photoList.get(photo);

								if (photoProperties != null) {
									Object imagePath = photoProperties.get("ImagePath");

									if (imagePath != null) {
										RealFile realFile = new RealFile(new File(imagePath.toString()));
										virtualFolder.addChild(realFile);
									}
								}
							}

							iPhotoVirtualFolder.addChild(virtualFolder);
						}
					}
				} else {
					logger.info("iPhoto folder not found");
				}
			} catch (Exception e) {
				logger.error("Something went wrong with the iPhoto Library scan: ", e);
			} finally {
				IOUtils.closeQuietly(inputStream);
			}

		return iPhotoVirtualFolder;
	}

	@Override
	public String getName() {
		return "iPhoto";
	}
	
	@Override
	public void setDisplayName(String name){
		rootFolderName = name;
	}

	@Override
	public void loadInstanceConfiguration(String configFilePath) throws IOException {
		//do nothing
	}

	@Override
	public void saveInstanceConfiguration(String configFilePath) throws IOException {
		//do nothing
	}

	@Override
	public String toString() {
		return getName();
	}

	@Override
    public boolean isInstanceAvailable() {
	    return System.getProperty("os.name").toLowerCase().indexOf( "mac" ) >= 0;
    }

	@Override
	public boolean isPluginAvailable() {
	    return System.getProperty("os.name").toLowerCase().indexOf( "mac" ) >= 0;
	}

	@Override
	public String getVersion() {
		return properties.getProperty("project.version");
	}

	@Override
	public String getShortDescription() {
		return RESOURCE_BUNDLE.getString("iPhotoFolderPlugin.ShortDescription");
	}

	@Override
	public String getLongDescription() {
		return RESOURCE_BUNDLE.getString("iPhotoFolderPlugin.LongDescription");
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
		return new ImageIcon(getClass().getResource("/iphoto-32.png"));
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
	public Icon getTreeNodeIcon() {
		return new ImageIcon(getClass().getResource("/iphoto-16.png"));
	}
	
	/**
	 * Loads the properties from the plugin properties file
	 */
	private void loadProperties() {
		String fileName = "/iphotofolderplugin.properties";
		InputStream inputStream = getClass().getResourceAsStream(fileName);
		try {
			properties.load(inputStream);
		} catch (Exception e) {
			logger.error("Failed to load properties", e);
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					logger.error("Failed to properly close stream properties", e);
				}
			}
		}
	}
}
