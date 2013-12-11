package net.pms.plugin.fileimport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.omertron.themoviedbapi.TheMovieDbApi;
import com.omertron.themoviedbapi.results.*;
import com.omertron.themoviedbapi.model.MovieDb;
import com.omertron.themoviedbapi.MovieDbException;

import net.pms.medialibrary.commons.enumarations.FileProperty;
import net.pms.medialibrary.commons.enumarations.FileType;
import net.pms.medialibrary.commons.exceptions.FileImportException;
import net.pms.plugin.fileimport.tmdb.configuration.GlobalConfiguration;
import net.pms.plugin.fileimport.tmdb.gui.GlobalConfigurationPanel;
import net.pms.plugins.FileImportPlugin;
import net.pms.util.PmsProperties;

/** 
 * Class used to collect information about a movie from tmdb
 * 
 * @author pw
 *
 */
public class TmdbMovieImportPlugin implements FileImportPlugin {
	private static final Logger logger = LoggerFactory.getLogger(TmdbMovieImportPlugin.class);
	public static final ResourceBundle messages = ResourceBundle.getBundle("net.pms.plugin.fileimport.tmdb.lang.messages");

	/** Holds only the project version. It's used to always use the maven build number in code */
	private static final PmsProperties properties = new PmsProperties();
	static {
		try {
			properties.loadFromResourceFile("/tmdbmovieimportplugin.properties", TmdbMovieImportPlugin.class);
		} catch (IOException e) {
			logger.error("Could not load filesystemfolderplugin.properties", e);
		}
	}
	
	//available tags
	private enum Tag {
		Actor,
		Studio,
		Author,
		Producer
	}
	
	// The TMDB API class
	private TheMovieDbApi api;

	//the tmdb movie having been imported
	private MovieDb movie;
	
	//constants used to manage the min polling interval
	private final int MIN_POLLING_INTERVAL_MS = 1000;
	private final int POLLING_INCREMENT_MS = 200;

	//tmdb states that 10 requests every 10 seconds per IP are allowed.
	//http://help.themoviedb.org/kb/general/api-request-limits
	private int currentPollingIntervalMs = MIN_POLLING_INTERVAL_MS;

	private final int MAX_RETRIES = 3;
	private int nbRetriesDone = 0;
	private GlobalConfigurationPanel pGlobalConfiguration;

	/** The global configuration is shared amongst all plugin instances. */
	private static final GlobalConfiguration globalConfig;
	static {
		globalConfig = new GlobalConfiguration();
		try {
			globalConfig.load();
		} catch (IOException e) {
			logger.error("Failed to load global configuration", e);
		}
	}

	@Override
	public String getName() {
		return "tmdb";
	}

	@Override
    public void importFile(String title, String filePath) throws FileImportException {
		if(logger.isDebugEnabled()) logger.debug("importing TMDb movie with title: " + title);
		
		//delete information which might still be cached since the last query
		movie = null;
		
	    try {
	    	//search for the title
	        TmdbResultsList<MovieDb> movies = api.searchMovie(title, 0 , globalConfig.getImportLanguage(), true, 0);
	        int size = movies.getTotalResults();
	        
			if (movies != null && size > 0) {
				//we've found at least one result
				
				//use the first one
				movie = api.getMovieInfo(movies.getResults().get(0).getId(), globalConfig.getImportLanguage(), "casts,trailers");
				
				//log the results received
				String moviesStr = String.format("Movie matched for '%s' on TMDb has id=%s, name='%s'", title, movies.getResults().get(0).getId(), movies.getResults().get(0).getTitle());
				if(size > 1){
					moviesStr += ". other (not considered) matches are ";
					for(int i = 0; i < size; i++) {
						moviesStr += String.format("id=%s, name='%s';", movies.getResults().get(i).getId(), movies.getResults().get(i).getTitle());
					}
					moviesStr = moviesStr.substring(0, moviesStr.length() - 2);
				}
				
				//set the polling interval to the min value and reset nbRetriesDone if we could execute the query
				currentPollingIntervalMs = MIN_POLLING_INTERVAL_MS;
				nbRetriesDone = 0;
				
				if(logger.isInfoEnabled()) logger.info(moviesStr);
			}else {
	        	throw new FileImportException(String.format("No movie information found for title='%s'", title));			
			}
	    } catch(MovieDbException ex) {
	    	if(ex.getMessage().contains("response code: 503")){
	    		//sometimes tmdb craps out with a 503 error. Try again if the max retries limit hasn't been reached
	    		if(nbRetriesDone < MAX_RETRIES - 1) {
	    			//increment the wait timeout and min polling interval
	    			nbRetriesDone++;
	    			currentPollingIntervalMs += POLLING_INCREMENT_MS;
	    			
	    			logger.info(String.format("Incremented polling interval after 503 error response. Polling interval=%s", currentPollingIntervalMs));
	    			
	    			//wait before trying again
	    			try {
						Thread.sleep(currentPollingIntervalMs);
					} catch (InterruptedException e) {
						logger.error("Failed to pause thread to respect wait timeout");
					}
	    			
	    			//do a recursive call and try again
	    			importFile(title, filePath);
	    		} else {
	            	throw new FileImportException(String.format("Failed to import movie information for title='%s'", title), ex);	    			
	    		}
	    	}
        } catch (Throwable t) {
        	throw new FileImportException(String.format("Failed to import movie information for title='%s'", title), t);
        }	    
    }

	@Override
	public boolean isImportByIdPossible() {
		return true;
	}

	@Override
	public void importFileById(String id) throws FileImportException {
		int tmdbId;
		
		//try to convert the received id to an int
		try {
			tmdbId = Integer.parseInt(id);
		} catch (NumberFormatException ex) {
			throw new FileImportException(String.format("Failed to import film by tmdb id='%s' because it couldn't be converted to an Integer", id));
		}
		
		logger.debug("Importing TMDb movie by id=" + id);
	    try {
			movie = api.getMovieInfo(tmdbId, globalConfig.getImportLanguage(), "casts,trailers");
			logger.debug("Imported TMDb movie by id=" + id);
        } catch (Throwable t) {
        	throw new FileImportException(String.format("Failed to import movie information for id='%s'", id), t);
        }
	}

	@Override
	public List<FileType> getSupportedFileTypes() {
		return Arrays.asList(FileType.VIDEO);
	}

	@Override
	public List<String> getSupportedTags(FileType fileType) {
		List<String> res = new ArrayList<String>();
		for(Tag t : Tag.values()) {
			res.add(t.toString());
		}
		
		return res;
	}

	@Override
	public List<String> getTags(String tagName) {
		List<String> res = null;
		if (tagName.equals(Tag.Actor.toString())) {
			res = new ArrayList<String>();
			if (movie != null && movie.getCast() != null) {
				for (int i=0; i<movie.getCast().size(); i++) {
					res.add(movie.getCast().get(i).getName());
				}
			}
			
		} else if (tagName.equals(Tag.Studio.toString())) {
			res = new ArrayList<String>();
			if (movie != null && movie.getProductionCompanies() != null) {
				for (int i=0; i<movie.getProductionCompanies().size(); i++) {
					res.add(movie.getProductionCompanies().get(i).getName());
				}
			}
			
		} else if (tagName.equals(Tag.Author.toString())) {
			res = new ArrayList<String>();
			if (movie != null && movie.getCrew() != null) {
				for (int i=0; i<movie.getCrew().size(); i++) {
					if (movie.getCrew().get(i).getJob().equals("Author")) {
						res.add(movie.getCrew().get(i).getName());
					}
				}
			}
			
		} else if (tagName.equals(Tag.Producer.toString())) {
			res = new ArrayList<String>();
			if (movie != null && movie.getCrew() != null) {
				for (int i=0; i<movie.getCrew().size(); i++) {
					if (movie.getCrew().get(i).getJob().equals("Producer")) {
						res.add(movie.getCrew().get(i).getName());
					}
				}
			}
		}
		return res;
	}

	@Override
	public String getVersion() {
		return properties.get("project.version");
	}

	@Override
	public List<FileProperty> getSupportedFileProperties() {
		//add all supported properties
		List<FileProperty> res = new ArrayList<FileProperty>();
		// res.add(FileProperty.VIDEO_CERTIFICATION); // Fixme: see getFileProperty(FileProperty property) comments
		res.add(FileProperty.VIDEO_BUDGET);
		res.add(FileProperty.VIDEO_COVERURL);
		res.add(FileProperty.VIDEO_DIRECTOR);
		res.add(FileProperty.VIDEO_GENRES);
		res.add(FileProperty.VIDEO_HOMEPAGEURL);
		res.add(FileProperty.VIDEO_IMDBID);
		res.add(FileProperty.VIDEO_ORIGINALNAME);
		res.add(FileProperty.VIDEO_OVERVIEW);
		res.add(FileProperty.VIDEO_RATINGPERCENT);
		res.add(FileProperty.VIDEO_RATINGVOTERS);
		res.add(FileProperty.VIDEO_REVENUE);
		res.add(FileProperty.VIDEO_TAGLINE);
		res.add(FileProperty.VIDEO_NAME);
		res.add(FileProperty.VIDEO_SORTNAME);
		// res.add(FileProperty.VIDEO_TRAILERURL); // Fixme: see getFileProperty(FileProperty property) comments.
		res.add(FileProperty.VIDEO_YEAR);
		res.add(FileProperty.VIDEO_TMDBID);
		
		return res;
	}

	@Override
	public Object getFileProperty(FileProperty property) {
		//return the proper object for every supported file property
		switch (property) {
		case VIDEO_CERTIFICATION:
			return null; //movie == null ? null : movie.getCertification();  // Fixme: Unsure what this is & can't find a movie with a value that would make sense here.
		case VIDEO_BUDGET:
		    return movie == null ? null : (int)movie.getBudget();
		case VIDEO_COVERURL:
			return movie == null ? null : "http://d3gtl9l2a4fn1j.cloudfront.net/t/p/original" + movie.getPosterPath();
		case VIDEO_DIRECTOR:
			String director = null;
			if(movie != null && movie.getCrew() != null){
			    for(int i=0; i<movie.getCrew().size(); i++){
					if(movie.getCrew().get(i).getJob().equals("Director")){
						director = movie.getCrew().get(i).getName();
						break;
			    	}
			    }			
			}
		    return director;
		case VIDEO_GENRES:
			List<String> genres = null;
			if(movie != null && movie.getGenres() != null && !movie.getGenres().isEmpty()){
				genres = new ArrayList<String>();
			    for(int i=0; i<movie.getGenres().size(); i++){
					genres.add(movie.getGenres().get(i).getName());
			    }
			}
		    return genres;
		case VIDEO_HOMEPAGEURL:
		    return movie == null || movie.getHomepage() == null ? null : movie.getHomepage().toString();
		case VIDEO_IMDBID:
		    return movie == null ? null : movie.getImdbID();
		case VIDEO_ORIGINALNAME:
		    return movie == null ? null : movie.getOriginalTitle();
		case VIDEO_OVERVIEW:
		    return movie == null || movie.getOverview() == null || movie.getOverview().equals("null") ? null : movie.getOverview();
		case VIDEO_RATINGPERCENT:
			return movie == null ? null : (int)(movie.getVoteAverage() * 10);
		case VIDEO_RATINGVOTERS:
			return movie == null ? null : movie.getVoteCount();
		case VIDEO_REVENUE:
		    return movie == null ? null : (int)movie.getRevenue();
		case VIDEO_TAGLINE:
		    return movie == null ? null : movie.getTagline();
		case VIDEO_TMDBID:
		    return movie == null ? null : movie.getId();
		case VIDEO_NAME:
		    return movie == null ? null : movie.getTitle();
		case VIDEO_SORTNAME:
		    return movie == null ? null : movie.getTitle();
		case VIDEO_TRAILERURL:
		    return null; //movie == null || movie.getTrailers().get(0) == null ? null : movie.getTrailers().get(0).toString(); // Fixme: Needs to be parsed for video location.
		case VIDEO_YEAR:
		    return movie == null || movie.getReleaseDate() == null ? null : Integer.parseInt(movie.getReleaseDate().substring(0,4));
		default:
			logger.warn("Unsupportede FileProperty: %s", property);
			break;
		}
		return null;
	}

	@Override
	public int getMinPollingIntervalMs() {
		return currentPollingIntervalMs;
	}

	@Override
	public String getShortDescription() {
		return messages.getString("TmdbMovieImportPlugin.ShortDescription");
	}

	@Override
	public String getLongDescription() {
		return messages.getString("TmdbMovieImportPlugin.LongDescription");
	}

	@Override
	public void shutdown() {
		// do nothing
	}

	@Override
	public boolean isSearchForFilePossible() {
		return true;
	}

	@Override
	public void importFileBySearchObject(Object searchObject) {
		if(searchObject != null && searchObject instanceof TmdbMovieInfoPluginMovie) {
			movie = ((TmdbMovieInfoPluginMovie)searchObject).getMovie();
			try {
				movie = api.getMovieInfo(movie.getId(), globalConfig.getImportLanguage(), "casts,trailers");
			} catch (MovieDbException ex){
			}
		}
	}

	@Override
	public List<Object> searchForFile(String name) {
		List<Object> res = null;
	    try {
	    	//search for the name
	        TmdbResultsList<MovieDb> movies = api.searchMovie(name, 0, globalConfig.getImportLanguage(), true, 0);
	        
	        //create the return list if any movies were found
			if (movies != null && movies.getTotalResults() > 0) {
				res = new ArrayList<Object>();
				for(int i = movies.getTotalResults(); i > 0; i--){
					res.add(new TmdbMovieInfoPluginMovie(movies.getResults().get(i-1)));
				}
			}
        } catch (Throwable t) {
        	//don't propagate any error, return the default value and log the error
        	logger.error(String.format("Failed to search for movie for name=%s'", name), t);
        }
	    return res;
	}

	@Override
	public JComponent getGlobalConfigurationPanel() {
		if(pGlobalConfiguration == null ) {
			pGlobalConfiguration = new GlobalConfigurationPanel(globalConfig);
		}
		pGlobalConfiguration.applyConfig();
		return pGlobalConfiguration;
	}

	@Override
	public Icon getPluginIcon() {
		return new ImageIcon(getClass().getResource("/tmdb-32.png"));
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
		try{
			api = new TheMovieDbApi("4cdddc892213dd24e5011fd710f8abf0");
		} catch (MovieDbException ex) {
		}
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
}
