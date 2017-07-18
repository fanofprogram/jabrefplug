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

		// 产生对话框
		dialog = new UpdateDialog(frame, "更新记录条目");

		// 产生更新线程
		UpdateFile updatefile = new UpdateFile(this);
		Thread update = new Thread(updatefile);
		update.start();

		dialog.setVisible(true);
		
		// 没有文献item的话就不调用文献导入框了
		if (updatefile.sbEntries.length() != 0)
			//调用文献导入框
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
		// 读取文件的内容
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
					// 有可能url为doi，需要转换为实际的url
					url = CommandUtil.DOItoURL(url);
					// 重新获取文献的内容
					String item = ImapMail.getItem(url, dig);
					if (item == null) {
						dig.output("网址" + url + "文献引用获取失败");
						sbNotRec.add(urls.get(numurl));
					} else {
						sbEntries.append(item);
					}
				}
				numurl++;
			}
			// 总结信息
			dig.output("共有文献" + urls.size() + "篇，下面的" + sbNotRec.size() + "篇没有能够获取到文献信息：");
			for (int i = 0; i < sbNotRec.size(); i++) {
				dig.output(sbNotRec.get(i));
			}
			dig.output("完成所有文献引用的收集。");
		}else{
			dig.output(file.getAbsolutePath()+"是空的。");
		}
		
		dig.btnCancel.setText("关闭对话框");
	}

}