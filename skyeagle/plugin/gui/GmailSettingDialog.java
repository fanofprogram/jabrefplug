package skyeagle.plugin.gui;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Container;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import net.sf.jabref.JabRefFrame;

public class GmailSettingDialog extends JDialog implements ActionListener {

	private static final long serialVersionUID = 1L;
	private JTextField username, gmailKeyword, gmailIp;
	private JPasswordField password;
	private DateChooser dateChooserStart, dateChooserEnd;
	private JabRefFrame parent;
	private JButton okButton, cancelButton, applyButton;

	private File pluginDir = new File(System.getProperty("user.home") + "/.jabref/plugins");
	private File gmailfile = new File(pluginDir, "GmailSetting.prop");

	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

	GmailSettingDialog(JabRefFrame prentFrame, String title) {
		super(prentFrame, title, false);
		parent = prentFrame;
		Container container = getContentPane();
		
		//提示面板和label
		JPanel tipPanel=new JPanel();
		JLabel tipLabel = new JLabel("gmail address默认填入imap.gmail.com,但鉴于长期被封,请自行查找新的gmail的imap服务器ip填入.");
		tipPanel.add(tipLabel);
		container.add("North", tipPanel);
		// 产生上面板，采用网格布局，6行2列，行行间隔5像素
		JPanel inputPanel = new JPanel();
		GridLayout gLayout = new GridLayout(6, 2);
		gLayout.setVgap(5);
		inputPanel.setLayout(gLayout);

		JLabel ipLabel = new JLabel("Gmail imap IP address:");
		inputPanel.add(ipLabel);
		gmailIp = new JTextField(15);
		gmailIp.addActionListener(this);
		inputPanel.add(gmailIp);
				
		JLabel userLabel = new JLabel("Gmail username:");
		inputPanel.add(userLabel);
		username = new JTextField(15);
		username.addActionListener(this);
		inputPanel.add(username);

		JLabel pwdLabel = new JLabel("Gmail password:");
		inputPanel.add(pwdLabel);
		password = new JPasswordField(20);
		password.addActionListener(this);
		inputPanel.add(password);

		JLabel searchLabel = new JLabel("Google scholar keyword:");
		inputPanel.add(searchLabel);
		gmailKeyword = new JTextField(20);
		gmailKeyword.addActionListener(this);
		inputPanel.add(gmailKeyword);

		JLabel dateLabel = new JLabel("Start Date:");
		inputPanel.add(dateLabel);

		// 从文件中读取设置信息，设置为当前设置窗口的默认值。
		// 如果文件不存在，所有信息都是空白，日期为当前日期。
		if (!gmailfile.exists()) {
			dateChooserStart = new DateChooser();
			dateChooserEnd = new DateChooser();
		} else {
			try {
				BufferedReader bfr = new BufferedReader(new FileReader(gmailfile));
				Properties prop = new Properties();
				prop.load(bfr);
				gmailIp.setText(prop.getProperty("gmailIp"));
				username.setText(prop.getProperty("username"));
				String newPwd = prop.getProperty("password");
				String pwd = Endecrypt.convertMD5(newPwd);
				password.setText(pwd);
				gmailKeyword.setText(prop.getProperty("searchkeyword"));

				String dateString = prop.getProperty("startday");
				Date date = sdf.parse(dateString, new ParsePosition(0));
				dateChooserStart = new DateChooser(date);

				dateString = prop.getProperty("endday");
				date = sdf.parse(dateString, new ParsePosition(0));
				dateChooserEnd = new DateChooser(date);

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		inputPanel.add(dateChooserStart);

		JLabel dateLabe2 = new JLabel("End Date:");
		inputPanel.add(dateLabe2);
		inputPanel.add(dateChooserEnd);

		// 上面板边框
		inputPanel.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
		container.add("Center", inputPanel);

		// 下面板，就是放置3个按钮的面板
		JPanel btnPanel = new JPanel();
		FlowLayout fLayout = new FlowLayout(FlowLayout.CENTER);
		fLayout.setHgap(35);
		btnPanel.setLayout(fLayout);
		cancelButton = new JButton("取消");
		cancelButton.addActionListener(this);
		okButton = new JButton("确认");
		okButton.addActionListener(this);
		applyButton = new JButton("应用");
		applyButton.addActionListener(this);
		btnPanel.add(applyButton);
		btnPanel.add(okButton);
		btnPanel.add(cancelButton);
		container.add("South", btnPanel);

		// 设置对话框的位置
		Point pt = parent.getLocation();
		Dimension dm = parent.getSize();
		setLocation(pt.x + (int) dm.getWidth() / 3, pt.y + (int) dm.getHeight() / 3);

		pack();
	}

	public void actionPerformed(ActionEvent event) {
		Object source = event.getSource();
		String gmailAdd = gmailIp.getText().trim();
		String userName = username.getText().trim();
		String pwd = new String(password.getPassword()).trim();
		String searchKeyword = gmailKeyword.getText().trim();
		Date Start = dateChooserStart.getDate();
		Date End = dateChooserEnd.getDate();
		String StartDay = sdf.format(Start);
		String EndDay = sdf.format(End);

		// 取消按钮，直接关闭对话框
		// 其他两个按钮先判断几个参数是否正确
		// 正确的话，如果是apply按钮，直接存入文件中
		// ok按钮的话，存入文件，同时关闭对话框。
		if (source == cancelButton)
			dispose();
		else if (isGmailIpOk() && isUsernameOk() && isPasswordOk() && isSearchKeywordOk()) {
			if ((source == applyButton)) {
				applyBtnSetting(gmailAdd, userName, pwd, searchKeyword, StartDay, EndDay);
			} else {
				if (source == okButton) {
					applyBtnSetting(gmailAdd, userName, pwd, searchKeyword, StartDay, EndDay);
				}
				dispose();
			}
		}
	}

	public boolean isGmailIpOk() {
		// TODO Auto-generated method stub
		if (gmailIp.getText().isEmpty()) {
			parent.showMessage("请填入Gmail的ip地址（默认填入imap.gmail.com):");
			return false;
		}
		return true;
	}

	public boolean isSearchKeywordOk() {
		// TODO Auto-generated method stub
		if (gmailKeyword.getText().isEmpty()) {
			parent.showMessage("Google scholar 关键词不能为空!");
			return false;
		}
		return true;
	}

	public boolean isPasswordOk() {
		// TODO Auto-generated method stub
		if (new String(password.getPassword()).isEmpty()) {
			parent.showMessage("密码不能为空!密码已经加密，请放心输入。");
			return false;
		}
		return true;
	}

	public boolean isUsernameOk() {
		// TODO Auto-generated method stub
		if (username.getText().isEmpty()) {
			parent.showMessage("用户名不能为空!");
			return false;
		} else if (username.getText().indexOf('@') != -1) {
			parent.showMessage("用户名不需要输入 @gmail.com!");
			return false;
		}
		return true;
	}

	private void applyBtnSetting(String gmailadd, String userName, String pwd, String searchKeyword, String startDay,
			String endDay) {
		// 这里为了避免从文件中直接看到密码明文，进行了md5加密
		String newPwd = Endecrypt.convertMD5(pwd);
		Properties prop = new Properties();
		prop.setProperty("gmailIp", gmailadd);
		prop.setProperty("username", userName);
		prop.setProperty("password", newPwd);
		prop.setProperty("searchkeyword", searchKeyword);
		prop.setProperty("startday", startDay);
		prop.setProperty("endday", endDay);

		try {
			BufferedWriter bfw = new BufferedWriter(new FileWriter(gmailfile));
			prop.store(bfw, "Gmail Setting");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
