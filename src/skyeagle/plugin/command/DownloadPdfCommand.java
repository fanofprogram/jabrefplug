package skyeagle.plugin.command;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import javax.swing.JOptionPane;

import net.sf.jabref.BasePanel;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.BibtexFields;
import net.sf.jabref.GUIGlobals;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefFrame;
import net.sf.jabref.MetaData;
import net.sf.jabref.external.ExternalFileType;
import net.sf.jabref.gui.FileListEntry;
import net.sf.jabref.gui.FileListTableModel;
import skyeagle.plugin.getmail.UrlKeywords;
import skyeagle.plugin.getpdf.GetPdfFile;
import skyeagle.plugin.gui.UpdateDialog;

/*
 * 下载pdf文件的类
 */
public class DownloadPdfCommand {
	public UpdateDialog dialog;
	public JabRefFrame frame;
	//需要读取代理文件
	private File pluginDir = new File(System.getProperty("user.home") + "/.jabref/plugins");
	private File file = new File(pluginDir, "proxy.prop");
	public Boolean usingProxy=false;

	public DownloadPdfCommand(JabRefFrame f) {
		frame = f;
		//下载开始的时候提示是否使用代理
		int select = JOptionPane.showConfirmDialog(frame, "是否使用代理下载文献？", "提示：", JOptionPane.YES_NO_OPTION);
		if (select == JOptionPane.OK_OPTION) {
			//代理文件不存在
			if (!file.exists()){
				frame.showMessage("请设置代理。");
				return;
			}
			usingProxy=true;
		}
		//产生下载对话框
		dialog = new UpdateDialog(frame, "下载pdf文件");
		//产生文件下载线程
		DownloadFile download = new DownloadFile(this);
		Thread downThread = new Thread(download);
		downThread.start();

		dialog.setVisible(true);
	}
}

/*
 * 下载pdf文件线程
 */
class DownloadFile implements Runnable {
	private UpdateDialog dig;
	private JabRefFrame frame;
	public BibtexEntry[] bes;
	private BasePanel panel;
	public MetaData metaData;
	public File pdfFile;
	public Boolean usingProxy;

	public DownloadFile(DownloadPdfCommand df) {
		dig = df.dialog;
		frame = df.frame;
		usingProxy=df.usingProxy;
		panel = frame.basePanel();
		//获取被选择的文献条目
		bes = panel.mainTable.getSelectedEntries();
		metaData = panel.metaData();

	}

	public void run() {
		dig.output("开始进行文献下载：");

		int numbes=0;
		while (!dig.stop && numbes < bes.length){
		//使用循环下载每一个被选择的文献
			BibtexEntry be=bes[numbes];
			String url = be.getField("url");
			if (url != null) {
				//有可能url为doi，需要转换为实际的url
				url=CommandUtil.DOItoURL(url);
				//下载的文件名统一用bibtexkey
				String link = be.getField(BibtexFields.KEY_FIELD) + ".pdf";
				//获取pdf文件保存目录
				String dir = getFileDir(metaData);
				File file = expandFilename(link, dir);
				dig.output("进行文献" + file.getAbsolutePath() + "的下载：");
				if (file != null && !file.exists()) {
					//pdf文件不存在，开始下载
					Downloading(url, dig, file);
					if (file.exists()) {
						//下载完成后，进行文献条目设置
						FileListTableModel tm = new FileListTableModel();
						tm.setContent(be.getField("file"));
						ExternalFileType fileType = Globals.prefs.getExternalFileTypeByExt("pdf");
						FileListEntry fle = new FileListEntry(link, null, fileType);
						//去除文件的路径
						UpdateField.getNewLink(fle, file, metaData);
						//加入文献条目
						tm.addEntry(0, fle);
						be.setField("file", tm.getStringRepresentation());
					} else {
						dig.output(file.toString() + "下载失败。");
					}
				} else
					dig.output(file.toString() + "文件存在。");
			} else {
				//获得文献的序号
				int id = panel.mainTable.findEntry(be) + 1;
				dig.output("第" + id + "条记录没有网址，无法下载文献");
				continue;
			}
			numbes++;
		}
		dig.btnCancel.setText("下载完成，关闭对话框");
	}

	private String getFileDir(MetaData metaData) {
		String[] dir = metaData.getFileDirectory("file");
		// Include the standard "file" directory:
		String[] fileDir = metaData.getFileDirectory(GUIGlobals.FILE_FIELD);
		// Include the directory of the bib file:
		ArrayList<String> al = new ArrayList<String>();
		for (int i = 0; i < dir.length; i++)
			if (!al.contains(dir[i]))
				al.add(dir[i]);
		for (int i = 0; i < fileDir.length; i++)
			if (!al.contains(fileDir[i]))
				al.add(fileDir[i]);
		String[] dirs = al.toArray(new String[al.size()]);
		for (String tmp : dirs) {
			if (tmp != null && tmp.length() != 0)
				return tmp;
		}
		return null;
	}

	public File expandFilename(String name, String dir) {
		File file = new File(name);
		if (!file.exists() && (dir != null)) {
			if (dir.endsWith(System.getProperty("file.separator")))
				name = dir + name;
			else
				name = dir + System.getProperty("file.separator") + name;

			file = new File(name);

			// Ok, try to fix / and \ problems:
			if (Globals.ON_WIN) {
				// workaround for catching Java bug in regexp replacer
				// and, why, why, why ... I don't get it - wegner 2006/01/22
				try {
					name = name.replaceAll("/", "\\\\");
				} catch (java.lang.StringIndexOutOfBoundsException exc) {
					System.err.println("An internal Java error was caused by the entry " + "\"" + name + "\"");
				}
			} else
				name = name.replaceAll("\\\\", "/");
			// System.out.println("expandFilename: "+name);
			file = new File(name);
		}
		return file;
	}

	private void Downloading(String url, UpdateDialog dig, File file) {
		// 枚举，给出了所有能够下载pdf的类名
		UrlKeywords tmp[] = UrlKeywords.values();

		String urlClassName = null;
		boolean isFind = false;

		// 循环判断给的网址符合那个类，获取用于处理给定网址的类的类名
		for (UrlKeywords className : tmp) {
			if (className.isThisUrl(url)) {
				urlClassName = className.name();
				isFind = true;
				break;
			}
		}

		// 找到类名以后，使用反射调用对应的类
		if (isFind) {
			try {
				// 根据类名产生class类实例
				Class<?> clazz = Class.forName("skyeagle.plugin.getpdf." + urlClassName);
				// 产生对应类的构造函数
				Constructor<?> con = clazz.getConstructor(String.class);
				// 生成对应类的实例
				GetPdfFile getFile = (GetPdfFile) con.newInstance(url);
				// 调用对应类的方法获取pdf
				getFile.getFile(dig, file,usingProxy);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} else {
			dig.output("找不到" + url + "的匹配器。");
		}
	}

}
