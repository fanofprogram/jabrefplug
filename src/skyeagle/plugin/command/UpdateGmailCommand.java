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
		//产生对话框
		dialog = new UpdateDialog(frame, "Importing Gmail content");
		gmail = new ImapMail(frame, dialog);
		//产生邮件处理及获取引用的线程
		getMail = new GetMails(dialog,gmail);
		Thread mail = new Thread(getMail);
		mail.start();

		dialog.setVisible(true);
		
		// 没有文献item的话就不调用文献导入框了
		if (getMail.sbEntries.length() != 0)
			//调用文献导入框
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

		// 获取邮件中的文献网址
		urls = gmail.getEmailContent();
		dialog.output("开始获取网址中的文献信息.....");

		// 通过循环来获取所有网址中的引用，如果stop的话，停止获取
		int numUrl = 0;
		while (urls != null && !dialog.stop && numUrl < urls.size()) {
			String item = ImapMail.getItem(urls.get(numUrl),dialog);
			if (item == null) {
				dialog.output("网址" + urls.get(numUrl) + "文献引用获取失败");
				sbNotRec.add(urls.get(numUrl));
			} else {
				sbEntries.append(item);
			}
			numUrl++;
		}

		// 总结信息
		dialog.output("共有文献" + urls.size() + "篇，下面的" + sbNotRec.size() + "篇没有能够获取到文献信息：");
		for (int i = 0; i < sbNotRec.size(); i++) {
			dialog.output(sbNotRec.get(i));
		}
		dialog.output("完成所有文献引用的收集。");
		dialog.btnCancel.setText("关闭对话框");
	}
}