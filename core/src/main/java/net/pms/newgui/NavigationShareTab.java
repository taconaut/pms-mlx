/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2008  A.Brochard
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
package net.pms.newgui;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.util.FormLayoutUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Locale;

public class NavigationShareTab {
	public static final String ALL_DRIVES = Messages.getString("FoldTab.0");

	private static final String PANEL_COL_SPEC = "left:pref, 50dlu, pref, 150dlu, pref, 25dlu, pref, 25dlu, pref, default:grow";
	private static final String PANEL_ROW_SPEC = "p, 3dlu,  p, 3dlu, p, 3dlu,  p, 3dlu, p, 3dlu, p, 8dlu, p, 3dlu,  p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 8dlu, fill:default:grow";
	private static final String SHARED_FOLDER_COL_SPEC = "left:pref, left:pref, pref, pref, pref, 0:grow";
	private static final String SHARED_FOLDER_ROW_SPEC = "p, 3dlu, p, 3dlu, fill:default:grow";
	
	private JCheckBox mplayer_thumb;
	private JCheckBox cacheenable;
	private JButton cachereset;

	private final PmsConfiguration configuration;

	NavigationShareTab(PmsConfiguration configuration) {
		this.configuration = configuration;
	}

	public JComponent build() {
		// Apply the orientation for the locale
		Locale locale = new Locale(configuration.getLanguage());
		ComponentOrientation orientation = ComponentOrientation.getOrientation(locale);
		String colSpec = FormLayoutUtil.getColSpec(PANEL_COL_SPEC, orientation);

		// Set basic layout
		FormLayout layout = new FormLayout(colSpec, PANEL_ROW_SPEC);
		PanelBuilder builder = new PanelBuilder(layout);
		builder.border(Borders.DLU4);
		builder.opaque(true);

		CellConstraints cc = new CellConstraints();

		// Init all gui components
		initSimpleComponents(cc);
		PanelBuilder builderSharedFolder = initSharedFoldersGuiComponents(cc);

		// Build gui with initialized components
		JComponent cmp = builder.addSeparator(Messages.getString("FoldTab.13"),
				FormLayoutUtil.flip(cc.xyw(1, 1, 10), colSpec, orientation));
		cmp = (JComponent) cmp.getComponent(0);
		cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));

		builder.add(mplayer_thumb, FormLayoutUtil.flip(cc.xyw(1, 5, 3), colSpec, orientation));
		
		cmp = builder.addSeparator(Messages.getString("NetworkTab.15"), FormLayoutUtil.flip(cc.xyw(1, 13, 10), colSpec, orientation));
		cmp = (JComponent) cmp.getComponent(0);
		cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));

		builder.add(cacheenable, FormLayoutUtil.flip(cc.xy(1, 21), colSpec, orientation));
		builder.add(cachereset, FormLayoutUtil.flip(cc.xyw(4, 21, 2), colSpec, orientation));
		
		builder.add(builderSharedFolder.getPanel(), FormLayoutUtil.flip(cc.xyw(1, 27, 10), colSpec, orientation));

		JPanel panel = builder.getPanel();

		// Apply the orientation to the panel and all components in it
		panel.applyComponentOrientation(orientation);

		JScrollPane scrollPane = new JScrollPane(
			panel,
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		return scrollPane;
	}

	private void initSimpleComponents(CellConstraints cc) {

		// UseMplayerForVideoThumbs
		mplayer_thumb = new JCheckBox(Messages.getString("FoldTab.14"));
		mplayer_thumb.setContentAreaFilled(false);
		mplayer_thumb.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				configuration.setUseMplayerForVideoThumbs((e.getStateChange() == ItemEvent.SELECTED));
			}
		});
		if (configuration.isUseMplayerForVideoThumbs()) {
			mplayer_thumb.setSelected(true);
		}

		cachereset = new JButton(Messages.getString("NetworkTab.18"));

		cacheenable = new JCheckBox(Messages.getString("NetworkTab.17"));
		cacheenable.setContentAreaFilled(false);
		cacheenable.setSelected(configuration.getUseCache());

		cachereset.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int option = JOptionPane.showConfirmDialog(
					(Component) PMS.get().getFrame(),
					Messages.getString("NetworkTab.13") + Messages.getString("NetworkTab.19"),
					Messages.getString("Dialog.Question"),
					JOptionPane.YES_NO_OPTION);
				if (option == JOptionPane.YES_OPTION) {
					PMS.get().getDatabase().init(true);
				}

			}
		});
		cachereset.setEnabled(configuration.getUseCache());
	}

	private PanelBuilder initSharedFoldersGuiComponents(CellConstraints cc) {
		// Apply the orientation for the locale
		Locale locale = new Locale(configuration.getLanguage());
		ComponentOrientation orientation = ComponentOrientation.getOrientation(locale);
		String colSpec = FormLayoutUtil.getColSpec(SHARED_FOLDER_COL_SPEC, orientation);
		
		FormLayout layoutFolders = new FormLayout(colSpec, SHARED_FOLDER_ROW_SPEC);
		PanelBuilder builderFolder = new PanelBuilder(layoutFolders);
		builderFolder.opaque(true);
		
		return builderFolder;
	}
}
