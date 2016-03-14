package skyeagle.plugin.gui;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.sf.jabref.JabRefFrame;
import skyeagle.plugin.command.SingleUrlFetchCommand;

public class UrlDialog extends JDialog implements ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private JTextField urlField;
	private JabRefFrame frame;
	private JButton okButton, cancelButton;

	public UrlDialog(JabRefFrame f) {
		super(f, "通过文献网址获取文献条目", false);
		frame = f;

		Container container = getContentPane();

		JPanel inputPanel = new JPanel();
		urlField = new JTextField(100);
		urlField.addActionListener(this);
		inputPanel.add(urlField);

		JPanel btnPanel = new JPanel();
		cancelButton = new JButton("取消");
		cancelButton.addActionListener(this);
		okButton = new JButton("确认");
		okButton.addActionListener(this);
		btnPanel.add(okButton);
		btnPanel.add(cancelButton);

		container.add("Center", inputPanel);
		container.add("South", btnPanel);

		// 设置对话框的位置
		Point pt = frame.getLocation();
		Dimension dm = frame.getSize();
		setLocation(pt.x + (int) dm.getWidth() / 3, pt.y + (int) dm.getHeight() / 3);

		pack();
		setVisible(true);
	}

	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		String url = urlField.getText().trim();

		if (source == cancelButton)
			dispose();
		else if (!url.isEmpty() & url != null) {
			if (source == okButton) {
				dispose();
				new SingleUrlFetchCommand(frame,url);
			}
			dispose();
		}
	}

}
