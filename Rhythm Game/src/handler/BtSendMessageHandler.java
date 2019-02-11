package handler;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JOptionPane;

import client.ClientUI;
import clientPanel.MessageSendFrame;

public class BtSendMessageHandler implements ActionListener{
	MessageSendFrame m;
	public BtSendMessageHandler(MessageSendFrame m) {
		// TODO Auto-generated constructor stub
		this.m = m;
	}
	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		String name = m.tfId.getText().trim();
		String ment = m.tfMessage.getText();
		if(ment.equals("")) {
			JOptionPane.showMessageDialog(m, "보내실 메시지를 입력하세요.");
			return;
		}
		ClientUI.net.sendMessageRequest(name, ment);
		m.dispose();
	}
	
}
