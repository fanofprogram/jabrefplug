package skyeagle.plugin.command;

import java.util.ArrayList;

import net.sf.jabref.JabRefFrame;
import skyeagle.plugin.getmail.ImapMail;
import skyeagle.plugin.gui.UpdateDialog;

public class UpdateGmailCommand {
	public ImapMail gmail;
	public UpdateDialog dialog;
	private GetMails getMail;

	public UpdateGmailCommand(JabRefFrame frame) {
		//�����Ի���
		dialog = new UpdateDialog(frame, "Importing Gmail content");
		gmail = new ImapMail(frame, dialog);
		//�����ʼ�������ȡ���õ��߳�
		getMail = new GetMails(dialog,gmail);
		Thread mail = new Thread(getMail);
		mail.start();

		dialog.setVisible(true);
		
		// û������item�Ļ��Ͳ��������׵������
		if (getMail.sbEntries.length() != 0)
			//�������׵����
			gmail.setItems(getMail.sbEntries.toString());
	}
}

class GetMails implements Runnable {
	private UpdateDialog dialog;
	private ImapMail gmail;
	public StringBuilder sbEntries;

	public GetMails(UpdateDialog dialog, ImapMail gmail) {
		this.dialog = dialog;
		this.gmail=gmail;
	}

	public void run() {
		sbEntries= new StringBuilder();
		ArrayList<String> urls = new ArrayList<String>();
		ArrayList<String> sbNotRec = new ArrayList<String>();

		// ��ȡ�ʼ��е�������ַ
		urls = gmail.getEmailContent();
		dialog.output("��ʼ��ȡ��ַ�е�������Ϣ.....");

		// ͨ��ѭ������ȡ������ַ�е����ã����stop�Ļ���ֹͣ��ȡ
		int numUrl = 0;
		while (urls != null && !dialog.stop && numUrl < urls.size()) {
			String item = ImapMail.getItem(urls.get(numUrl),dialog);
			if (item == null) {
				dialog.output("��ַ" + urls.get(numUrl) + "�������û�ȡʧ��");
				sbNotRec.add(urls.get(numUrl));
			} else {
				sbEntries.append(item);
			}
			numUrl++;
		}

		// �ܽ���Ϣ
		dialog.output("��������" + urls.size() + "ƪ�������" + sbNotRec.size() + "ƪû���ܹ���ȡ��������Ϣ��");
		for (int i = 0; i < sbNotRec.size(); i++) {
			dialog.output(sbNotRec.get(i));
		}
		dialog.output("��������������õ��ռ���");
		dialog.btnCancel.setText("�رնԻ���");
	}
}