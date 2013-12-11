package net.pms.plugin.fileimport;

import java.io.IOException;
import java.util.List;
import javax.swing.Icon;
import javax.swing.JComponent;

import net.pms.medialibrary.commons.enumarations.FileProperty;
import net.pms.medialibrary.commons.enumarations.FileType;
import net.pms.medialibrary.commons.exceptions.FileImportException;
import net.pms.util.PmsProperties;

import java.util.*;
import javax.swing.ImageIcon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.omertron.thetvdbapi.TheTVDBApi;
import com.omertron.thetvdbapi.model.Banner;
import com.omertron.thetvdbapi.model.Banners;
import com.omertron.thetvdbapi.model.BannerType;
import com.omertron.thetvdbapi.model.Episode;
import com.omertron.thetvdbapi.model.Series;

import net.pms.plugin.fileimport.thetvdb.configuration.GlobalConfiguration;
import net.pms.plugin.fileimport.thetvdb.fileparser.EpisodeFile;
import net.pms.plugin.fileimport.thetvdb.fileparser.EpisodeFileParser;
import net.pms.plugin.fileimport.thetvdb.fileparser.EpisodeFileParserException;
import net.pms.plugin.fileimport.thetvdb.gui.GlobalConfigurationPanel;
import net.pms.plugins.FileImportPlugin;

/**
 *
 * @author Corey
 */
public class TheTVDBImportPlugin implements FileImportPlugin {

    private static final Logger logger = LoggerFactory.getLogger(TheTVDBImportPlugin.class);
    private TheTVDBApi tvDB = new TheTVDBApi("D19EF2AFF971007D");
    /**
     * The found episode object
     */
    private Episode currentEpisode;
    private Series currentSeries;
    private String cover;
    
    public static final ResourceBundle messages = ResourceBundle.getBundle("net.pms.plugin.fileimport.thetvdb.lang.messages");
    
    // Holds only the project version. It's used to always use the maven buildnumber in code
    private static final PmsProperties properties = new PmsProperties();
    static {
        try {
            properties.loadFromResourceFile("/thetvdbimportplugin.properties", TheTVDBImportPlugin.class);
        } catch (IOException e) {
        	logger.error("Could not load thetvdbimportplugin.properties", e);
        }
    }
    
    private GlobalConfigurationPanel pGlobalConfiguration;
    
    // The global configuration is shared amongst all plugin instances.
    private static final GlobalConfiguration globalConfig;

    static {
        globalConfig = new GlobalConfiguration();
        try {
            globalConfig.load();
        } catch (IOException e) {
        	logger.error("Failed to load global configuration", e);
        }
    }

    /**
     * Available tags.
     */
    private enum Tag {
    	EpisodeNumber,
    	SeasonNumber,
    	FirstAired,
    	GuestStars,
        Writers,
    	Runtime,
    	Network,
    	SeriesName
    }

    public void importFile(String title, String filePath) throws FileImportException {
        currentSeries = null;
        currentEpisode = null;
        cover = null;

        logDebug("importing TheTVDB episode with file: " + filePath);

        Banners banners;
        boolean parseOk = true;
        EpisodeFileParser fileParser = new EpisodeFileParser(filePath);
        EpisodeFile fileObg = new EpisodeFile();
        try {
            fileObg = fileParser.parse();
        } catch (EpisodeFileParserException ex) {
            parseOk = false;
        }

        logDebug("Got season '" + fileObg.getSeason() + "'");
        logDebug("Got episode '" + fileObg.getEpisode() + "'");

        if (parseOk) {
            logDebug("Search TVDB for series '" + fileObg.getSeries() + "'");
            List<Series> series = tvDB.searchSeries(fileObg.getSeries(), globalConfig.getImportLanguage());
            if (series != null && series.size() > 0) {
                //we've found at least one result

                //use the first one
                currentSeries = tvDB.getSeries(series.get(0).getId(), globalConfig.getImportLanguage());

                //log the results received
                logInfo("Series matched for '" + fileObg.getSeries() + "' on TvDB has imdbDb='" + currentSeries.getImdbId() + "', name='" + currentSeries.getSeriesName() + "'.");
                currentEpisode = tvDB.getEpisode(currentSeries.getId(), fileObg.getSeason(), fileObg.getEpisode(), globalConfig.getImportLanguage());

                if (currentEpisode != null) {
                    //log the results received
                    logInfo("Episode matched for series '" + currentSeries.getSeriesName() + "' Title='" + currentEpisode.getEpisodeName() + "'");
                }

                // Find the most suitable cover
                banners = tvDB.getBanners(series.get(0).getId());
                if (!banners.getSeasonList().isEmpty()) {
                    for (Banner banner : banners.getSeasonList()) {
                        if ((banner.getSeason() == fileObg.getSeason()) && (banner.getBannerType2() == BannerType.Season)) {
                            cover = banner.getUrl();
                            break;
                        }
                    }
                } else {
                    cover = banners.getPosterList().get(0).getUrl();
                }
                if (cover == null) {
                    cover = currentSeries.getPoster();
                }
                logInfo("Using cover from " + cover);

                if (series.size() > 1) {
                    String seriesStr = "Other (not considered) matches are ";
                    for (int i = 1; i < series.size(); i++) {
                        seriesStr += String.format("id=%s, name='%s';", series.get(i).getId(), series.get(i).getSeriesName());
                    }
                    seriesStr = seriesStr.substring(0, seriesStr.length() - 2);
                    logInfo(seriesStr);
                }
            } else {
                throw new FileImportException(String.format("No TV Episode information found for title='%s'", title));
            }
        } else {
            throw new FileImportException(String.format("Unable to parse TV Episode information from file '%s'", filePath));
        }
    }

    @Override
    public void importFileById(String id) throws FileImportException {
        currentSeries = null;
        currentEpisode = null;

        currentEpisode = tvDB.getEpisodeById(id, globalConfig.getImportLanguage());
        currentSeries = tvDB.getSeries(currentEpisode.getSeriesId(), globalConfig.getImportLanguage());
    }

    @Override
    public boolean isImportByIdPossible() {
        return true;
    }

    @Override
    public boolean isSearchForFilePossible() {
        return false;
    }

    @Override
    public List<Object> searchForFile(String name) {
        List<Object> res = new ArrayList<Object>();
        return res;
    }

    @Override
    public void importFileBySearchObject(Object searchObject) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<FileProperty> getSupportedFileProperties() {
        //add all supported properties
        List<FileProperty> res = new ArrayList<FileProperty>();
        res.add(FileProperty.VIDEO_COVERURL);
        res.add(FileProperty.VIDEO_DIRECTOR);
        res.add(FileProperty.VIDEO_GENRES);
        res.add(FileProperty.VIDEO_IMDBID);
        res.add(FileProperty.VIDEO_OVERVIEW);
        res.add(FileProperty.VIDEO_RATINGPERCENT);
        res.add(FileProperty.VIDEO_NAME);
        res.add(FileProperty.VIDEO_SORTNAME);
        res.add(FileProperty.VIDEO_YEAR);
        return res;
    }

    @Override
    public Object getFileProperty(FileProperty property) {
        Object res = null;
        // return the proper object for every supported file property
		switch (property) {
		case VIDEO_COVERURL:
			res = cover;
			break;
		case VIDEO_DIRECTOR:
			res = currentEpisode == null && currentEpisode.getDirectors().size() > 0 ? null : currentEpisode.getDirectors().get(0);
			break;
		case VIDEO_GENRES:
			res = currentSeries == null ? null : currentSeries.getGenres();
			break;
		case VIDEO_IMDBID:
			res = currentEpisode == null ? null : currentEpisode.getImdbId();
			break;
		case VIDEO_OVERVIEW:
			res = currentEpisode == null ? null : currentEpisode.getOverview();
			break;
		case VIDEO_RATINGPERCENT:
			if (currentEpisode != null && currentEpisode.getRating() != null && !currentEpisode.getRating().equals("")) {
				try {
					double rating = Double.parseDouble(currentEpisode.getRating());
					res = (int) (10 * rating);
				} catch (Exception ex) {
					logger.error("Failed to parse rating as a double. value=" + currentEpisode.getRating());
				}
			}
			break;
		case VIDEO_NAME:
			res = currentEpisode == null ? null : currentEpisode.getEpisodeName();
			break;
		case VIDEO_SORTNAME:
			res = currentEpisode == null ? null : String.format("%d%03d", currentEpisode.getSeasonNumber(), currentEpisode.getEpisodeNumber());
			break;
		case VIDEO_YEAR:
			if (currentEpisode != null && currentEpisode.getFirstAired() != null && !currentEpisode.getFirstAired().equals("")) {
				try {
					res = Integer.parseInt(currentEpisode.getFirstAired().substring(0, 4));
				} catch (Exception ex) {
					logger.error("Failed to parse the year in first air date. value=" + currentEpisode.getFirstAired());
				}
			}
			break;
		default:
			logger.warn("Unexpected FileProperty received: " + property);
			break;
		}

        return res;
    }

    @Override
    public List<String> getSupportedTags(FileType fileType) {
        List<String> res = new ArrayList<String>();
        for (Tag t : Tag.values()) {
            res.add(t.toString());
        }

        return res;
    }

    @Override
    public List<String> getTags(String tagName) {
        List<String> res = new ArrayList<String>();
        if (tagName.equals(Tag.EpisodeNumber.toString())) {
            res.add(String.valueOf(currentEpisode.getEpisodeNumber()));
        } else if (tagName.equals(Tag.SeasonNumber.toString())) {
            res.add(String.valueOf(currentEpisode.getSeasonNumber()));
        } else if (tagName.equals(Tag.FirstAired.toString())) {
            res.add(String.valueOf(currentEpisode.getFirstAired()));
        } else if (tagName.equals(Tag.GuestStars.toString())) {
            res.addAll(currentEpisode.getGuestStars());
        } else if (tagName.equals(Tag.Writers.toString())) {
            res.addAll(currentEpisode.getWriters());
        } else if (tagName.equals(Tag.Runtime.toString())) {
            res.add(String.valueOf(currentSeries.getRuntime()));
        } else if (tagName.equals(Tag.Network.toString())) {
            res.add(String.valueOf(currentSeries.getNetwork()));
        } else if (tagName.equals(Tag.SeriesName.toString())) {
            res.add(String.valueOf(currentSeries.getSeriesName()));
        }
        
        return res;
    }

    @Override
    public List<FileType> getSupportedFileTypes() {
        return Arrays.asList(FileType.VIDEO);
    }

    public int getMinPollingIntervalMs() {
        return 1000;
    }

    public String getName() {
        return "TheTVDB";
    }

    @Override
    public String getVersion() {
        return properties.get("project.version");
    }

    @Override
    public Icon getPluginIcon() {
        return new ImageIcon(getClass().getResource("/thetvdb-32.png"));
    }

    @Override
    public String getShortDescription() {
        return messages.getString("TheTVDBEpisodeImportPlugin.ShortDescription");
    }

    @Override
    public String getLongDescription() {
        return messages.getString("TheTVDBEpisodeImportPlugin.LongDescription");
    }

    @Override
    public String getUpdateUrl() {
        return null;
    }

    @Override
    public String getWebSiteUrl() {
        return null;
    }

    @Override
    public void initialize() {
    }

    @Override
    public void shutdown() {
    }

    @Override
    public JComponent getGlobalConfigurationPanel() {
        if (pGlobalConfiguration == null) {
            pGlobalConfiguration = new GlobalConfigurationPanel(globalConfig);
        }
        pGlobalConfiguration.applyConfig();

        return pGlobalConfiguration;
    }

    @Override
    public void saveConfiguration() {
		if(pGlobalConfiguration != null) {
			pGlobalConfiguration.updateConfiguration(globalConfig);
			try {
				globalConfig.save();
			} catch (IOException e) {
				logger.error("Failed to save global configuration", e);
			}
		}
    }

    @Override
    public boolean isPluginAvailable() {
        return true;
    }

    private void logDebug(String message) {
        if (logger.isDebugEnabled()) {
        	logger.debug(message);
        }

    }

    private void logInfo(String message) {
        if (logger.isInfoEnabled()) {
        	logger.info(message);
        }
    }
}
