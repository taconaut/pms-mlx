/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.pms.plugin.fileimport.thetvdb.fileparser;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Corey
 */
public class EpisodeFileParser {

    private String thePath;

    public EpisodeFileParser(String filePath) {
        thePath = filePath;
    }

    /**
     * @todo .... Build it better?
     * @return 
     * @throws EpisodeFileParserException 
     */
    public EpisodeFile parse() throws EpisodeFileParserException {
        EpisodeFile fileObj = new EpisodeFile();
        File theFile = new File(thePath);
        File sasonPath = theFile.getParentFile();
        File seriesPath = sasonPath.getParentFile();

        String fileName = theFile.getName();
        String seasonName = sasonPath.getName();
        String seriesName = seriesPath.getName();
        Pattern pattern = Pattern.compile(".*S([0-9]+)E([0-9]+).*");
        Matcher matcher = pattern.matcher(fileName);
        fileObj.setSeasonName(seasonName);
        fileObj.setSeries(seriesName);
        if (matcher.matches()) {
            int season = Integer.parseInt(matcher.group(1));
            int episode = Integer.parseInt(matcher.group(2));
            fileObj.setSeason(season);
            fileObj.setEpisode(episode);
        } else {
            throw new EpisodeFileParserException("Unable to parse file '" + thePath + "'");
        }

        return fileObj;
    }
}
