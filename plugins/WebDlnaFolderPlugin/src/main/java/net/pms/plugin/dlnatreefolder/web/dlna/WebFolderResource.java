/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2013  Ph.Waeber
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
package net.pms.plugin.dlnatreefolder.web.dlna;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.pms.dlna.AudiosFeed;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.ImagesFeed;
import net.pms.dlna.VideosFeed;
import net.pms.dlna.WebAudioStream;
import net.pms.dlna.WebVideoStream;
import net.pms.dlna.virtual.VirtualFolder;

public class WebFolderResource extends VirtualFolder {
	private static final Logger logger = LoggerFactory.getLogger(WebFolderResource.class);
	private String webConfigFilepath;
	private String previousWebConfigFilehash;

	/**
	 * The Constructor.
	 *
	 * @param name the name to display for the folder
	 * @param webConfigFilepath the web config filepath
	 */
	public WebFolderResource(String name, String webConfigFilepath) {
		super(name, null);
		this.webConfigFilepath = webConfigFilepath;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.pms.dlna.DLNAResource#discoverChildren()
	 */
	@Override
	public void discoverChildren() {
		refreshChildren();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.pms.dlna.DLNAResource#isRefreshNeeded()
	 */
	@Override
	public boolean isRefreshNeeded() {
		return previousWebConfigFilehash == null
				|| !previousWebConfigFilehash.equals(getWebConfigFilehash());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.pms.dlna.DLNAResource#refreshChildren()
	 */
	@Override
	public boolean refreshChildren() {
		if (!isRefreshNeeded()) {
			logger.debug(String.format("No refresh needed, as the file contents haven't changed for '%s'", webConfigFilepath));
		} else {
			logger.debug(String.format("Start refresh for '%s'", webConfigFilepath));

			File webConf = new File(webConfigFilepath);
			if (!webConf.exists()) {
				logger.error(String.format("The specified configuration file does not exist '%s'", webConfigFilepath));
			} else {
				// Remove all existing children
				getChildren().clear();

				// Add new children according to the configuration
				FileReader fr = null;
				LineNumberReader lnr = null;
				try {
					fr = new FileReader(webConf);
					lnr = new LineNumberReader(fr);
					
					String line = null;
					while ((line = lnr.readLine()) != null) {
						line = line.trim();
						logger.debug("Line read: " + line);
						if (line.length() > 0 && !line.startsWith("#")
								&& line.indexOf("=") > -1) {
							String key = line.substring(0, line.indexOf("="));
							String value = line
									.substring(line.indexOf("=") + 1);
							String keys[] = parseFeedKey((String) key);
							try {
								if (keys[0].equals("imagefeed")
										|| keys[0].equals("audiofeed")
										|| keys[0].equals("videofeed")
										|| keys[0].equals("audiostream")
										|| keys[0].equals("videostream")) {

									String values[] = parseFeedValue((String) value);
									DLNAResource parent = null;
									if (keys[1] != null) {
										StringTokenizer st = new StringTokenizer(
												keys[1], ",");
										DLNAResource currentRoot = this;
										while (st.hasMoreTokens()) {
											String folder = st.nextToken();
											parent = currentRoot.searchByName(folder);
											if (parent == null) {
												parent = new VirtualFolder(folder, "");
												currentRoot.addChild(parent);
											}
											currentRoot = parent;
										}
									}
									if (parent == null) {
										parent = this;
									}

									if (keys[0].equals("imagefeed")) {
										parent.addChild(new ImagesFeed(values[0]));
									} else if (keys[0].equals("videofeed")) {
										parent.addChild(new VideosFeed(values[0]));
									} else if (keys[0].equals("audiofeed")) {
										parent.addChild(new AudiosFeed(values[0]));
									} else if (keys[0].equals("audiostream")) {
										parent.addChild(new WebAudioStream(values[0], values[1], values[2]));
									} else if (keys[0].equals("videostream")) {
										parent.addChild(new WebVideoStream(values[0], values[1], values[2]));
									}
								}

								// catch exception here and go with parsing
							} catch (ArrayIndexOutOfBoundsException e) {
								logger.error("Error in line " + lnr.getLineNumber() + " of file WEB.conf", e);
							}
						}
					}

					previousWebConfigFilehash = getWebConfigFilehash();

					logger.debug(String.format("Folders have been refreshed for WebFolder '%s'. File hash=%s", webConfigFilepath, previousWebConfigFilehash));
					return true;

				} catch (Exception e) {
					logger.error("Unexpected error in WEB.conf", e);
				} finally {
					// closes the stream and releases system resources
					if (fr != null)
						try {
							fr.close();
						} catch (IOException e) {
							logger.error("Failed to properly close FileReader", e);
						}
					if (lnr != null)
						try {
							lnr.close();
						} catch (IOException e) {
							logger.error("Failed to properly close LineNumberReader", e);
						}
				}
			}
		}
		return false;
	}

	/**
	 * Parses the feed key.
	 *
	 * @param entry the entry
	 * @return the parsed feed key string[]
	 */
	private String[] parseFeedKey(String entry) {
		StringTokenizer st = new StringTokenizer(entry, ".");
		String results[] = new String[2];
		int i = 0;
		while (st.hasMoreTokens()) {
			results[i++] = st.nextToken();
		}
		return results;
	}

	/**
	 * Parses the feed value.
	 *
	 * @param entry the entry
	 * @return the parsed feed value string[]
	 */
	private String[] parseFeedValue(String entry) {
		StringTokenizer st = new StringTokenizer(entry, ",");
		String results[] = new String[3];
		int i = 0;
		while (st.hasMoreTokens()) {
			results[i++] = st.nextToken();
		}
		return results;
	}

	/**
	 * Gets the web config filehash. Source:
	 * http://examples.javacodegeeks.com/core-java/security/messagedigest/generate-a-file-checksum-value-in-java/
	 * 
	 * @return the web config filehash
	 * @throws NoSuchAlgorithmException
	 *             the no such algorithm exception
	 * @throws IOException
	 *             the IO exception
	 */
	private String getWebConfigFilehash() {
		MessageDigest messageDigest;
		try {
			messageDigest = MessageDigest.getInstance("SHA1");
		} catch (NoSuchAlgorithmException e) {
			logger.error("Failed to get MessageDigest for SHA1", e);
			return null;
		}

		FileInputStream fileInput;
		try {
			fileInput = new FileInputStream(webConfigFilepath);
		} catch (FileNotFoundException e) {
			logger.error("Failed to open FileInputStream for file="
					+ webConfigFilepath, e);
			return null;
		}

		try {
			int bytesRead = 0;
			byte[] dataBytes = new byte[1024];

			while ((bytesRead = fileInput.read(dataBytes)) != -1) {
				messageDigest.update(dataBytes, 0, bytesRead);
			}

			byte[] digestBytes = messageDigest.digest();

			StringBuffer sb = new StringBuffer("");

			for (int i = 0; i < digestBytes.length; i++) {
				sb.append(Integer.toString((digestBytes[i] & 0xff) + 0x100, 16)
						.substring(1));
			}

			return sb.toString();
		} catch (IOException e) {
			logger.error("Failed to read FileInputStream for file="
					+ webConfigFilepath, e);
		} finally {
			try {
				fileInput.close();
			} catch (IOException e) {
				logger.error("Failed to close FileInputStream for file="
						+ webConfigFilepath, e);
			}
		}

		return null;
	}
}
