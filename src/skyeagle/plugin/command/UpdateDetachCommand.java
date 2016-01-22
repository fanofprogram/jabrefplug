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
 * 这个类是删除选择的文献所连接的pdf文件
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
		//获取选择的文献
		bes = panel.mainTable.getSelectedEntries();
		metaData = panel.metaData();

		//产生对话框
		dialog = new UpdateDialog(frame, "Delete pdf link and pdf file");
		dialog.modifiedDialog();

		//产生删除文件线程
		getFile = new GetFileLink(this);
		Thread fileProc = new Thread(getFile);
		fileProc.start();

		dialog.setVisible(true);
		//对话框确认删除后才删除
		if (dialog.flagOK) {
			DeleteUpdate(getFile.alFile);
		}
	}

	//删除存储在ArrayList中的文件
	private void DeleteUpdate(ArrayList<File> alFile) {
		for (File file : alFile) {
			if (file == null) {
				frame.showMessage("文件不存在！");
			} else {
				if (!file.delete()) {
					frame.showMessage(file.getAbsolutePath() + "无法删除。");
				} 
			}
		}
		//清除对应的文献链接
		for (int i = 0; i < bes.length; i++) {
			bes[i].clearField("file");
		}
		//文献条目更新
		panel.markBaseChanged();
	}

	//获得需要删除的文件的线程
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
				dig.output("将要删除以下文件：");
				for (int i = 0; i < bes.length; i++) {
					//使用tm模板
					FileListTableModel tm = new FileListTableModel();
					//用文献中的file填充tm模板
					tm.setContent(bes[i].getField("file"));
					int row = tm.getRowCount();
					if (row > 0) {
						for (int j = 0; j < row; j++) {
							//tm中的每一个entry对应一个文件连接
							FileListEntry fle = tm.getEntry(j);
							String link = fle.getLink();
							File file = Util.expandFilename(metaData, link);
							dig.output(file.getAbsolutePath());
							alFile.add(file);
						}
					} else {
						int id =panel.mainTable.findEntry(bes[i])+1;
						dig.output("第" + id + "条记录没有连接文件！");
					}
				}
			}
		}
	}
}
