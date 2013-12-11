package net.pms.plugin.dlnatreefolder;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLDecoder;
import java.text.Collator;
import java.text.Normalizer;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xmlwise.Plist;

import com.sun.jna.Platform;

import net.pms.Messages;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.RealFile;
import net.pms.dlna.virtual.VirtualFolder;
import net.pms.plugin.dlnatreefolder.itunes.configuration.InstanceConfiguration;
import net.pms.plugin.dlnatreefolder.itunes.gui.InstanceConfigurationPanel;
import net.pms.plugins.DlnaTreeFolderPlugin;
import net.pms.util.PmsProperties;

public class iTunesFolderPlugin implements DlnaTreeFolderPlugin {
	private static final Logger logger = LoggerFactory.getLogger(iTunesFolderPlugin.class);
	
	private String rootFolderName = "root";
	
	public static final ResourceBundle messages = ResourceBundle.getBundle("net.pms.plugin.dlnatreefolder.itunes.lang.messages");
	
	/** Holds only the project version. It's used to always use the maven build number in code */
	private static final PmsProperties properties = new PmsProperties();
	static {
		try {
			properties.loadFromResourceFile("/itunesfolderplugin.properties", iTunesFolderPlugin.class);
		} catch (IOException e) {
			logger.error("Could not load itunesfolderplugin.properties", e);
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
			pInstanceConfiguration = new InstanceConfigurationPanel(instanceConfig);
		}
		
		//make sure the iTunes file path has been set
		if(instanceConfig.getiTunesFilePath() == null || instanceConfig.getiTunesFilePath().equals("")) {
			try {
				instanceConfig.setiTunesFilePath(getiTunesFile());
			} catch (Exception e) {
				logger.error("Failed to resolve iTunes file path", e);
			}
		}
		pInstanceConfiguration.applyConfig();
		
		return pInstanceConfiguration;
	}

	@Override
	public DLNAResource getDLNAResource() {
		DLNAResource res = null;

		logger.info("Start building iTunes folder...");

		if (Platform.isMac() || Platform.isWindows()) {
			Map<String, Object> iTunesLib;
			List<?> Playlists;
			Map<?, ?> Playlist;
			Map<?, ?> Tracks;
			Map<?, ?> track;
			List<?> PlaylistTracks;

			try {
				String iTunesFile = getiTunesFile();

				if (iTunesFile != null && (new File(iTunesFile)).exists()) {
					iTunesLib = Plist.load(URLDecoder.decode(iTunesFile, System.getProperty("file.encoding"))); // loads the (nested) properties.
					Tracks = (Map<?, ?>) iTunesLib.get("Tracks"); // the list of tracks
					Playlists = (List<?>) iTunesLib.get("Playlists"); // the list of Playlists
					res = new VirtualFolder(rootFolderName, null);

					VirtualFolder playlistsFolder = null;

					for (Object item : Playlists) {
						Playlist = (Map<?, ?>) item;

						if (Playlist.containsKey("Visible") && Playlist.get("Visible").equals(Boolean.FALSE))
							continue;

						if (Playlist.containsKey("Music") && Playlist.get("Music").equals(Boolean.TRUE)) {
							// Create virtual folders for artists, albums and genres

							VirtualFolder musicFolder = new VirtualFolder(Playlist.get("Name").toString(), null);
							res.addChild(musicFolder);

							VirtualFolder virtualFolderArtists = new VirtualFolder(Messages.getString("PMS.13"), null);
							VirtualFolder virtualFolderAlbums = new VirtualFolder(Messages.getString("PMS.16"), null);
							VirtualFolder virtualFolderGenres = new VirtualFolder(Messages.getString("PMS.19"), null);
							VirtualFolder virtualFolderAllTracks = new VirtualFolder(Messages.getString("PMS.11"), null);

							PlaylistTracks = (List<?>) Playlist.get("Playlist Items"); // list of tracks in a playlist

							String artistName;
							String albumName;
							String genreName;
							if (PlaylistTracks != null) {
								for (Object t : PlaylistTracks) {
									Map<?, ?> td = (Map<?, ?>) t;
									track = (Map<?, ?>) Tracks.get(td.get("Track ID").toString());

									if (track != null && track.get("Location") != null && track.get("Location").toString().startsWith("file://")) {

										String name = Normalizer.normalize((String)track.get("Name"), Normalizer.Form.NFC);
										// remove dots from name to prevent media renderer from trimming
										name = name.replace('.', '-');

										if (track.containsKey("Protected") && track.get("Protected").equals(Boolean.TRUE))
											name = String.format(Messages.getString("RootFolder.1"), name);

										boolean isCompilation = (track.containsKey("Compilation") && track.get("Compilation").equals(Boolean.TRUE));

										artistName = (String) (isCompilation ? "Compilation" :
															   track.containsKey("Album Artist") ? track.get("Album Artist") : track.get("Artist"));
										albumName = (String) track.get("Album");
										genreName = (String) track.get("Genre");

										if (artistName == null) {
											artistName = "Unknown Artist";
										} else {
											artistName = Normalizer.normalize(artistName, Normalizer.Form.NFC);
										}

										if (albumName == null) {
											albumName = "Unknown Album";
										} else {
											albumName = Normalizer.normalize(albumName, Normalizer.Form.NFC);
										}

										if (genreName == null) {
											genreName = "Unknown Genre";
										} else if ("".equals(genreName.replaceAll("[^a-zA-Z]", ""))) {
											// This prevents us from adding blank or numerical genres
											genreName = "Unknown Genre";
										} else {
											genreName = Normalizer.normalize(genreName, Normalizer.Form.NFC);
										}

										// Replace &nbsp with space and then trim
										artistName = artistName.replace('\u0160', ' ').trim();
										albumName = albumName.replace('\u0160', ' ').trim();
										genreName = genreName.replace('\u0160', ' ').trim();

										URI tURI2 = new URI(track.get("Location").toString());
										File refFile = new File(URLDecoder.decode(tURI2.toURL().getFile(), "UTF-8"));
										RealFile file = new RealFile(refFile, name);

										// ARTISTS FOLDER - Put the track into the artist's album folder and the artist's "All tracks" folder
										{
											VirtualFolder individualArtistFolder = null;
											VirtualFolder individualArtistAllTracksFolder = null;
											VirtualFolder individualArtistAlbumFolder = null;

											for (DLNAResource artist : virtualFolderArtists.getChildren()) {
												if (areNamesEqual(artist.getName(), artistName)) {
													individualArtistFolder = (VirtualFolder) artist;
													for (DLNAResource album : individualArtistFolder.getChildren()) {
														if (areNamesEqual(album.getName(), albumName)) {
															individualArtistAlbumFolder = (VirtualFolder) album;
														}
													}
													break;
												}
											}

											if (individualArtistFolder == null) {
												individualArtistFolder = new VirtualFolder(artistName, null);
												virtualFolderArtists.addChild(individualArtistFolder);
												individualArtistAllTracksFolder = new VirtualFolder(Messages.getString("PMS.11"), null);
												individualArtistFolder.addChild(individualArtistAllTracksFolder);
											} else {
												individualArtistAllTracksFolder = (VirtualFolder)individualArtistFolder.getChildren().get(0);
											}

											if (individualArtistAlbumFolder == null) {
												individualArtistAlbumFolder = new VirtualFolder(albumName, null);
												individualArtistFolder.addChild(individualArtistAlbumFolder);
											}

											individualArtistAlbumFolder.addChild(file.clone());
											individualArtistAllTracksFolder.addChild(file);
										}

										// ALBUMS FOLDER - Put the track into its album folder
										{
											if (!isCompilation)
												albumName += " – " + artistName;

											VirtualFolder individualAlbumFolder = null;
											for (DLNAResource album : virtualFolderAlbums.getChildren()) {
												if (areNamesEqual(album.getName(), albumName)) {
													individualAlbumFolder = (VirtualFolder) album;
													break;
												}
											}
											if (individualAlbumFolder == null) {
												individualAlbumFolder = new VirtualFolder(albumName, null);
												virtualFolderAlbums.addChild(individualAlbumFolder);
											}
											individualAlbumFolder.addChild(file.clone());
										}

										// GENRES FOLDER - Put the track into its genre folder
										{
											VirtualFolder individualGenreFolder = null;
											for (DLNAResource genre : virtualFolderGenres.getChildren()) {
												if (areNamesEqual(genre.getName(), genreName)) {
													individualGenreFolder = (VirtualFolder) genre;
													break;
												}
											}
											if (individualGenreFolder == null) {
												individualGenreFolder = new VirtualFolder(genreName, null);
												virtualFolderGenres.addChild(individualGenreFolder);
											}
											individualGenreFolder.addChild(file.clone());
										}

										// ALL TRACKS - Put the track into the global "All tracks" folder
										virtualFolderAllTracks.addChild(file.clone());
									}
								}
							}

							musicFolder.addChild(virtualFolderArtists);
							musicFolder.addChild(virtualFolderAlbums);
							musicFolder.addChild(virtualFolderGenres);
							musicFolder.addChild(virtualFolderAllTracks);

							// Sort the virtual folders alphabetically
							Collections.sort(virtualFolderArtists.getChildren(), new Comparator<DLNAResource>() {
								@Override
								public int compare(DLNAResource o1, DLNAResource o2) {
									VirtualFolder a = (VirtualFolder) o1;
									VirtualFolder b = (VirtualFolder) o2;
									return a.getName().compareToIgnoreCase(b.getName());
								}
							});

							Collections.sort(virtualFolderAlbums.getChildren(), new Comparator<DLNAResource>() {
								@Override
								public int compare(DLNAResource o1, DLNAResource o2) {
									VirtualFolder a = (VirtualFolder) o1;
									VirtualFolder b = (VirtualFolder) o2;
									return a.getName().compareToIgnoreCase(b.getName());
								}
							});

							Collections.sort(virtualFolderGenres.getChildren(), new Comparator<DLNAResource>() {
								@Override
								public int compare(DLNAResource o1, DLNAResource o2) {
									VirtualFolder a = (VirtualFolder) o1;
									VirtualFolder b = (VirtualFolder) o2;
									return a.getName().compareToIgnoreCase(b.getName());
								}
							});

						} else {
							// Add all playlists
							VirtualFolder pf = new VirtualFolder(Playlist.get("Name").toString(), null);
							PlaylistTracks = (List<?>) Playlist.get("Playlist Items"); // list of tracks in a playlist

							if (PlaylistTracks != null) {
								for (Object t : PlaylistTracks) {
									Map<?, ?> td = (Map<?, ?>) t;
									track = (Map<?, ?>) Tracks.get(td.get("Track ID").toString());

									if (track != null
												&& track.get("Location") != null
												&& track.get("Location").toString().startsWith("file://")
											) {
										String name = Normalizer.normalize(track.get("Name").toString(), Normalizer.Form.NFC);
										// remove dots from name to prevent media renderer from trimming
										name = name.replace('.', '-');

										if (track.containsKey("Protected") && track.get("Protected").equals(Boolean.TRUE))
											name = String.format(Messages.getString("RootFolder.1"), name);

										URI tURI2 = new URI(track.get("Location").toString());
										RealFile file = new RealFile(new File(URLDecoder.decode(tURI2.toURL().getFile(), "UTF-8")), name);
										pf.addChild(file);
									}
								}
							}

							int kind = Playlist.containsKey("Distinguished Kind") ? ((Number)Playlist.get("Distinguished Kind")).intValue() : -1;
							if (kind >= 0 && kind != 17 && kind != 19 && kind != 20) {
								// System folder, but not voice memos (17) and purchased items (19 & 20)
								res.addChild(pf);

							} else {
								// User playlist or playlist folder
								if (playlistsFolder == null) {
									playlistsFolder = new VirtualFolder("Playlists", null);
									res.addChild(playlistsFolder);
								}
								playlistsFolder.addChild(pf);
							}
						}
					}
				} else {
					logger.info("Could not find the iTunes file");
				}
			} catch (Exception e) {
				logger.error("Something went wrong with the iTunes Library scan: ", e);
			}
		}

		logger.info("Done building iTunes folder.");

		return res;
	}
	
	private String getiTunesFile() throws Exception {
		String line = null;
		String iTunesFile = null;
		if (Platform.isMac()) {
			Process prc = Runtime.getRuntime().exec("defaults read com.apple.iApps iTunesRecentDatabases");
			BufferedReader in = new BufferedReader(new InputStreamReader(prc.getInputStream()));

			// we want the 2nd line
			if ((line = in.readLine()) != null && (line = in.readLine()) != null) {
				line = line.trim(); // remove extra spaces
				line = line.substring(1, line.length() - 1); // remove quotes and spaces
				URI tURI = new URI(line);
				iTunesFile = URLDecoder.decode(tURI.toURL().getFile(), "UTF8");
			}
			if (in != null) {
				in.close();
			}
		} else if (Platform.isWindows()) {
			Process prc = Runtime.getRuntime().exec("reg query \"HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\Shell Folders\" /v \"My Music\"");
			BufferedReader in = new BufferedReader(new InputStreamReader(prc.getInputStream()));
			String location = null;
			while ((line = in.readLine()) != null) {
				final String LOOK_FOR = "REG_SZ";
				if (line.contains(LOOK_FOR)) {
					location = line.substring(line.indexOf(LOOK_FOR) + LOOK_FOR.length()).trim();
				}
			}
			if (in != null) {
				in.close();
			}
			if (location != null) {
				// add the itunes folder to the end
				location = location + "\\iTunes\\iTunes Music Library.xml";
				iTunesFile = location;
			} else {
				logger.info("Could not find the My Music folder");
			}
		}

		return iTunesFile;
	}

	private static boolean areNamesEqual(String aThis, String aThat) {
		Collator collator = Collator.getInstance(Locale.getDefault());
		collator.setStrength(Collator.PRIMARY);
		int comparison = collator.compare(aThis, aThat);
		return (comparison == 0);
	}

	@Override
	public String getName() {
		return "iTunes";
	}
	
	@Override
	public void setDisplayName(String name){
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
		return isPluginAvailable();
    }

	@Override
	public boolean isPluginAvailable() {
		if(System.getProperty("os.name").toLowerCase().indexOf("nix") < 0) {
			return true;
        }
        return false;
	}

	@Override
	public String getVersion() {
		return properties.get("project.version");
	}

	@Override
	public String getShortDescription() {
		return messages.getString("iTunesFolderPlugin.ShortDescription");
	}

	@Override
	public String getLongDescription() {
		return messages.getString("iTunesFolderPlugin.LongDescription");
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
		return new ImageIcon(getClass().getResource("/itunes-32.png"));
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
		instanceConfig = new InstanceConfiguration();
	}

	@Override
	public void saveConfiguration() {
	}

	@Override
	public Icon getTreeNodeIcon() {
		return new ImageIcon(getClass().getResource("/itunes-16.png"));
	}
}
