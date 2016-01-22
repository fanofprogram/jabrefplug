package skyeagle.plugin.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;

import net.sf.jabref.JabRefFrame;

public class UpdateDialog extends JDialog implements ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4781649571554164666L;

	// 在另两个线程中需要用到，设置为public
	public JButton btnCancel;
	public JButton btnOk;

	public Boolean flagOK = false;

	private JTextArea taskOutput;
	private JScrollPane scroll;

	// 控制显示线程是否显示，如果没有这个的话
	// 让其一直刷新显示，屏幕会闪。
	// 通过这个控制只有文本内容变化后，才刷新显示内容。
	public boolean display = false;

	public boolean stop = false;

	// 存储要显示在对话框中的内容
	public ArrayList<String> showTexts = new ArrayList<String>();

	public static final String NEWLINE = System.getProperty("line.separator");

	public UpdateDialog(JabRefFrame frame, String frmTitle) {

		super(frame, frmTitle, true);

		init();

		this.addWindowListener(new WindowAdapter() {
			// 用右上角的按钮关闭时也要关掉线程
			public void windowDeactivated(WindowEvent e) {
				close();
			}
		});

	}

	private void init() {
		// 设置对话框大小
		Toolkit kit = Toolkit.getDefaultToolkit(); // 定义工具包
		Dimension screenSize = kit.getScreenSize(); // 获取屏幕的尺寸
		int width = screenSize.width / 2 - 50; // 宽
		int height = screenSize.height / 2 - 100; // 高
		setSize(width, height);
		// setResizable(false);
		// 设置对话框的起始位置
		Point pt = new Point();
		pt.x = screenSize.width / 4;
		pt.y = screenSize.height / 4;
		setLocation(pt);

		taskOutput = new JTextArea(15, 60);
		taskOutput.setMargin(new Insets(5, 5, 5, 5));
		taskOutput.setEditable(false);
		taskOutput.setBackground(Color.white);

		JPanel p = new JPanel(new BorderLayout());
		p.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
		scroll = new JScrollPane(taskOutput);
		scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		p.add(scroll, BorderLayout.CENTER);

		// 按钮
		btnCancel = new JButton("取消");
		btnCancel.addActionListener(this);

		add(p, BorderLayout.CENTER);
		add(btnCancel, BorderLayout.PAGE_END);
	}

	public void actionPerformed(ActionEvent e) {
		// 取消按钮，关掉线程
		if (e.getSource() == btnCancel) {
			close();
		} else if (e.getSource() == btnOk) {
			close();
			flagOK = true;
		}
	}

	public void close() {
		stop = true;
		dispose();
	}

	// 显示内容
	public void output(String str) {
		String content = taskOutput.getText();
		taskOutput.insert(str + NEWLINE, content.length());

		// 设置滚动条到底部
		int height = 10;
		Point p = new Point();
		p.setLocation(0, taskOutput.getLineCount() * height);
		scroll.getViewport().setViewPosition(p);
	}

	public void downloadRatioOutput(File file, int ratio, int totalDown, int getFileSize) {
		String content = taskOutput.getText();
		String tips = "文件" + file.getAbsolutePath();
		int tmpNum = 0;
		int newNum = 0;
		String lastStr = null;
		if (getFileSize == -1) {
			lastStr = "KB";
			tmpNum = lastStr.length();
			newNum = totalDown/1024;
			tips = tips + "下载的大小为:";
		} else {
			lastStr = "%";
			tmpNum = lastStr.length();
			newNum = ratio;
			tips = tips + "下载的百分比为:";
		}
		int beginIndex = content.indexOf(tips);
		if (beginIndex == -1) {
			taskOutput.insert(tips + newNum + lastStr, content.length());
		} else {
			beginIndex = beginIndex + tips.length();
			String oldNum = content.substring(beginIndex, content.length() - tmpNum);
			
			if (Integer.valueOf(oldNum) == newNum)
				return;
			try {
				taskOutput.getDocument().remove(content.length() - oldNum.length() - tmpNum, oldNum.length() + tmpNum);
				String tmp = newNum + lastStr;
				if (getFileSize != -1)
					if (ratio == 100)
						tmp = tmp + NEWLINE;
				String newcontent = taskOutput.getText() + tmp;
				taskOutput.setText(newcontent);
			} catch (BadLocationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void modifiedDialog() {
		// TODO Auto-generated method stub
		btnOk = new JButton("确认");
		btnOk.addActionListener(this);

		btnOk.setMaximumSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
		btnCancel.setMaximumSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));

		JPanel downPan = new JPanel();
		downPan.setLayout(new BoxLayout(downPan, BoxLayout.X_AXIS));
		downPan.add(btnOk);
		downPan.add(btnCancel);
		add(downPan, BorderLayout.PAGE_END);
	}

}
