package net.pms.plugin.fileimport;

import com.omertron.themoviedbapi.model.MovieDb;

public class TmdbMovieInfoPluginMovie {
	private MovieDb movie;
	
	public TmdbMovieInfoPluginMovie(MovieDb movie) {
		this.movie = movie;
	}

	public MovieDb getMovie() {
		return movie;
	}
	
	@Override
	public String toString() {
		String res = "";
		if(movie != null) {
			String yearString = "";
			if(movie.getReleaseDate() != null) {
				yearString = String.format(" (%s)", movie.getReleaseDate().substring(0,4));
			}
			res = String.format("%s%s", movie.getTitle(), yearString);
		}
		return res;
	}
}
