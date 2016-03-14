package skyeagle.plugin.command;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import net.sf.jabref.JabRefFrame;
import skyeagle.plugin.getmail.ImapMail;
import skyeagle.plugin.gui.UpdateDialog;

public class UpdateFileCommand {

	public UpdateDialog dialog;
	public JabRefFrame frame;
	public File file;

	public UpdateFileCommand(JabRefFrame frame, String filename) {
		this.frame = frame;
		file = new File(filename);

		// �����Ի���
		dialog = new UpdateDialog(frame, "���¼�¼��Ŀ");

		// ���������߳�
		UpdateFile updatefile = new UpdateFile(this);
		Thread update = new Thread(updatefile);
		update.start();

		dialog.setVisible(true);
		
		// û������item�Ļ��Ͳ��������׵������
		if (updatefile.sbEntries.length() != 0)
			//�������׵����
			ImapMail.setItems(frame,updatefile.sbEntries.toString());
	}

}

class UpdateFile implements Runnable {
	private UpdateDialog dig;
	private File file;
	public StringBuilder sbEntries;

	public UpdateFile(UpdateFileCommand ufc) {
		dig = ufc.dialog;
		file = ufc.file;
	}

	public void run() {
		sbEntries = new StringBuilder();
		ArrayList<String> urls = new ArrayList<String>();
		ArrayList<String> sbNotRec = new ArrayList<String>();
		// ��ȡ�ļ�������
		BufferedReader bfr=null;
		try {
			bfr = new BufferedReader(new FileReader(file));
			String temp = null;
			while ((temp = bfr.readLine()) != null) {
				urls.add(temp);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			try {
				bfr.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		if (!urls.isEmpty()) {
			int numurl = 0;
			while (!dig.stop & numurl < urls.size()) {
				String url = urls.get(numurl);
				if (!url.isEmpty()) {
					// �п���urlΪdoi����Ҫת��Ϊʵ�ʵ�url
					url = CommandUtil.DOItoURL(url);
					// ���»�ȡ���׵�����
					String item = ImapMail.getItem(url, dig);
					if (item == null) {
						dig.output("��ַ" + url + "�������û�ȡʧ��");
						sbNotRec.add(urls.get(numurl));
					} else {
						sbEntries.append(item);
					}
				}
				numurl++;
			}
			// �ܽ���Ϣ
			dig.output("��������" + urls.size() + "ƪ�������" + sbNotRec.size() + "ƪû���ܹ���ȡ��������Ϣ��");
			for (int i = 0; i < sbNotRec.size(); i++) {
				dig.output(sbNotRec.get(i));
			}
			dig.output("��������������õ��ռ���");
		}else{
			dig.output(file.getAbsolutePath()+"�ǿյġ�");
		}
		
		dig.btnCancel.setText("�رնԻ���");
	}

}