package skyeagle.plugin.getpdf;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import skyeagle.plugin.gui.UpdateDialog;

public class GetPDFUtil {

	public GetPDFUtil() {

	}

	/*
	 * 从代理文件中读取代理IP和端口，生成proxy，方便使用。
	 */
	public static Proxy getProxy() {
		try {
			File pluginDir = new File(System.getProperty("user.home") + "/.jabref/plugins");
			File proxyfile = new File(pluginDir, "proxy.prop");
			BufferedReader bfr;
			bfr = new BufferedReader(new FileReader(proxyfile));
			Properties prop = new Properties();
			prop.load(bfr);
			String ip = prop.getProperty("ip");
			String port = prop.getProperty("port");
			InetSocketAddress addr = new InetSocketAddress(ip, Integer.valueOf(port));
			// 创建代理服务器
			Proxy proxy = new Proxy(Proxy.Type.HTTP, addr);
			return proxy;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/*
	 * 由给定的网址和代理，获取页面内容
	 */
	public static String getPageContent(String url, Proxy proxy) {
		HttpURLConnection con = null;
		try {
			URL u = new URL(url);
			if (proxy != null)
				con = (HttpURLConnection) u.openConnection(proxy);
			else
				con = (HttpURLConnection) u.openConnection();
		} catch (Exception e) {
			e.printStackTrace();
		}
		String pagecontent = getPage(con);
		con.disconnect();
		return pagecontent;
	}

	public static String getPageContent(String url) {

		return getPageContent(url, null);
	}

	/*
	 * 获取页面内容的核心代码
	 */
	public static String getPage(HttpURLConnection con) {
		con.setDoOutput(true);
		con.setDoInput(true);
		con.setUseCaches(false);
		con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; rv:37.0) Gecko/20100101 Firefox/37.0");
		con.setConnectTimeout(50000);
		// 下面获取网页
		StringBuilder buffer = new StringBuilder();
		try {
			int res = con.getResponseCode();
			System.out.println(con.getHeaderFields().toString());
			BufferedReader br = null;
			//有时候页面会返回未授权或禁止，导致无法读取页面内容
			//下面就是避免这种情况
			if (res == HttpURLConnection.HTTP_UNAUTHORIZED | res == HttpURLConnection.HTTP_FORBIDDEN)
				br = new BufferedReader(new InputStreamReader(con.getErrorStream(), "UTF-8"));
			else
				br = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
			String temp;
			while ((temp = br.readLine()) != null) {
				buffer.append(temp);
				buffer.append("\n");
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return buffer.toString();
	}

	/*
	 * 由pdf的连接网址，cookies来生成下载pdf文件的连接。
	 */
	public static HttpURLConnection createPDFLink(String pdflink, Map<String, String> cookies, Boolean usingProxy) {
		
		//对IEEE网站，设置的cookie一直不起作用，添加下面两行就行了了，不知道具体原因
		//其它网站没有这两行，也可以，具体不知道为什么。
		CookieManager cookieManager = new CookieManager();
		CookieHandler.setDefault(cookieManager);
		
		HttpURLConnection con = null;
		try {
			URL u = new URL(pdflink);

			if (usingProxy)
				con = (HttpURLConnection) u.openConnection(getProxy());
			else
				con = (HttpURLConnection) u.openConnection();

			con.setDoOutput(true);
			con.setDoInput(true);
			con.setUseCaches(false);
			con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; rv:37.0) Gecko/20100101 Firefox/37.0");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// 设置cookies
		Set<String> set = cookies.keySet();
		for (Iterator<String> it = set.iterator(); it.hasNext();) {
			String tmp = it.next();
			String value = cookies.get(tmp);
			con.setRequestProperty("Cookie", tmp + "=" + value);
		}
		
		return con;
	}
	
	/*
	 * 这个函数就是下载pdf文件的。
	 */
	public static void getPDFFile(File file, int filesize, UpdateDialog dig, HttpURLConnection con) {

		int ratio = 0;
		int totalBytesRead = 0;
		InputStream in = null;
		OutputStream out = null;
		try {
			out = new BufferedOutputStream(new FileOutputStream(file));
			in = new BufferedInputStream(con.getInputStream());
			int bytesRead = 0;
			byte[] buffer = new byte[512]; // 缓冲区大小
			while ((bytesRead = in.read(buffer)) != -1) { // 读取数据
				out.write(buffer, 0, bytesRead); // 写入数据到文件
				totalBytesRead += bytesRead;
				//计算下载的百分比
				ratio = (int) ((float) totalBytesRead / (float) filesize * 100);
				//下载的时候在对话框中显示下载的百分比
				dig.downloadRatioOutput(file, ratio, totalBytesRead, filesize);
//			System.out.println(ratio);
			}
			//如果filesize等于-1代表无法从返回的http头中获取文件尺寸。
			//这个时候对话框中显示下载的字节数
			//由于无法判断什么时候下载完，所以只能在这里确定下载完成。
			//最后加一个换行,因为output中加了换行，所以这里输入一个空白字符就行
			if (filesize == -1)
				dig.output("");
		} catch (Exception e) {
			e.printStackTrace();
			dig.output("网络连接出现问题，文件没有下载完整，请重新下载。");
			file.delete();
		} finally {
			try {
				out.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/*
	 * 获取网站的cookies
	 */
	public static Map<String, String> getCookies(String pdflink, Proxy proxy) {
		// 获取cookies
		Map<String, String> cookies = new TreeMap<>();
		HttpURLConnection con = null;
		try {
			URL u = new URL(pdflink);
			if (proxy != null)
				con = (HttpURLConnection) u.openConnection(proxy);
			else
				con = (HttpURLConnection) u.openConnection();
			con.setRequestMethod("GET");
			con.setDoOutput(true);
			con.setDoInput(true);
			con.setUseCaches(false);
			con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; rv:37.0) Gecko/20100101 Firefox/37.0");
		} catch (Exception e) {
			e.printStackTrace();
		}
		String key = null;
		for (int i = 1; (key = con.getHeaderFieldKey(i)) != null; i++) {
			if (key.equalsIgnoreCase("set-cookie")) {
				String cookie = null;
				cookie = con.getHeaderField(i);
				int i1 = cookie.indexOf("=");
				int i2 = cookie.indexOf(";");
				if (i1 != -1 && i2 != -1) {
					String _value = cookie.substring(i1 + 1, i2);
					String _key = cookie.substring(0, i1);
					cookies.put(_key, _value);
				}
			}
		}
		con.disconnect();
		return cookies;
	}

	public static Map<String, String> getCookies(String url) {
		return getCookies(url, null);
	}

	/*
	 * 获取代理，html和cookies
	 * 一般都需要这一步。
	 */
	public static String initGetPDF(String url, Boolean usingProxy, Map<String, String> cookies) {
		Proxy proxy = null;
		if (usingProxy) {
			proxy = GetPDFUtil.getProxy();
			if (proxy == null) {
				return null;
			}
		}
		// 获取网址的内容（html)和cookies
		String pagecontent = null;
		if (usingProxy) {
			pagecontent = getPageContent(url, proxy);
			cookies = getCookies(url, proxy);
		} else {
			pagecontent = getPageContent(url);
			cookies = getCookies(url);
		}
		return pagecontent;
	}


}
