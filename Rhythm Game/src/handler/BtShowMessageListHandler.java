package handler;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import client.ClientUI;
import clientPanel.MessageListFrame;

public class BtShowMessageListHandler implements ActionListener {
	ClientUI target;
	public BtShowMessageListHandler(ClientUI c) {
		target = c;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		target.m.setLocationRelativeTo(null);
		target.m.setVisible(true);
		ClientUI.net.sendShowMessageListRequest();
		
	}
	
	
}
