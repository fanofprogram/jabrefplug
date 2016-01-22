package skyeagle.plugin.gui;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.sf.jabref.JabRefFrame;

public class ProxyDialog extends JDialog implements ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JTextField ipTxtField, portTxtField;
	private JabRefFrame parent;
	private JButton okButton, cancelButton, applyButton;

	private File pluginDir = new File(System.getProperty("user.home") + "/.jabref/plugins");
	private File file = new File(pluginDir, "proxy.prop");

	public ProxyDialog(JabRefFrame frame) {
		super(frame, "代理设置", false);
		parent = frame;
		Container container = getContentPane();

		// 产生上面板，采用网格布局，4行2列，行行间隔5像素
		JPanel inputPanel = new JPanel();
		GridLayout gLayout = new GridLayout(2, 2);
		gLayout.setVgap(5);
		inputPanel.setLayout(gLayout);

		JLabel ipLabel = new JLabel("代理IP地址:");
		inputPanel.add(ipLabel);
		ipTxtField = new JTextField(25);
		ipTxtField.addActionListener(this);
		inputPanel.add(ipTxtField);

		JLabel portLabel = new JLabel("代理端口:");
		inputPanel.add(portLabel);
		portTxtField = new JTextField(8);
		portTxtField.addActionListener(this);
		inputPanel.add(portTxtField);

		inputPanel.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
		container.add("Center", inputPanel);

		// 设置ip和端口的初始值，如果配置文件存在，从配置文件中读取
		// 配置文件不存在，则什么都不做，也就是ip和端口为空白。
		if (file.exists()) {
			try {
				BufferedReader bfr = new BufferedReader(new FileReader(file));
				Properties prop = new Properties();
				prop.load(bfr);
				ipTxtField.setText(prop.getProperty("ip"));
				portTxtField.setText(prop.getProperty("port"));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// 下面板，就是放置3个按钮的面板
		JPanel btnPanel = new JPanel();
		FlowLayout fLayout = new FlowLayout(FlowLayout.CENTER);
		fLayout.setHgap(30);
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

		setVisible(true);

	}

	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		String ip = ipTxtField.getText().trim();
		String port = portTxtField.getText().trim();

		if (source == cancelButton)
			dispose();
		else if (isIPOk() && isPortOk()) {
			if ((source == applyButton)) {
				applyBtnSetting(ip, port);
			} else {
				if (source == okButton) {
					applyBtnSetting(ip, port);
				}
				dispose();
			}
		}
	}

	public boolean isIPOk() {
		if (ipTxtField.getText().isEmpty()) {
			parent.showMessage("ip地址不能为空！");
			return false;
		}
		return true;
	}

	public boolean isPortOk() {
		if (portTxtField.getText().isEmpty()) {
			parent.showMessage("端口不能为空！");
			return false;
		}
		return true;
	}

	private void applyBtnSetting(String ip, String port) {
			try {
				BufferedWriter bfw = new BufferedWriter(new FileWriter(file ));
				Properties prop = new Properties();
				prop.setProperty("ip", ip);
				prop.setProperty("port", port);
				prop.store(bfw, "proxy");
			} catch (IOException e) {
				e.printStackTrace();
			}
	}

}
