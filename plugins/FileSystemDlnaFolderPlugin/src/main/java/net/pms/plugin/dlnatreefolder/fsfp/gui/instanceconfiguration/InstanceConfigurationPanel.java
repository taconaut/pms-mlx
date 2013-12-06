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
package net.pms.plugin.dlnatreefolder.fsfp.gui.instanceconfiguration;

import java.awt.GridLayout;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import net.pms.plugin.dlnatreefolder.FileSystemFolderPlugin;
import net.pms.plugin.dlnatreefolder.fsfp.configuration.GlobalConfiguration;
import net.pms.plugin.dlnatreefolder.fsfp.configuration.InstanceConfiguration;
import net.pms.plugin.dlnatreefolder.fsfp.gui.DLNAResourceConfigurationPanel;

/**
 * The configuration panel for the {@link FileSystemFolderPlugin}.
 */
public class InstanceConfigurationPanel extends JPanel {
	private static final long serialVersionUID = -3135055693133367382L;
	
	private FolderConfigurationPanel folderConfigurationPanel;
	private DLNAResourceConfigurationPanel dLNAResourceConfigurationPanel;

	/**
	 * Instantiates a new configuration panel without any shared folders
	 */
	public InstanceConfigurationPanel() {
		this(new InstanceConfiguration());
	}

	/**
	 * Instantiates a new configuration panel with shared folders
	 *
	 * @param folderPaths the folder paths
	 */
	public InstanceConfigurationPanel(InstanceConfiguration instanceConfiguration) {
		setLayout(new GridLayout());
		init(instanceConfiguration);
		buildPanel();
	}

	/**
	 * Applies the instance configuration from the instanceConfig to the GUI.
	 *
	 * @param instanceConfig the instance configuration
	 */
	public void applyModelToGui(InstanceConfiguration instanceConfig) {
		folderConfigurationPanel.applyModelToGui(instanceConfig);
		dLNAResourceConfigurationPanel.applyModelToGui(instanceConfig);
	}

	/**
	 * Applies the global configuration from the instanceConfig to the GUI.
	 *
	 * @param globalconfig the global configuration
	 */
	public void applyModelToGui(GlobalConfiguration globalconfig) {
		folderConfigurationPanel.applyModelToGui(new InstanceConfiguration());
		dLNAResourceConfigurationPanel.applyModelToGui(globalconfig);
	}

	/**
	 * Applies the configuration from the GUI to the instanceConfig.
	 *
	 * @param instanceConfig the instance config
	 */
	public void applyGuiToModel(InstanceConfiguration instanceConfig) {
		folderConfigurationPanel.applyGuiToModel(instanceConfig);
		dLNAResourceConfigurationPanel.applyGuiToModel(instanceConfig);
	}

	/**
	 * Initializes the graphical components
	 */
	private void init(InstanceConfiguration instanceConfiguration) {
		folderConfigurationPanel = new FolderConfigurationPanel();
		dLNAResourceConfigurationPanel = new DLNAResourceConfigurationPanel();
	}

	private void buildPanel() {
		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.addTab(FileSystemFolderPlugin.messages.getString("InstanceConfigurationPanel.1"), folderConfigurationPanel);
		tabbedPane.addTab(FileSystemFolderPlugin.messages.getString("InstanceConfigurationPanel.2"), dLNAResourceConfigurationPanel);
		
		add(tabbedPane);
	}
}
