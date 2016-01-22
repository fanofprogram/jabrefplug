package skyeagle.plugin.command;

import java.io.File;
import java.util.ArrayList;

import net.sf.jabref.BasePanel;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.JabRefFrame;
import net.sf.jabref.MetaData;
import net.sf.jabref.Util;
import net.sf.jabref.gui.FileListEntry;
import net.sf.jabref.gui.FileListTableModel;
import skyeagle.plugin.gui.UpdateDialog;
/*
 * �������ɾ��ѡ������������ӵ�pdf�ļ�
 */
public class UpdateDetachCommand {

	public UpdateDialog dialog;
	private GetFileLink getFile;
	private JabRefFrame frame;
	public BibtexEntry[] bes;
	private BasePanel panel;
	public MetaData metaData;

	public UpdateDetachCommand(JabRefFrame f) {
		frame = f;
		panel = frame.basePanel();
		//��ȡѡ�������
		bes = panel.mainTable.getSelectedEntries();
		metaData = panel.metaData();

		//�����Ի���
		dialog = new UpdateDialog(frame, "Delete pdf link and pdf file");
		dialog.modifiedDialog();

		//����ɾ���ļ��߳�
		getFile = new GetFileLink(this);
		Thread fileProc = new Thread(getFile);
		fileProc.start();

		dialog.setVisible(true);
		//�Ի���ȷ��ɾ�����ɾ��
		if (dialog.flagOK) {
			DeleteUpdate(getFile.alFile);
		}
	}

	//ɾ���洢��ArrayList�е��ļ�
	private void DeleteUpdate(ArrayList<File> alFile) {
		for (File file : alFile) {
			if (file == null) {
				frame.showMessage("�ļ������ڣ�");
			} else {
				if (!file.delete()) {
					frame.showMessage(file.getAbsolutePath() + "�޷�ɾ����");
				} 
			}
		}
		//�����Ӧ����������
		for (int i = 0; i < bes.length; i++) {
			bes[i].clearField("file");
		}
		//������Ŀ����
		panel.markBaseChanged();
	}

	//�����Ҫɾ�����ļ����߳�
	class GetFileLink implements Runnable {
		private UpdateDialog dig;
		public BibtexEntry[] bes;
		private MetaData metaData;
		private BasePanel panel;
		public ArrayList<File> alFile = new ArrayList<>();

		public GetFileLink(UpdateDetachCommand ud) {
			dig = ud.dialog;
			panel=ud.panel;
			bes = ud.bes;
			metaData = ud.metaData;
		}

		@Override
		public void run() {
			if (metaData != null) {
				dig.output("��Ҫɾ�������ļ���");
				for (int i = 0; i < bes.length; i++) {
					//ʹ��tmģ��
					FileListTableModel tm = new FileListTableModel();
					//�������е�file���tmģ��
					tm.setContent(bes[i].getField("file"));
					int row = tm.getRowCount();
					if (row > 0) {
						for (int j = 0; j < row; j++) {
							//tm�е�ÿһ��entry��Ӧһ���ļ�����
							FileListEntry fle = tm.getEntry(j);
							String link = fle.getLink();
							File file = Util.expandFilename(metaData, link);
							dig.output(file.getAbsolutePath());
							alFile.add(file);
						}
					} else {
						int id =panel.mainTable.findEntry(bes[i])+1;
						dig.output("��" + id + "����¼û�������ļ���");
					}
				}
			}
		}
	}
}
