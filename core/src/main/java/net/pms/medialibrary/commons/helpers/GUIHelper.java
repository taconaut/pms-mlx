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
package net.pms.medialibrary.commons.helpers;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.swing.ImageIcon;
import javax.swing.JComponent;

public class GUIHelper {
	/**
	 * Calculates the coordinates to position the dialog center on the top level ancestor of the initial component
	 * @param dialogDimensions the dimension of the dialog to calculate the position for
	 * @param initialComponent the component
	 * @return a point representing the position to set for the dialog
	 */
	public static Point getCenterDialogOnParentLocation(Dimension dialogDimensions, JComponent initialComponent){
		Dimension containerDimension = initialComponent.getTopLevelAncestor().getSize();
		Point containerTopLeftCorner = initialComponent.getTopLevelAncestor().getLocationOnScreen();
		return new Point(containerTopLeftCorner.x + containerDimension.width / 2 - dialogDimensions.width / 2 , containerTopLeftCorner.y + containerDimension.height / 2 - dialogDimensions.height / 2);
    }
	
	/**
	 * Sorts the collection based on the natural sort order
	 * @param c the collection to sort
	 * @return the sorted list
	 */
	public static<T extends Comparable<? super T>> List<T> asSortedList(Collection<T> c) {
	  List<T> list = new ArrayList<T>(c);
	  java.util.Collections.sort(list);
	  return list;
	}

	/**
	 * Method used to resize images to fit the cover into the label
	 * @param srcImg source image icon
	 * @param h height of the image
	 * @param maxWidth 
	 * @return resized image icon
	 */
	public static ImageIcon getScaledImage(ImageIcon srcImg, int h, int maxWidth) {
		int w = srcImg.getIconWidth() * h / srcImg.getIconHeight();
		
		//respect the max width constraint
		if(w > maxWidth) {
			w = maxWidth;
			h = srcImg.getIconHeight() * w / srcImg.getIconWidth();
		}
		
		//don't make the image bigger then its original size
		if(w > srcImg.getIconWidth() || h > srcImg.getIconHeight()) {
			w = srcImg.getIconWidth();
			h = srcImg.getIconHeight();
		}
		
		BufferedImage resizedImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = resizedImg.createGraphics();
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g2.drawImage(srcImg.getImage(), 0, 0, w, h, null);
		g2.dispose();
		return new ImageIcon(resizedImg);
	}

	/**
	 * Format seconds to display string containing days, hours, minutes and seconds.
	 *
	 * @param secs the number of seconds
	 * @return the formatted string
	 */
	public static String formatSecondsToDisplayString(int secs) {
		if(secs == 0){
			return "0s";
		}
		
		int days = (int)TimeUnit.SECONDS.toDays(secs);
		long hours = TimeUnit.SECONDS.toHours(secs) - (days *24);
		long minutes = TimeUnit.SECONDS.toMinutes(secs) - (TimeUnit.SECONDS.toHours(secs)* 60);
		long seconds = TimeUnit.SECONDS.toSeconds(secs) - (TimeUnit.SECONDS.toMinutes(secs) *60);
		 
		String formattedString = "";
		if(days > 0) {
			formattedString += String.format(" %sd", days);
		}
		if(hours > 0 || !formattedString.equals("")) {
			formattedString += String.format(" %02dh", hours);
		}
		if(minutes > 0 || !formattedString.equals("")) {
			formattedString += String.format(" %02dm", minutes);
		}
		if(seconds > 0 || !formattedString.equals("")) {
			formattedString += String.format(" %02ds", seconds);
		}
		
		return formattedString.trim();
	}

	/**
	 * Format size to display string.
	 * The input size is Kb, it will be returned in KB, MB, GB, TB, PB or EB
	 *
	 * @param sizeKb the size kb
	 * @return the string
	 */
	public static String formatSizeToDisplayString(long bytes) {
		if(bytes == 0) {
			return "0B";
		}
		
		// long bytes = sizeKb * 1024;
	    int exp = (int) (Math.log(bytes) / Math.log(1024));
	    String pre = String.valueOf("KMGTPE".charAt(exp - 1));
	    return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
	}
}
