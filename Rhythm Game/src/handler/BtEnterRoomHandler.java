package handler;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.border.LineBorder;

import client.ClientUI;
import clientPanel.SelectPanel;

public class BtEnterRoomHandler implements ActionListener{
	ClientUI ui;
	public BtEnterRoomHandler(ClientUI c) {
		// TODO Auto-generated constructor stub
		ui = c;
	}
	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		
		int idx = ui.pnRoom.btList.indexOf(e.getSource());
		ui.net.sendEnterRoomRequest(idx);
	}
	
}
