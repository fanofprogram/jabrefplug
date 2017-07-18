package skyeagle.plugin.command;

import net.sf.jabref.JabRefFrame;
import skyeagle.plugin.getmail.ImapMail;
import skyeagle.plugin.gui.UpdateDialog;

public class SingleUrlFetchCommand {

	public UpdateDialog dialog;
	public JabRefFrame frame;
	public String url;

	public SingleUrlFetchCommand(JabRefFrame frame, String url) {
		this.frame = frame;
		this.url = url;
		// �����Ի���
		dialog = new UpdateDialog(frame, "���¼�¼��Ŀ");
		// ���������߳�
		UpdateUrl updateurl = new UpdateUrl(this);
		Thread update = new Thread(updateurl);
		update.start();

		dialog.setVisible(true);

		// û������item�Ļ��Ͳ��������׵������
		if (updateurl.strEntries.length() != 0)
			// �������׵����
			ImapMail.setItems(frame, updateurl.strEntries);
	}

}

class UpdateUrl implements Runnable {
	private UpdateDialog dig;
	private String url;
	public String strEntries;

	public UpdateUrl(SingleUrlFetchCommand sc) {
		dig = sc.dialog;
		url = sc.url;
	}

	public void run() {
		// �п���urlΪdoi����Ҫת��Ϊʵ�ʵ�url
		url = CommandUtil.DOItoURL(url);
		// ��ȡ���׵�����
		strEntries = ImapMail.getItem(url, dig);
		if (strEntries == null) {
			dig.output("��ַ" + url + "�������û�ȡʧ��");
		}
		dig.btnCancel.setText("�رնԻ���");
	}
}
