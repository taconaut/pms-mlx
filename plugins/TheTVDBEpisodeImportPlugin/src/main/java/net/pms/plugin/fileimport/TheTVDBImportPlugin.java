package net.pms.plugin.fileimport;

import java.io.IOException;
import java.util.List;
import javax.swing.Icon;
import javax.swing.JComponent;
import net.pms.medialibrary.commons.enumarations.FileProperty;
import net.pms.medialibrary.commons.enumarations.FileType;
import net.pms.medialibrary.commons.exceptions.FileImportException;
import net.pms.util.PmsProperties;

import com.moviejukebox.thetvdb.TheTVDB;
import com.moviejukebox.thetvdb.model.Episode;
import com.moviejukebox.thetvdb.model.Series;
import java.util.*;
import javax.swing.ImageIcon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.pms.plugin.fileimport.thetvdb.fileparser.EpisodeFile;
import net.pms.plugin.fileimport.thetvdb.fileparser.EpisodeFileParser;
import net.pms.plugin.fileimport.thetvdb.fileparser.EpisodeFileParserException;
import net.pms.plugin.fileimport.tvdb.configuration.GlobalConfiguration;
import net.pms.plugin.fileimport.tvdb.gui.GlobalConfigurationPanel;
import net.pms.plugins.FileImportPlugin;

/**
 *
 * @author Corey
 */
public class TheTVDBImportPlugin implements FileImportPlugin {

    private static final Logger log = LoggerFactory.getLogger(TheTVDBImportPlugin.class);
    private TheTVDB tvDB = new TheTVDB("D19EF2AFF971007D");
    /**
     * The found episode object
     */
    private Episode episode;
    /**
     * The found series. (Serial is the singular of series, right?)
     */
    private Series serial;
    private String lang = "en";
    public static final ResourceBundle messages = ResourceBundle.getBundle("net.pms.plugin.fileimport.thetvdb.lang.messages");
    /**
     * Holds only the project version. It's used to always use the maven build
     * number in code
     */
    private static final PmsProperties properties = new PmsProperties();

    static {
        try {
            properties.loadFromResourceFile("/thetvdbimportplugin.properties", TheTVDBImportPlugin.class);
        } catch (IOException e) {
            log.error("Could not load thetvdbimportplugin.properties", e);
        }
    }
    /**
     * GUI
     */
    private GlobalConfigurationPanel pGlobalConfiguration;
    /**
     * The global configuration is shared amongst all plugin instances.
     */
    private static final GlobalConfiguration globalConfig;

    static {
        globalConfig = new GlobalConfiguration();
        try {
            globalConfig.load();
        } catch (IOException e) {
            log.error("Failed to load global configuration", e);
        }
    }

    /**
     * @todo See if we can scroll the tag list in the GUI as the full tag list
     * pushed to config dialog off the bottom of the screen
     */
    /**
     * Available tags. (Shortened as the gui does not scroll the list).
     */
    private enum Tag {

        CombinedEpisodeNumber,
        CombinedSeason,
        DvdChapter,
        DvdDiscId,
        DvdEpisodeNumber,
        DvdSeason,
        EpImgFlag,
        EpisodeNumber,
        GuestStars,
        Language,
        ProductionCode,
        SeasonNumber,
        Writers,
        AbsoluteNumber,
        Filename,
        LastUpdated,
        SeriesId //,
//        SeasonId,
//        SeriesName,
//        Banner,
//        SeriesOverview,
//        FirstAired,
//        SeriesImdbId,
//        Actors,
//        SeriesZap2ItId,
//        AirsDayOfWeek,
//        AirsTime,
//        ContentRating,
//        Network,
//        SeriesRating,
//        Runtime,
//        Status,
//        Faxnart,
//        SeriesLastUpdated
    }

    public void importFile(String title, String filePath) throws FileImportException {
        serial = null;
        episode = null;
        logDebug("importing TheTVDB episode with file: " + filePath);

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
            List<Series> series = tvDB.searchSeries(fileObg.getSeries(), "en");
            if (series != null && series.size() > 0) {
                //we've found at least one result

                //use the first one
                serial = tvDB.getSeries(series.get(0).getId(), "en");

                //log the results received
                logInfo("Series matched for '" + fileObg.getSeries() + "' on TvDB has imdbDb='" + serial.getImdbId() + "', name='" + serial.getSeriesName() + "'.");
                episode = tvDB.getEpisode(serial.getId(), fileObg.getSeason(), fileObg.getEpisode(), lang);

                if (episode != null) {
                    //log the results received
                    logInfo("Episode matched for series '" + serial.getSeriesName() + "' Title='" + episode.getEpisodeName() + "'");
                }
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
        serial = null;
        episode = null;

        episode = tvDB.getEpisodeById(id, lang);
        serial = tvDB.getSeries(episode.getSeriesId(), lang);
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
        res.add(FileProperty.VIDEO_CERTIFICATION);
        res.add(FileProperty.VIDEO_COVERURL);
        res.add(FileProperty.VIDEO_DIRECTOR);
        res.add(FileProperty.VIDEO_GENRES);
        res.add(FileProperty.VIDEO_IMDBID);
        res.add(FileProperty.VIDEO_OVERVIEW);
        res.add(FileProperty.VIDEO_RATINGPERCENT);
        res.add(FileProperty.VIDEO_NAME);
        res.add(FileProperty.VIDEO_YEAR);
        return res;
    }

    @Override
    public Object getFileProperty(FileProperty property) {
        Object res = null;
        // return the proper object for every supported file property
        switch (property) {
            case VIDEO_CERTIFICATION:
                res = episode == null ? null : episode.getRating();
                break;
            case VIDEO_COVERURL:
                res = serial == null ? null : serial.getPoster();
                break;
            case VIDEO_DIRECTOR:
                res = episode == null ? null : episode.getDirectors();
                break;
            case VIDEO_GENRES:
                res = serial == null ? null : serial.getGenres();
                break;
            case VIDEO_IMDBID:
                res = episode == null ? null : episode.getImdbId();
                break;
            case VIDEO_OVERVIEW:
                res = episode == null ? null : episode.getOverview();
                break;
            case VIDEO_RATINGPERCENT:
                res = episode == null ? null : episode.getRating();
                break;
            case VIDEO_NAME:
                res = episode == null ? null : episode.getEpisodeName();
                break;
            case VIDEO_YEAR:
                res = episode == null ? null : episode.getFirstAired();
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
        if (tagName.equals(Tag.AbsoluteNumber.toString())) {
            res.add(episode.getAbsoluteNumber());
//        } else if (tagName.equals(Tag.Actors.toString())) {
//            res = serial.getActors();
//        } else if (tagName.equals(Tag.AirsDayOfWeek.toString())) {
//            res.add(serial.getAirsDayOfWeek().toString());
//        } else if (tagName.equals(Tag.AirsTime.toString())) {
//            res.add(serial.getAirsTime().toString());
//        } else if (tagName.equals(Tag.Banner.toString())) {
            res.add(serial.getBanner());
        } else if (tagName.equals(Tag.CombinedEpisodeNumber.toString())) {
            res.add(episode.getCombinedEpisodeNumber());
        } else if (tagName.equals(Tag.CombinedSeason.toString())) {
            res.add(episode.getCombinedSeason());
//        } else if (tagName.equals(Tag.ContentRating.toString())) {
//            res.add(serial.getContentRating());
        } else if (tagName.equals(Tag.DvdChapter.toString())) {
            res.add(episode.getDvdChapter());
        } else if (tagName.equals(Tag.DvdEpisodeNumber.toString())) {
            res.add(episode.getDvdEpisodeNumber());
        } else if (tagName.equals(Tag.DvdSeason.toString())) {
            res.add(episode.getDvdSeason());
        } else if (tagName.equals(Tag.EpImgFlag.toString())) {
            res.add(episode.getEpImgFlag());
        } else if (tagName.equals(Tag.EpisodeNumber.toString())) {
            res.add(Integer.toString(episode.getEpisodeNumber()));
//        } else if (tagName.equals(Tag.Fanart.toString())) {
//            res.add(serial.getFanart());
        } else if (tagName.equals(Tag.Filename.toString())) {
            res.add(episode.getFilename());
//        } else if (tagName.equals(Tag.FirstAired.toString())) {
//            res.add(serial.getFirstAired());
        } else if (tagName.equals(Tag.GuestStars.toString())) {
            res = episode.getGuestStars();
        } else if (tagName.equals(Tag.Language.toString())) {
            res.add(serial.getLanguage());
        } else if (tagName.equals(Tag.LastUpdated.toString())) {
            res.add(episode.getLastUpdated());
//        } else if (tagName.equals(Tag.Network.toString())) {
//            res.add(serial.getNetwork());
        } else if (tagName.equals(Tag.ProductionCode.toString())) {
            res.add(episode.getProductionCode());
//        } else if (tagName.equals(Tag.Runtime.toString())) {
//            res.add(serial.getRuntime());
//        } else if (tagName.equals(Tag.SeasonId.toString())) {
//            res.add(episode.getSeasonId());
        } else if (tagName.equals(Tag.SeasonNumber.toString())) {
            res.add(Integer.toString(episode.getSeasonNumber()));
        } else if (tagName.equals(Tag.SeriesId.toString())) {
            res.add(serial.getSeriesId());
//        } else if (tagName.equals(Tag.SeriesImdbId.toString())) {
//            res.add(serial.getImdbId());
//        } else if (tagName.equals(Tag.SeriesLastUpdated.toString())) {
//            res.add(serial.getLastUpdated());
//        } else if (tagName.equals(Tag.SeriesName.toString())) {
//            res.add(serial.getSeriesName());
//        } else if (tagName.equals(Tag.SeriesOverview.toString())) {
//            res.add(serial.getOverview());
//        } else if (tagName.equals(Tag.SeriesRating.toString())) {
//            res.add(serial.getRating());
//        } else if (tagName.equals(Tag.SeriesZap2ItId.toString())) {
//            res.add(serial.getZap2ItId());
//        } else if (tagName.equals(Tag.Status.toString())) {
//            res.add(serial.getStatus());
        } else if (tagName.equals(Tag.Writers.toString())) {
            res = episode.getWriters();
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
        return "theTvDB";
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
//        return pGlobalConfiguration;
        return null;
    }

    @Override
    public void saveConfiguration() {
    }

    @Override
    public boolean isPluginAvailable() {
        return true;
    }

    private void logDebug(String message) {
        if (log.isDebugEnabled()) {
            log.debug(message);
        }

    }

    private void logInfo(String message) {
        if (log.isInfoEnabled()) {
            log.info(message);
        }
    }
}
