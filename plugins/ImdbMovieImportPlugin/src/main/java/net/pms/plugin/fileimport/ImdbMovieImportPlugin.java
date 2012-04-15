package net.pms.plugin.fileimport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.ResourceBundle;

import javax.swing.JComponent;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.pms.PMS;
import net.pms.medialibrary.commons.enumarations.FileProperty;
import net.pms.medialibrary.commons.enumarations.FileType;
import net.pms.medialibrary.commons.exceptions.FileImportException;
import net.pms.medialibrary.external.FileImportPlugin;

/** 
 * Class used to collect information about a movie from tmdb
 * 
 * Some properties can be configured in pms.cnf:<br>
 * - imdb_cover_width (int): returns the original cover if its width is smaller then the set value, or the one resized to this value. Default is 320px<br>
 * - imdb_plot (long/short): long or short plot. Default is long<br>
 * - imdb_use_rotten_tomatoes (true/false): if true, the rotten tomatoes rating will be used. Default is false<br>
 * 
 * @author pw
 *
 */
public class ImdbMovieImportPlugin implements FileImportPlugin {	
	private static final Logger log = LoggerFactory.getLogger(ImdbMovieImportPlugin.class);
	protected static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("net.pms.plugin.fileimport.imdbmovieimportpluginmessages.messages");
	
	private static final Dictionary<String, String> tags; //key=tag name, value=value to query on imdb
	
	static {
		tags = new Hashtable<String, String>();
		tags.put("Actor", "Actors");
		tags.put("Writer", "Writer");
	}
	
	private JSONObject movieObject;
	private boolean useRottenTomatoes = false;
	
	public ImdbMovieImportPlugin() {
		//load the rotten tomatoes property
		Object useRottenTomatoesObj = PMS.getConfiguration().getCustomProperty("imdb_use_rotten_tomatoes");
		if(useRottenTomatoesObj != null) {
			if(useRottenTomatoesObj.equals("true")) {
				useRottenTomatoes = true;
			}
		}
	}

	@Override
	public String getName() {
		return "imdb";
	}

	@Override
	public int getVersion() {
		return 3;
	}

	@Override
	public String getDescription() {
		return RESOURCE_BUNDLE.getString("ImdbMovieImportPlugin.Description");
	}

	@Override
	public void shutdown() {
		//do nothing
	}

	@Override
	public JComponent getGlobalConfigurationPanel() {
		return null;
	}

	@Override
	public void importFile(String title, String filePath) throws FileImportException {
		//re-init object to avoid having obsolete data hanging around
		movieObject = null;
		
		try {
			String jsonString = getJsonResponse(title);
			movieObject = new JSONObject(jsonString);
			Object response = movieObject.get("Response");
			if(response == null || response.toString().equals("Parse Error")) {
				movieObject = null;
				throw new FileImportException(String.format("Parse error in response when searching for title='%s'", title));
			}
		} catch (IOException e) {
			throw new FileImportException(String.format("IOException when trying to query imdb for title='%s'", title), e);
		} catch (JSONException e) {
			throw new FileImportException(String.format("JSONException when trying to query imdb for title='%s'", title), e);
		}
	}

	@Override
	public void importFileById(String id) throws FileImportException {
		//re-init object to avoid having obsolete data hanging around
		movieObject = null;
		
		try {
			URL call = new URL(String.format("http://www.imdbapi.com/?i=%s%s", id, getUrlProperties()));
			String jsonString = readUrlResponse(call).trim();

			movieObject = new JSONObject(jsonString.toString());
			Object response = movieObject.get("Response");
			if(response == null || response.toString().equals("Parse Error")) {
				movieObject = null;
				throw new FileImportException(String.format("Parse error in response when searching for id='%s'", id));
			}			
		} catch (IOException e) {
			throw new FileImportException(String.format("IOException when trying to query imdb for id='%s'", id), e);
		} catch (JSONException e) {
			throw new FileImportException(String.format("JSONException when trying to query imdb for id='%s'", id), e);
		}
	}

	@Override
	public boolean isImportByIdPossible() {
		return true;
	}

	@Override
	public boolean isSearchForFilePossible() {
		return true;
	}

	@Override
	public List<Object> searchForFile(String name) {
		List<Object> res = new ArrayList<Object>();
		
		try {
			String jsonString = getJsonResponse(name);
			JSONObject jsonObject = new JSONObject(jsonString);
			Object response = jsonObject.get("Response");
			if (response != null && !response.toString().equals("Parse Error")) {
				res.add(new FileSearchObject(jsonObject));
			}
		} catch (IOException e) {
			log.error(String.format("IOException when trying to query imdb for name='%s'", name), e);
		} catch (JSONException e) {
			log.error(String.format("JSONException when trying to query imdb for name='%s'", name), e);
		}
		
		return res;
	}

	@Override
	public void importFileBySearchObject(Object searchObject) {
		if(searchObject instanceof FileSearchObject) {
			movieObject = ((FileSearchObject)searchObject).getJsonObject();
		}
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
		res.add(FileProperty.VIDEO_RATINGVOTERS);
		res.add(FileProperty.VIDEO_NAME);
		res.add(FileProperty.VIDEO_YEAR);
		
		return res;
	}

	@Override
	public Object getFileProperty(FileProperty property) {
		Object res = null;
		// return the proper object for every supported file property
		String queryString;
		switch (property) {
		case VIDEO_CERTIFICATION:
			res = getValue("Rated");
			//clean out some values
			if(res != null && (res.toString().equals("Not Rated") || res.toString().equals("N/A") 
					|| res.toString().equals("o.AI.") || res.toString().equals("Unrated"))) {
				res = null;
			}
			break;
		case VIDEO_COVERURL:
			res = getValue("Poster");
			
			//use the custom cover width if configured
			Object customCoverWidth = PMS.getConfiguration().getCustomProperty("imdb_cover_width");
			if(customCoverWidth != null && !customCoverWidth.equals("")) {
				try {
					int customWidth = Integer.parseInt(customCoverWidth.toString());
					res = ((String)res).replaceAll("SX320", "SX" + customWidth);
				} catch(NumberFormatException ex) {
					//do nothing
				}
			}
			break;
		case VIDEO_DIRECTOR:
			res = getValue("Director");
			break;
		case VIDEO_GENRES:
			Object val = getValue("Genre");
			if(val != null) {
				List<String> genres = new ArrayList<String>();
				for(String genre : val.toString().split(",")) {
					String g = genre.trim();
					if(!genres.contains(g)) {
						genres.add(g);
					}
				}
				res = genres;
			}
			break;
		case VIDEO_IMDBID:
			res = getValue("ID");
			break;
		case VIDEO_OVERVIEW:
			res = getValue("Plot");
			break;
		case VIDEO_RATINGPERCENT:
			if(useRottenTomatoes) {
				queryString = "tomatoMeter";
			} else {
				queryString = "Rating";				
			}
			Object ratingObj = getValue(queryString);
			if(ratingObj != null) {
				try {
					double r = Double.parseDouble(ratingObj.toString());
					if(!useRottenTomatoes) {
						res = (int)(10 * r);
					}
				} catch (NumberFormatException ex) {
					log.error(String.format("Failed to parse rating='%s' as a double", ratingObj.toString()), ex);
				}
			}
			break;
		case VIDEO_RATINGVOTERS:
			if(useRottenTomatoes) {
				queryString = "tomatoReviews";
			} else {
				queryString = "Votes";				
			}
			ratingObj = getValue(queryString);
			if(ratingObj != null) {
				try {
					res = Integer.parseInt(ratingObj.toString());
				} catch (NumberFormatException ex) {
					log.error(String.format("Failed to parse rating='%s' as a double", ratingObj.toString()), ex);
				}
			}
			break;
		case VIDEO_NAME:
			res = getValue("Title");
			break;
		case VIDEO_YEAR:
			ratingObj = getValue("Released");
			if(ratingObj != null) {
				try {
					String dStr = ratingObj.toString();
					if(dStr.length() > 3) {
						res = Integer.parseInt(dStr.substring(dStr.length() - 4, dStr.length()));
					}
				} catch (NumberFormatException ex) {
					log.error("Failed to parse release year='%s' as a double", ex);
				}
			}
			break;
		}
		return res;
	}

	private Object getValue(String key) {
		Object res = null;
		try {
			res = movieObject.get(key);
		} catch (JSONException e) {
			log.warn(String.format("Failed to get key='%s'", key));
		}
		return res;
	}

	@Override
	public List<String> getSupportedTags(FileType fileType) {
		return Collections.list(tags.keys());
	}

	@Override
	public List<String> getTags(String tagName) {
		List<String> res = new ArrayList<String>();
		if(tagName != null) {
			String stringToQuery = tags.get(tagName);
			if(stringToQuery != null && !stringToQuery.equals("")) {
				Object value = getValue(stringToQuery);
				if(value != null && value instanceof String) {
					for(String tagValue : ((String)value).split(",")) {
						if(!tagValue.equals("") && !tagValue.equals("N/A")) {
							res.add(tagValue.trim());
						}
					}
				}
			}
		}
		return res;
	}

	@Override
	public List<FileType> getSupportedFileTypes() {
		return Arrays.asList(FileType.VIDEO);
	}

	@Override
	public int getMinPollingIntervalMs() {
		return 1000;
	}
	
	/**
	 * This method will open a connection to the provided url and return its
	 * response.
	 * 
	 * @param url The url to open a connection to.
	 * @return The respone.
	 * @throws IOException
	 */
	public static String readUrlResponse(URL url) throws IOException {
		URLConnection yc = url.openConnection();
		BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));
		String inputLine;
		StringBuffer responce = new StringBuffer();
		while ((inputLine = in.readLine()) != null) {
			responce.append(inputLine);
		}
		in.close();
		return responce.toString();
	}
	
	private String getJsonResponse(String title) throws IOException {
		URL call = new URL(String.format("http://www.imdbapi.com/?t=%s%s", URLEncoder.encode(String.format("%s", title), "UTF8"), getUrlProperties()));
		return readUrlResponse(call).trim();
	}
	
	private String getUrlProperties() {
		String urlProperties = "";
		
		//plot
		Object shortPlotObj = PMS.getConfiguration().getCustomProperty("imdb_plot");
		if(shortPlotObj != null) {
			if(shortPlotObj.equals("long")) {
				urlProperties += "&plot=full";
			}
		} else {
			//use the long plot as default
			urlProperties += "&plot=full";
		}
		
		//rotten tomatoes
		if(useRottenTomatoes) {
			urlProperties += "&tomatoes=true";
		}
		
		return urlProperties;
	}
}
