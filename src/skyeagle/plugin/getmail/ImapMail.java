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

	private static String dateFormat = "yyyy-MM-dd"; // Ĭ�ϵ�������ʾ��ʽ
	// �ʼ��������Ͷ˿�
	// private String host = "imap.gmail.com";
	private String host = "64.233.189.109";
	private String port = "993";
	// ���ô洢������ַ�ļ���
	private ArrayList<String> urls = new ArrayList<String>();

	// �洢�Ѷ�ȡ�����ʼ�����
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

			// ������û���������
			userName = prop.getProperty("username");
			String newPwd = prop.getProperty("password");
			userPassword = Endecrypt.convertMD5(newPwd);
			// �ʼ����������ؼ���
			searchKeyword = "ѧ��������Ѷ - [ " + prop.getProperty("searchkeyword") + " ]";

			// �������ڣ������ʼ����ڵıȽ�
			String dateString = prop.getProperty("startday");
			SimpleDateFormat sDateFormat = new SimpleDateFormat(dateFormat);
			startdate = sDateFormat.parse(dateString, new ParsePosition(0));

			dateString = prop.getProperty("endday");
			sDateFormat = new SimpleDateFormat(dateFormat);
			enddate = sDateFormat.parse(dateString, new ParsePosition(0));
			diag.output("��ʼ����Gmail����.....");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public ArrayList<String> getEmailContent() {
		// ����session������
		Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
		Properties prop = System.getProperties();
		prop.setProperty("mail.store.protocol", "imaps");
		prop.setProperty("mail.imap.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
		prop.setProperty("mail.imap.socketFactory.fallback", "false");
		prop.setProperty("mail.imap.socketFactory.port", port);
		prop.setProperty("mail.imap.auth.plain.disable", "true");
		prop.setProperty("mail.imap.auth.login.disable", "true");
		// ����sessionʵ��������
		try {
			Session session = Session.getInstance(prop);
			URLName urln = new URLName("imaps", host, 993, null, userName, userPassword);

			Store store = session.getStore(urln);
			store.connect();
			// ���öԻ����е���ʾ��Ϣ��
			diag.output("�Ѿ���������.");
			diag.output("��ʼ��ȡ�ռ����е��ʼ�.....");
			// ���ռ��䣬����ֻ��ģʽ
			Folder folder = store.getFolder("INBOX");
			folder.open(Folder.READ_ONLY);
			// �����ʼ�����ʼ�ͽ�������֮��
			ReceivedDateTerm rdtStart=new ReceivedDateTerm(ComparisonTerm.GT, startdate);
			ReceivedDateTerm rdtEnd=new ReceivedDateTerm(ComparisonTerm.LT, enddate);
			FromStringTerm fstTerm=new FromStringTerm("scholaralerts-noreply@google.com");
			//ͬʱ����������������
			SearchTerm[] sts={rdtStart,rdtEnd,fstTerm};
			SearchTerm st = new AndTerm(sts);
			Message[] messages = folder.search(st);

			// ���öԻ����е���ʾ��Ϣ��
			diag.output("�ռ����й���" + messages.length + "��" + searchKeyword + "�ʼ���");
			if (messages.length != 0) {
				diag.output("========��ʼ������Щ�ʼ�=========");
				for (Message message : messages) {
					IMAPMessage msg = (IMAPMessage) message;
					String subject = MimeUtility.decodeText(msg.getSubject());
					if (subject.equals(searchKeyword)) {
						// ��ȡ�ʼ��ķ������ڲ���ʾ
						Date sentDate = msg.getSentDate();
						SimpleDateFormat format = new SimpleDateFormat(dateFormat);
						String strSentDate = format.format(sentDate);
						// ���öԻ����е���ʾ��Ϣ��
						diag.output("������������Ϊ" + strSentDate + "���ʼ�...");
						readedDay.add(strSentDate);
						// ��ȡ�ʼ�����,��Ϊ�ʼ�����Ϊ��text/html"�����Կ���
						// ����ֱ�ӻ�ȡ��
						String strTmp = null;
						if (msg.isMimeType("text/html")) {
							if (msg.getContent() instanceof String) {
								strTmp = msg.getContent().toString();
							} else if (msg.getContent() instanceof QPDecoderStream) {
								// ��ʱ�����ڱ�������ⷵ�صĲ����ַ��������Ǳ�������������Ҫ����ĳ���
								// ������ַ���
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
								// ���ɵ��ַ�����������'%'�͡�+'������������ĳ�����Ǳ����������
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
						// ���ʼ��л�ȡ��ַ
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
			frame.showMessage("����������Gmail���䣬�����û�������������硣");
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
			// �ȶ�ȡԭ���ļ��е����ڣ�
			// readedDay ��treeset���ϣ������ظ�����������Ȼ��
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
			// �����ڵ����ڸ������������´����ļ���
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
			// ��Ҫ��ʼ��href="������"
			beginIndex = beginIndex + 6;
			endIndex = endIndex - 1;
			String url = strTmp.substring(beginIndex, endIndex);
			al.add(url);
		}

		// ���������ַû�ã�ȥ����ע���������Ǽ�һ
		// ��һ������Ŀ�����仯�ˡ�
		al.remove(al.size() - 1);
		al.remove(al.size() - 1);
		// ȥ��google��������Ϣ��ֻ��������ַ��
		String element = null;
		for (int i = 0; i < al.size(); i++) {
			String tmpStr = al.get(i);
			// ���ҵڶ���http�����Դ�10��ʼ����һ����google��
			int begin = tmpStr.indexOf("http", 10);
			// �п���google�����в�������ַ����ô�Ͳ�����google���޳�
			// begin������-1��������
			try {
				if (begin != -1) {
					int end = tmpStr.indexOf("&amp;", 10);
					// google����ַ�е�������Ž����˱��룬�����Ҫ���н��롣
					element = URLDecoder.decode(tmpStr.substring(begin, end), "utf-8");
				} else {
					element = URLDecoder.decode(tmpStr.substring(0, tmpStr.length()), "utf-8");
				}
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// ���öԻ����е���ʾ��Ϣ��
			diag.output(element);
			al.set(i, element);
		}
		return al;
	}

	public static String getItem(String itemUrl, UpdateDialog diag) {
		// ö�٣������������ܹ���ȡ�������õ�����
		UrlKeywords tmp[] = UrlKeywords.values();

		String urlClassName = null;
		String itemString = null;
		boolean isFind = false;

		// ѭ���жϸ�����ַ�����Ǹ��࣬��ȡ���ڴ��������ַ���������
		for (UrlKeywords className : tmp) {
			if (className.isThisUrl(itemUrl)) {
				urlClassName = className.name();
				isFind = true;
				break;
			}
		}

		// �ҵ������Ժ�ʹ�÷�����ö�Ӧ����
		if (isFind) {
			try {
				// ������������class��ʵ��
				Class<?> clazz = Class.forName("skyeagle.plugin.geturlcite." + urlClassName);
				// ������Ӧ��Ĺ��캯��
				Constructor<?> con = clazz.getConstructor(String.class);
				// ���ɶ�Ӧ���ʵ��
				GetCite getCite = (GetCite) con.newInstance(itemUrl);
				// ���ö�Ӧ��ķ�����ȡ��������
				itemString = getCite.getCiteItem();
				if (itemString != null) {
					diag.output("��ɶ�" + itemUrl + "���������õĻ�ȡ��");
				} else {
					diag.output("�޷�����" + itemUrl + "�� �������硣");
					return null;
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 

		} else {
			diag.output("�Ҳ���" + itemUrl + "��ƥ������");
		}
		return itemString;
	}

	/*
	 * �������ַ������뵽jabref�У�ʹ����jabref�ĵ���Ի���
	 */
	public static void setItems(JabRefFrame frame,String sbEntries) {
		try {

			// ����jabref�еĺ������ַ���ת���Ժ󣬵��뵽jabref��
			BasePanel panel = frame.basePanel();
			ParserResult pr = BibtexParser.parse(new StringReader(sbEntries.toString()));
			List<BibtexEntry> entries = new ArrayList<BibtexEntry>(pr.getDatabase().getEntries());
			ImportInspectionDialog diagImporter = new ImportInspectionDialog(frame, panel,
					BibtexFields.DEFAULT_INSPECTION_FIELDS, Globals.lang("Import"), false);
			diagImporter.addEntries(entries);
			diagImporter.entryListComplete();

			// ����Ի�����������
			diagImporter.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
			Toolkit kit = Toolkit.getDefaultToolkit(); // ���幤�߰�
			Dimension screenSize = kit.getScreenSize(); // ��ȡ��Ļ�ĳߴ�
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
