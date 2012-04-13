/*
 * Shutdown Plugin for PS3 Media Server
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
package net.pms.plugin.shutdown;

/**
 * This class is overruled for specific platforms.
 */
public abstract class CommandUtils {
	/**
	 * Return the platform specific command to power off the system.
	 * @return The command and its arguments.
	 */
	abstract String[] getPowerOffCommand();

	/**
	 * Return the platform specific command to restart the system.
	 * @return The command and its arguments.
	 */
	abstract String[] getRestartCommand();
}
