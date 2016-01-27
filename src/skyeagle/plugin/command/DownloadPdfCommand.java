package skyeagle.plugin.command;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import javax.swing.JOptionPane;

import net.sf.jabref.BasePanel;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.BibtexFields;
import net.sf.jabref.GUIGlobals;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefFrame;
import net.sf.jabref.MetaData;
import net.sf.jabref.external.ExternalFileType;
import net.sf.jabref.gui.FileListEntry;
import net.sf.jabref.gui.FileListTableModel;
import skyeagle.plugin.getmail.UrlKeywords;
import skyeagle.plugin.getpdf.GetPdfFile;
import skyeagle.plugin.gui.UpdateDialog;

/*
 * ����pdf�ļ�����
 */
public class DownloadPdfCommand {
	public UpdateDialog dialog;
	public JabRefFrame frame;
	//��Ҫ��ȡ�����ļ�
	private File pluginDir = new File(System.getProperty("user.home") + "/.jabref/plugins");
	private File file = new File(pluginDir, "proxy.prop");
	public Boolean usingProxy=false;

	public DownloadPdfCommand(JabRefFrame f) {
		frame = f;
		//���ؿ�ʼ��ʱ����ʾ�Ƿ�ʹ�ô���
		int select = JOptionPane.showConfirmDialog(frame, "�Ƿ�ʹ�ô����������ף�", "��ʾ��", JOptionPane.YES_NO_OPTION);
		if (select == JOptionPane.OK_OPTION) {
			//�����ļ�������
			if (!file.exists()){
				frame.showMessage("�����ô���");
				return;
			}
			usingProxy=true;
		}
		//�������ضԻ���
		dialog = new UpdateDialog(frame, "����pdf�ļ�");
		//�����ļ������߳�
		DownloadFile download = new DownloadFile(this);
		Thread downThread = new Thread(download);
		downThread.start();

		dialog.setVisible(true);
	}
}

/*
 * ����pdf�ļ��߳�
 */
class DownloadFile implements Runnable {
	private UpdateDialog dig;
	private JabRefFrame frame;
	public BibtexEntry[] bes;
	private BasePanel panel;
	public MetaData metaData;
	public File pdfFile;
	public Boolean usingProxy;

	public DownloadFile(DownloadPdfCommand df) {
		dig = df.dialog;
		frame = df.frame;
		usingProxy=df.usingProxy;
		panel = frame.basePanel();
		//��ȡ��ѡ���������Ŀ
		bes = panel.mainTable.getSelectedEntries();
		metaData = panel.metaData();

	}

	public void run() {
		dig.output("��ʼ�����������أ�");

		int numbes=0;
		while (!dig.stop && numbes < bes.length){
		//ʹ��ѭ������ÿһ����ѡ�������
			BibtexEntry be=bes[numbes];
			String url = be.getField("url");
			if (url != null) {
				//�п���urlΪdoi����Ҫת��Ϊʵ�ʵ�url
				url=CommandUtil.DOItoURL(url);
				//���ص��ļ���ͳһ��bibtexkey
				String link = be.getField(BibtexFields.KEY_FIELD) + ".pdf";
				//��ȡpdf�ļ�����Ŀ¼
				String dir = getFileDir(metaData);
				File file = expandFilename(link, dir);
				dig.output("��������" + file.getAbsolutePath() + "�����أ�");
				if (file != null && !file.exists()) {
					//pdf�ļ������ڣ���ʼ����
					Downloading(url, dig, file);
					if (file.exists()) {
						//������ɺ󣬽���������Ŀ����
						FileListTableModel tm = new FileListTableModel();
						tm.setContent(be.getField("file"));
						ExternalFileType fileType = Globals.prefs.getExternalFileTypeByExt("pdf");
						FileListEntry fle = new FileListEntry(link, null, fileType);
						//ȥ���ļ���·��
						UpdateField.getNewLink(fle, file, metaData);
						//����������Ŀ
						tm.addEntry(0, fle);
						be.setField("file", tm.getStringRepresentation());
					} else {
						dig.output(file.toString() + "����ʧ�ܡ�");
					}
				} else
					dig.output(file.toString() + "�ļ����ڡ�");
			} else {
				//������׵����
				int id = panel.mainTable.findEntry(be) + 1;
				dig.output("��" + id + "����¼û����ַ���޷���������");
				continue;
			}
			numbes++;
		}
		dig.btnCancel.setText("������ɣ��رնԻ���");
	}

	private String getFileDir(MetaData metaData) {
		String[] dir = metaData.getFileDirectory("file");
		// Include the standard "file" directory:
		String[] fileDir = metaData.getFileDirectory(GUIGlobals.FILE_FIELD);
		// Include the directory of the bib file:
		ArrayList<String> al = new ArrayList<String>();
		for (int i = 0; i < dir.length; i++)
			if (!al.contains(dir[i]))
				al.add(dir[i]);
		for (int i = 0; i < fileDir.length; i++)
			if (!al.contains(fileDir[i]))
				al.add(fileDir[i]);
		String[] dirs = al.toArray(new String[al.size()]);
		for (String tmp : dirs) {
			if (tmp != null && tmp.length() != 0)
				return tmp;
		}
		return null;
	}

	public File expandFilename(String name, String dir) {
		File file = new File(name);
		if (!file.exists() && (dir != null)) {
			if (dir.endsWith(System.getProperty("file.separator")))
				name = dir + name;
			else
				name = dir + System.getProperty("file.separator") + name;

			file = new File(name);

			// Ok, try to fix / and \ problems:
			if (Globals.ON_WIN) {
				// workaround for catching Java bug in regexp replacer
				// and, why, why, why ... I don't get it - wegner 2006/01/22
				try {
					name = name.replaceAll("/", "\\\\");
				} catch (java.lang.StringIndexOutOfBoundsException exc) {
					System.err.println("An internal Java error was caused by the entry " + "\"" + name + "\"");
				}
			} else
				name = name.replaceAll("\\\\", "/");
			// System.out.println("expandFilename: "+name);
			file = new File(name);
		}
		return file;
	}

	private void Downloading(String url, UpdateDialog dig, File file) {
		// ö�٣������������ܹ�����pdf������
		UrlKeywords tmp[] = UrlKeywords.values();

		String urlClassName = null;
		boolean isFind = false;

		// ѭ���жϸ�����ַ�����Ǹ��࣬��ȡ���ڴ��������ַ���������
		for (UrlKeywords className : tmp) {
			if (className.isThisUrl(url)) {
				urlClassName = className.name();
				isFind = true;
				break;
			}
		}

		// �ҵ������Ժ�ʹ�÷�����ö�Ӧ����
		if (isFind) {
			try {
				// ������������class��ʵ��
				Class<?> clazz = Class.forName("skyeagle.plugin.getpdf." + urlClassName);
				// ������Ӧ��Ĺ��캯��
				Constructor<?> con = clazz.getConstructor(String.class);
				// ���ɶ�Ӧ���ʵ��
				GetPdfFile getFile = (GetPdfFile) con.newInstance(url);
				// ���ö�Ӧ��ķ�����ȡpdf
				getFile.getFile(dig, file,usingProxy);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} else {
			dig.output("�Ҳ���" + url + "��ƥ������");
		}
	}

}
