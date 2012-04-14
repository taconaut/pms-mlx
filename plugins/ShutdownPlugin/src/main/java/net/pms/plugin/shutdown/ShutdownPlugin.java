/*
 * Shutdown Plugin for PS3 Media Server
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
package net.pms.plugin.shutdown;

import java.io.IOException;
import java.util.ResourceBundle;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.tree.MutableTreeNode;

import net.pms.dlna.DLNAResource;
import net.pms.dlna.virtual.VirtualFolder;
import net.pms.dlna.virtual.VirtualVideoAction;
import net.pms.io.Gob;
import net.pms.medialibrary.external.DlnaTreeFolderPlugin;
import net.pms.util.ProcessUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Platform;

/**
 * This class implements a computer shutdown plugin for PS3 Media Server.
 */
public class ShutdownPlugin implements DlnaTreeFolderPlugin {
	/**
	 * Logger for writing messages to the log file.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(ShutdownPlugin.class);

	/**
	 * Resource bundle that holds the locale dependent messages.
	 */
	private static final ResourceBundle MESSAGES = ResourceBundle.getBundle("messages");

	/**
	 * Resource bundle that holds the project properties.
	 */
	private static final ResourceBundle PROPERTIES = ResourceBundle.getBundle("project");

	/**
	 * Constructor for the plugin.
	 */
	public ShutdownPlugin() {
		LOG.trace("Initializing shutdown plugin");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void shutdown() {
		LOG.trace("Shutting down shutdown plugin");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DLNAResource getDLNAResource() {
		// Create computer shutdown folder.
		DLNAResource shutdownFolder = new VirtualFolder(MESSAGES.getString("menu.foldername"), null);

		// Add power off menu item
		shutdownFolder.addChild(getPowerOffAction());

		// Add restart menu item
		shutdownFolder.addChild(getRestartAction());

		return shutdownFolder;
	}

	/**
	 * Constructs and returns the virtual video action to power off the computer.
	 * @return The virtual video action.
	 */
	private DLNAResource getPowerOffAction() {
		DLNAResource action = new VirtualVideoAction(MESSAGES.getString("menu.poweroff"), true) {
			/**
			 * This method is called when the user selects the menu item.
			 * @return Always returns true.
			 */
			@Override
			public boolean enable() {
				ProcessBuilder pb;
				CommandUtils utils;

		    	LOG.info("Attempting to shut down the computer.");

		        if (Platform.isWindows()) {
		        	utils = new WindowsCommandUtils();
		        } else {
		        	utils = new LinuxCommandUtils();
		        }

		        pb = new ProcessBuilder(utils.getPowerOffCommand());
		        pb.redirectErrorStream(true);

				try {
					// Start the command process
					Process process = pb.start();

					// Capture the output, but discard it.
					new Gob(process.getErrorStream()).start();
					new Gob(process.getInputStream()).start();

					// Interesting conundrum: waiting for the power off to finish?
					// I guess we're waiting for it to fail to finish. ;-)
					ProcessUtil.waitFor(process);

					if (process.exitValue() != 0) {
						LOG.error("Failed to execute power off command.");
					}
				} catch (IOException e) {
					LOG.error("Failed to execute power off command.");
				}

				return true;

			}
		};

		return action;
	}

	/**
	 * Constructs and returns the virtual video action to restart the computer.
	 * @return The virtual video action.
	 */
	private DLNAResource getRestartAction() {
		DLNAResource action = new VirtualVideoAction(MESSAGES.getString("menu.restart"), true) {
			/**
			 * This method is called when the user selects the menu item.
			 * @return Always returns true.
			 */
			@Override
			public boolean enable() {
				ProcessBuilder pb;
				CommandUtils utils;

		    	LOG.info("Attempting to restart the computer.");

		        if (Platform.isWindows()) {
		        	utils = new WindowsCommandUtils();
		        } else {
		        	utils = new LinuxCommandUtils();
		        }

		        pb = new ProcessBuilder(utils.getRestartCommand());
				pb.redirectErrorStream(true);

				try {
					// Start the command process
					Process process = pb.start();

					// Capture the output, but discard it.
					new Gob(process.getErrorStream()).start();
					new Gob(process.getInputStream()).start();

					// Interesting conundrum: waiting for the restart to finish?
					// I guess we're waiting for it to fail to finish. ;-)
					ProcessUtil.waitFor(process);

					if (process.exitValue() != 0) {
						LOG.error("Failed to execute restart command.");
					}
				} catch (IOException e) {
					LOG.error("Failed to execute restart command.");
				}
				return true;
			}
		};

		return action;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDescription() {
		return MESSAGES.getString("plugin.description");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public JComponent getGlobalConfigurationPanel() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getName() {
		return MESSAGES.getString("plugin.name");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getVersion() {
		return Integer.parseInt(PROPERTIES.getString("plugin.version"));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public JPanel getConfigurationPanel() {
		// Nothing to configure
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Icon getIcon() {
		ImageIcon res = new ImageIcon(getClass().getResource("/shutdown_icon.png"));
		return res;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public MutableTreeNode getTreeNode() {
		// No mutable tree node
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isAvailable() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void loadConfiguration(String arg0) throws IOException {
		// Do nothing
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void saveConfiguration(String arg0) throws IOException {
		// Do nothing
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setDisplayName(String arg0) {
		// Do nothing
	}
}
