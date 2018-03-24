package skyeagle.plugin.command;

import java.io.File;
import java.io.IOException;

import net.sf.jabref.BasePanel;
import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.BibtexFields;
import net.sf.jabref.DuplicateCheck;
import net.sf.jabref.GUIGlobals;
import net.sf.jabref.JabRefFrame;
import net.sf.jabref.MetaData;
import net.sf.jabref.Util;
import net.sf.jabref.external.ExternalFileType;
import net.sf.jabref.gui.FileListEntry;
import net.sf.jabref.gui.FileListTableModel;
import net.sf.jabref.imports.BibtexParser;
import net.sf.jabref.labelPattern.LabelPatternUtil;
import skyeagle.plugin.getmail.ImapMail;
import skyeagle.plugin.gui.UpdateDialog;

/*
 * ������Ŀ���Ǵ���վ�������������׵����ݣ���ԭ�ȵ����ݽ��и���
 */
public class UpdateFieldCommand {

	public JabRefFrame frame;
	public UpdateDialog dialog;
	public BibtexEntry[] bes;
	public BasePanel panel;

	public UpdateFieldCommand(JabRefFrame f) {
		frame = f;
		//��ȡѡ�������
		panel = frame.basePanel();
		bes = panel.mainTable.getSelectedEntries();

		//�����Ի���
		dialog = new UpdateDialog(frame, "���¼�¼��Ŀ");

		//���������߳�
		UpdateField updateField = new UpdateField(this);
		Thread update = new Thread(updateField);
		update.start();

		dialog.setVisible(true);
	}

}

class UpdateField implements Runnable {
	private UpdateDialog dig;
	public BibtexEntry[] bes;
	public BasePanel panel;
	public BibtexDatabase database;
	public MetaData metaData;
	public JabRefFrame frame;

	final static int TYPE_MISMATCH = -1, NOT_EQUAL = 0, EQUAL = 1, EMPTY_IN_ONE = 2, EMPTY_IN_TWO = 3,
			EMPTY_IN_BOTH = 4;

	public UpdateField(UpdateFieldCommand ud) {
		dig = ud.dialog;
		bes = ud.bes;
		panel = ud.panel;
		database = panel.database();
		metaData = panel.metaData();
		frame = ud.frame;
	}

	public void run() {
		int numbes=0;
		while(!dig.stop&&numbes<bes.length){
			String url = bes[numbes].getField("url");
			if (url != null&&!url.isEmpty()) {
				//�п���urlΪdoi����Ҫת��Ϊʵ�ʵ�url
				url=CommandUtil.DOItoURL(url);
				//���»�ȡ���׵�����
				String item = ImapMail.getItem(url, dig);
				if (item == null) {
					dig.output("��ַ" + url + "�������û�ȡʧ��");
				} else {
					BibtexEntry oldEntry = bes[numbes];
					//���ַ���ת��Ϊ��Ӧ������entry
					BibtexEntry newEntry = BibtexParser.singleFromString(item);
					//�ȽϺ�����
					checkAndUpdate(oldEntry, newEntry);
				}
			} else {
				int id = panel.mainTable.findEntry(bes[numbes]) + 1;
				dig.output("��" + id + "����¼û����ַ���޷�����");
			}
			numbes++;
		}
		dig.btnCancel.setText("�رնԻ���");
	}

	private void checkAndUpdate(BibtexEntry oldEntry, BibtexEntry newEntry) {
		//ֻ�Ƚ�������
		String[] fields = { "year", "volume", "pages" };
		//��ȡ��key
		String oldKey = oldEntry.getField(BibtexFields.KEY_FIELD);
		for (int i = 0; i < fields.length; i++) {
			//���бȽ�
			int result = compareSingleField(fields[i], oldEntry, newEntry);
			if (result == NOT_EQUAL || result == EMPTY_IN_ONE) {
				int id = panel.mainTable.findEntry(oldEntry) + 1;
				String oldField = oldEntry.getField(fields[i]);
				String newField = newEntry.getField(fields[i]);
				dig.output("����" + id + "����¼��" + fields[i] + "��" + oldField + "��Ϊ" + newField);
				//��ͬ������
				oldEntry.setField(fields[i], newField);
			}
		}
		//�����趨��key���ɹ��������µ�key
		LabelPatternUtil.makeLabel(metaData, database, oldEntry);
		String newKey = oldEntry.getField(BibtexFields.KEY_FIELD);
		if (oldKey != newKey) {
			//key��ͬ������Ҫ�޸��ļ���
			fileChangeName(oldEntry);
		}
		//����
		panel.markBaseChanged();
	}

	private void fileChangeName(BibtexEntry entry) {
		//��ȡ����file��Ӧ��tm
		FileListTableModel tm = new FileListTableModel();
		tm.setContent(entry.getField("file"));
		int row = tm.getRowCount();
		if (row > 0) {
			for (int j = 0; j < row; j++) {
				//tm��ÿһ��entry��Ӧһ���ļ�
				FileListEntry fle = tm.getEntry(j);
				//��ȡ�ļ�������
				ExternalFileType filetype = fle.getType();
				if ("PDF" == filetype.getName()) {
					//��ȡ���ļ�����·��
					String link = fle.getLink();
					File file = Util.expandFilename(metaData, link);
					//���ļ���
					String newLink = entry.getField(BibtexFields.KEY_FIELD) + ".pdf";
					if (file != null && file.exists()) {
						File dir = file.getParentFile();
						File newFile = new File(dir, newLink);
						dig.output("���ļ�" + file.getAbsolutePath() + "�����˸�����");
						file.renameTo(newFile);
						//ȥ������·��
						getNewLink(fle, newFile,metaData);
						//���޸ĺ�ķŻص�����file��
						entry.setField("file", tm.getStringRepresentation());
					}
				}
			}
		} else {
			int id = panel.mainTable.findEntry(entry) + 1;
			dig.output("��" + id + "����¼û�������ļ�������Ҫ�����ļ�������");
		}
	}

	private int compareSingleField(String field, BibtexEntry one, BibtexEntry two) {
		String s1 = one.getField(field), s2 = two.getField(field);
		
		//�ж��Ƿ�Ϊ��
		if (s1 == null) {
			if (s2 == null)
				return EMPTY_IN_BOTH;
			else
				return EMPTY_IN_ONE;
		} else if (s2 == null)
			return EMPTY_IN_TWO;

		// Util.pr(field+": '"+s1+"' vs '"+s2+"'");
		if (field.equals("pages")) {
			// Pages can be given with a variety of delimiters, "-", "--", " -
			// ", " -- ".
			// We do a replace to harmonize these to a simple "-":
			// After this, a simple test for equality should be enough:
			s1 = s1.replaceAll("[- ]+", "-");
			s2 = s2.replaceAll("[- ]+", "-");
			if (s1.equals(s2))
				return EQUAL;
			else
				return NOT_EQUAL;

		} else {
			s1 = s1.toLowerCase();
			s2 = s2.toLowerCase();
			double similarity = DuplicateCheck.correlateByWords(s1, s2, false);
			if (similarity > 0.8)
				return EQUAL;
			else
				return NOT_EQUAL;
		}
	}

	public static void getNewLink(FileListEntry fle, File fl,MetaData metaData) {
		// See if we should trim the file link to be relative to the file
		// directory:

		String[] dirs = metaData.getFileDirectory(GUIGlobals.FILE_FIELD);
		try {
			for (int i = 0; i < dirs.length; i++) {
				String canPath;
				canPath = (new File(dirs[i])).getCanonicalPath();
				if (fl.isAbsolute()) {
					String flPath = fl.getCanonicalPath();
					if ((flPath.length() > canPath.length()) && (flPath.startsWith(canPath))) {
						String relFileName = fl.getCanonicalPath().substring(canPath.length());
						fle.setLink(relFileName);
						break;
					}
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
