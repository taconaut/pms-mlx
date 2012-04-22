/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2011  G.Zsombor
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
package net.pms.test;

import static org.junit.Assert.assertEquals;
import net.pms.medialibrary.external.ExternalFactory;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;

/**
 * This class is designed to verify the outcome of
 * {@link net.pms.medialibrary.external.ExternalFactory#compareVersion(String, String)}. 
 */
public class VersionComparisonTest {
	@Before
    public void setUp() {
        // Silence all log messages from the PMS code that is being tested
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.reset(); 
	}

	/**
	 * Maven default artifact version comparison probably works flawless. This
	 * test is merely to determine the details of version comparison. 
	 */
	@Test
	public void compareVersionTests() {
		// Regular version numbers
		compareVersion("1.0.1", "0.9", 1);
		compareVersion("1.0.1", "1.0.9", -1);
		compareVersion("1.0", "1.0.1", -1);
		compareVersion("1.9.2-b1", "1.11.1-SNAPSHOT", -1);

		// Detail doesn't matter
		compareVersion("1.0", "1.0.0", 0);
		compareVersion("1", "1.0.0", 0);

		// Extensions are taken into account and are less than the release
		compareVersion("1.2.34", "1.2.34-SNAPSHOT", 1);
		compareVersion("1.2.34", "1.2.34-alpha1", 5);
		compareVersion("1.2.34-alpha1", "1.2.34", -5);
		compareVersion("1.2.34", "1.2.34-b2", 4);
		compareVersion("1.2.34-rc2", "1.2.34", -2);

		// Extension details are taken into account
		compareVersion("2.0.12-rc1", "2.0.12.-rc2", -1);
		compareVersion("1.17-beta1", "1.17-alpha1", 1);
		compareVersion("3.0-a1", "3.0-a1", 0);
	}

	/**
	 * Compare two version strings and assert the expected outcome of comparing
	 * the two.
	 *
	 * @param version1 First version to compare.
	 * @param version2 Second version to compare.
	 * @param expectedOutcome Expected outcome of the comparison.
	 */
	private void compareVersion(String version1, String version2, int expectedOutcome) {
		String outcomeString;

		if (expectedOutcome == -1) {
			outcomeString = "is less than";
		} else {
			if (expectedOutcome == 1) {
				outcomeString = "is greater than";
			} else {
				outcomeString = "equals";
			}
		}

		assertEquals("Version \"" + version1 + "\" " + outcomeString + " \"" + version2 + "\"",
				expectedOutcome, ExternalFactory.compareVersion(version1, version2));
	}
}
