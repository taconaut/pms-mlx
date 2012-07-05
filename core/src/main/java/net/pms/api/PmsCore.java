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
package net.pms.api;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.DLNAMediaDatabase;
import net.pms.dlna.virtual.MediaLibrary;
import net.pms.gui.IFrame;
import net.pms.io.SystemUtils;
import net.pms.io.WinUtils;
import net.pms.medialibrary.dlna.RootFolder;
import net.pms.network.HTTPServer;
import net.pms.network.ProxyServer;

import com.sun.jna.Platform;

public interface PmsCore {

	public static final String AVS_SEPARATOR = "\1";

	/**
	 * Returns a pointer to the main PMS GUI.
	 * 
	 * @return {@link IFrame} Main PMS window.
	 */
	public abstract IFrame getFrame();

	/**
	 * getRootFolder returns the Root Folder for a given renderer. There could
	 * be the case where a given media renderer needs a different root
	 * structure.
	 * 
	 * @param renderer
	 *            {@link RendererConfiguration} is the renderer for which to get
	 *            the RootFolder structure. If <b>null</b>, then the default
	 *            renderer is used.
	 * @return {@link RootFolder} The root folder structure for a given renderer
	 */
	public abstract RootFolder getRootFolder(RendererConfiguration renderer);

	/**
	 * Adds a {@link RendererConfiguration} to the list of media renderers
	 * found. The list is being used, for example, to give the user a graphical
	 * representation of the found media renderers.
	 * 
	 * @param mediarenderer
	 *            {@link RendererConfiguration}
	 */
	public abstract void setRendererfound(RendererConfiguration mediarenderer);

	public abstract ProxyServer getProxy();

	/**
	 * @see Platform#isWindows()
	 */
	public abstract boolean isWindows();

	/**
	 * @see WinUtils
	 */
	public abstract SystemUtils getRegistry();

	/**
	 * Used to get the database. Needed in the case of the Xbox 360, that
	 * requires a database. for its queries.
	 * 
	 * @return (DLNAMediaDatabase) a reference to the database instance or
	 *         <b>null</b> if one isn't defined (e.g. if the cache is disabled).
	 */
	public abstract DLNAMediaDatabase getDatabase();

	/**
	 * Returns the MediaLibrary used by PMS.
	 * 
	 * @return (MediaLibrary) Used mediaLibrary, if any. null if none is in use.
	 */
	public abstract MediaLibrary getLibrary();

	/**
	 * Executes the needed commands in order to make PMS a Windows service that
	 * starts whenever the machine is started. This function is called from the
	 * Network tab.
	 * 
	 * @return true if PMS could be installed as a Windows service.
	 * @see GeneralTab#build()
	 */
	public abstract boolean installWin32Service();

	/**
	 * Transforms a comma separated list of directory entries into an array of
	 * {@link String}. Checks that the directory exists and is a valid
	 * directory.
	 * 
	 * @param log
	 *            whether to output log information
	 * @return {@link File}[] Array of directories.
	 * @throws IOException
	 */
	public abstract File[] getFoldersConf(boolean log);

	public abstract File[] getFoldersConf();

	/**
	 * Restarts the server. The trigger is either a button on the main PMS
	 * window or via an action item.
	 * 
	 * @throws IOException
	 */
	public abstract void reset();

	/**
	 * Creates a new {@link #uuid} for the UPnP server to use. Tries to follow
	 * the RFCs for creating the UUID based on the link MAC address. Defaults to
	 * a random one if that method is not available.
	 * 
	 * @return {@link String} with an Universally Unique Identifier.
	 */
	public abstract String usn();

	/**
	 * Returns the user friendly name of the UPnP server.
	 * 
	 * @return {@link String} with the user friendly name.
	 */
	public abstract String getServerName();

	public abstract HTTPServer getServer();

	public abstract void save();

	public abstract void storeFileInCache(File file, int formatType);

	/**
	 * Returns the list of active processes. This method returns the list as a
	 * reference that can be used to append and remove processes. PMS will
	 * clean up the list of remaining active processes when it terminates.
	 *
	 * @return The list of current processes
	 */
	public ArrayList<Process> getCurrentProcesses(); 
}