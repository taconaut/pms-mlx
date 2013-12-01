package net.pms.plugin.fileimport.thetvdb.gui;

import java.awt.GridLayout;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.pms.configuration.PmsConfiguration;
import net.pms.plugin.fileimport.TheTVDBImportPlugin;
import net.pms.plugin.fileimport.thetvdb.configuration.GlobalConfiguration;
import net.pms.util.KeyedComboBoxModel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 *
 * @author Corey
 */
public class GlobalConfigurationPanel extends JPanel {
	private static final long serialVersionUID = -2570813466143646366L;
	private final GlobalConfiguration globalConfig;
	
	private JComboBox cbImportLanguage;

	/**
	 * Instantiates a new global configuration panel.
	 *
	 * @param globalConfig the global configuration
	 */
	public GlobalConfigurationPanel(GlobalConfiguration globalConfig) {
		setLayout(new GridLayout());
		this.globalConfig = globalConfig;
		init();
		build();
	}

	/**
	 * Initializes the graphical components
	 */
	private void init() {
		Map<String, String> languages = PmsConfiguration.getSupportedLanguages();
		List<String> supportedLanguages = GlobalConfiguration.getSupportedLanguages();
		
		final KeyedComboBoxModel kcbm = new KeyedComboBoxModel();
		for(String languageId : languages.keySet()) {
			if(supportedLanguages.contains(languageId)) {
				String languageDisplayName = languages.get(languageId);
				kcbm.add(languageId, languageDisplayName);
			}
		}
		cbImportLanguage = new JComboBox(kcbm);
	}

	/**
	 * Builds the panel
	 */
	private void build() {
		// Set basic layout
		FormLayout layout = new FormLayout("5px, p, 5px, f:p:g, 5px", //columns
				"5px, p, 5px"); //rows
		PanelBuilder builder = new PanelBuilder(layout);
		builder.opaque(true);

		CellConstraints cc = new CellConstraints();
		
		builder.addLabel(TheTVDBImportPlugin.messages.getString("GlobalConfigurationPanel.LanguageCombobox"), cc.xy(2, 2, CellConstraints.RIGHT, CellConstraints.DEFAULT));
		builder.add(cbImportLanguage, cc.xy(4, 2));

		JScrollPane sp = new JScrollPane(builder.getPanel());
		sp.setBorder(BorderFactory.createEmptyBorder());
		
		add(sp);
	}
	
	/**
	 * Updates all graphical components to show the global configuration.<br>
	 * This is being used to roll back changes after editing properties and
	 * canceling the dialog.
	 */
	public void applyConfig() {
		String importLanguage = globalConfig.getImportLanguage();
		if(importLanguage != null && !importLanguage.equals("")) {
			((KeyedComboBoxModel) cbImportLanguage.getModel()).setSelectedKey(importLanguage);
		}
	}

	/**
	 * Updates the configuration to reflect the GUI
	 *
	 * @param gc the global configuration
	 */
	public void updateConfiguration(GlobalConfiguration gc) {
		gc.setImportLanguage((String) ((KeyedComboBoxModel) cbImportLanguage.getModel()).getSelectedKey());
	}
}
