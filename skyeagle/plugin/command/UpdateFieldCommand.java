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
 * 这个类的目的是从网站中重新下载文献的内容，对原先的内容进行更新
 */
public class UpdateFieldCommand {

	public JabRefFrame frame;
	public UpdateDialog dialog;
	public BibtexEntry[] bes;
	public BasePanel panel;

	public UpdateFieldCommand(JabRefFrame f) {
		frame = f;
		//获取选择的文献
		panel = frame.basePanel();
		bes = panel.mainTable.getSelectedEntries();

		//产生对话框
		dialog = new UpdateDialog(frame, "更新记录条目");

		//产生更新线程
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
				//有可能url为doi，需要转换为实际的url
				url=CommandUtil.DOItoURL(url);
				//重新获取文献的内容
				String item = ImapMail.getItem(url, dig);
				if (item == null) {
					dig.output("网址" + url + "文献引用获取失败");
				} else {
					BibtexEntry oldEntry = bes[numbes];
					//将字符串转换为对应的文献entry
					BibtexEntry newEntry = BibtexParser.singleFromString(item);
					//比较和升级
					checkAndUpdate(oldEntry, newEntry);
				}
			} else {
				int id = panel.mainTable.findEntry(bes[numbes]) + 1;
				dig.output("第" + id + "条记录没有网址，无法更新");
			}
			numbes++;
		}
		dig.btnCancel.setText("关闭对话框");
	}

	private void checkAndUpdate(BibtexEntry oldEntry, BibtexEntry newEntry) {
		//只比较这三个
		String[] fields = { "year", "volume", "pages" };
		//获取旧key
		String oldKey = oldEntry.getField(BibtexFields.KEY_FIELD);
		for (int i = 0; i < fields.length; i++) {
			//进行比较
			int result = compareSingleField(fields[i], oldEntry, newEntry);
			if (result == NOT_EQUAL || result == EMPTY_IN_ONE) {
				int id = panel.mainTable.findEntry(oldEntry) + 1;
				String oldField = oldEntry.getField(fields[i]);
				String newField = newEntry.getField(fields[i]);
				dig.output("将第" + id + "条记录的" + fields[i] + "从" + oldField + "变为" + newField);
				//不同，更改
				oldEntry.setField(fields[i], newField);
			}
		}
		//根据设定的key生成规则生成新的key
		LabelPatternUtil.makeLabel(metaData, database, oldEntry);
		String newKey = oldEntry.getField(BibtexFields.KEY_FIELD);
		if (oldKey != newKey) {
			//key不同，则需要修改文件名
			fileChangeName(oldEntry);
		}
		//更新
		panel.markBaseChanged();
	}

	private void fileChangeName(BibtexEntry entry) {
		//获取文献file对应的tm
		FileListTableModel tm = new FileListTableModel();
		tm.setContent(entry.getField("file"));
		int row = tm.getRowCount();
		if (row > 0) {
			for (int j = 0; j < row; j++) {
				//tm中每一个entry对应一个文件
				FileListEntry fle = tm.getEntry(j);
				//获取文件的类型
				ExternalFileType filetype = fle.getType();
				if ("PDF" == filetype.getName()) {
					//获取旧文件名及路径
					String link = fle.getLink();
					File file = Util.expandFilename(metaData, link);
					//新文件名
					String newLink = entry.getField(BibtexFields.KEY_FIELD) + ".pdf";
					if (file != null && file.exists()) {
						File dir = file.getParentFile();
						File newFile = new File(dir, newLink);
						dig.output("对文件" + file.getAbsolutePath() + "进行了改名。");
						file.renameTo(newFile);
						//去除绝对路径
						getNewLink(fle, newFile,metaData);
						//将修改后的放回的文献file中
						entry.setField("file", tm.getStringRepresentation());
					}
				}
			}
		} else {
			int id = panel.mainTable.findEntry(entry) + 1;
			dig.output("第" + id + "条记录没有连接文件！不需要进行文件改名。");
		}
	}

	private int compareSingleField(String field, BibtexEntry one, BibtexEntry two) {
		String s1 = one.getField(field), s2 = two.getField(field);
		
		//判断是否为空
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
