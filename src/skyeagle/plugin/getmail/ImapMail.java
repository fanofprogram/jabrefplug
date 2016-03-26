package skyeagle.plugin.getmail;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.net.URLDecoder;
import java.security.Security;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.URLName;
import javax.mail.internet.MimeUtility;
import javax.mail.search.AndTerm;
import javax.mail.search.ComparisonTerm;
import javax.mail.search.FromStringTerm;
import javax.mail.search.ReceivedDateTerm;
import javax.mail.search.SearchTerm;

import net.sf.jabref.BasePanel;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.BibtexFields;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefFrame;
import net.sf.jabref.gui.ImportInspectionDialog;
import net.sf.jabref.imports.BibtexParser;
import net.sf.jabref.imports.ParserResult;
import skyeagle.plugin.geturlcite.GetCite;
import skyeagle.plugin.gui.Endecrypt;
import skyeagle.plugin.gui.UpdateDialog;

import com.sun.mail.imap.IMAPMessage;
import com.sun.mail.util.QPDecoderStream;

public class ImapMail {

	private static String dateFormat = "yyyy-MM-dd"; // 默认的日期显示格式
	// 邮件服务器和端口
	// private String host = "imap.gmail.com";
	private String host = "64.233.189.109";
	private String port = "993";
	// 设置存储所有网址的集合
	private ArrayList<String> urls = new ArrayList<String>();

	// 存储已读取过的邮件日期
	private TreeSet<String> readedDay = new TreeSet<String>();

	// store info from setting dialog
	private String userName;
	private String userPassword;
	private String searchKeyword;
	private Date startdate;
	private Date enddate;

	private JabRefFrame frame;
	private UpdateDialog diag;

	// file used to store setting information
	private File pluginDir = new File(System.getProperty("user.home") + "/.jabref/plugins");
	private File gmailfile = new File(pluginDir, "GmailSetting.prop");
	private File dayfile = new File(pluginDir, "day.prop");

	public ImapMail(JabRefFrame frame, UpdateDialog diag) {
		this.frame = frame;
		this.diag = diag;
		try {
			BufferedReader bfr = new BufferedReader(new FileReader(gmailfile));
			Properties prop = new Properties();
			prop.load(bfr);

			// 邮箱的用户名，密码
			userName = prop.getProperty("username");
			String newPwd = prop.getProperty("password");
			userPassword = Endecrypt.convertMD5(newPwd);
			// 邮件主题搜索关键字
			searchKeyword = "学术搜索快讯 - [ " + prop.getProperty("searchkeyword") + " ]";

			// 设置日期，用于邮件日期的比较
			String dateString = prop.getProperty("startday");
			SimpleDateFormat sDateFormat = new SimpleDateFormat(dateFormat);
			startdate = sDateFormat.parse(dateString, new ParsePosition(0));

			dateString = prop.getProperty("endday");
			sDateFormat = new SimpleDateFormat(dateFormat);
			enddate = sDateFormat.parse(dateString, new ParsePosition(0));
			diag.output("开始连接Gmail信箱.....");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public ArrayList<String> getEmailContent() {
		// 设置session的属性
		Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
		Properties prop = System.getProperties();
		prop.setProperty("mail.store.protocol", "imaps");
		prop.setProperty("mail.imap.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
		prop.setProperty("mail.imap.socketFactory.fallback", "false");
		prop.setProperty("mail.imap.socketFactory.port", port);
		prop.setProperty("mail.imap.auth.plain.disable", "true");
		prop.setProperty("mail.imap.auth.login.disable", "true");
		// 产生session实例并连接
		try {
			Session session = Session.getInstance(prop);
			URLName urln = new URLName("imaps", host, 993, null, userName, userPassword);

			Store store = session.getStore(urln);
			store.connect();
			// 设置对话框中的显示信息。
			diag.output("已经连上信箱.");
			diag.output("开始读取收件箱中的邮件.....");
			// 读收件箱，设置只读模式
			Folder folder = store.getFolder("INBOX");
			folder.open(Folder.READ_ONLY);
			// 搜索邮件，开始和结束日期之间
			ReceivedDateTerm rdtStart=new ReceivedDateTerm(ComparisonTerm.GT, startdate);
			ReceivedDateTerm rdtEnd=new ReceivedDateTerm(ComparisonTerm.LT, enddate);
			FromStringTerm fstTerm=new FromStringTerm("scholaralerts-noreply@google.com");
			//同时满足上面三个条件
			SearchTerm[] sts={rdtStart,rdtEnd,fstTerm};
			SearchTerm st = new AndTerm(sts);
			Message[] messages = folder.search(st);

			// 设置对话框中的显示信息。
			diag.output("收件箱中共有" + messages.length + "封" + searchKeyword + "邮件。");
			if (messages.length != 0) {
				diag.output("========开始解析这些邮件=========");
				for (Message message : messages) {
					IMAPMessage msg = (IMAPMessage) message;
					String subject = MimeUtility.decodeText(msg.getSubject());
					if (subject.equals(searchKeyword)) {
						// 获取邮件的发送日期并显示
						Date sentDate = msg.getSentDate();
						SimpleDateFormat format = new SimpleDateFormat(dateFormat);
						String strSentDate = format.format(sentDate);
						// 设置对话框中的显示信息。
						diag.output("分析发送日期为" + strSentDate + "的邮件...");
						readedDay.add(strSentDate);
						// 获取邮件内容,因为邮件类型为”text/html"，所以可以
						// 这样直接获取。
						String strTmp = null;
						if (msg.isMimeType("text/html")) {
							if (msg.getContent() instanceof String) {
								strTmp = msg.getContent().toString();
							} else if (msg.getContent() instanceof QPDecoderStream) {
								// 有时候由于编码的问题返回的不是字符串，而是编码输入流，需要下面的程序将
								// 流输出字符串
								BufferedInputStream bis = new BufferedInputStream((InputStream) msg.getContent());
								ByteArrayOutputStream baos = new ByteArrayOutputStream();
								while (true) {
									int c = bis.read();
									if (c == -1) {
										break;
									}
									baos.write(c);
								}
								strTmp = new String(baos.toByteArray());
								// 生成的字符串还会由于'%'和‘+'产生错误，下面的程序就是避免这种情况
								try {
									StringBuffer tempBuffer = new StringBuffer();
									int incrementor = 0;
									int dataLength = strTmp.length();
									while (incrementor < dataLength) {
										char charecterAt = strTmp.charAt(incrementor);
										if (charecterAt == '%') {
											tempBuffer.append("<percentage>");
										} else if (charecterAt == '+') {
											tempBuffer.append("<plus>");
										} else {
											tempBuffer.append(charecterAt);
										}
										incrementor++;
									}
									strTmp = tempBuffer.toString();
									strTmp = URLDecoder.decode(strTmp, "utf-8");
									strTmp = strTmp.replaceAll("<percentage>", "%");
									strTmp = strTmp.replaceAll("<plus>", "+");
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						}
						// 从邮件中获取网址
						ArrayList<String> al = getURL(strTmp);
						urls.addAll(al);
					}
				}
			}
			storeDay();
			folder.close(false);
			store.close();
		} catch (NoSuchProviderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			frame.showMessage("不能连接上Gmail信箱，请检查用户名，密码和网络。");
			diag.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return urls;
	}

	private void storeDay() {
		// TODO Auto-generated method stub
		try {
			// 先读取原先文件中的日期，
			// readedDay 是treeset集合，不会重复，并会有自然序
			Properties prop = new Properties();
			if (dayfile.exists()) {
				BufferedReader bfr = new BufferedReader(new FileReader(dayfile));
				prop.load(bfr);
				String count = prop.getProperty("count");
				if (count != null) {
					int c = Integer.parseInt(prop.getProperty("count"));
					for (int i = 1; i <= c; i++) {
						String day = prop.getProperty("day" + i);
						readedDay.add(day);
					}
				}
			}
			// 把现在的日期个数及日期重新存入文件中
			prop.setProperty("count", String.valueOf(readedDay.size()));
			int i = 1;
			for (Iterator<String> it = readedDay.iterator(); it.hasNext(); i++) {
				prop.setProperty("day" + i, it.next());
			}
			BufferedWriter bfw = new BufferedWriter(new FileWriter(dayfile));
			prop.store(bfw, "days");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private ArrayList<String> getURL(String strTmp) {
		// TODO Auto-generated method stub
		ArrayList<String> al = new ArrayList<String>();
		int beginIndex = 0;
		int endIndex = 0;
		while (true) {
			beginIndex = strTmp.indexOf("href=", endIndex);
			if (beginIndex == -1)
				break;
			endIndex = strTmp.indexOf(">", beginIndex);
			// 不要开始的href="和最后的"
			beginIndex = beginIndex + 6;
			endIndex = endIndex - 1;
			String url = strTmp.substring(beginIndex, endIndex);
			al.add(url);
		}

		// 最后两个网址没用，去掉，注意两个都是减一
		// 减一后，总数目发生变化了。
		al.remove(al.size() - 1);
		al.remove(al.size() - 1);
		// 去掉google搜索的信息，只留文献网址。
		String element = null;
		for (int i = 0; i < al.size(); i++) {
			String tmpStr = al.get(i);
			// 查找第二个http，所以从10开始，第一个是google的
			int begin = tmpStr.indexOf("http", 10);
			// 有可能google搜索中不包含网址，那么就不进行google的剔除
			// begin不等于-1表明包含
			try {
				if (begin != -1) {
					int end = tmpStr.indexOf("&amp;", 10);
					// google对网址中的特殊符号进行了编码，因此需要进行解码。
					element = URLDecoder.decode(tmpStr.substring(begin, end), "utf-8");
				} else {
					element = URLDecoder.decode(tmpStr.substring(0, tmpStr.length()), "utf-8");
				}
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// 设置对话框中的显示信息。
			diag.output(element);
			al.set(i, element);
		}
		return al;
	}

	public static String getItem(String itemUrl, UpdateDialog diag) {
		// 枚举，给出了所有能够获取文献引用的类名
		UrlKeywords tmp[] = UrlKeywords.values();

		String urlClassName = null;
		String itemString = null;
		boolean isFind = false;

		// 循环判断给的网址符合那个类，获取用于处理给定网址的类的类名
		for (UrlKeywords className : tmp) {
			if (className.isThisUrl(itemUrl)) {
				urlClassName = className.name();
				isFind = true;
				break;
			}
		}

		// 找到类名以后，使用反射调用对应的类
		if (isFind) {
			try {
				// 根据类名产生class类实例
				Class<?> clazz = Class.forName("skyeagle.plugin.geturlcite." + urlClassName);
				// 产生对应类的构造函数
				Constructor<?> con = clazz.getConstructor(String.class);
				// 生成对应类的实例
				GetCite getCite = (GetCite) con.newInstance(itemUrl);
				// 调用对应类的方法获取文献引用
				itemString = getCite.getCiteItem();
				if (itemString != null) {
					diag.output("完成对" + itemUrl + "中文献引用的获取。");
				} else {
					diag.output("无法连接" + itemUrl + "。 请检查网络。");
					return null;
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 

		} else {
			diag.output("找不到" + itemUrl + "的匹配器。");
		}
		return itemString;
	}

	/*
	 * 将引用字符串导入到jabref中，使用了jabref的导入对话框
	 */
	public static void setItems(JabRefFrame frame,String sbEntries) {
		try {

			// 调用jabref中的函数将字符串转化以后，导入到jabref中
			BasePanel panel = frame.basePanel();
			ParserResult pr = BibtexParser.parse(new StringReader(sbEntries.toString()));
			List<BibtexEntry> entries = new ArrayList<BibtexEntry>(pr.getDatabase().getEntries());
			ImportInspectionDialog diagImporter = new ImportInspectionDialog(frame, panel,
					BibtexFields.DEFAULT_INSPECTION_FIELDS, Globals.lang("Import"), false);
			diagImporter.addEntries(entries);
			diagImporter.entryListComplete();

			// 导入对话框的相关设置
			diagImporter.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
			Toolkit kit = Toolkit.getDefaultToolkit(); // 定义工具包
			Dimension screenSize = kit.getScreenSize(); // 获取屏幕的尺寸
			Point pt = new Point();
			pt.x = screenSize.width / 4;
			pt.y = screenSize.height / 4;
			diagImporter.setLocation(pt);
			diagImporter.setVisible(true);
			diagImporter.toFront();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
