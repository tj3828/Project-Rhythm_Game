package handler;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import Common.Message;
import client.ClientUI;
import clientPanel.MessageListFrame;
import clientPanel.ShowMessageFrame;

public class ShowMessageHandler extends MouseAdapter {
	MessageListFrame target;
	public ShowMessageHandler(MessageListFrame m) {
		target = m;
	}
	@Override
	public void mouseClicked(MouseEvent e) {
		// TODO Auto-generated method stub
		if(e.getClickCount() == 2) {
			String str = ((String) target.messageList.getSelectedValue()).trim();
			System.out.println(str);
			
			int idx = target.messageList.getSelectedIndex();
			String[] data = str.split("]");
			String result = data[0].substring(data[0].length()-5, data[0].length()-2).trim();
			System.out.println(result);
			ShowMessageFrame s = new ShowMessageFrame();
			
			
			System.out.println("11");
			ClientUI.net.sendCheckReadMessageRequest(idx);
			ClientUI.net.sendShowMessageListRequest();
			Message m = ClientUI.net.sendShowMessageRequest(idx);
			s.setVisible(true);
			System.out.println("호출");
			if(result.equals("수신")) {
				s.lbText.setText("발신자 : ");
			} else {
				s.lbText.setText("수신자 : ");
			}
			
			System.out.println("22");
			s.tfId.setText(m.getName());
			s.tfMessage.setText(m.getMent());
			
		}
	}
}
