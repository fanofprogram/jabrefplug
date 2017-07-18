package skyeagle.plugin.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;

public class APSImageDialog extends JDialog implements ActionListener{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JButton btnOK, btnCancel;
	private ArrayList<JRadioButton> radios =new ArrayList<>();
	private ArrayList<String> imgUrls;
	public String nameEinsteinImg;
	public APSImageDialog(Dialog dialog,ArrayList<String> imgUrls) {
		super(dialog,"请选择爱因斯坦头像",true);
		this.imgUrls=imgUrls;
		
		//获取对话框的容器
		Container container = getContentPane();
		
		// 产生上面板（就是放图像面板用的），采用网格布局，2行4列，间隔5像素
		JPanel imgPanel = new JPanel();
		GridLayout gLayout = new GridLayout(2, 4);
		gLayout.setHgap(5);
		imgPanel.setLayout(gLayout);
		imgPanel.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));

		//根据url产生图像面板
		ButtonGroup btnGroup=new ButtonGroup();
		try {
			for(int i=0;i<imgUrls.size();i++){
				
				URL imageURL = new URL(imgUrls.get(i));
	            InputStream is = imageURL.openConnection().getInputStream();
	            BufferedImage image = ImageIO.read(is);
	            BufferedImage newImage=resize(image,100,100);
	            
	            JPanel panel = new JPanel();
	            panel.setLayout(new BorderLayout());
	            JLabel lbl = new JLabel();
	            lbl.setIcon(new ImageIcon(newImage));
	            panel.add(lbl,BorderLayout.NORTH);
	            JRadioButton radio=new JRadioButton();
	            radios.add(radio);
	            btnGroup.add(radio);
	            //下面这句是将radio按钮居中，否则的话靠左边
	            radio.setHorizontalAlignment(SwingConstants.CENTER);
	            panel.add(radio,BorderLayout.CENTER);
	            
	            imgPanel.add(panel);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//产生按钮面板
		JPanel btnPanel = new JPanel();
		
		btnOK=new JButton("提交");
		btnCancel=new JButton("取消");
		btnOK.addActionListener(this);
		btnCancel.addActionListener(this);
		btnPanel.add(btnOK);
		btnPanel.add(btnCancel);
		
		
		container.add(imgPanel,BorderLayout.NORTH);
		container.add(btnPanel,BorderLayout.CENTER);
		// 设置对话框的位置
		Point pt = dialog.getLocation();
		Dimension dm = dialog.getSize();
		setLocation(pt.x + (int) dm.getWidth() / 3, pt.y + (int) dm.getHeight() / 3);
		pack();
		setAutoRequestFocus(true);
		setAlwaysOnTop(true);
		setVisible(true);
	}
	
	private BufferedImage resize(BufferedImage image, int newWidth, int newHeight) {
		 BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
		 if(image != null){
		     Graphics2D graph = resizedImage.createGraphics();
		     graph.drawImage(image, 0, 0, newWidth, newHeight, null);
		     graph.dispose(); 
		 }else{
			 System.err.println("Getting images meet trouble when re-size it!");
		 }
	     return resizedImage;
	}
	
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();

		if (source == btnCancel)
			dispose();
		else if (isSelected()) {
				if (source == btnOK) {
					dispose();
				}
			}
		}
	
	private boolean isSelected() {
		for(int i=0;i<imgUrls.size();i++){
			JRadioButton radio=radios.get(i);
			if(radio.isSelected()){
				String temp=imgUrls.get(i);
				int begin=temp.lastIndexOf('/');
				nameEinsteinImg=temp.substring(begin+1);
				return true;
			}
		}
		return false;
	}

	public static void main(String[] args){
		
		JDialog dialog=new JDialog();
		dialog.setVisible(true);
		dialog.setSize(500, 300);
		dialog.setLocation(300, 300);
		
		ArrayList<String> urls=new ArrayList<>();
		urls.add("http://www.henu.edu.cn/attach/2016/03/07/29291.jpg");
		urls.add("http://www.henu.edu.cn/attach/2015/12/17/28271.jpg");
		urls.add("http://www.henu.edu.cn/attach/2015/03/04/14170.jpg");
		urls.add("http://www.henu.edu.cn/attach/2015/03/04/14169.jpg");
		urls.add("http://www.henu.edu.cn/attach/2015/01/14/12159.jpg");
		urls.add("http://www.henu.edu.cn/attach/2015/01/14/12158.jpg");
		urls.add("http://www.henu.edu.cn/attach/2016/03/07/29291.jpg");
		urls.add("http://www.henu.edu.cn/attach/2016/03/07/29291.jpg");
		new APSImageDialog(dialog, urls);
	}
	
}
