/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2012  pw
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
package net.pms.medialibrary.gui.tab;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.pms.Messages;
import net.pms.medialibrary.commons.MediaLibraryConfiguration;
import net.pms.medialibrary.commons.VersionConstants;
import net.pms.medialibrary.commons.dataobjects.DOManagedFile;
import net.pms.medialibrary.commons.dataobjects.OmitPrefixesConfiguration;
import net.pms.medialibrary.commons.enumarations.FileType;
import net.pms.medialibrary.commons.enumarations.MetaDataKeys;
import net.pms.medialibrary.commons.enumarations.ScanState;
import net.pms.medialibrary.commons.exceptions.InitialisationException;
import net.pms.medialibrary.commons.exceptions.ScanStateException;
import net.pms.medialibrary.commons.helpers.GUIHelper;
import net.pms.medialibrary.commons.interfaces.IFileScannerEventListener;
import net.pms.medialibrary.gui.dialogs.ScanFolderDialog;
import net.pms.medialibrary.library.LibraryManager;
import net.pms.medialibrary.scanner.FileScanner;
import net.pms.medialibrary.storage.MediaLibraryStorage;
import net.pms.notifications.NotificationCenter;
import net.pms.notifications.NotificationSubscriber;
import net.pms.notifications.types.DBEvent;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class GeneralOptionsView extends JPanel {
	private static final Logger log = LoggerFactory.getLogger(GeneralOptionsView.class);
	private static final long         serialVersionUID = -3580997978960568334L;

	private LibraryManager            libraryManager;
	private MediaLibraryConfiguration libConfig;
	private ScanState                 scanState;
	private JLabel                    lVideoCount;
	private JButton                   bClearVideo;
	private JButton                   bRefreshVideo;
	private JLabel                    lRefreshVideo;
	private JLabel                    lAudioCount;
	private JButton                   bClearAudio;
	private JLabel                    lPicturesCount;
	private JButton                   bClearPictures;
	private JLabel                    lScanState;
	private JButton                   bStartPauseScan;
	private JButton                   bStopScan;
	private JButton                   bScanFolder;
	private JTextField                tfPictureFolderPathValue;
	private JCheckBox                 cbEnableMediaLibrary;
	private JLabel                    lOmitPrefix;
	private JTextField                tfOmitPrefix;
	private JCheckBox                 cbOmitFiltering;
	private JCheckBox                 cbOmitSorting;
	private ManagedFoldersPanel       pManagedFolders;

	private JComponent                pOptions;
	
	public GeneralOptionsView() {
		libConfig = MediaLibraryConfiguration.getInstance();

		try {
			this.libraryManager = LibraryManager.getInstance();
		} catch (InitialisationException ex) {
			log.error("Failed to get LibraryManager", ex);
			return;
		}

		setLayout(new BorderLayout());
		add(buildUseMediaLibrary(), BorderLayout.NORTH);
		pOptions = build();
		add(pOptions, BorderLayout.CENTER);
		
		refreshVideoFields();

		this.scanState = libraryManager.getScanState().getScanState();
		updateScanState();
		pOptions.setVisible(cbEnableMediaLibrary.isSelected());
		
		registerNotifications();
	}

	/**
	 * Gets the managed folders as they have been configured in the GUI.
	 *
	 * @return the managed folders
	 */
	public List<DOManagedFile> getManagedFolders(){
		pManagedFolders.cleanManagedFolders();
		return pManagedFolders.getManagedFolders();
	}

	private JComponent buildUseMediaLibrary() {
		FormLayout layout = new FormLayout("10:grow", "p, 7px, p");
		PanelBuilder builder = new PanelBuilder(layout);
		builder.opaque(true);
		CellConstraints cc = new CellConstraints();

		// Header
		JComponent sGeneral = builder.addSeparator(Messages.getString("ML.GeneralOptionsView.sGeneral"), cc.xy(1, 1));
		sGeneral = (JComponent) sGeneral.getComponent(0);
		sGeneral.setFont(sGeneral.getFont().deriveFont(Font.BOLD));

		// Enable
		cbEnableMediaLibrary = new JCheckBox(Messages.getString("ML.GeneralOptionsView.cbEnableMediaLibrary"));
		builder.add(cbEnableMediaLibrary, cc.xy(1, 3, CellConstraints.LEFT, CellConstraints.CENTER));

		cbEnableMediaLibrary.setSelected(libConfig.isMediaLibraryEnabled());

		cbEnableMediaLibrary.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				pOptions.setVisible(cbEnableMediaLibrary.isSelected());
				libConfig.setMediaLibraryEnabled(cbEnableMediaLibrary.isSelected());
			}
		});

		return builder.getPanel();
	}

	private JComponent build() {
		FormLayout layout = new FormLayout("3px, 10:grow, 3px", "3px, p, 7px, p, 7px, p, 7px, fill:p:grow, 3px, p, 3px");
		PanelBuilder builder = new PanelBuilder(layout);
		builder.opaque(true);
		CellConstraints cc = new CellConstraints();

		builder.add(buildGeneral(), cc.xy(2, 2));
		builder.add(buildLibrary(), cc.xy(2, 4));
		builder.add(buildScanner(), cc.xy(2, 6));
		builder.add(buildFolderManager(), cc.xy(2, 8));
		
		JScrollPane sp = new JScrollPane(builder.getPanel());
		sp.setBorder(BorderFactory.createEmptyBorder());

		return sp;
	}

	private Component buildFolderManager() {
		FormLayout layout = new FormLayout("fill:p:grow", "p, fill:150:grow");
		PanelBuilder builder = new PanelBuilder(layout);
		builder.opaque(true);
		CellConstraints cc = new CellConstraints();

		JComponent sManageFolders = builder.addSeparator(Messages.getString("ML.GeneralOptionsView.sManageFolders"), cc.xy(1, 1));
		sManageFolders = (JComponent) sManageFolders.getComponent(0);
		sManageFolders.setFont(sManageFolders.getFont().deriveFont(Font.BOLD));

		pManagedFolders = new ManagedFoldersPanel();
		builder.add(pManagedFolders, cc.xy(1, 2));

		return builder.getPanel();
	}

	private Component buildScanner() {
		FormLayout layout = new FormLayout("p, 5px, 100px, 3dlu, p, 3dlu, p, 3dlu, fill:10:grow", "p, 3dlu,  p, 3dlu, p, 3dlu, p");
		PanelBuilder builder = new PanelBuilder(layout);
		builder.opaque(true);
		CellConstraints cc = new CellConstraints();

		JComponent sScanner = builder.addSeparator(Messages.getString("ML.GeneralOptionsView.sScanner"), cc.xyw(1, 1, 9));
		sScanner = (JComponent) sScanner.getComponent(0);
		sScanner.setFont(sScanner.getFont().deriveFont(Font.BOLD));

		JLabel lScanState = builder.addLabel(Messages.getString("ML.GeneralOptionsView.lScanState"), cc.xy(1, 3));
		this.lScanState = new JLabel(Messages.getString("ML.ScanState.IDLE"));
		builder.add(this.lScanState, cc.xy(3, 3));
		this.bStartPauseScan = new JButton(Messages.getString("ML.GeneralOptionsView.bPause"));
		lScanState.setPreferredSize(new Dimension(lScanState.getPreferredSize().width, bStartPauseScan.getPreferredSize().height));
		this.bStartPauseScan.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (scanState == ScanState.RUNNING) {
					try {
						libraryManager.pauseScan();
						bStartPauseScan.setText(Messages.getString("ML.GeneralOptionsView.bResume"));
					} catch (ScanStateException ex) {
						if(log.isInfoEnabled()) log.info("Unable to pause scan when its state is " + ex.getCurrentState() + ". It can only be used when in state " + ex.getExpectedState());
					}
				} else if (scanState == ScanState.PAUSED) {
					try {
						libraryManager.unPauseScan();
						bStartPauseScan.setText(Messages.getString("ML.GeneralOptionsView.bPause"));
					} catch (ScanStateException ex) {
						if(log.isInfoEnabled()) log.info("Unable to pause scan when its state is " + ex.getCurrentState() + ". It can only be used when in state " + ex.getExpectedState());
					}
				}
			}
		});
		builder.add(this.bStartPauseScan, cc.xy(5, 3));
		this.bStopScan = new JButton(Messages.getString("ML.GeneralOptionsView.bStopScan"));
		this.bStopScan.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				libraryManager.stopScan();
			}

		});
		builder.add(this.bStopScan, cc.xy(7, 3));

		this.bScanFolder = new JButton(Messages.getString("ML.GeneralOptionsView.bScanFolder"));
		this.bScanFolder.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				JFileChooser fc = new JFileChooser(MediaLibraryStorage.getInstance().getMetaDataValue(MetaDataKeys.LAST_SCAN_FOLDER_PATH.toString()));
				fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

				if (fc.showOpenDialog(bScanFolder.getTopLevelAncestor()) == JFileChooser.APPROVE_OPTION) {
					ScanFolderDialog d = new ScanFolderDialog(fc.getSelectedFile().getAbsolutePath());
					d.setModal(true);
					d.setResizable(false);
					d.pack();
					d.setLocation(GUIHelper.getCenterDialogOnParentLocation(d.getSize(), bScanFolder));

					d.setVisible(true);
					if (d.isDoImport()) {
						File f = new File(d.getManagedFolder().getPath());
						MediaLibraryStorage.getInstance().setMetaDataValue(MetaDataKeys.LAST_SCAN_FOLDER_PATH.toString(), f.getParent());
						FileScanner.getInstance().scanFolder(d.getManagedFolder());
					}
				}
			}

		});
		builder.add(this.bScanFolder, cc.xyw(1, 5, 3));
		return builder.getPanel();
	}

	private Component buildGeneral() {
		FormLayout layout = new FormLayout("r:p, 3px, fill:10:grow, 3px, p, 3px, p, 3px, p, 3px, p", 
				"p, 3px, p, 3px, p");
		PanelBuilder builder = new PanelBuilder(layout);
		builder.opaque(true);
		CellConstraints cc = new CellConstraints();

		//header
		JLabel lPictureFolderPathTitle = new JLabel(Messages.getString("ML.GeneralOptionsView.lPictureFolderPathTitle"));
		builder.add(lPictureFolderPathTitle, cc.xy(1, 1));

		//picture save folder path
		tfPictureFolderPathValue = new JTextField();
		tfPictureFolderPathValue.setEditable(false);
		builder.add(tfPictureFolderPathValue, cc.xyw(3, 1, 7));

		JButton bBrowsePictureFolderPath = new JButton(Messages.getString("ML.GeneralOptionsView.bBrowsePictureFolderPath"));
		bBrowsePictureFolderPath.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				JFileChooser chooser = null;
				File f = new File(tfPictureFolderPathValue.getText());
				if (f.isDirectory()) {
					chooser = new JFileChooser(f.getAbsoluteFile());
				} else {
					chooser = new JFileChooser();
				}

				chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				int returnVal = chooser.showDialog((Component) e.getSource(), Messages.getString("ML.General.FolderChooser.Title"));
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					String folderPath = chooser.getSelectedFile().getAbsolutePath();
					tfPictureFolderPathValue.setText(folderPath);
					
					// Save the configuration
					libConfig.setPictureSaveFolderPath(tfPictureFolderPathValue.getText());
				}
			}
		});
		builder.add(bBrowsePictureFolderPath, cc.xy(11, 1));

		//prefixes to ignore
		lOmitPrefix = new JLabel(Messages.getString("ML.General.OmitPrefixes.Heading"));
		builder.add(lOmitPrefix, cc.xy(1, 3));

		tfOmitPrefix = new JTextField();
		tfOmitPrefix.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				saveOmitPrefixesConfiguration();
			}
		});
		builder.add(tfOmitPrefix, cc.xyw(3, 3, 3));

		builder.addLabel(Messages.getString("ML.General.OmitPrefixes.When"), cc.xy(7, 3));

		cbOmitSorting = new JCheckBox(Messages.getString("ML.General.OmitPrefixes.Sort"));
		cbOmitSorting.addItemListener(new ItemListener() {
			
			@Override
			public void itemStateChanged(ItemEvent arg0) {
				saveOmitPrefixesConfiguration();
			}
		});
		builder.add(cbOmitSorting, cc.xy(9, 3));

		cbOmitFiltering = new JCheckBox(Messages.getString("ML.General.OmitPrefixes.Filter"));
		cbOmitFiltering.addItemListener(new ItemListener() {
			
			@Override
			public void itemStateChanged(ItemEvent arg0) {
				saveOmitPrefixesConfiguration();
			}
		});
		builder.add(cbOmitFiltering, cc.xy(11, 3));

		//set initial values
		tfPictureFolderPathValue.setText(libConfig.getPictureSaveFolderPath());

		OmitPrefixesConfiguration omitCfg = libConfig.getOmitPrefixesConfiguration();
		String prefixes = "";
		for (String k : omitCfg.getPrefixes()) {
			prefixes += k + " ";
		}
		prefixes = prefixes.trim();

		tfOmitPrefix.setText(prefixes);
		cbOmitSorting.setSelected(omitCfg.isSorting());
		cbOmitFiltering.setSelected(omitCfg.isFiltering());

		return builder.getPanel();
	}

	private JComponent buildLibrary() {
		FormLayout layout = new FormLayout("p, right:p, 5px, 40px, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, fill:0:grow, p", 
				"p, p, p, p");
		PanelBuilder builder = new PanelBuilder(layout);
		builder.opaque(true);
		CellConstraints cc = new CellConstraints();

		JComponent sManageLibrary = builder.addSeparator(Messages.getString("ML.GeneralOptionsView.sManageLibrary"), cc.xyw(1, 1, 13));
		sManageLibrary = (JComponent) sManageLibrary.getComponent(0);
		sManageLibrary.setFont(sManageLibrary.getFont().deriveFont(Font.BOLD));

		builder.addLabel(Messages.getString("ML.GeneralOptionsView.lVideos"), cc.xy(2, 2));
		this.lVideoCount = new JLabel();
		builder.add(this.lVideoCount, cc.xy(4, 2));
		this.bClearVideo = new JButton(Messages.getString("ML.GeneralOptionsView.bClear"));
		this.bClearVideo.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				switch (JOptionPane.showConfirmDialog(bClearVideo.getTopLevelAncestor(), Messages.getString("ML.GeneralOptionsView.DeleteAllVideosMsg"))) {
					case JOptionPane.YES_OPTION:
						libraryManager.clearVideo();
						break;
				}
			}

		});
		builder.add(this.bClearVideo, cc.xy(6, 2));
		
		this.bRefreshVideo = new JButton(Messages.getString("ML.GeneralOptionsView.bRefresh"));
		this.bRefreshVideo.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				FileScanner.getInstance().updateFilesRequiringFileUpdate(Arrays.asList(new FileType[] { FileType.VIDEO }));
			}

		});
		builder.add(this.bRefreshVideo, cc.xy(8, 2));
		
		this.lRefreshVideo = new JLabel();
		builder.add(this.lRefreshVideo, cc.xy(10, 2));

		JButton bResetLibrary = new JButton(Messages.getString("ML.GeneralOptionsView.bResetLibrary"));
		bResetLibrary.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (JOptionPane.showConfirmDialog(bClearVideo.getTopLevelAncestor(), String.format(Messages.getString("ML.GeneralOptionsView.ResetDBMsg"), System
				        .getProperty("line.separator"))) == JOptionPane.YES_OPTION) {
					try {						
						// reset the storage
						libraryManager.resetStorage();
						
						// update the configuration fields that have to be
						MediaLibraryConfiguration config = MediaLibraryConfiguration.getInstance();
						cbEnableMediaLibrary.setSelected(config.isMediaLibraryEnabled());
						tfPictureFolderPathValue.setText(config.getPictureSaveFolderPath());
						
						OmitPrefixesConfiguration omitCfg = libConfig.getOmitPrefixesConfiguration();
						String prefixes = "";
						for (String k : omitCfg.getPrefixes()) {
							prefixes += k + " ";
						}
						prefixes = prefixes.trim();
						tfOmitPrefix.setText(prefixes);
						cbOmitFiltering.setSelected(omitCfg.isFiltering());
						cbOmitSorting.setSelected(omitCfg.isSorting());
						
						// Show message in the pms status bar
						net.pms.PMS.get().getFrame().setStatusLine(Messages.getString("ML.GeneralOptionsView.ResetDBDoneMsg"));
					} catch (Exception ex) {
						log.error("Failed to reset data base", ex);
					}
				}
			}
		});
		builder.add(bResetLibrary, cc.xy(13, 2));

		// TODO: uncomment audio and pictures parts, once implemented
		// builder.addLabel(Messages.getString("ML.GeneralOptionsView.lTracks"), cc.xy(2, 3));
		this.lAudioCount = new JLabel(String.valueOf(this.libraryManager.getAudioCount()));
		// builder.add(this.lAudioCount, cc.xy(4, 3));
		this.bClearAudio = new JButton(Messages.getString("ML.GeneralOptionsView.bClear"));
		this.bClearAudio.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				switch (JOptionPane.showConfirmDialog(bClearAudio.getTopLevelAncestor(), Messages.getString("ML.GeneralOptionsView.DeleteAllTracksMsg"))) {
					case JOptionPane.YES_OPTION:
						libraryManager.clearAudio();
						break;
				}
			}

		});
		// builder.add(this.bClearAudio, cc.xy(6, 3));

		JButton bCleanLibrary = new JButton(Messages.getString("ML.GeneralOptionsView.bClearLibrary"));
		bCleanLibrary.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (JOptionPane.showConfirmDialog(bClearVideo.getTopLevelAncestor(), String.format(Messages.getString("ML.GeneralOptionsView.CleanDBMsg"), System
				        .getProperty("line.separator"))) == JOptionPane.YES_OPTION) {
					try {
						// clean the storage
						libraryManager.cleanStorage();
					} catch (Exception ex) {
						log.error("Failed the library", ex);
					}
				}
			}
		});
		builder.add(bCleanLibrary, cc.xy(13, 3));

		// builder.addLabel(Messages.getString("ML.GeneralOptionsView.lPictures"), cc.xy(2, 4));
		this.lPicturesCount = new JLabel(String.valueOf(this.libraryManager.getPictureCount()));
		// builder.add(this.lPicturesCount, cc.xy(4, 4));
		this.bClearPictures = new JButton(Messages.getString("ML.GeneralOptionsView.bClear"));
		this.bClearPictures.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				switch (JOptionPane.showConfirmDialog(bClearPictures.getTopLevelAncestor(), Messages.getString("ML.GeneralOptionsView.DeleteAllPicturesMsg"))) {
					case JOptionPane.YES_OPTION:
						libraryManager.clearPictures();
						break;
				}
			}

		});
		// builder.add(this.bClearPictures, cc.xy(6, 4));

		return builder.getPanel();
	}
	
	private void registerNotifications() {
		NotificationCenter.getInstance(DBEvent.class).subscribe(new NotificationSubscriber<DBEvent>() {
			
			@Override
			public void onMessage(DBEvent obj) {
				switch(obj.getType()) {
				case VideoInserted:
				case VideoUpdated:
				case VideoDeleted:
					refreshVideoFields();
					break;
				default:
					// Do nothing
					break;
				}
			}
		});
		
		this.libraryManager.addFileScannerEventListener(new IFileScannerEventListener() {

			@Override
			public void scanStateChanged(ScanState state) {
				scanState = state;
				updateScanState();
			}

			@Override
			public void itemInserted(FileType type) {
				// Don't do anything as those notifications are being handled through the notification center
			}			
		});
	}
	
	private void refreshVideoFields() {
		this.lVideoCount.setText(String.valueOf(this.libraryManager.getVideoCount()));

		int nbItemsRequiringUpdate = MediaLibraryStorage.getInstance().getFileCountRequiringUpdate(FileType.VIDEO, VersionConstants.VIDEO_FILE_VERSION);
		if(nbItemsRequiringUpdate > 0) {
			this.lRefreshVideo.setText(String.format(Messages.getString("ML.GeneralOptionsView.lRefresh"), nbItemsRequiringUpdate));
		}
		this.bRefreshVideo.setVisible(nbItemsRequiringUpdate > 0);
		this.lRefreshVideo.setVisible(nbItemsRequiringUpdate > 0);
	}

	private void updateScanState() {
		this.lScanState.setText(Messages.getString("ML.ScanState." + this.scanState));
		if (this.scanState == ScanState.PAUSED) {
			this.bStartPauseScan.setText(Messages.getString("ML.GeneralOptionsView.bResume"));
			this.bStartPauseScan.setVisible(true);
			this.bStopScan.setVisible(true);
			this.bStartPauseScan.setEnabled(true);
			this.bStopScan.setEnabled(true);
		} else if (this.scanState == ScanState.PAUSING || this.scanState == ScanState.STARTING || this.scanState == ScanState.STOPPING) {
			this.bStartPauseScan.setEnabled(false);
			this.bStopScan.setEnabled(false);
		} else if (this.scanState == ScanState.RUNNING) {
			this.bStartPauseScan.setText(Messages.getString("ML.GeneralOptionsView.bPause"));
			this.bStartPauseScan.setVisible(true);
			this.bStopScan.setVisible(true);
			this.bStartPauseScan.setEnabled(true);
			this.bStopScan.setEnabled(true);
		} else if (this.scanState == ScanState.IDLE) {
			this.bStartPauseScan.setVisible(false);
			this.bStopScan.setVisible(false);
		}
	}

	private void saveOmitPrefixesConfiguration() {
		OmitPrefixesConfiguration omitCfg = new OmitPrefixesConfiguration();
		omitCfg.setFiltering(cbOmitFiltering.isSelected());
		omitCfg.setSorting(cbOmitSorting.isSelected());
		omitCfg.setPrefixes(Arrays.asList(tfOmitPrefix.getText().trim().split(" ")));
		
		// Save the configuration
		libConfig.setOmitPrefixesConfiguration(omitCfg);
	}
}
