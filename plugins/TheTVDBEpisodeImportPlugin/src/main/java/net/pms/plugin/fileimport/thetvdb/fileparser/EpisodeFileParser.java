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
        Pattern pattern = Pattern.compile(".*[Ss]([0-9]+)[Ee]([0-9]+).*");
        Matcher matcher = pattern.matcher(fileName);
        if (matcher.matches()) {
            int season = Integer.parseInt(matcher.group(1));
            int episode = Integer.parseInt(matcher.group(2));
            fileObj.setSeason(season);
            fileObj.setEpisode(episode);

            fileObj.setSeasonName(sasonPath.getName());
            fileObj.setSeries(seriesPath.getName());
        } else {
            throw new EpisodeFileParserException("Unable to parse file '" + fileName + "'");
        }

        return fileObj;
    }
}
