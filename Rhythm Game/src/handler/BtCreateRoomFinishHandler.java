package handler;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JOptionPane;

import client.ClientUI;
import clientPanel.CreateRoomFrame;

public class BtCreateRoomFinishHandler implements ActionListener {
	CreateRoomFrame target;
	public BtCreateRoomFinishHandler(CreateRoomFrame c) {
		target = c;
	}
	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		String title = target.tfTitle.getText().trim();
		String pw = "";
		if(target.rbOneUser.isSelected()) {
			target.ui.net.sendCreateRoomRequest(title, pw, false);
			target.setVisible(false);
		} else {
			if(target.tfPw.isEditable() ) {
				if(String.valueOf(target.tfPw.getPassword()).equals("")) {
					JOptionPane.showMessageDialog(target, "비밀번호를 입력해주세요.");
					return;
				} else {
					pw = String.valueOf(target.tfPw.getPassword()).trim();
				}
			}
			target.ui.net.sendCreateRoomRequest(title, pw, true);
			target.setVisible(false);
		}
	}
	
	
}
