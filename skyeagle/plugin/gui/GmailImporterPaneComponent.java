package skyeagle.plugin.gui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import net.sf.jabref.BasePanel;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.GUIGlobals;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefFrame;
import net.sf.jabref.SidePaneComponent;
import net.sf.jabref.SidePaneManager;
import net.sf.jabref.gui.FileDialogs;
import skyeagle.plugin.command.DownloadPdfCommand;
import skyeagle.plugin.command.UpdateDetachCommand;
import skyeagle.plugin.command.UpdateFieldCommand;
import skyeagle.plugin.command.UpdateFileCommand;
import skyeagle.plugin.command.UpdateGmailCommand;

class GmailImporterPaneComponent extends SidePaneComponent implements ActionListener {

	private static final long serialVersionUID = 1L;

	private GridBagLayout gbl = new GridBagLayout();
	private GridBagConstraints con = new GridBagConstraints();

	private JButton btnUpdate = new JButton(GUIGlobals.getImage("ranking"));
	private JButton btnSettings = new JButton(GUIGlobals.getImage("preferences"));
	private JButton btnDown = new JButton(GUIGlobals.getImage("pdfSmall"));
	private JButton btnProxy = new JButton(GUIGlobals.getImage("autoGroup"));
	private JButton btnDetach = new JButton(GUIGlobals.getImage("duplicate"));
	private JButton btnField = new JButton(GUIGlobals.getImage("dragNdropArrow"));
	private JButton btnOpenFile = new JButton(GUIGlobals.getImage("open"));
	private JButton btnOpenUrl = new JButton(GUIGlobals.getImage("search"));


	private SidePaneManager manager;
	private JMenuItem menu;
	private JabRefFrame frame;
	private GmailSettingDialog settingDialog;

	private File pluginDir = new File(System.getProperty("user.home") + "/.jabref/plugins");
	private File file = new File(pluginDir, "GmailSetting.prop");

	private BasePanel panel;
	private BibtexEntry[] bes;

	public GmailImporterPaneComponent(SidePaneManager manager, JabRefFrame frame, JMenuItem menu) {
		super(manager, GUIGlobals.getIconUrl("wwwSmall"), "Importer and Download");
		this.manager = manager;
		this.menu = menu;
		this.frame = frame;
		this.panel = frame.basePanel();

		Font font = menu.getFont();
		int fontSize = font.getSize();

		Dimension butDim = new Dimension(fontSize, fontSize + 10);

		btnUpdate.setPreferredSize(butDim);
		btnUpdate.setMinimumSize(butDim);
		btnUpdate.addActionListener(this);
		btnUpdate.setText("Update Gmail");
		btnUpdate.setToolTipText("Get Reference from Gmail.");

		btnSettings.addActionListener(this);
		btnSettings.setToolTipText("Settings");

		btnDown.setPreferredSize(butDim);
		btnDown.setMinimumSize(butDim);
		btnDown.addActionListener(this);
		btnDown.setText("Download pdf");
		btnDown.setToolTipText("Download pdf files from web");

		btnProxy.addActionListener(this);
		btnProxy.setToolTipText("�����ļ����ش���");

		btnField.setPreferredSize(butDim);
		btnField.setMinimumSize(butDim);
		btnField.addActionListener(this);
		btnField.setText("Update Fields");
		btnField.setToolTipText("Reload the fields from web");

		btnDetach.addActionListener(this);
		btnDetach.setToolTipText("Delete pdf link and pdf file");

		btnOpenFile.setPreferredSize(butDim);
		btnOpenFile.setMinimumSize(butDim);
		btnOpenFile.addActionListener(this);
		btnOpenFile.setText("Open url file");
		btnOpenFile.setToolTipText("�򿪰�����ַ���ļ�");

		btnOpenUrl.addActionListener(this);
		btnOpenUrl.setToolTipText("������ַ");

		JPanel main = new JPanel();
		main.setLayout(gbl);
		con.gridwidth = GridBagConstraints.REMAINDER;
		con.fill = GridBagConstraints.BOTH;
		con.weightx = 1;

		JPanel split = new JPanel();
		split.setLayout(new BoxLayout(split, BoxLayout.LINE_AXIS));
		btnUpdate.setMaximumSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
		split.add(btnUpdate);
		split.add(btnSettings);

		JPanel downPan = new JPanel();
		downPan.setLayout(new BoxLayout(downPan, BoxLayout.LINE_AXIS));
		btnDown.setMaximumSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
		downPan.add(btnDown);
		downPan.add(btnProxy);

		JPanel fieldPan = new JPanel();
		fieldPan.setLayout(new BoxLayout(fieldPan, BoxLayout.LINE_AXIS));
		btnField.setMaximumSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
		fieldPan.add(btnField);
		fieldPan.add(btnDetach);

		JPanel filePan = new JPanel();
		filePan.setLayout(new BoxLayout(filePan, BoxLayout.LINE_AXIS));
		btnOpenFile.setMaximumSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
		filePan.add(btnOpenFile);
		filePan.add(btnOpenUrl);

		JTextPane author = new JTextPane();
		author.setText("This plugin is written by ChaoWang.");

		gbl.setConstraints(split, con);
		main.add(split);

		gbl.setConstraints(downPan, con);
		main.add(downPan);

		gbl.setConstraints(fieldPan, con);
		main.add(fieldPan);

		gbl.setConstraints(filePan, con);
		main.add(filePan);

//		gbl.setConstraints(pdffilePan, con);
//		main.add(pdffilePan);

		gbl.setConstraints(author, con);
		main.add(author);

		main.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
		setContent(main);
		setName("GmailImporter");
	}

	public void setActiveBasePanel(BasePanel panel) {
		super.setActiveBasePanel(panel);
		if (panel == null) {
			boolean status = Globals.prefs.getBoolean("GmailImporter");
			manager.hide("GmailImporter");
			Globals.prefs.putBoolean("GmailImporter", status);
			menu.setEnabled(false);
		} else {
			if (Globals.prefs.getBoolean("GmailImporter")) {
				manager.show("GmailImporter");
			}
			menu.setEnabled(true);
		}
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == btnSettings) {
			// ����gmail���ð�ť
			settingDialog = new GmailSettingDialog(frame, "Gmail Importer Setting");
			settingDialog.setVisible(true);
		} else if (e.getSource() == btnUpdate) {
			// ����update Gmail��ť
			if (!file.exists()) {
				// �����ļ������ڣ������϶�û�е��ù����öԻ��򣬸��û���ʾ
				frame.showMessage("�������öԻ����������û���������.");
				return;
			} else {
				settingDialog = new GmailSettingDialog(frame, "Gmail Importer Setting");
				// ����ļ����ڣ����Ҹ���������ȷ��ֱ�ӵ���UpdateGmail�Ի���
				if (settingDialog.isUsernameOk() && settingDialog.isPasswordOk() && settingDialog.isSearchKeywordOk()) {
					new UpdateGmailCommand(frame);
				}
			}
		} else if (e.getSource() == btnProxy) {
			if (!file.exists()) {
				// �����ļ������ڣ������϶�û�е��ù����öԻ��򣬸��û���ʾ
				frame.showMessage("���������ip�Ͷ˿�.");
				return;
			} else {
				new ProxyDialog(frame);
			}

		} else if (e.getSource() == btnDown) {
			// ��������pdf��ť
			if (isEntriesNotNull()) {
				new DownloadPdfCommand(frame);
			}

		} else if (e.getSource() == btnDetach) {
			// ����ɾ��pdf���Ӻ�ɾ��pdf�ļ���ť
			if (isEntriesNotNull()) {
				new UpdateDetachCommand(frame);
			}
		} else if (e.getSource() == btnField) {
			// ���¸������ð�ť
			if (isEntriesNotNull()) {
				new UpdateFieldCommand(frame);
			}
		} else if (e.getSource() == btnOpenFile) {
			// �õ�ѡ����ļ���������·����
			String fileName = FileDialogs.getNewFile(frame, null, null, JFileChooser.OPEN_DIALOG, true);
			if (fileName != null)
				new UpdateFileCommand(frame, fileName);

		} else if (e.getSource() == btnOpenUrl) {
			new UrlDialog(frame);
		} 
	}

	public Boolean isEntriesNotNull() {
		if (panel != null) {
			bes = panel.mainTable.getSelectedEntries();
			if ((bes != null) && (bes.length > 0))
				return true;
			else
				frame.showMessage("��ѡ��Ҫ��������Ŀ��");
		}
		return false;
	}

	public void componentOpening() {
		Globals.prefs.putBoolean("GmailImporter", true);
	}

	public void componentClosing() {
		Globals.prefs.putBoolean("GmailImporter", false);
	}
}
