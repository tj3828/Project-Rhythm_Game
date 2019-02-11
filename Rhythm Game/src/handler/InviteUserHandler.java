package handler;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import client.ClientUI;
import clientPanel.InviteUsetListFrame;
import clientPanel.ShowMessageFrame;

public class InviteUserHandler extends MouseAdapter{
	InviteUsetListFrame i;
	public InviteUserHandler(InviteUsetListFrame i) {
		// TODO Auto-generated constructor stub
		this.i = i;
	}
	@Override
	public void mouseClicked(MouseEvent e) {
		if(e.getClickCount() == 2) {
			String str = ((String) i.userList.getSelectedValue());
			System.out.println(str);
			String data = str.substring(1);
		
			String[] result = data.split("]");
			
			System.out.println(result[0]);
			ClientUI.net.sendinviteRequest(result[0]);
			i.dispose();
		}
	}
}
