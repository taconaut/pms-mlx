/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2008  A.Brochard
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package net.pms;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.BindException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.LogManager;

import javax.swing.JOptionPane;

import net.pms.api.PmsCore;
import net.pms.configuration.Build;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.di.PmsGuice;
import net.pms.dlna.DLNAMediaDatabase;
import net.pms.dlna.virtual.MediaLibrary;
import net.pms.encoders.PlayerFactory;
import net.pms.gui.DummyFrame;
import net.pms.gui.IFrame;
import net.pms.io.BasicSystemUtils;
import net.pms.io.MacSystemUtils;
import net.pms.io.OutputParams;
import net.pms.io.OutputTextConsumer;
import net.pms.io.ProcessWrapperImpl;
import net.pms.io.SolarisUtils;
import net.pms.io.SystemUtils;
import net.pms.io.WinUtils;
import net.pms.logging.LoggingConfigFileLoader;
import net.pms.medialibrary.commons.MediaLibraryConfiguration;
import net.pms.medialibrary.dlna.RootFolder;
import net.pms.medialibrary.scanner.FullDataCollector;
import net.pms.medialibrary.storage.MediaLibraryStorage;
import net.pms.network.HTTPServer;
import net.pms.network.NetworkConfiguration;
import net.pms.network.ProxyServer;
import net.pms.network.UPNPHelper;
import net.pms.newgui.LooksFrame;
import net.pms.plugins.PluginsFactory;
import net.pms.plugins.notifications.StartStopNotifier;
import net.pms.update.AutoUpdater;
import net.pms.util.ProcessUtil;
import net.pms.util.PropertiesUtil;
import net.pms.util.SystemErrWrapper;
import net.pms.util.TaskRunner;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.event.ConfigurationEvent;
import org.apache.commons.configuration.event.ConfigurationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Platform;

public class PmsCoreImpl implements PmsCore {
	private static final String CONSOLE = "console";

	// (innot): The logger used for all logging.
	private static final Logger logger = LoggerFactory.getLogger(PmsCoreImpl.class);

	// TODO(tcox):  This shouldn't be static
	private static PmsConfiguration configuration;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IFrame getFrame() {
		return frame;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public RootFolder getRootFolder(RendererConfiguration renderer) {
		// something to do here for multiple directories views for each renderer
		if (renderer == null) {
			renderer = RendererConfiguration.getDefaultConf();
		}
		return renderer.getRootFolder();
	}

	/**
	 * Pointer to a running PMS server.
	 */
	private static PmsCoreImpl instance = null;

	/**
	 * Array of {@link RendererConfiguration} that have been found by PMS.
	 */
	private final ArrayList<RendererConfiguration> foundRenderers = new ArrayList<RendererConfiguration>();

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setRendererfound(final RendererConfiguration mediarenderer) {
		if (!foundRenderers.contains(mediarenderer) && !mediarenderer.isFDSSDP()) {
			foundRenderers.add(mediarenderer);
			frame.addRendererIcon(mediarenderer.getRank(), mediarenderer.getRendererName(), mediarenderer.getRendererIcon());
			frame.setStatusCode(0, Messages.getString("PMS.18"), "apply-220.png");
		}
	}

	/**
	 * HTTP server that serves the XML files needed by UPnP server and the media files.
	 */
	private HTTPServer server;

	/**
	 * User friendly name for the server.
	 */
	private String serverName;

	private ProxyServer proxyServer;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ProxyServer getProxy() {
		return proxyServer;
	}

	public ArrayList<Process> currentProcesses = new ArrayList<Process>();

	/**
	 * {@link IFrame} object that represents PMS GUI.
	 */
	IFrame frame;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isWindows() {
		return Platform.isWindows();
	}

	private int proxy;

	/**Interface to Windows specific functions, like Windows Registry. registry is set by {@link #init()}.
	 * @see WinUtils
	 */
	private SystemUtils registry;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SystemUtils getRegistry() {
		return registry;
	}

	/**Executes a new Process and creates a fork that waits for its results.
	 * TODO:Extend explanation on where this is being used.
	 * @param name Symbolic name for the process to be launched, only used in the trace log
	 * @param error (boolean) Set to true if you want PMS to add error messages to the trace pane
	 * @param workDir (File) optional working directory to run the process in
	 * @param params (array of Strings) array containing the command to call and its arguments
	 * @return Returns true if the command exited as expected
	 * @throws Exception TODO: Check which exceptions to use
	 */
	private boolean checkProcessExistence(final String name, final boolean error, final File workDir, final String... params) throws Exception {
		logger.debug("launching: " + params[0]);

		try {
			final ProcessBuilder pb = new ProcessBuilder(params);
			if (workDir != null) {
				pb.directory(workDir);
			}
			final Process process = pb.start();

			final OutputTextConsumer stderrConsumer = new OutputTextConsumer(process.getErrorStream(), false);
			stderrConsumer.start();

			final OutputTextConsumer outConsumer = new OutputTextConsumer(process.getInputStream(), false);
			outConsumer.start();

			final Runnable r = new Runnable() {
				@Override
				public void run() {
					ProcessUtil.waitFor(process);
				}
			};

			Thread checkThread = new Thread(r, "PMS Checker");
			checkThread.start();
			checkThread.join(60000);
			checkThread.interrupt();
			checkThread = null;

			// XXX no longer used
			if (params[0].equals("vlc") && stderrConsumer.getResults().get(0).startsWith("VLC")) {
				return true;
			}

			// XXX no longer used
			if (params[0].equals("ffmpeg") && stderrConsumer.getResults().get(0).startsWith("FF")) {
				return true;
			}

			final int exit = process.exitValue();
			if (exit != 0) {
				if (error) {
					logger.info("[" + exit + "] Cannot launch " + name + " / Check the presence of " + params[0] + " ...");
				}
				return false;
			}
			return true;
		} catch (final Exception e) {
			if (error) {
				logger.error("Cannot launch " + name + " / Check the presence of " + params[0] + " ...", e);
			}
			return false;
		}
	}

	/**
	 * @see System#err
	 */
	@SuppressWarnings("unused")
	private final PrintStream stderr = System.err;

	/**Main resource database that supports search capabilities. Also known as media cache.
	 * @see DLNAMediaDatabase
	 */
	private DLNAMediaDatabase database;

	private void initializeDatabase() {
		database = new DLNAMediaDatabase("medias");
		database.init(false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized DLNAMediaDatabase getDatabase() {
		return database;
	}

	/**Initialisation procedure for PMS.
	 * @return true if the server has been initialized correctly. false if the server could
	 * not be set to listen on the UPnP port.
	 * @throws Exception
	 */
	private boolean init() throws Exception {
		AutoUpdater autoUpdater = null;

		if (Build.isUpdatable()) {
			final String serverURL = Build.getUpdateServerURL();
			autoUpdater = new AutoUpdater(serverURL, getVersion());
		}

		registry = createSystemUtils();

		RendererConfiguration.loadRendererConfigurations(configuration);

		//initialize plugins early
		PluginsFactory.lookup();

		initMediaLibrary();

		if (System.getProperty(CONSOLE) == null) {
			frame = new LooksFrame(autoUpdater, configuration);
		} else {
			logger.info("GUI environment not available");
			logger.info("Switching to console mode");
			frame = new DummyFrame();
		}
		configuration.addConfigurationListener(new ConfigurationListener() {
			@Override
			public void configurationChanged(final ConfigurationEvent event) {
				if ((!event.isBeforeUpdate())
						&& PmsConfiguration.NEED_RELOAD_FLAGS.contains(event.getPropertyName())) {
					frame.setReloadable(true);
				}
			}
		});

		frame.setStatusCode(0, Messages.getString("PMS.130"), "connect_no-220.png");
		proxy = -1;

		logger.info("Starting " + PropertiesUtil.getProjectProperties().get("project.name") + " " + getVersion());
		logger.info("by shagrath / 2008-2012");
		logger.info("http://ps3mediaserver.org");
		logger.info("https://github.com/ps3mediaserver/ps3mediaserver");
		logger.info("http://ps3mediaserver.blogspot.com");
		logger.info("");

		final String commitId = PropertiesUtil.getProjectProperties().get("git.commit.id");
		final String commitTime = PropertiesUtil.getProjectProperties().get("git.commit.time");
		final String shortCommitId = commitId.substring(0,  9);

		logger.info("Build: " + shortCommitId + " (" + commitTime + ")");

		// Log system properties
		logSystemInfo();

		final String cwd = new File("").getAbsolutePath();
		logger.info("Working directory: " + cwd);

		logger.info("Temp folder: " + configuration.getTempFolder());
		logger.info("Logging config file: " + LoggingConfigFileLoader.getConfigFilePath());

		final HashMap<String, String> lfps = LoggingConfigFileLoader.getLogFilePaths();

		if (lfps != null && lfps.size() > 0) {
			if (lfps.size() == 1) {
				final Entry<String, String> entry = lfps.entrySet().iterator().next();
				logger.info(String.format("%s: %s", entry.getKey(), entry.getValue()));
			} else {
				logger.info("Logging to multiple files:");
				final Iterator<Entry<String, String>> logsIterator = lfps.entrySet().iterator();
				Entry<String, String> entry;
				while (logsIterator.hasNext()) {
					entry = logsIterator.next();
					logger.info(String.format("%s: %s", entry.getKey(), entry.getValue()));
				}
			}
		}

		logger.info("");

		logger.info("Profile directory: " + configuration.getProfileDirectory());
		final String profilePath = configuration.getProfilePath();
		logger.info("Profile path: " + profilePath);

		final File profileFile = new File(profilePath);

		if (profileFile.exists()) {
			final String status = String.format("%s%s",
				profileFile.canRead()  ? "r" : "-",
				profileFile.canWrite() ? "w" : "-"
			);
			logger.info("Profile status: " + status);
		} else {
			logger.info("Profile status: no such file");
		}

		logger.info("Profile name: " + configuration.getProfileName());
		logger.info("");

		logger.info("Checking MPlayer font cache. It can take a minute or so.");
		checkProcessExistence("MPlayer", true, null, configuration.getMplayerPath(), "dummy");
		if (isWindows()) {
			checkProcessExistence("MPlayer", true, configuration.getTempFolder(), configuration.getMplayerPath(), "dummy");
		}
		logger.info("Done!");

		// check the existence of Vsfilter.dll
		if (registry.isAvis() && registry.getAvsPluginsDir() != null) {
			logger.info("Found AviSynth plugins dir: " + registry.getAvsPluginsDir().getAbsolutePath());
			final File vsFilterdll = new File(registry.getAvsPluginsDir(), "VSFilter.dll");
			if (!vsFilterdll.exists()) {
				logger.info("VSFilter.dll is not in the AviSynth plugins directory. This can cause problems when trying to play subtitled videos with AviSynth");
			}
		}

		if (registry.getVlcv() != null && registry.getVlcp() != null) {
			logger.info("Found VideoLAN version " + registry.getVlcv() + " at: " + registry.getVlcp());
		}

		//check if Kerio is installed
		if (registry.isKerioFirewall()) {
			logger.info("Detected Kerio firewall");
		}

		// force use of specific dvr ms muxer when it's installed in the right place
		final File dvrsMsffmpegmuxer = new File("win32/dvrms/ffmpeg_MPGMUX.exe");
		if (dvrsMsffmpegmuxer.exists()) {
			configuration.setFfmpegAlternativePath(dvrsMsffmpegmuxer.getAbsolutePath());
		}

		// disable jaudiotagger logging
		LogManager.getLogManager().readConfiguration(new ByteArrayInputStream("org.jaudiotagger.level=OFF".getBytes()));

		// wrap System.err
		System.setErr(new PrintStream(new SystemErrWrapper(), true));

		server = new HTTPServer(configuration.getServerPort());

		// Initialize a player factory to register all players
		PlayerFactory.initialize(configuration);

		// Add registered player engines
		frame.addEngines();

		// Initialize the plugins before starting the server
		PluginsFactory.initializePlugins();

		boolean binding = false;
		try {
			binding = server.start();
		} catch (final BindException b) {
			logger.info("FATAL ERROR: Unable to bind on port: " + configuration.getServerPort() + ", because: " + b.getMessage());
			logger.info("Maybe another process is running or the hostname is wrong.");
		}

		new Thread("Connection Checker") {
			@Override
			public void run() {
				try {
					Thread.sleep(7000);
				} catch (final InterruptedException e) {
				}
				if (foundRenderers.isEmpty()) {
					frame.setStatusCode(0, Messages.getString("PMS.0"), "messagebox_critical-220.png");
				} else {
					frame.setStatusCode(0, Messages.getString("PMS.18"), "apply-220.png");
				}
			}
		}.start();

		if (!binding) {
			return false;
		}

		if (proxy > 0) {
			logger.info("Starting HTTP Proxy Server on port: " + proxy);
			proxyServer = new ProxyServer(proxy);
		}

		// initialize the cache
		if (configuration.getUseCache()) {
			initializeDatabase(); // XXX: this must be done *before* new MediaLibrary -> new MediaLibraryFolder
			mediaLibrary = new MediaLibrary();
			logger.info("A tiny cache admin interface is available at: http://" + server.getHost() + ":" + server.getPort() + "/console/home");
		}

		// XXX: this must be called:
		//     a) *after* loading plugins i.e. plugins register root folders then RootFolder.discoverChildren adds them
		//     b) *after* mediaLibrary is initialized, if enabled (above)
		getRootFolder(RendererConfiguration.getDefaultConf());

		StartStopNotifier.initialize();

		frame.serverReady();

		//UPNPHelper.sendByeBye();
		Runtime.getRuntime().addShutdownHook(new Thread("PMS Listeners Stopper") {
			@Override
			public void run() {
				try {
					PluginsFactory.shutdownPlugins();

					UPNPHelper.shutDownListener();
					UPNPHelper.sendByeBye();
					logger.debug("Forcing shutdown of all active processes");
					for (final Process p : currentProcesses) {
						try {
							p.exitValue();
						} catch (final IllegalThreadStateException ise) {
							logger.trace("Forcing shutdown of process: " + p);
							ProcessUtil.destroy(p);
						}
					}
					get().getServer().stop();
					Thread.sleep(500);
				} catch (final IOException e) {
					logger.debug("Caught exception", e);
				} catch (final InterruptedException e) {
					logger.debug("Caught exception", e);
				}
			}
		});

		UPNPHelper.sendAlive();
		logger.trace("Waiting 250 milliseconds...");
		Thread.sleep(250);
		UPNPHelper.listen();

		return true;
	}

	private void initMediaLibrary() {
		//Initialize media library (only let pms start if the media library is working)
		MediaLibraryStorage.configure("pms_media_library.db");
	    if (!MediaLibraryStorage.getInstance().isFunctional()) {
	        logger.error("Failed to properly initialize MediaLibraryStorage");
	        JOptionPane.showMessageDialog(null, Messages.getString("PMS.100"), Messages.getString("PMS.101"), JOptionPane.ERROR_MESSAGE);
	        System.exit(1);
	    }

	    final String picFolderPath = MediaLibraryConfiguration.getInstance().getPictureSaveFolderPath();
		FullDataCollector.configure(picFolderPath);

		//delete thumbnails for every session
		final File thumbFolder = new File(picFolderPath + "thumbnails");
		if(!thumbFolder.isDirectory()){
			thumbFolder.mkdirs();
		} else {
			for(final File f : thumbFolder.listFiles()){
				f.delete();
			}
		}
    }

	private MediaLibrary mediaLibrary;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public MediaLibrary getLibrary() {
		return mediaLibrary;
	}

	private SystemUtils createSystemUtils() {
		if (Platform.isWindows()) {
			return new WinUtils();
		} else {
			if (Platform.isMac()) {
				return new MacSystemUtils();
			} else {
				if (Platform.isSolaris()) {
					return new SolarisUtils();
				} else {
					return new BasicSystemUtils();
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean installWin32Service() {
		logger.info(Messages.getString("PMS.41"));
		String cmdArray[] = new String[]{"win32/service/wrapper.exe", "-r", "wrapper.conf"};
		final OutputParams output = new OutputParams(configuration);
		output.noexitcheck = true;
		final ProcessWrapperImpl pwuninstall = new ProcessWrapperImpl(cmdArray, output);
		pwuninstall.runInSameThread();
		cmdArray = new String[]{"win32/service/wrapper.exe", "-i", "wrapper.conf"};
		final ProcessWrapperImpl pwinstall = new ProcessWrapperImpl(cmdArray, new OutputParams(configuration));
		pwinstall.runInSameThread();
		return pwinstall.isSuccess();
	}

	/**
	 * {@inheritDoc}
	 */
	// this is called *way* too often (e.g. a dozen times with 1 renderer and 1 shared folder),
	// so log it by default so we can fix it.
	// BUT it's also called when the GUI is initialized (to populate the list of shared folders),
	// and we don't want this message to appear *before* the PMS banner, so allow that call to suppress logging
	@Override
	public File[] getFoldersConf(final boolean log) {
		final String folders = getConfiguration().getFolders();
		if (folders == null || folders.length() == 0) {
			return null;
		}
		final ArrayList<File> directories = new ArrayList<File>();
		final String[] foldersArray = folders.split(",");
		for (String folder : foldersArray) {
			// unescape embedded commas. note: backslashing isn't safe as it conflicts with
			// Windows path separators:
			// http://ps3mediaserver.org/forum/viewtopic.php?f=14&t=8883&start=250#p43520
			folder = folder.replaceAll("&comma;", ",");
			if (log) {
				logger.info("Checking shared folder: " + folder);
			}
			final File file = new File(folder);
			if (file.exists()) {
				if (!file.isDirectory()) {
					logger.warn("The file " + folder + " is not a directory! Please remove it from your Shared folders list on the Navigation/Share Settings tab");
				}
			} else {
				logger.warn("The directory " + folder + " does not exist. Please remove it from your Shared folders list on the Navigation/Share Settings tab");
			}

			// add the file even if there are problems so that the user can update the shared folders as required.
			directories.add(file);
		}
		final File f[] = new File[directories.size()];
		directories.toArray(f);
		return f;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public File[] getFoldersConf() {
		return getFoldersConf(true);
	}

	/**
	 * {@inheritDoc}
	 */
	// XXX: don't try to optimize this by reusing the same server instance.
	// see the comment above HTTPServer.stop()
	@Override
	public void reset() {
		TaskRunner.getInstance().submitNamed("restart", true, new Runnable() {
			@Override
			public void run() {
				try {
					logger.trace("Waiting 1 second...");
					UPNPHelper.sendByeBye();
					server.stop();
					server = null;
					RendererConfiguration.resetAllRenderers();
					try {
						Thread.sleep(1000);
					} catch (final InterruptedException e) {
						logger.trace("Caught exception", e);
					}
					server = new HTTPServer(configuration.getServerPort());
					server.start();
					UPNPHelper.sendAlive();
					frame.setReloadable(false);
				} catch (final IOException e) {
					logger.error("error during restart :" +e.getMessage(), e);
				}
			}
		});
	}

	// Cannot remove these methods because of backwards compatibility;
	// none of the PMS code uses it, but some plugins still do.

	/**
	 * Universally Unique Identifier used in the UPnP server.
	 */
	private String uuid;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String usn() {
		if (uuid == null) {
			//retrieve UUID from configuration
			uuid = getConfiguration().getUuid();

			if (uuid == null) {
				//create a new UUID based on the MAC address of the used network adapter
				NetworkInterface ni = null;
				try {
					ni = NetworkConfiguration.getInstance().getNetworkInterfaceByServerName();
					// if no ni comes from the server host name, we should get the default.
					if (ni != null) {
						ni = get().getServer().getNi();
					}

					if (ni != null) {
						final byte[] addr = getRegistry().getHardwareAddress(ni); // return null when java.net.preferIPv4Stack=true
						if (addr != null) {
							uuid = UUID.nameUUIDFromBytes(addr).toString();
							logger.info(String.format("Generated new UUID based on the MAC address of the network adapter '%s'", ni.getDisplayName()));
						}
					}
				} catch (final SocketException e) {
					logger.debug("Caught exception", e);
				} catch (final UnknownHostException e) {
					logger.debug("Caught exception", e);
				}

				//create random UUID if the generation by MAC address failed
				if (uuid == null) {
					uuid = UUID.randomUUID().toString();
					logger.info("Generated new random UUID");
				}

				//save the newly generated UUID
				getConfiguration().setUuid(uuid);
				try {
					getConfiguration().save();
				} catch (final ConfigurationException e) {
					logger.error("Failed to save configuration with new UUID", e);
				}
			}

			logger.info("Using the following UUID configured in PMS.conf: " + uuid);
		}
		return "uuid:" + uuid;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getServerName() {
		if (serverName == null) {
			final StringBuilder sb = new StringBuilder();
			sb.append(System.getProperty("os.name").replace(" ", "_"));
			sb.append("-");
			sb.append(System.getProperty("os.arch").replace(" ", "_"));
			sb.append("-");
			sb.append(System.getProperty("os.version").replace(" ", "_"));
			sb.append(", UPnP/1.0, PMS/" + getVersion());
			serverName = sb.toString();
		}
		return serverName;
	}

	/**
	 * Returns the PMS instance.
	 * 
	 * @return {@link PmsCoreImpl}
	 */
	public static PmsCore get() {
		// XXX when PMS is run as an application, the instance is initialized via the createInstance call in main().
		// However, plugin tests may need access to a PMS instance without going
		// to the trouble of launching the PMS application, so we provide a fallback
		// initialization here. Either way, createInstance() should only be called once (see below)
		if (instance == null) {
			createInstance();
		}

		return instance;
	}

	protected synchronized static void createInstance() {
		assert instance == null; // this should only be called once
		instance = new PmsCoreImpl();

		new PmsGuice(); // init the Injector

		try {
			if (instance.init()) {
				logger.info("The server should now appear on your renderer");
			} else {
				logger.error("A serious error occurred during PMS init");
			}
		} catch (final Exception e) {
			logger.error("A serious error occurred during PMS init", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public HTTPServer getServer() {
		return server;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void save() {
		try {
			configuration.save();
		} catch (final ConfigurationException e) {
			logger.error("Could not save configuration", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void storeFileInCache(final File file, final int formatType) {
		if (getConfiguration().getUseCache()
				&& !getDatabase().isDataExists(file.getAbsolutePath(), file.lastModified())) {

			getDatabase().insertData(file.getAbsolutePath(), file.lastModified(), formatType, null);
		}
	}

	/**
	 * Retrieves the {@link net.pms.configuration.PmsConfiguration PmsConfiguration} object
	 * that contains all configured settings for PMS. The object provides getters for all
	 * configurable PMS settings.
	 *
	 * @return The configuration object
	 */
	public static PmsConfiguration getConfiguration() {
		return configuration;
	}

	/**
	 * {@inheritDoc}
	 */
	public static void setConfiguration(final PmsConfiguration conf) {
		configuration = conf;
	}

	/**
	 * {@inheritDoc}
	 */
	public static String getVersion() {
		return PropertiesUtil.getProjectProperties().get("project.version");
	}

	/**
	 * Log system properties identifying Java, the OS and encoding and log
	 * warnings where appropriate.
	 */
	private void logSystemInfo() {
		logger.info("Java: " + System.getProperty("java.version") + "-" + System.getProperty("java.vendor"));
		logger.info("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.arch") + " "
				+ System.getProperty("os.version"));
		logger.info("Encoding: " + System.getProperty("file.encoding"));
		logger.info("");

		if (Platform.isMac()) {
			// The binaries shipped with the Mac OS X version of PMS are being
			// compiled against specific OS versions, making them incompatible
			// with older versions. Warn the user about this when necessary.
			final String osVersion = System.getProperty("os.version");

			// Split takes a regular expression, so escape the dot.
			final String[] versionNumbers = osVersion.split("\\.");

			if (versionNumbers.length > 1) {
				try {
					final int osVersionMinor = Integer.parseInt(versionNumbers[1]);

					if (osVersionMinor < 6) {
						logger.warn("-----------------------------------------------------------------");
						logger.warn("WARNING!");
						logger.warn("PMS ships with binaries compiled for Mac OS X 10.6 or higher.");
						logger.warn("You are running an older version of Mac OS X so PMS may not work!");
						logger.warn("More information in the FAQ:");
						logger.warn("http://www.ps3mediaserver.org/forum/viewtopic.php?f=6&t=3507&p=66371#p66371");
						logger.warn("-----------------------------------------------------------------");
						logger.warn("");
					}
				} catch (final NumberFormatException e) {
					logger.debug("Cannot parse minor os.version number");
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public ArrayList<Process> getCurrentProcesses() {
		return currentProcesses;
	}
}