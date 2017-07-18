package skyeagle.plugin.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import net.sf.jabref.GUIGlobals;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefFrame;
import net.sf.jabref.SidePaneComponent;
import net.sf.jabref.SidePaneManager;
import net.sf.jabref.plugin.SidePanePlugin;

public class GmailImporterPane implements SidePanePlugin, ActionListener {

	protected SidePaneManager manager;
	private JMenuItem toggleMenu;
	private JabRefFrame frame;
	private GmailImporterPaneComponent c = null;

	public void init(JabRefFrame frame, SidePaneManager manager) {
		this.manager = manager;
		this.frame = frame;

		//²å¼þ²Ëµ¥
		toggleMenu = new JMenuItem("Gmail Importer", new ImageIcon(
				GUIGlobals.getIconUrl("search")));
		toggleMenu.setMnemonic(KeyEvent.VK_G);
		toggleMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G,
				ActionEvent.ALT_MASK));

		toggleMenu.addActionListener(this);

		Globals.prefs.defaults.put("GmailImporter", true);
	}

	public SidePaneComponent getSidePaneComponent() {
		c = new GmailImporterPaneComponent(manager, frame, toggleMenu);
		return c;
	}

	public JMenuItem getMenuItem() {
		if (Globals.prefs.getBoolean("GmailImporter")) {
			manager.show("GmailImporter");
		}
		if (c != null)
			c.setActiveBasePanel(frame.basePanel());
		return toggleMenu;
	}

	public String getShortcutKey() {
		return "alt G";
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == toggleMenu) {
			manager.toggle("GmailImporter");
		}
	}
}