/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.pms.plugin.fileimport.thetvdb.fileparser;

/**
 *
 * @author Corey
 */
public class EpisodeFile {

    private int episode;
    private int season;
    private String series;
    private String title;
    private String seasonName;

    /**
     * @return the episode
     */
    public int getEpisode() {
        return episode;
    }

    /**
     * @return the season
     */
    public int getSeason() {
        return season;
    }

    /**
     * @param season the season to set
     */
    public void setSeason(int season) {
        this.season = season;
    }

    /**
     * @return the series
     */
    public String getSeries() {
        return series;
    }

    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param episode the episode to set
     */
    public void setEpisode(int episode) {
        this.episode = episode;
    }

    /**
     * @param series the series to set
     */
    public void setSeries(String series) {
        this.series = series;
    }

    /**
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @return the seasonName
     */
    public String getSeasonName() {
        return seasonName;
    }

    /**
     * @param seasonName the seasonName to set
     */
    public void setSeasonName(String seasonName) {
        this.seasonName = seasonName;
    }
}
