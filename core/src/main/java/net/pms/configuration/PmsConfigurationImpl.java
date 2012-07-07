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
package net.pms.configuration;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.pms.Messages;
import net.pms.api.PmsConfiguration;
import net.pms.io.SystemUtils;
import net.pms.util.PropertiesUtil;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.ConversionException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.event.ConfigurationListener;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.sun.jna.Platform;

/**
 * Container for all configurable PMS settings. Settings are typically defined by three things:
 * a unique key for use in the configuration file "PMS.conf", a getter (and setter) method and
 * a default value. When a key cannot be found in the current configuration, the getter will
 * return a default value. Setters only store a value, they do not permanently save it to
 * file.
 */
public class PmsConfigurationImpl implements PmsConfiguration {
	private static final Logger LOGGER = LoggerFactory.getLogger(PmsConfigurationImpl.class);
	private static final int DEFAULT_PROXY_SERVER_PORT = -1;
	private static final int DEFAULT_SERVER_PORT = 5001;

	// MEncoder has a hardwired maximum of 8 threads for -lavcopts and -lavdopts:
	// https://code.google.com/p/ps3mediaserver/issues/detail?id=517
	private static final int MENCODER_MAX_THREADS = 8;

	private static final String KEY_ALTERNATE_SUBS_FOLDER = "alternate_subs_folder";
	private static final String KEY_ALTERNATE_THUMB_FOLDER = "alternate_thumb_folder";
	private static final String KEY_APERTURE_ENABLED = "aperture";
	private static final String KEY_AUDIO_BITRATE = "audiobitrate";
	private static final String KEY_AUDIO_CHANNEL_COUNT = "audiochannels";
	private static final String KEY_AUDIO_RESAMPLE = "audio_resample";
	private static final String KEY_AUDIO_THUMBNAILS_METHOD = "audio_thumbnails_method";
	private static final String KEY_AUTO_UPDATE = "auto_update";
	private static final String KEY_AVISYNTH_CONVERT_FPS = "avisynth_convertfps";
	private static final String KEY_AVISYNTH_SCRIPT = "avisynth_script";
	private static final String KEY_BUFFER_TYPE = "buffertype";
	private static final String KEY_CHAPTER_INTERVAL = "chapter_interval";
	private static final String KEY_CHAPTER_SUPPORT = "chapter_support";
	private static final String KEY_CHARSET_ENCODING = "charsetencoding";
	private static final String KEY_CODEC_SPEC_SCRIPT = "codec_spec_script";
	private static final String KEY_DISABLE_FAKESIZE = "disable_fakesize";
	private static final String KEY_DVDISO_THUMBNAILS = "dvd_isos_thumbnails";
	private static final String KEY_EMBED_DTS_IN_PCM = "embed_dts_in_pcm";
	private static final String KEY_ENGINES = "engines";
	private static final String KEY_FFMPEG_ALTERNATIVE_PATH = "alternativeffmpegpath";
	private static final String KEY_FFMPEG_SETTINGS = "ffmpeg";
	private static final String KEY_FIX_25FPS_AV_MISMATCH = "fix_25fps_av_mismatch";
	private static final String KEY_FORCETRANSCODE = "forcetranscode";
	private static final String KEY_HIDE_EMPTY_FOLDERS = "hide_empty_folders";
	private static final String KEY_HIDE_ENGINENAMES = "hide_enginenames";
	private static final String KEY_HIDE_EXTENSIONS = "hide_extensions";
	private static final String KEY_HIDE_MEDIA_LIBRARY_FOLDER = "hide_media_library_folder";
	private static final String KEY_HIDE_TRANSCODE_FOLDER = "hide_transcode_folder";
	private static final String KEY_HIDE_VIDEO_SETTINGS = "hidevideosettings";
	private static final String KEY_HTTP_ENGINE_V2 = "http_engine_v2";
	private static final String KEY_IMAGE_THUMBNAILS_ENABLED = "image_thumbnails";
	private static final String KEY_IP_FILTER = "ip_filter";
	private static final String KEY_IPHOTO_ENABLED = "iphoto";
	private static final String KEY_ITUNES_ENABLED = "itunes";
	private static final String KEY_LANGUAGE = "language";
	private static final String KEY_MAX_AUDIO_BUFFER = "maxaudiobuffer";
	private static final String KEY_MAX_BITRATE = "maximumbitrate";
	private static final String KEY_MAX_MEMORY_BUFFER_SIZE = "maxvideobuffer";
	private static final String KEY_MENCODER_AC3_FIXED = "mencoder_ac3_fixed";
	private static final String KEY_MENCODER_ASS_DEFAULTSTYLE = "mencoder_ass_defaultstyle";
	private static final String KEY_MENCODER_ASS_MARGIN = "mencoder_ass_margin";
	private static final String KEY_MENCODER_ASS = "mencoder_ass";
	private static final String KEY_MENCODER_ASS_OUTLINE = "mencoder_ass_outline";
	private static final String KEY_MENCODER_ASS_SCALE = "mencoder_ass_scale";
	private static final String KEY_MENCODER_ASS_SHADOW = "mencoder_ass_shadow";
	private static final String KEY_MENCODER_AUDIO_LANGS = "mencoder_audiolangs";
	private static final String KEY_MENCODER_AUDIO_SUB_LANGS = "mencoder_audiosublangs";
	private static final String KEY_MENCODER_CUSTOM_OPTIONS = "mencoder_decode"; // TODO (breaking change): should be renamed to e.g. mencoder_custom_options
	private static final String KEY_MENCODER_DISABLE_SUBS = "mencoder_disablesubs";
	private static final String KEY_MENCODER_FONT_CONFIG = "mencoder_fontconfig";
	private static final String KEY_MENCODER_FONT = "mencoder_font";
	private static final String KEY_MENCODER_FORCED_SUB_LANG = "forced_sub_lang";
	private static final String KEY_MENCODER_FORCED_SUB_TAGS = "forced_sub_tags";
	private static final String KEY_MENCODER_FORCE_FPS = "mencoder_forcefps";
	private static final String KEY_MENCODER_INTELLIGENT_SYNC = "mencoder_intelligent_sync";
	private static final String KEY_MENCODER_MAIN_SETTINGS = "mencoder_encode";
	private static final String KEY_MENCODER_MAX_THREADS = "mencoder_max_threads";
	private static final String KEY_MENCODER_MT = "mencoder_mt";
	private static final String KEY_MENCODER_MUX_COMPATIBLE = "mencoder_mux_compatible";
	private static final String KEY_MENCODER_NOASS_BLUR = "mencoder_noass_blur";
	private static final String KEY_MENCODER_NOASS_OUTLINE = "mencoder_noass_outline";
	private static final String KEY_MENCODER_NOASS_SCALE = "mencoder_noass_scale";
	private static final String KEY_MENCODER_NOASS_SUBPOS = "mencoder_noass_subpos";
	private static final String KEY_MENCODER_NO_OUT_OF_SYNC = "mencoder_nooutofsync";
	private static final String KEY_MENCODER_OVERSCAN_COMPENSATION_HEIGHT = "mencoder_overscan_compensation_height";
	private static final String KEY_MENCODER_OVERSCAN_COMPENSATION_WIDTH = "mencoder_overscan_compensation_width";
	private static final String KEY_MENCODER_REMUX_AC3 = "mencoder_remux_ac3";
	private static final String KEY_MENCODER_REMUX_MPEG2 = "mencoder_remux_mpeg2";
	private static final String KEY_MENCODER_SCALER = "mencoder_scaler";
	private static final String KEY_MENCODER_SCALEX = "mencoder_scalex";
	private static final String KEY_MENCODER_SCALEY = "mencoder_scaley";
	private static final String KEY_MENCODER_SUB_CP = "mencoder_subcp";
	private static final String KEY_MENCODER_SUB_FRIBIDI = "mencoder_subfribidi";
	private static final String KEY_MENCODER_SUB_LANGS = "mencoder_sublangs";
	private static final String KEY_MENCODER_USE_PCM = "mencoder_usepcm";
	private static final String KEY_MENCODER_USE_PCM_FOR_HQ_AUDIO_ONLY = "mencoder_usepcm_for_hq_audio_only";
	private static final String KEY_MENCODER_VOBSUB_SUBTITLE_QUALITY = "mencoder_vobsub_subtitle_quality";
	private static final String KEY_MENCODER_YADIF = "mencoder_yadif";
	private static final String KEY_MINIMIZED = "minimized";
	private static final String KEY_MIN_MEMORY_BUFFER_SIZE = "minvideobuffer";
	private static final String KEY_MIN_STREAM_BUFFER = "minwebbuffer";
	private static final String KEY_MUX_ALLAUDIOTRACKS = "tsmuxer_mux_all_audiotracks";
	private static final String KEY_NETWORK_INTERFACE = "network_interface";
	private static final String KEY_NOTRANSCODE = "notranscode";
	private static final String KEY_NUMBER_OF_CPU_CORES = "nbcores";
	private static final String KEY_OPEN_ARCHIVES = "enable_archive_browsing";
	private static final String KEY_OVERSCAN = "mencoder_overscan";
	private static final String KEY_PLUGIN_DIRECTORY = "plugins";
	private static final String KEY_PREVENTS_SLEEP = "prevents_sleep_mode";
	private static final String KEY_PROFILE_NAME = "name";
	private static final String KEY_PROXY_SERVER_PORT = "proxy";
	private static final String KEY_RENDERER_DEFAULT = "renderer_default";
	private static final String KEY_RENDERER_FORCE_DEFAULT = "renderer_force_default";
	private static final String KEY_SERVER_HOSTNAME = "hostname";
	private static final String KEY_SERVER_PORT = "port";
	private static final String KEY_SHARES = "shares";
	private static final String KEY_SKIP_LOOP_FILTER_ENABLED = "skiploopfilter";
	private static final String KEY_SKIP_NETWORK_INTERFACES = "skip_network_interfaces";
	private static final String KEY_SORT_METHOD = "key_sort_method";
	private static final String KEY_SUBS_COLOR = "subs_color";
	private static final String KEY_TEMP_FOLDER_PATH = "temp";
	private static final String KEY_THUMBNAIL_GENERATION_ENABLED = "thumbnails"; // TODO (breaking change): should be renamed to e.g. generate_thumbnails
	private static final String KEY_THUMBNAIL_SEEK_POS = "thumbnail_seek_pos";
	private static final String KEY_TRANSCODE_BLOCKS_MULTIPLE_CONNECTIONS = "transcode_block_multiple_connections";
	private static final String KEY_TRANSCODE_KEEP_FIRST_CONNECTION = "transcode_keep_first_connection";
	private static final String KEY_TSMUXER_FORCEFPS = "tsmuxer_forcefps";
	private static final String KEY_TSMUXER_PREREMIX_AC3 = "tsmuxer_preremix_ac3";
	private static final String KEY_TURBO_MODE_ENABLED = "turbomode";
	private static final String KEY_UPNP_PORT = "upnp_port";
	private static final String KEY_USE_CACHE = "usecache";
	private static final String KEY_USE_MPLAYER_FOR_THUMBS = "use_mplayer_for_video_thumbs";
	private static final String KEY_USE_SUBTITLES = "autoloadsrt";
	private static final String KEY_UUID = "uuid";
	private static final String KEY_VIDEOTRANSCODE_START_DELAY = "key_videotranscode_start_delay";
	private static final String KEY_VIRTUAL_FOLDERS = "vfolders";
	private static final String KEY_BUFFER_MAX = "buffer_max";

	// the name of the subdirectory under which PMS config files are stored for this build (default: PMS).
	// see Build for more details
	private static final String PROFILE_DIRECTORY_NAME = Build.getProfileDirectoryName();

	// the default profile name displayed on the renderer
	private static String HOSTNAME;

	private static String DEFAULT_AVI_SYNTH_SCRIPT;
	private static final String BUFFER_TYPE_FILE = "file";
	private static final int MAX_MAX_MEMORY_DEFAULT_SIZE = 400;
	private static final int BUFFER_MEMORY_FACTOR = 368;
	private static int MAX_MAX_MEMORY_BUFFER_SIZE = MAX_MAX_MEMORY_DEFAULT_SIZE;
	private static final char LIST_SEPARATOR = ',';
	private static final String KEY_FOLDERS = "folders";
	private final PropertiesConfiguration configuration;
	private final TempFolder tempFolder;
	private final ProgramPathDisabler programPaths;

	/**
	 * The set of the keys defining when the HTTP server has to restarted due to a configuration change
	 */
	private static final ImmutableSet<String> NEED_RELOAD_FLAGS = ImmutableSet.of(
					KEY_ALTERNATE_THUMB_FOLDER, KEY_NETWORK_INTERFACE,
					KEY_IP_FILTER, KEY_SORT_METHOD, KEY_HIDE_EMPTY_FOLDERS,
					KEY_HIDE_TRANSCODE_FOLDER, KEY_HIDE_MEDIA_LIBRARY_FOLDER,
					KEY_OPEN_ARCHIVES, KEY_USE_CACHE, KEY_HIDE_ENGINENAMES,
					KEY_ITUNES_ENABLED, KEY_IPHOTO_ENABLED,
					KEY_APERTURE_ENABLED, KEY_ENGINES, KEY_FOLDERS,
					KEY_HIDE_VIDEO_SETTINGS, KEY_AUDIO_THUMBNAILS_METHOD,
					KEY_NOTRANSCODE, KEY_FORCETRANSCODE, KEY_SERVER_PORT,
					KEY_SERVER_HOSTNAME, KEY_CHAPTER_SUPPORT,
					KEY_HIDE_EXTENSIONS);

	private final IpFilter filter = new IpFilter();

	/*
		The following code enables a single setting - PMS_PROFILE - to be used to
		initialize PROFILE_PATH i.e. the path to the current session's profile (AKA PMS.conf).
		It also initializes PROFILE_DIRECTORY - i.e. the directory the profile is located in -
		which is needed for configuration-by-convention detection of WEB.conf (anything else?).

		While this convention - and therefore PROFILE_DIRECTORY - will remain,
		adding more configurables - e.g. web_conf = ... - is on the TODO list.

		PMS_PROFILE is read (in this order) from the property pms.profile.path or the
		environment variable PMS_PROFILE. If PMS is launched with the command-line option
		"profiles" (e.g. from a shortcut), it displays a file chooser dialog that
		allows the pms.profile.path property to be set. This makes it easy to run PMS
		under multiple profiles without fiddling with environment variables, properties or
		command-line arguments.

		1) if PMS_PROFILE is not set, PMS.conf is located in:

			Windows:             %ALLUSERSPROFILE%\$build
			Mac OS X:            $HOME/Library/Application Support/$build
			Everything else:     $HOME/.config/$build

		- where $build is a subdirectory that ensures incompatible PMS builds don't target/clobber
		the same configuration files. The default value for $build is "PMS". Other builds might use e.g.
		"PMS Rendr Edition" or "pms-mlx".

		2) if a relative or absolute *directory path* is supplied (the directory must exist),
		it is used as the profile directory and the profile is located there under the default profile name (PMS.conf):

			PMS_PROFILE = /absolute/path/to/dir
			PMS_PROFILE = relative/path/to/dir # relative to the working directory

		Amongst other things, this can be used to restore the legacy behaviour of locating PMS.conf in the current
		working directory e.g.:

			PMS_PROFILE=. ./PMS.sh

		3) if a relative or absolute *file path* is supplied (the file doesn't have to exist),
		it is taken to be the profile, and its parent dir is taken to be the profile (i.e. config file) dir: 

			PMS_PROFILE = PMS.conf            # profile dir = .
			PMS_PROFILE = folder/dev.conf     # profile dir = folder
			PMS_PROFILE = /path/to/some.file  # profile dir = /path/to/
	 */
	private static final String DEFAULT_PROFILE_FILENAME = "PMS.conf";
	private static final String ENV_PROFILE_PATH = "PMS_PROFILE";
	private static final String PROFILE_DIRECTORY; // path to directory containing PMS config files
	private static final String PROFILE_PATH; // abs path to profile file e.g. /path/to/PMS.conf
    private static final String SKEL_PROFILE_PATH ; // abs path to skel (default) profile file e.g. /etc/skel/.config/ps3mediaserver/PMS.conf
                                                    // "project.skelprofile.dir" project property
	private static final String PROPERTY_PROFILE_PATH = "pms.profile.path";

	static {
        // first try the system property, typically set via the profile chooser
		String profile = System.getProperty(PROPERTY_PROFILE_PATH);

		// failing that, try the environment variable
		if (profile == null) {
			profile = System.getenv(ENV_PROFILE_PATH);
		}

		if (profile != null) {
			File f = new File(profile);

			// if it exists, we know whether it's a file or directory
			// otherwise, it must be a file since we don't autovivify directories

			if (f.exists() && f.isDirectory()) {
				PROFILE_DIRECTORY = FilenameUtils.normalize(f.getAbsolutePath());
				PROFILE_PATH = FilenameUtils.normalize(new File(f, DEFAULT_PROFILE_FILENAME).getAbsolutePath());
			} else { // doesn't exist or is a file (i.e. not a directory)
				PROFILE_PATH = FilenameUtils.normalize(f.getAbsolutePath());
				PROFILE_DIRECTORY = FilenameUtils.normalize(f.getParentFile().getAbsolutePath());
			}
		} else {
			String profileDir = null;

			if (Platform.isWindows()) {
				String programData = System.getenv("ALLUSERSPROFILE");
				if (programData != null) {
					profileDir = String.format("%s\\%s", programData, PROFILE_DIRECTORY_NAME);
				} else {
					profileDir = ""; // i.e. current (working) directory
				}
			} else if (Platform.isMac()) {
				profileDir = String.format(
					"%s/%s/%s",
					System.getProperty("user.home"),
					"/Library/Application Support",
					PROFILE_DIRECTORY_NAME
				);
			} else {
				String xdgConfigHome = System.getenv("XDG_CONFIG_HOME");

				if (xdgConfigHome == null) {
					profileDir = String.format("%s/.config/%s", System.getProperty("user.home"), PROFILE_DIRECTORY_NAME);
				} else {
					profileDir = String.format("%s/%s", xdgConfigHome, PROFILE_DIRECTORY_NAME);
				}
			}

			File f = new File(profileDir);

			if ((f.exists() || f.mkdir()) && f.isDirectory()) {
				PROFILE_DIRECTORY = FilenameUtils.normalize(f.getAbsolutePath());
			} else {
				PROFILE_DIRECTORY = FilenameUtils.normalize(new File("").getAbsolutePath());
			}

			PROFILE_PATH = FilenameUtils.normalize(new File(PROFILE_DIRECTORY, DEFAULT_PROFILE_FILENAME).getAbsolutePath());
		}
        // set SKEL_PROFILE_PATH for Linux systems
        String skelDir = PropertiesUtil.getProjectProperties().get("project.skelprofile.dir");
        if (Platform.isLinux() && StringUtils.isNotBlank(skelDir)) {
            SKEL_PROFILE_PATH = FilenameUtils.normalize(new File(new File(skelDir, PROFILE_DIRECTORY_NAME).getAbsolutePath(), DEFAULT_PROFILE_FILENAME).getAbsolutePath());
        } else {
            SKEL_PROFILE_PATH = null;
        }
	}

	/**
	 * Default constructor that will attempt to load the PMS configuration file
	 * from the profile path.
	 *
	 * @throws org.apache.commons.configuration.ConfigurationException
	 * @throws java.io.IOException
	 */
	public PmsConfigurationImpl() throws ConfigurationException, IOException {
		this(true);
	}

	/**
	 * Constructor that will initialize the PMS configuration.
	 *
	 * @param loadFile Set to true to attempt to load the PMS configuration
	 * 					file from the profile path. Set to false to skip
	 * 					loading.
	 * @throws org.apache.commons.configuration.ConfigurationException
	 * @throws java.io.IOException
	 */
	public PmsConfigurationImpl(boolean loadFile) throws ConfigurationException, IOException {
		configuration = new PropertiesConfiguration();
		configuration.setListDelimiter((char) 0);

		if (loadFile) {
			File pmsConfFile = new File(PROFILE_PATH);
	
			if (pmsConfFile.isFile() && pmsConfFile.canRead()) {
				configuration.load(PROFILE_PATH);
			} else if (SKEL_PROFILE_PATH != null) {
                File pmsSkelConfFile = new File(SKEL_PROFILE_PATH);
                if (pmsSkelConfFile.isFile() && pmsSkelConfFile.canRead()) {
                    // load defaults from skel file, save them later to PROFILE_PATH
                    configuration.load(pmsSkelConfFile);
                    LOGGER.info("Default configuration loaded from " + SKEL_PROFILE_PATH);
                }
            }
		}

        configuration.setPath(PROFILE_PATH);

        tempFolder = new TempFolder(getString(KEY_TEMP_FOLDER_PATH, null));
		programPaths = createProgramPathsChain(configuration);
		Locale.setDefault(new Locale(getLanguage()));

		// Set DEFAULT_AVI_SYNTH_SCRIPT according to language
		DEFAULT_AVI_SYNTH_SCRIPT = 
			Messages.getString("MEncoderAviSynth.4") +
			Messages.getString("MEncoderAviSynth.5") +
			Messages.getString("MEncoderAviSynth.6") +
			Messages.getString("MEncoderAviSynth.7") +
			Messages.getString("MEncoderAviSynth.8") +
			Messages.getString("MEncoderAviSynth.10") +
			Messages.getString("MEncoderAviSynth.11");

		long usableMemory = (Runtime.getRuntime().maxMemory() / 1048576) - BUFFER_MEMORY_FACTOR;
		if (usableMemory > MAX_MAX_MEMORY_DEFAULT_SIZE) {
			MAX_MAX_MEMORY_BUFFER_SIZE = (int) usableMemory;
		}
	}

	/**
	 * Check if we have disabled something first, then check the config file,
	 * then the Windows registry, then check for a platform-specific
	 * default.
	 */
	private static ProgramPathDisabler createProgramPathsChain(Configuration configuration) {
		return new ProgramPathDisabler(
			new ConfigurationProgramPaths(configuration,
			new WindowsRegistryProgramPaths(
			new PlatformSpecificDefaultPathsFactory().get())));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public File getTempFolder() throws IOException {
		return tempFolder.getTempFolder();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getVlcPath() {
		return programPaths.getVlcPath();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void disableVlc() {
		programPaths.disableVlc();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getEac3toPath() {
		return programPaths.getEac3toPath();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getMencoderPath() {
		return programPaths.getMencoderPath();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getMencoderMaxThreads() {
		return Math.min(getInt(KEY_MENCODER_MAX_THREADS, getNumberOfCpuCores()), MENCODER_MAX_THREADS);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDCRawPath() {
		return programPaths.getDCRaw();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void disableMEncoder() {
		programPaths.disableMencoder();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getFfmpegPath() {
		return programPaths.getFfmpegPath();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void disableFfmpeg() {
		programPaths.disableFfmpeg();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getMplayerPath() {
		return programPaths.getMplayerPath();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void disableMplayer() {
		programPaths.disableMplayer();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getTsmuxerPath() {
		return programPaths.getTsmuxerPath();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getFlacPath() {
		return programPaths.getFlacPath();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isTsmuxerForceFps() {
		return configuration.getBoolean(KEY_TSMUXER_FORCEFPS, true);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isTsmuxerPreremuxAc3() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getAudioBitrate() {
		return getInt(KEY_AUDIO_BITRATE, 640);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setTsmuxerPreremuxAc3(boolean value) {
		configuration.setProperty(KEY_TSMUXER_PREREMIX_AC3, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setTsmuxerForceFps(boolean value) {
		configuration.setProperty(KEY_TSMUXER_FORCEFPS, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getServerPort() {
		return getInt(KEY_SERVER_PORT, DEFAULT_SERVER_PORT);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setServerPort(int value) {
		configuration.setProperty(KEY_SERVER_PORT, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getServerHostname() {
		String value = getString(KEY_SERVER_HOSTNAME, "");
		if (StringUtils.isNotBlank(value)) {
			return value;
		} else {
			return null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setHostname(String value) {
		configuration.setProperty(KEY_SERVER_HOSTNAME, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getProxyServerPort() {
		return getInt(KEY_PROXY_SERVER_PORT, DEFAULT_PROXY_SERVER_PORT);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getLanguage() {
		String def = Locale.getDefault().getLanguage();
		if (def == null) {
			def = "en";
		}
		String value = getString(KEY_LANGUAGE, def);
		return StringUtils.isNotBlank(value) ? value.trim() : def;
	}

	/**
	 * Return the <code>int</code> value for a given configuration key. First, the key
	 * is looked up in the current configuration settings. If it exists and contains a
	 * valid value, that value is returned. If the key contains an invalid value or
	 * cannot be found, the specified default value is returned.
	 * @param key The key to look up.
	 * @param def The default value to return when no valid key value can be found.
	 * @return The value configured for the key.
	 */
	private int getInt(String key, int def) {
		try {
			return configuration.getInt(key, def);
		} catch (ConversionException e) {
			return def;
		}
	}

	/**
	 * Return the <code>boolean</code> value for a given configuration key. First, the
	 * key is looked up in the current configuration settings. If it exists and contains
	 * a valid value, that value is returned. If the key contains an invalid value or
	 * cannot be found, the specified default value is returned.
	 * @param key The key to look up.
	 * @param def The default value to return when no valid key value can be found.
	 * @return The value configured for the key.
	 */
	private boolean getBoolean(String key, boolean def) {
		try {
			return configuration.getBoolean(key, def);
		} catch (ConversionException e) {
			return def;
		}
	}

	/**
	 * Return the <code>String</code> value for a given configuration key. First, the
	 * key is looked up in the current configuration settings. If it exists and contains
	 * a valid value, that value is returned. If the key contains an invalid value or
	 * cannot be found, the specified default value is returned.
	 * @param key The key to look up.
	 * @param def The default value to return when no valid key value can be found.
	 * @return The value configured for the key.
	 */
	private String getString(String key, String def) {
		String value = configuration.getString(key, def);
		if (value != null) {
			value = value.trim();
		}
		return value;
	}
	
	/**
	 * Return a <code>List</code> of <code>String</code> values for a given configuration
	 * key. First, the key is looked up in the current configuration settings. If it
	 * exists and contains a valid value, that value is returned. If the key contains an
	 * invalid value or cannot be found, a list with the specified default values is
	 * returned.
	 * @param key The key to look up.
	 * @param def The default values to return when no valid key value can be found.
	 *            These values should be entered as a comma separated string, whitespace
	 *            will be trimmed. For example: <code>"gnu,    gnat  ,moo "</code> will be
	 *            returned as <code>{ "gnu", "gnat", "moo" }</code>.
	 * @return The list of value strings configured for the key.
	 */
	private List<String> getStringList(String key, String def) {
		String value = getString(key, def);
		if (value != null) {
			String[] arr = value.split(",");
			List<String> result = new ArrayList<String>(arr.length);
			for (String str : arr) {
				if (str.trim().length() > 0) {
					result.add(str.trim());
				}
			}
			return result;
		} else {
			return Collections.emptyList();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getMinMemoryBufferSize() {
		return getInt(KEY_MIN_MEMORY_BUFFER_SIZE, 12);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getMaxMemoryBufferSize() {
		return Math.max(0, Math.min(MAX_MAX_MEMORY_BUFFER_SIZE, getInt(KEY_MAX_MEMORY_BUFFER_SIZE, 400)));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getMaxMemoryBufferSizeStr() {
		return String.valueOf(MAX_MAX_MEMORY_BUFFER_SIZE);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setMaxMemoryBufferSize(int value) {
		configuration.setProperty(KEY_MAX_MEMORY_BUFFER_SIZE, Math.max(0, Math.min(MAX_MAX_MEMORY_BUFFER_SIZE, value)));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getMencoderAssScale() {
		return getString(KEY_MENCODER_ASS_SCALE, "1.0");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isMencoderAc3Fixed() {
		return configuration.getBoolean(KEY_MENCODER_AC3_FIXED, false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getMencoderAssMargin() {
		return getString(KEY_MENCODER_ASS_MARGIN, "10");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getMencoderAssOutline() {
		return getString(KEY_MENCODER_ASS_OUTLINE, "1");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getMencoderAssShadow() {
		return getString(KEY_MENCODER_ASS_SHADOW, "1");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getMencoderNoAssScale() {
		return getString(KEY_MENCODER_NOASS_SCALE, "3");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getMencoderNoAssSubPos() {
		return getString(KEY_MENCODER_NOASS_SUBPOS, "2");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getMencoderNoAssBlur() {
		return getString(KEY_MENCODER_NOASS_BLUR, "1");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getMencoderNoAssOutline() {
		return getString(KEY_MENCODER_NOASS_OUTLINE, "1");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setMencoderNoAssOutline(String value) {
		configuration.setProperty(KEY_MENCODER_NOASS_OUTLINE, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setMencoderAc3Fixed(boolean value) {
		configuration.setProperty(KEY_MENCODER_AC3_FIXED, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setMencoderAssMargin(String value) {
		configuration.setProperty(KEY_MENCODER_ASS_MARGIN, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setMencoderAssOutline(String value) {
		configuration.setProperty(KEY_MENCODER_ASS_OUTLINE, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setMencoderAssShadow(String value) {
		configuration.setProperty(KEY_MENCODER_ASS_SHADOW, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setMencoderAssScale(String value) {
		configuration.setProperty(KEY_MENCODER_ASS_SCALE, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setMencoderNoAssScale(String value) {
		configuration.setProperty(KEY_MENCODER_NOASS_SCALE, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setMencoderNoAssBlur(String value) {
		configuration.setProperty(KEY_MENCODER_NOASS_BLUR, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setMencoderNoAssSubPos(String value) {
		configuration.setProperty(KEY_MENCODER_NOASS_SUBPOS, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setMencoderMaxThreads(int value) {
		configuration.setProperty(KEY_MENCODER_MAX_THREADS, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setLanguage(String value) {
		configuration.setProperty(KEY_LANGUAGE, value);
		Locale.setDefault(new Locale(getLanguage()));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getThumbnailSeekPos() {
		return getInt(KEY_THUMBNAIL_SEEK_POS, 1);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setThumbnailSeekPos(int value) {
		configuration.setProperty(KEY_THUMBNAIL_SEEK_POS, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isMencoderAss() {
		return getBoolean(KEY_MENCODER_ASS, Platform.isWindows() || Platform.isMac());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isMencoderDisableSubs() {
		return getBoolean(KEY_MENCODER_DISABLE_SUBS, false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isMencoderUsePcm() {
		return getBoolean(KEY_MENCODER_USE_PCM, false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isMencoderUsePcmForHQAudioOnly() {
		return getBoolean(KEY_MENCODER_USE_PCM_FOR_HQ_AUDIO_ONLY, false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getMencoderFont() {
		return getString(KEY_MENCODER_FONT, "");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getMencoderAudioLanguages() {
		return getString(KEY_MENCODER_AUDIO_LANGS, getDefaultLanguages());
	}

	/**
	 * Returns a string of comma separated audio or subtitle languages,
	 * ordered by priority. 
	 * @return The string of languages.
	 */
	private String getDefaultLanguages() {
		if ("fr".equals(getLanguage())) {
			return "fre,jpn,ger,eng,und";
		} else {
			return "eng,fre,jpn,ger,und";
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getMencoderSubLanguages() {
		return getString(KEY_MENCODER_SUB_LANGS, getDefaultLanguages());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getMencoderForcedSubLanguage() {
		return getString(KEY_MENCODER_FORCED_SUB_LANG, getLanguage());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getMencoderForcedSubTags() {
  		return getString(KEY_MENCODER_FORCED_SUB_TAGS, "forced");
  	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getMencoderAudioSubLanguages() {
		return getString(KEY_MENCODER_AUDIO_SUB_LANGS, "");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isMencoderSubFribidi() {
		return getBoolean(KEY_MENCODER_SUB_FRIBIDI, false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getMencoderSubCp() {
		return getString(KEY_MENCODER_SUB_CP, "cp1252");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isMencoderFontConfig() {
		return getBoolean(KEY_MENCODER_FONT_CONFIG, true);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setMencoderForceFps(boolean value) {
		configuration.setProperty(KEY_MENCODER_FORCE_FPS, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isMencoderForceFps() {
		return getBoolean(KEY_MENCODER_FORCE_FPS, false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setMencoderAudioLanguages(String value) {
		configuration.setProperty(KEY_MENCODER_AUDIO_LANGS, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setMencoderSubLanguages(String value) {
		configuration.setProperty(KEY_MENCODER_SUB_LANGS, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setMencoderForcedSubLanguage(String value) {
		configuration.setProperty(KEY_MENCODER_FORCED_SUB_LANG, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setMencoderForcedSubTags(String value) {
		configuration.setProperty(KEY_MENCODER_FORCED_SUB_TAGS, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setMencoderAudioSubLanguages(String value) {
		configuration.setProperty(KEY_MENCODER_AUDIO_SUB_LANGS, value);
	}

	/**
	 * @deprecated Use {@link #getMencoderCustomOptions()} instead.
	 * <p>
	 * Returns custom commandline options to pass on to MEncoder.
	 * @return The custom options string.
	 */
	@Deprecated
	public String getMencoderDecode() {
		return getMencoderCustomOptions();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getMencoderCustomOptions() {
		return getString(KEY_MENCODER_CUSTOM_OPTIONS, "");
	}

	/**
	 * @deprecated Use {@link #setMencoderCustomOptions(String)} instead.
	 * <p>
	 * Sets custom commandline options to pass on to MEncoder.
	 * @param value The custom options string.
	 */
	@Deprecated
	public void setMencoderDecode(String value) {
		setMencoderCustomOptions(value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setMencoderCustomOptions(String value) {
		configuration.setProperty(KEY_MENCODER_CUSTOM_OPTIONS, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setMencoderSubCp(String value) {
		configuration.setProperty(KEY_MENCODER_SUB_CP, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setMencoderSubFribidi(boolean value) {
		configuration.setProperty(KEY_MENCODER_SUB_FRIBIDI, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setMencoderFont(String value) {
		configuration.setProperty(KEY_MENCODER_FONT, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setMencoderAss(boolean value) {
		configuration.setProperty(KEY_MENCODER_ASS, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setMencoderFontConfig(boolean value) {
		configuration.setProperty(KEY_MENCODER_FONT_CONFIG, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setMencoderDisableSubs(boolean value) {
		configuration.setProperty(KEY_MENCODER_DISABLE_SUBS, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setMencoderUsePcm(boolean value) {
		configuration.setProperty(KEY_MENCODER_USE_PCM, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setMencoderUsePcmForHQAudioOnly(boolean value) {
		configuration.setProperty(KEY_MENCODER_USE_PCM_FOR_HQ_AUDIO_ONLY, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isArchiveBrowsing() {
		return getBoolean(KEY_OPEN_ARCHIVES, false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setArchiveBrowsing(boolean value) {
		configuration.setProperty(KEY_OPEN_ARCHIVES, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isMencoderYadif() {
		return getBoolean(KEY_MENCODER_YADIF, false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setMencoderYadif(boolean value) {
		configuration.setProperty(KEY_MENCODER_YADIF, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isMencoderScaler() {
		return getBoolean(KEY_MENCODER_SCALER, false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setMencoderScaler(boolean value) {
		configuration.setProperty(KEY_MENCODER_SCALER, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getMencoderScaleX() {
		return getInt(KEY_MENCODER_SCALEX, 0);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setMencoderScaleX(int value) {
		configuration.setProperty(KEY_MENCODER_SCALEX, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getMencoderScaleY() {
		return getInt(KEY_MENCODER_SCALEY, 0);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setMencoderScaleY(int value) {
		configuration.setProperty(KEY_MENCODER_SCALEY, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getAudioChannelCount() {
		return getInt(KEY_AUDIO_CHANNEL_COUNT, 6);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setAudioChannelCount(int value) {
		configuration.setProperty(KEY_AUDIO_CHANNEL_COUNT, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setAudioBitrate(int value) {
		configuration.setProperty(KEY_AUDIO_BITRATE, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getMaximumBitrate() {
		return getString(KEY_MAX_BITRATE, "110");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setMaximumBitrate(String value) {
		configuration.setProperty(KEY_MAX_BITRATE, value);
	}

	/**
	 * @deprecated Use {@link #isThumbnailGenerationEnabled()} instead.
	 * <p>
	 * Returns true if thumbnail generation is enabled, false otherwise.
	 * This only determines whether a thumbnailer (e.g. dcraw, MPlayer)
	 * is used to generate thumbnails. It does not reflect whether
	 * thumbnails should be displayed or not.
	 *
	 * @return boolean indicating whether thumbnail generation is enabled.
	 */
	@Deprecated
	public boolean getThumbnailsEnabled() {
		return isThumbnailGenerationEnabled();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isThumbnailGenerationEnabled() {
		return getBoolean(KEY_THUMBNAIL_GENERATION_ENABLED, true);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Deprecated
	public void setThumbnailsEnabled(boolean value) {
		setThumbnailGenerationEnabled(value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setThumbnailGenerationEnabled(boolean value) {
		configuration.setProperty(KEY_THUMBNAIL_GENERATION_ENABLED, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean getImageThumbnailsEnabled() {
		return getBoolean(KEY_IMAGE_THUMBNAILS_ENABLED, true);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setImageThumbnailsEnabled(boolean value) {
		configuration.setProperty(KEY_IMAGE_THUMBNAILS_ENABLED, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getNumberOfCpuCores() {
		int nbcores = Runtime.getRuntime().availableProcessors();
		if (nbcores < 1) {
			nbcores = 1;
		}
		return getInt(KEY_NUMBER_OF_CPU_CORES, nbcores);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setNumberOfCpuCores(int value) {
		configuration.setProperty(KEY_NUMBER_OF_CPU_CORES, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Deprecated
	public boolean isTurboModeEnabled() {
		return getBoolean(KEY_TURBO_MODE_ENABLED, false);
	}

	/**
	 * @deprecated This method is not used anywhere.
	 */
	@Deprecated
	public void setTurboModeEnabled(boolean value) {
		configuration.setProperty(KEY_TURBO_MODE_ENABLED, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isMinimized() {
		return getBoolean(KEY_MINIMIZED, false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setMinimized(boolean value) {
		configuration.setProperty(KEY_MINIMIZED, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean getUseSubtitles() {
		return getBoolean(KEY_USE_SUBTITLES, true);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setUseSubtitles(boolean value) {
		configuration.setProperty(KEY_USE_SUBTITLES, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean getHideVideoSettings() {
		return getBoolean(KEY_HIDE_VIDEO_SETTINGS, true);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setHideVideoSettings(boolean value) {
		configuration.setProperty(KEY_HIDE_VIDEO_SETTINGS, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean getUseCache() {
		return getBoolean(KEY_USE_CACHE, false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setUseCache(boolean value) {
		configuration.setProperty(KEY_USE_CACHE, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setAvisynthConvertFps(boolean value) {
		configuration.setProperty(KEY_AVISYNTH_CONVERT_FPS, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean getAvisynthConvertFps() {
		return getBoolean(KEY_AVISYNTH_CONVERT_FPS, false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getAvisynthScript() {
		return getString(KEY_AVISYNTH_SCRIPT, DEFAULT_AVI_SYNTH_SCRIPT);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setAvisynthScript(String value) {
		configuration.setProperty(KEY_AVISYNTH_SCRIPT, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getCodecSpecificConfig() {
		return getString(KEY_CODEC_SPEC_SCRIPT, "");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setCodecSpecificConfig(String value) {
		configuration.setProperty(KEY_CODEC_SPEC_SCRIPT, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getMaxAudioBuffer() {
		return getInt(KEY_MAX_AUDIO_BUFFER, 100);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getMinStreamBuffer() {
		return getInt(KEY_MIN_STREAM_BUFFER, 1);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isFileBuffer() {
		String bufferType = getString(KEY_BUFFER_TYPE, "").trim();
		return bufferType.equals(BUFFER_TYPE_FILE);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setFfmpegSettings(String value) {
		configuration.setProperty(KEY_FFMPEG_SETTINGS, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getFfmpegSettings() {
		return getString(KEY_FFMPEG_SETTINGS, "-threads 2 -g 1 -qscale 1 -qmin 2");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isMencoderNoOutOfSync() {
		return getBoolean(KEY_MENCODER_NO_OUT_OF_SYNC, true);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setMencoderNoOutOfSync(boolean value) {
		configuration.setProperty(KEY_MENCODER_NO_OUT_OF_SYNC, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean getTrancodeBlocksMultipleConnections() {
		return configuration.getBoolean(KEY_TRANSCODE_BLOCKS_MULTIPLE_CONNECTIONS, false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setTranscodeBlocksMultipleConnections(boolean value) {
		configuration.setProperty(KEY_TRANSCODE_BLOCKS_MULTIPLE_CONNECTIONS, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean getTrancodeKeepFirstConnections() {
		return configuration.getBoolean(KEY_TRANSCODE_KEEP_FIRST_CONNECTION, true);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setTrancodeKeepFirstConnections(boolean value) {
		configuration.setProperty(KEY_TRANSCODE_KEEP_FIRST_CONNECTION, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getCharsetEncoding() {
		return getString(KEY_CHARSET_ENCODING, "850");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setCharsetEncoding(String value) {
		configuration.setProperty(KEY_CHARSET_ENCODING, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isMencoderIntelligentSync() {
		return getBoolean(KEY_MENCODER_INTELLIGENT_SYNC, true);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setMencoderIntelligentSync(boolean value) {
		configuration.setProperty(KEY_MENCODER_INTELLIGENT_SYNC, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getFfmpegAlternativePath() {
		return getString(KEY_FFMPEG_ALTERNATIVE_PATH, null);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setFfmpegAlternativePath(String value) {
		configuration.setProperty(KEY_FFMPEG_ALTERNATIVE_PATH, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean getSkipLoopFilterEnabled() {
		return getBoolean(KEY_SKIP_LOOP_FILTER_ENABLED, false);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<String> getSkipNetworkInterfaces() {
		return getStringList(KEY_SKIP_NETWORK_INTERFACES, "tap,vmnet,vnic");
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setSkipLoopFilterEnabled(boolean value) {
		configuration.setProperty(KEY_SKIP_LOOP_FILTER_ENABLED, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getMencoderMainSettings() {
		return getString(KEY_MENCODER_MAIN_SETTINGS, "keyint=5:vqscale=1:vqmin=2");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setMencoderMainSettings(String value) {
		configuration.setProperty(KEY_MENCODER_MAIN_SETTINGS, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getMencoderVobsubSubtitleQuality() {
		return getString(KEY_MENCODER_VOBSUB_SUBTITLE_QUALITY, "3");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setMencoderVobsubSubtitleQuality(String value) {
		configuration.setProperty(KEY_MENCODER_VOBSUB_SUBTITLE_QUALITY, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getMencoderOverscanCompensationWidth() {
		return getString(KEY_MENCODER_OVERSCAN_COMPENSATION_WIDTH, "0");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setMencoderOverscanCompensationWidth(String value) {
		if (value.trim().length() == 0) {
			value = "0";
		}
		configuration.setProperty(KEY_MENCODER_OVERSCAN_COMPENSATION_WIDTH, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getMencoderOverscanCompensationHeight() {
		return getString(KEY_MENCODER_OVERSCAN_COMPENSATION_HEIGHT, "0");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setMencoderOverscanCompensationHeight(String value) {
		if (value.trim().length() == 0) {
			value = "0";
		}
		configuration.setProperty(KEY_MENCODER_OVERSCAN_COMPENSATION_HEIGHT, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setEnginesAsList(ArrayList<String> enginesAsList) {
		configuration.setProperty(KEY_ENGINES, listToString(enginesAsList));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<String> getEnginesAsList(SystemUtils registry) {
		List<String> engines = stringToList(getString(KEY_ENGINES, "mencoder,avsmencoder,tsmuxer,ffmpegaudio,mplayeraudio,tsmuxeraudio,vlcvideo,mencoderwebvideo,mplayervideodump,mplayerwebaudio,vlcaudio,ffmpegdvrmsremux,rawthumbs"));
		engines = hackAvs(registry, engines);
		return engines;
	}

	private static String listToString(List<String> enginesAsList) {
		return StringUtils.join(enginesAsList, LIST_SEPARATOR);
	}

	private static List<String> stringToList(String input) {
		List<String> output = new ArrayList<String>();
		Collections.addAll(output, StringUtils.split(input, LIST_SEPARATOR));
		return output;
	}
	// TODO: Get this out of here
	private static boolean avsHackLogged = false;

	// TODO: Get this out of here
	private static List<String> hackAvs(SystemUtils registry, List<String> input) {
		List<String> toBeRemoved = new ArrayList<String>();
		for (String engineId : input) {
			if (engineId.startsWith("avs") && !registry.isAvis() && Platform.isWindows()) {
				if (!avsHackLogged) {
					LOGGER.info("AviSynth is not installed. You cannot use " + engineId + " as a transcoding engine.");
					avsHackLogged = true;
				}
				toBeRemoved.add(engineId);
			}
		}
		List<String> output = new ArrayList<String>();
		output.addAll(input);
		output.removeAll(toBeRemoved);
		return output;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void save() throws ConfigurationException {
		configuration.save();
		LOGGER.info("Configuration saved to: " + PROFILE_PATH);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getFolders() {
		return getString(KEY_FOLDERS, "");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setFolders(String value) {
		configuration.setProperty(KEY_FOLDERS, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getNetworkInterface() {
		return getString(KEY_NETWORK_INTERFACE, "");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setNetworkInterface(String value) {
		configuration.setProperty(KEY_NETWORK_INTERFACE, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isHideEngineNames() {
		return getBoolean(KEY_HIDE_ENGINENAMES, false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setHideEngineNames(boolean value) {
		configuration.setProperty(KEY_HIDE_ENGINENAMES, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isHideExtensions() {
		return getBoolean(KEY_HIDE_EXTENSIONS, false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setHideExtensions(boolean value) {
		configuration.setProperty(KEY_HIDE_EXTENSIONS, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getShares() {
		return getString(KEY_SHARES, "");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setShares(String value) {
		configuration.setProperty(KEY_SHARES, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getNoTranscode() {
		return getString(KEY_NOTRANSCODE, "");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setNoTranscode(String value) {
		configuration.setProperty(KEY_NOTRANSCODE, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getForceTranscode() {
		return getString(KEY_FORCETRANSCODE, "");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setForceTranscode(String value) {
		configuration.setProperty(KEY_FORCETRANSCODE, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setMencoderMT(boolean value) {
		configuration.setProperty(KEY_MENCODER_MT, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean getMencoderMT() {
		boolean isMultiCore = getNumberOfCpuCores() > 1;
		return getBoolean(KEY_MENCODER_MT, isMultiCore);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setRemuxAC3(boolean value) {
		configuration.setProperty(KEY_MENCODER_REMUX_AC3, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isRemuxAC3() {
		return getBoolean(KEY_MENCODER_REMUX_AC3, true);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setMencoderRemuxMPEG2(boolean value) {
		configuration.setProperty(KEY_MENCODER_REMUX_MPEG2, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isMencoderRemuxMPEG2() {
		return getBoolean(KEY_MENCODER_REMUX_MPEG2, true);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setDisableFakeSize(boolean value) {
		configuration.setProperty(KEY_DISABLE_FAKESIZE, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isDisableFakeSize() {
		return getBoolean(KEY_DISABLE_FAKESIZE, false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setMencoderAssDefaultStyle(boolean value) {
		configuration.setProperty(KEY_MENCODER_ASS_DEFAULTSTYLE, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isMencoderAssDefaultStyle() {
		return getBoolean(KEY_MENCODER_ASS_DEFAULTSTYLE, true);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getMEncoderOverscan() {
		return getInt(KEY_OVERSCAN, 0);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setMEncoderOverscan(int value) {
		configuration.setProperty(KEY_OVERSCAN, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getSortMethod() {
		return getInt(KEY_SORT_METHOD, 0);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setSortMethod(int value) {
		configuration.setProperty(KEY_SORT_METHOD, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getAudioThumbnailMethod() {
		return getInt(KEY_AUDIO_THUMBNAILS_METHOD, 0);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setAudioThumbnailMethod(int value) {
		configuration.setProperty(KEY_AUDIO_THUMBNAILS_METHOD, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getAlternateThumbFolder() {
		return getString(KEY_ALTERNATE_THUMB_FOLDER, "");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setAlternateThumbFolder(String value) {
		configuration.setProperty(KEY_ALTERNATE_THUMB_FOLDER, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getAlternateSubsFolder() {
		return getString(KEY_ALTERNATE_SUBS_FOLDER, "");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setAlternateSubsFolder(String value) {
		configuration.setProperty(KEY_ALTERNATE_SUBS_FOLDER, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setDTSEmbedInPCM(boolean value) {
		configuration.setProperty(KEY_EMBED_DTS_IN_PCM, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isDTSEmbedInPCM() {
		return getBoolean(KEY_EMBED_DTS_IN_PCM, false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setMencoderMuxWhenCompatible(boolean value) {
		configuration.setProperty(KEY_MENCODER_MUX_COMPATIBLE, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isMencoderMuxWhenCompatible() {
		return getBoolean(KEY_MENCODER_MUX_COMPATIBLE, true);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setMuxAllAudioTracks(boolean value) {
		configuration.setProperty(KEY_MUX_ALLAUDIOTRACKS, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isMuxAllAudioTracks() {
		return getBoolean(KEY_MUX_ALLAUDIOTRACKS, false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setUseMplayerForVideoThumbs(boolean value) {
		configuration.setProperty(KEY_USE_MPLAYER_FOR_THUMBS, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isUseMplayerForVideoThumbs() {
		return getBoolean(KEY_USE_MPLAYER_FOR_THUMBS, false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getIpFilter() {
		return getString(KEY_IP_FILTER, "");
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized IpFilter getIpFiltering() {
	    filter.setRawFilter(getIpFilter());
	    return filter;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setIpFilter(String value) {
		configuration.setProperty(KEY_IP_FILTER, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setPreventsSleep(boolean value) {
		configuration.setProperty(KEY_PREVENTS_SLEEP, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isPreventsSleep() {
		return getBoolean(KEY_PREVENTS_SLEEP, false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setHTTPEngineV2(boolean value) {
		configuration.setProperty(KEY_HTTP_ENGINE_V2, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isHTTPEngineV2() {
		return getBoolean(KEY_HTTP_ENGINE_V2, true);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean getIphotoEnabled() {
		return getBoolean(KEY_IPHOTO_ENABLED, false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setIphotoEnabled(boolean value) {
		configuration.setProperty(KEY_IPHOTO_ENABLED, value);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean getApertureEnabled() {
		return getBoolean(KEY_APERTURE_ENABLED, false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setApertureEnabled(boolean value) {
		configuration.setProperty(KEY_APERTURE_ENABLED, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean getItunesEnabled() {
		return getBoolean(KEY_ITUNES_ENABLED, false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setItunesEnabled(boolean value) {
		configuration.setProperty(KEY_ITUNES_ENABLED, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isHideEmptyFolders() {
		return getBoolean(PmsConfigurationImpl.KEY_HIDE_EMPTY_FOLDERS, false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setHideEmptyFolders(final boolean value) {
		this.configuration.setProperty(PmsConfigurationImpl.KEY_HIDE_EMPTY_FOLDERS, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isHideMediaLibraryFolder() {
		return getBoolean(PmsConfigurationImpl.KEY_HIDE_MEDIA_LIBRARY_FOLDER, false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setHideMediaLibraryFolder(final boolean value) {
		this.configuration.setProperty(PmsConfigurationImpl.KEY_HIDE_MEDIA_LIBRARY_FOLDER, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean getHideTranscodeEnabled() {
		return getBoolean(KEY_HIDE_TRANSCODE_FOLDER, false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setHideTranscodeEnabled(boolean value) {
		configuration.setProperty(KEY_HIDE_TRANSCODE_FOLDER, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isDvdIsoThumbnails() {
		return getBoolean(KEY_DVDISO_THUMBNAILS, false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setDvdIsoThumbnails(boolean value) {
		configuration.setProperty(KEY_DVDISO_THUMBNAILS, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object getCustomProperty(String property) {
		return configuration.getProperty(property);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setCustomProperty(String property, Object value) {
		configuration.setProperty(property, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isChapterSupport() {
		return getBoolean(KEY_CHAPTER_SUPPORT, false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setChapterSupport(boolean value) {
		configuration.setProperty(KEY_CHAPTER_SUPPORT, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getChapterInterval() {
		return getInt(KEY_CHAPTER_INTERVAL, 5);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setChapterInterval(int value) {
		configuration.setProperty(KEY_CHAPTER_INTERVAL, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getSubsColor() {
		return getInt(KEY_SUBS_COLOR, 0xffffffff);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setSubsColor(int value) {
		configuration.setProperty(KEY_SUBS_COLOR, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isFix25FPSAvMismatch() {
		return getBoolean(KEY_FIX_25FPS_AV_MISMATCH, false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setFix25FPSAvMismatch(boolean value) {
		configuration.setProperty(KEY_FIX_25FPS_AV_MISMATCH, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getVideoTranscodeStartDelay() {
		return getInt(KEY_VIDEOTRANSCODE_START_DELAY, 6);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setVideoTranscodeStartDelay(int value) {
		configuration.setProperty(KEY_VIDEOTRANSCODE_START_DELAY, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isAudioResample() {
		return getBoolean(KEY_AUDIO_RESAMPLE, true);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setAudioResample(boolean value) {
		configuration.setProperty(KEY_AUDIO_RESAMPLE, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getRendererDefault() {
		return getString(KEY_RENDERER_DEFAULT, "");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setRendererDefault(String value) {
		configuration.setProperty(KEY_RENDERER_DEFAULT, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isRendererForceDefault() {
		return getBoolean(KEY_RENDERER_FORCE_DEFAULT, false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setRendererForceDefault(boolean value) {
		configuration.setProperty(KEY_RENDERER_FORCE_DEFAULT, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getVirtualFolders() {
		return getString(KEY_VIRTUAL_FOLDERS, "");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getProfilePath() {
		return PROFILE_PATH;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getProfileDirectory() {
		return PROFILE_DIRECTORY;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getPluginDirectory() {
		return getString(KEY_PLUGIN_DIRECTORY, PropertiesUtil.getProjectProperties().get("project.plugins.dir"));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setPluginDirectory(String value) {
		configuration.setProperty(KEY_PLUGIN_DIRECTORY, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getProfileName() {
		if (HOSTNAME == null) { // calculate this lazily
			try {
				HOSTNAME = InetAddress.getLocalHost().getHostName();
			} catch (UnknownHostException e) {
				LOGGER.info("Can't determine hostname");
				HOSTNAME = "unknown host";
			}
		}

		return getString(KEY_PROFILE_NAME, HOSTNAME);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isAutoUpdate() {
		return Build.isUpdatable() && configuration.getBoolean(KEY_AUTO_UPDATE, false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setAutoUpdate(boolean value) {
		configuration.setProperty(KEY_AUTO_UPDATE, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getIMConvertPath() {
		return programPaths.getIMConvertPath();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getUpnpPort() {
		return getInt(KEY_UPNP_PORT, 1900);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getUuid() {
		return getString(KEY_UUID, null);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setUuid(String value){
		configuration.setProperty(KEY_UUID, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void addConfigurationListener(ConfigurationListener l) {
		configuration.addConfigurationListener(l);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void removeConfigurationListener(ConfigurationListener l) {
		configuration.removeConfigurationListener(l);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean initBufferMax() {
		return getBoolean(KEY_BUFFER_MAX, false);
	}

	/**
	 * {@inheritDoc}
	 */
	public Set<String> getNeedReloadFlags() {
		return NEED_RELOAD_FLAGS;
	}
}
