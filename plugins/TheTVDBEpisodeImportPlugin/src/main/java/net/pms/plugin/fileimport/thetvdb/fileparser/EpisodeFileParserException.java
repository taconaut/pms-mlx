package net.pms.plugin.fileimport.thetvdb.fileparser;

/**
 *
 * @author Corey
 */
public class EpisodeFileParserException extends Exception {
	private static final long serialVersionUID = 3748437689753978939L;

	/**
     * Creates a new instance of
     * <code>EpisodeFileParserException</code> without detail message.
     */
    public EpisodeFileParserException() {
    }

    /**
     * Constructs an instance of
     * <code>EpisodeFileParserException</code> with the specified detail
     * message.
     *
     * @param msg the detail message.
     */
    public EpisodeFileParserException(String msg) {
        super(msg);
    }
}
