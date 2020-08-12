package skyeagle.plugin.getmail;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.net.URLDecoder;
import java.security.Security;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Properties;
import java.util.TreeSet;

import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.NoSuchProviderException;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.URLName;
import javax.mail.internet.MimeUtility;
import javax.mail.search.AndTerm;
import javax.mail.search.ComparisonTerm;
import javax.mail.search.FromStringTerm;
import javax.mail.search.ReceivedDateTerm;
import javax.mail.search.SearchTerm;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import skyeagle.plugin.geturlcite.GetCite;
import skyeagle.plugin.gui.UpdateDialog;

import com.sun.mail.imap.IMAPMessage;
import com.sun.mail.util.QPDecoderStream;

public class GmailTest {
	private static String dateFormat = "yyyy-MM-dd"; // Ĭ�ϵ�������ʾ��ʽ
	// �ʼ��������Ͷ˿�
	private String host = "imap.gmail.com";
	private String port = "993";
	// ���ô洢������ַ�ļ���
	private ArrayList<String> urls = new ArrayList<String>();

	// �洢�Ѷ�ȡ�����ʼ�����
	private TreeSet<String> readedDay = new TreeSet<String>();

	// store info from setting dialog
	private String userName = "wangchao.henu";
	private String userPassword = "wang9706025Chao";
	private String searchKeyword = "thermoelectric";
	private Date startdate;
	private Date enddate;
	
	public static String filename;

	public GmailTest() {
		try {
			// �������ڣ������ʼ����ڵıȽ�
			String sDateString = "2020-08-01";
			filename=sDateString;
			SimpleDateFormat sDateFormat = new SimpleDateFormat(dateFormat);
			startdate = sDateFormat.parse(sDateString, new ParsePosition(0));

			sDateString = "2020-08-12";
			filename=filename+"-"+sDateString;
			sDateFormat = new SimpleDateFormat(dateFormat);
			enddate = sDateFormat.parse(sDateString, new ParsePosition(0));
			System.out.println("��ʼ����Gmail����.....");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public ArrayList<String> getEmailContent() {
		// ����session������
		Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
		Properties prop = System.getProperties();
		prop.setProperty("proxySet", "true");
		prop.setProperty("socksProxyHost", "127.0.0.1");
		prop.setProperty("socksProxyPort", "1080");

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
			System.out.println("�Ѿ���������.");
			System.out.println("��ʼ��ȡ�ռ����е��ʼ�.....");
			// ���ռ��䣬����ֻ��ģʽ
			Folder folder = store.getFolder("INBOX");
			folder.open(Folder.READ_ONLY);
			// �����ʼ�����ʼ�ͽ�������֮��
			ReceivedDateTerm rdtStart = new ReceivedDateTerm(ComparisonTerm.GT, startdate);
			ReceivedDateTerm rdtEnd = new ReceivedDateTerm(ComparisonTerm.LT, enddate);
			FromStringTerm fstTerm = new FromStringTerm("scholaralerts-noreply@google.com");
			// ͬʱ����������������
			SearchTerm[] sts = { rdtStart, rdtEnd, fstTerm };
			SearchTerm st = new AndTerm(sts);
			Message[] messages = folder.search(st);

			// ���öԻ����е���ʾ��Ϣ��
			System.out.println("�ռ����й���" + messages.length + "��" + searchKeyword + "�ʼ���");
			if (messages.length != 0) {
				System.out.println("========��ʼ������Щ�ʼ�=========");
				for (Message message : messages) {
					IMAPMessage msg = (IMAPMessage) message;
					String subject = MimeUtility.decodeText(msg.getSubject());
					if (subject.indexOf(searchKeyword) != -1) {
						// ��ȡ�ʼ��ķ������ڲ���ʾ
						Date sentDate = msg.getSentDate();
						SimpleDateFormat format = new SimpleDateFormat(dateFormat);
						String strSentDate = format.format(sentDate);
						// ���öԻ����е���ʾ��Ϣ��
						System.out.println("������������Ϊ" + strSentDate + "���ʼ�...");
						readedDay.add(strSentDate);
//						System.out.println(msg.getContentType());
//						System.out.println(msg.isMimeType("multipart/*"));
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
						} else if (msg.isMimeType("multipart/alternative")) {
							{
								Multipart mp = (Multipart) msg.getContent();
								int partCount = 0;
								if (mp.getCount() > 1)
									partCount = 1;
								BodyPart bodyPart = mp.getBodyPart(partCount);
								strTmp=bodyPart.getContent().toString();
							}
						}
						// ���ʼ��л�ȡ��ַ
						ArrayList<String> al = getURL(strTmp);
						urls.addAll(al);
					}
				}
			}
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
			System.out.println("����������Gmail���䣬�����û�������������硣");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return urls;
	}

	private ArrayList<String> getURL(String strTmp) {
		// TODO Auto-generated method stub
		ArrayList<String> al = new ArrayList<String>();
		Document doc = Jsoup.parse(strTmp, "utf-8");
		Elements links = doc.select("a[class=gse_alrt_title]");
		for (Element e : links) {
			al.add(e.attr("href"));
		}

		// ȥ��google��������Ϣ��ֻ��������ַ��
		String element = null;
		for (int i = 0; i < al.size(); i++) {
			String tmpStr = al.get(i);
			int begin = tmpStr.indexOf("url=", 10);
			begin += "url=".length();
			// �п���google�����в�������ַ����ô�Ͳ�����google���޳�
			// begin������-1��������
			try {
				if (begin != -1) {
					int end = tmpStr.indexOf("&hl=zh-CN", begin);
					// google����ַ�е�������Ž����˱��룬�����Ҫ���н��롣
					element = URLDecoder.decode(tmpStr.substring(begin, end), "utf-8");
				} else {
					element = URLDecoder.decode(tmpStr.substring(0, tmpStr.length()), "utf-8");
				}
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			al.set(i, element);
		}

		// ȥ���ظ�����ַ��2016.8.20��google �ʼ����ݷ����˱仯����Ҫȥ�ظ���
		// ����Set��������ȥ�ظ�
		HashSet<String> tmpSet = new HashSet<>(al);
		ArrayList<String> noDupAl = new ArrayList<>(tmpSet);
//		for (String s : noDupAl)
//			System.out.println(s);
		return noDupAl;
	}

	/**ʹ��FileOutputStream��д��txt�ļ�
     * @param txtPath txt�ļ�·��
     * @param content ��Ҫд����ı�
     */
    public static void writeTxt(String txtPath,String content){    
       File file = new File(txtPath);
       try {
           if(file.exists()){
               //�ж��ļ��Ƿ���ڣ���������ھ��½�һ��txt
               file.createNewFile();
           }
           FileWriter fw = new FileWriter(file, true);
           fw.write(content+'\n');
           fw.flush();
           fw.close();
       } catch (Exception e) {
           e.printStackTrace();
       }
    }

	public static void main(String[] args) {
		GmailTest gt = new GmailTest();
		ArrayList<String> al=gt.getEmailContent();
		for (String s : al) {
			System.out.println(s);
			writeTxt(filename, s);
		}
	}

}
