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
	private static String dateFormat = "yyyy-MM-dd"; // 默认的日期显示格式
	// 邮件服务器和端口
	private String host = "imap.gmail.com";
	private String port = "993";
	// 设置存储所有网址的集合
	private ArrayList<String> urls = new ArrayList<String>();

	// 存储已读取过的邮件日期
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
			// 设置日期，用于邮件日期的比较
			String sDateString = "2020-08-01";
			filename=sDateString;
			SimpleDateFormat sDateFormat = new SimpleDateFormat(dateFormat);
			startdate = sDateFormat.parse(sDateString, new ParsePosition(0));

			sDateString = "2020-08-12";
			filename=filename+"-"+sDateString;
			sDateFormat = new SimpleDateFormat(dateFormat);
			enddate = sDateFormat.parse(sDateString, new ParsePosition(0));
			System.out.println("开始连接Gmail信箱.....");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public ArrayList<String> getEmailContent() {
		// 设置session的属性
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
		// 产生session实例并连接
		try {
			Session session = Session.getInstance(prop);
			URLName urln = new URLName("imaps", host, 993, null, userName, userPassword);

			Store store = session.getStore(urln);
			store.connect();
			// 设置对话框中的显示信息。
			System.out.println("已经连上信箱.");
			System.out.println("开始读取收件箱中的邮件.....");
			// 读收件箱，设置只读模式
			Folder folder = store.getFolder("INBOX");
			folder.open(Folder.READ_ONLY);
			// 搜索邮件，开始和结束日期之间
			ReceivedDateTerm rdtStart = new ReceivedDateTerm(ComparisonTerm.GT, startdate);
			ReceivedDateTerm rdtEnd = new ReceivedDateTerm(ComparisonTerm.LT, enddate);
			FromStringTerm fstTerm = new FromStringTerm("scholaralerts-noreply@google.com");
			// 同时满足上面三个条件
			SearchTerm[] sts = { rdtStart, rdtEnd, fstTerm };
			SearchTerm st = new AndTerm(sts);
			Message[] messages = folder.search(st);

			// 设置对话框中的显示信息。
			System.out.println("收件箱中共有" + messages.length + "封" + searchKeyword + "邮件。");
			if (messages.length != 0) {
				System.out.println("========开始解析这些邮件=========");
				for (Message message : messages) {
					IMAPMessage msg = (IMAPMessage) message;
					String subject = MimeUtility.decodeText(msg.getSubject());
					if (subject.indexOf(searchKeyword) != -1) {
						// 获取邮件的发送日期并显示
						Date sentDate = msg.getSentDate();
						SimpleDateFormat format = new SimpleDateFormat(dateFormat);
						String strSentDate = format.format(sentDate);
						// 设置对话框中的显示信息。
						System.out.println("分析发送日期为" + strSentDate + "的邮件...");
						readedDay.add(strSentDate);
//						System.out.println(msg.getContentType());
//						System.out.println(msg.isMimeType("multipart/*"));
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
						// 从邮件中获取网址
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
			System.out.println("不能连接上Gmail信箱，请检查用户名，密码和网络。");
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

		// 去掉google搜索的信息，只留文献网址。
		String element = null;
		for (int i = 0; i < al.size(); i++) {
			String tmpStr = al.get(i);
			int begin = tmpStr.indexOf("url=", 10);
			begin += "url=".length();
			// 有可能google搜索中不包含网址，那么就不进行google的剔除
			// begin不等于-1表明包含
			try {
				if (begin != -1) {
					int end = tmpStr.indexOf("&hl=zh-CN", begin);
					// google对网址中的特殊符号进行了编码，因此需要进行解码。
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

		// 去掉重复的网址（2016.8.20后google 邮件内容发生了变化，需要去重复）
		// 利用Set的性质来去重复
		HashSet<String> tmpSet = new HashSet<>(al);
		ArrayList<String> noDupAl = new ArrayList<>(tmpSet);
//		for (String s : noDupAl)
//			System.out.println(s);
		return noDupAl;
	}

	/**使用FileOutputStream来写入txt文件
     * @param txtPath txt文件路径
     * @param content 需要写入的文本
     */
    public static void writeTxt(String txtPath,String content){    
       File file = new File(txtPath);
       try {
           if(file.exists()){
               //判断文件是否存在，如果不存在就新建一个txt
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
