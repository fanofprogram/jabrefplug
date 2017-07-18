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
		// 产生对话框
		dialog = new UpdateDialog(frame, "更新记录条目");
		// 产生更新线程
		UpdateUrl updateurl = new UpdateUrl(this);
		Thread update = new Thread(updateurl);
		update.start();

		dialog.setVisible(true);

		// 没有文献item的话就不调用文献导入框了
		if (updateurl.strEntries.length() != 0)
			// 调用文献导入框
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
		// 有可能url为doi，需要转换为实际的url
		url = CommandUtil.DOItoURL(url);
		// 获取文献的内容
		strEntries = ImapMail.getItem(url, dig);
		if (strEntries == null) {
			dig.output("网址" + url + "文献引用获取失败");
		}
		dig.btnCancel.setText("关闭对话框");
	}
}
