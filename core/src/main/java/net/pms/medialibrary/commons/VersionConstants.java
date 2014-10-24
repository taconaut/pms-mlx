/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2013  Ph.Waeber
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
package net.pms.medialibrary.commons;

/**
 * This class holds constants for the media library
 */
public class VersionConstants {
	
	/** Specifies the database version. */
	public static final String DB_VERSION = "1.3";
	
	/** Specifies the version of imported data for video files. */
	public static final int VIDEO_FILE_VERSION = 1;
	
	/** Specifies the version of imported data for audio files. */
	public static final int AUDIO_FILE_VERSION = 0;
	
	/** Specifies the version of imported data for pictures. */
	public static final int PICTURE_FILE_VERSION = 0;
}
