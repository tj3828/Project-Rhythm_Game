package client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.URL;
import java.util.List;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.ListCellRenderer;

import Common.Account;
import Common.Message;
import Common.Room;
import clientPanel.InviteUsetListFrame;
import clientPanel.MessageListFrame;
import clientPanel.MusicChoicePanel;
import game.GameFrame;

public class ClientNetwork extends Thread {
	private Socket soc; // 핵심 연결 소켓
	private ObjectOutputStream oos = null;
	private ObjectInputStream ois = null;

	private DatagramSocket ds; // 서브 소켓(receive용)
	private ClientUI ui;
	private Account user;
	private String nick;	// user을 저장하기 위한 nick

	public ClientNetwork(ClientUI c) {
		this.ui = c;
		System.out.println("연결중");
		try {
			soc = new Socket(c.ip, 56789);
			System.out.println("??");
			oos = new ObjectOutputStream(soc.getOutputStream());
			ois = new ObjectInputStream(soc.getInputStream());
			System.out.println("[client] connected to server");
			ds = new DatagramSocket(soc.getLocalPort()); // // TCP와 UDP는 같은 포트로 사용할 수 있음.

		} catch (IOException e) {
			System.out.println("[client] network error " + e.toString());
		}
		start();
	}

	@Override
	public void run() {
		while (!ds.isClosed()) {
			DatagramPacket dp = new DatagramPacket(new byte[2048], 2048);
			try {
				ds.receive(dp);
				System.out.println("client UDP received");
				String data = new String(dp.getData(), 0, dp.getLength());
				System.out.println(data);
				String[] str = data.split("#");
				switch (str[0]) {
				case "userListChange":	// 로그인 유저 목록
					sendUserListRequest();
					break;
				case "changerooms":		// 방 목록 최신화
					sendStateRoomRequest();
					break;
				case "changechat":		// 받은 채팅 띄우기
					System.out.println(str[1]);
					String txt = str[1];
					ui.pnMusicChoice.txArea.append(txt + "\n");
					break;
				case "readysuccess":	// 상대방 준비 완료시
					System.out.println("받음");
					ui.pnMusicChoice.p.leftButton.setEnabled(false);
					ui.pnMusicChoice.p.rightButton.setEnabled(false);
					ui.pnMusicChoice.btReady.setEnabled(false);
					ui.pnMusicChoice.btStart.setEnabled(true);
					ui.pnMusicChoice.btRoomSetting.setEnabled(false);
					break;
				case "readycancel":		// 상대방 준비 취소시
					ui.pnMusicChoice.p.leftButton.setEnabled(true);
					ui.pnMusicChoice.p.rightButton.setEnabled(true);
					ui.pnMusicChoice.btReady.setEnabled(false);
					ui.pnMusicChoice.btStart.setEnabled(false);
					ui.pnMusicChoice.btRoomSetting.setEnabled(false);
					break;
				case "roominfo":	// 상대방이 들어왔을 때
					ui.pnMusicChoice.tfusernick2.setText(str[1]);
					ui.pnMusicChoice.tfuserinfo2.setText(str[2]);
					ui.pnMusicChoice.btInvite.setEnabled(false);
					ui.pnMusicChoice.btExpel.setEnabled(true);
					ui.pnMusicChoice.btRoomSetting.setEnabled(false);
					break;
				case "reroominfo":	// 상대방이 나갔을 때
					ui.pnMusicChoice.tfusernick2.setText("사용자 없음");
					ui.pnMusicChoice.tfuserinfo2.setText("");
					ui.pnMusicChoice.lbCrownRight.setIcon(null);
					ui.pnMusicChoice.lbCrownLeft.setIcon(new ImageIcon(getClass().getClassLoader().getResource("clientPanel/imge/crown1.png")));
					ui.pnMusicChoice.btReady.setEnabled(false);
					ui.pnMusicChoice.btStart.setEnabled(false);
					ui.pnMusicChoice.p.leftButton.setEnabled(true);
					ui.pnMusicChoice.p.rightButton.setEnabled(true);
					ui.pnMusicChoice.btInvite.setEnabled(true);
					ui.pnMusicChoice.btExpel.setEnabled(false);
					ui.pnMusicChoice.btRoomSetting.setEnabled(true);;
					break;
				case "startgame":	// 게임 시작시 게임 프레임 호출
					ui.pnMusicChoice.p.isRoomScreen = false;
					GameFrame g = new GameFrame(ui);
					ui.pnMusicChoice.p.selectedMusic.close();
					if(str[2].equals("1")) {
						g.twouser = false;
					} else {
						g.twouser = true;
					}
					g.setLocationRelativeTo(null);
					g.setTitle("Drop the beat!! - Game");
					g.setVisible(true);
					ui.setVisible(false);
					break;
				case "right":	// 곡 선택 오른쪽 이동
					System.out.println("곡 바뀜 받음");
					ui.pnMusicChoice.p.selectRight();
					break;
				case "left":	// 곡 선택 왼쪽 이동
					ui.pnMusicChoice.p.selectLeft();
					break;
				case "score":	// 상대방 스코어 받아서 셋팅
					String score = str[1];
					GameFrame.game.vsScore = Integer.valueOf(score);
					break;
				case "gameresultSetted":	// 상대방도 게임이 끝났을 때 방에 정보 갱신
					sendReRoomUserInfoRequest();
					break;
				case "sendmessage":			// 쪽지 받았을 때 갯수 갱신
					sendMessageStateRequest();
					break;
				case "resetmessage":		// 쪽지 리스트 갱신
					sendShowMessageListRequest();
					break;
				case "invite":				// 초대 받았을 때
					int idx = JOptionPane.showConfirmDialog(ui, str[1] + "님이 초대하셨습니다. \n 수락하시겠습니까?" );
					
					if(idx == 0) {
						sendStateRoomRequest();
						if(!(ui.pnRoom.btList.get(Integer.valueOf(str[2])).isEnabled())) {
							JOptionPane.showMessageDialog(ui, "방이 꽉 찾습니다.");
							return;
						}
						sendEnterRoomRequest(Integer.valueOf(str[2]));
					}
					break;
				case "expel":				// 강퇴 당할 때
					sendLeaveRequest();
					JOptionPane.showMessageDialog(ui, "강퇴당하였습니다.");
					break;
				}
			} catch (IOException e) {
				System.out.println("dp failed .. " + e.toString());
				ds.close();
				break;
			}

		}
	}
	
	// 회원가입
	public void sendCreateRequest(String nick, String pass, String name, String repass) {
		String resp = null;
		System.out.println(" [client] request : ");
		if (nick.trim().equals("") || pass.trim().equals("")) {
			JOptionPane.showMessageDialog(ui, "아이디와 비밀번호를 입력하세요.");
			return;
		}
		if (!pass.equals(repass)) {
			JOptionPane.showMessageDialog(ui, "비밀번호를 확인하세요");
			return;
		}
		if (!ui.pnSignup.rbAgree.isSelected()) {
			JOptionPane.showMessageDialog(ui, "약관을 읽고 동의해주세요.");
			return;
		}
		synchronized (oos) {
			try {
				oos.writeObject("create#" + nick + "#" + pass + "#" + name);

				resp = (String) ois.readObject();
				System.out.println("[client] response : " + resp);
				String[] data = resp.split("#");

				// 여기서 ui 제어.
				if (data[0].equals("true")) {
					ui.pnSignup.tfId.setText(nick);
					ui.pnSignup.tfpw.setText("");
					ui.pnSignup.tfname.setText("");
					ui.pnSignup.tfrpw.setText("");
					JOptionPane.showMessageDialog(ui, "아이디가 생성되었습니다.");
					// 로그인 페이지로 이동.
					ui.setSize(496, 748);
					ui.setTitle("Drop the beat!! - Login");
					ui.setContentPane(ui.pnLogin);
					ui.pnLogin.tfid.setText(nick);

				} else {
					JOptionPane.showMessageDialog(ui, data[1]);
				}

			} catch (ClassNotFoundException | IOException e) {
				System.out.println("[client] network error " + e.toString());
			}
		}
	}
	
	// 로그인 요청
	public void sendLoginRequest(String nick, String pass) {
		this.nick = nick;
		String resp = null;
		System.out.println("[client] request : ");
		if (nick.trim().equals("") || pass.trim().equals("")) {
			JOptionPane.showMessageDialog(ui, "아이디와 비밀번호를 입력하세요.");
			return;
		}
		synchronized (oos) {
			try {
				oos.writeObject("join#" + nick + "#" + pass);

				resp = (String) ois.readObject();
				System.out.println("[client] response : " + resp);

				// 여기서 ui 제어.
				String[] data = resp.split("#");
				if (data[0].equals("true")) {
					System.out.println("come");
					ui.pnLogin.tfid.setText("");
					ui.pnLogin.tfpw.setText("");
					ui.setSize(800, 800);
					ui.setTitle("Drop the beat!! - Wating Room");
					ui.setLocationRelativeTo(null);
					ui.setContentPane(ui.pnRoom);
					ui.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
					sendStateRoomRequest();
					sendMessageStateRequest();
				} else {
					ui.pnLogin.tfid.setText("");
					ui.pnLogin.tfpw.setText("");
					JOptionPane.showMessageDialog(ui, data[1]);
				}

			} catch (ClassNotFoundException | IOException e) {
				System.out.println("[client] network error " + e.toString());
			}
		}
	}
	
	// 방 나갈 때 or 추방당했을 때
	public void sendLeaveRequest() {
		String resp = null;
		synchronized (oos) {
			try {
				System.out.println("방 나갔음.");
				oos.writeObject("leave");
				resp = (String) ois.readObject();
				if (resp == null) {
					ui.setSize(800, 800);
					ui.setTitle("Drop the beat!! - Wating Room");
					ui.setLocationRelativeTo(null);
					ui.setContentPane(ui.pnRoom);
					sendUserListRequest();
					ui.pnMusicChoice.p.selectedMusic.close();
				}
			} catch (ClassNotFoundException | IOException e) {
			}
		}
	}
	
	// 로그인 유저 목록과 내 정보 갱신 or 변경
	public void sendUserListRequest() {
		Set<Account> resp = null;
		synchronized (oos) {
			try {
				oos.writeObject("get");
				resp = (Set<Account>) ois.readObject();
				System.out.println(resp);
				String[] ar = new String[resp.size()];
				int i = 0;
				for (Account a : resp) {
					if(a.getNick().equals(nick)) {
						user = a;
						ui.pnRoom.pnInfo.lbId.setText(a.getNick());
						ui.pnRoom.pnInfo.lbState.setText(a.tell());
						
					}
					ar[i++] = a.toString();
				}
				ui.pnRoom.userList.setListData(ar);
			} catch (Exception e) {
				System.out.println(e.toString());
			}

		}
	}
	
	// 방 목록 불러오기 or 갱신
	public void sendStateRoomRequest() {
		List<Room> resp = null;
		synchronized (oos) {
			try {
				oos.writeObject("roomlist");
				resp = (List<Room>) ois.readObject();
				int i = 0;
				System.out.println(resp);
				if (!resp.isEmpty()) {
					do {
						System.out.println("룸 !null");
						ui.pnRoom.btList.get(i).setText("");
						ui.pnRoom.btList.get(i).setEnabled(true);
						ui.pnRoom.btList.get(i).setText("<html>제목 : " +resp.get(i).getTitle() + "<br/>" + "방장 : " + resp.get(i).getJoiner().get(0).getNick() + "<br/>" + "인원 : " + resp.get(i).getJoiner().size() + " / 2" + "<br/>" + "암호방 : " + (resp.get(i).getPass().equals("") ? "NO" : "YES") + "<br/>" + "방 상태 : " + (resp.get(i).isGameStart() ? "게임 중.." : "대기 중..")+  "</html>");               
						if(resp.get(i).getJoiner().size() == 2) {
							ui.pnRoom.btList.get(i).setEnabled(false);
						}
						if(!resp.get(i).isTwoUserRoom()) {
							ui.pnRoom.btList.get(i).setText("<html>제목 : " +resp.get(i).getTitle() + "<br/>" + "방장 : " + resp.get(i).getJoiner().get(0).getNick() + "<br/>" + "인원 : " + resp.get(i).getJoiner().size() + " / 1" + "<br/>" + "암호방 : " + (resp.get(i).getPass().equals("") ? "NO" : "YES") + "<br/>" + "방 상태 : " + (resp.get(i).isGameStart() ? "게임 중.." : "대기 중..")+  "</html>");               
							ui.pnRoom.btList.get(i).setEnabled(false);
						}
						i++;
					} while (i < resp.size());
				}
				while (i < 8) {
					System.out.println("룸 null");
					ui.pnRoom.btList.get(i).setEnabled(false);
					ui.pnRoom.btList.get(i).setText("");
					i++;
				}

			} catch (ClassNotFoundException | IOException e) {
			}
		}
	}
	
	// 로그아웃시에 처리될 동작
	public void sendLogoutRequest() {
		boolean resp = false;
		synchronized (oos) {
			try {
				oos.writeObject("logout");
				resp = (Boolean) ois.readObject();
				if (resp == true) {
					JOptionPane.showMessageDialog(ui, "로그아웃되었습니다.");
					ui.setSize(496, 748);
					ui.setTitle("Drop the beat!! - Login");
					ui.setLocationRelativeTo(null);
					ui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
					ui.setContentPane(ui.pnLogin);
				} else {

				}
			} catch (ClassNotFoundException | IOException e) {
			}
		}
	}


	
	// 방 입장
	public void sendEnterRoomRequest(int idx) {
		synchronized (oos) {
			try {
				System.out.println("enter" + idx);
				oos.writeObject("enterroom#" + idx);
				Room resp = (Room) ois.readObject();
				if (!(resp == null)) {		// 비번방이 아니고 성공시
					ui.setSize(1400, 730);
					ui.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
					ui.pnMusicChoice = new MusicChoicePanel(ui);
					ui.setTitle("Drop the beat!! - Room");
					ui.setContentPane(ui.pnMusicChoice);
					sendChatRequest(" 입장했습니다.");
					ui.pnMusicChoice.lbCrownRight.setIcon(new ImageIcon(getClass().getClassLoader().getResource("clientPanel/imge/crown1.png")));
					ui.pnMusicChoice.btReady.setEnabled(true);
					ui.pnMusicChoice.btStart.setEnabled(false);
					ui.pnMusicChoice.btInvite.setEnabled(false);
					ui.pnMusicChoice.btExpel.setEnabled(false);
					ui.pnMusicChoice.tfusernick2.setText(resp.getJoiner().get(0).getNick());
					ui.pnMusicChoice.tfuserinfo2.setText(resp.getJoiner().get(0).tell());
					ui.pnMusicChoice.tfusernick1.setText(resp.getJoiner().get(1).getNick());
					ui.pnMusicChoice.tfuserinfo1.setText(resp.getJoiner().get(1).tell());
					ui.pnMusicChoice.p.leftButton.setEnabled(false);
					ui.pnMusicChoice.p.rightButton.setEnabled(false);
					sendRoomUserInfoRequest();
					ui.pnMusicChoice.p.selectedMusic.close();
					ui.pnMusicChoice.p.nowSelected = resp.getSelectMusic();
					ui.pnMusicChoice.p.selectTrack(resp.getSelectMusic());
					ui.pnMusicChoice.btRoomSetting.setEnabled(false);
					ui.pnMusicChoice.p.isRoomScreen = true;
					
					} else {		// 비번방일 때
						String pw = JOptionPane.showInputDialog("비밀번호를 입력하세요.");
						if (!(pw.equals(""))) {
								oos.writeObject("secretroom#" + pw);
								resp = (Room) ois.readObject();
								if(resp != null) {	// 입장 성공시
									ui.setSize(1400, 730);
									ui.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
									ui.pnMusicChoice = new MusicChoicePanel(ui);
									ui.setTitle("Drop the beat!! - Room");
									ui.setContentPane(ui.pnMusicChoice);
									System.out.println("sendChat 호출");
									sendChatRequest("이/가 입장했습니다.");
									ui.pnMusicChoice.btReady.setEnabled(true);
									ui.pnMusicChoice.btStart.setEnabled(false);
									ui.pnMusicChoice.btInvite.setEnabled(false);
									ui.pnMusicChoice.btExpel.setEnabled(false);
									ui.pnMusicChoice.lbCrownRight.setIcon(new ImageIcon(getClass().getClassLoader().getResource("clientPanel/imge/crown1.png")));
									ui.pnMusicChoice.tfusernick2.setText(resp.getJoiner().get(0).getNick());
									ui.pnMusicChoice.tfuserinfo2.setText(resp.getJoiner().get(0).tell());
									ui.pnMusicChoice.tfusernick1.setText(resp.getJoiner().get(1).getNick());
									ui.pnMusicChoice.tfuserinfo1.setText(resp.getJoiner().get(1).tell());
									ui.pnMusicChoice.p.leftButton.setEnabled(false);
									ui.pnMusicChoice.p.rightButton.setEnabled(false);
									ui.pnMusicChoice.btRoomSetting.setEnabled(false);
									sendRoomUserInfoRequest();
									ui.pnMusicChoice.p.selectedMusic.close();
									ui.pnMusicChoice.p.nowSelected = resp.getSelectMusic();
									ui.pnMusicChoice.p.selectTrack(resp.getSelectMusic());
									ui.pnMusicChoice.p.isRoomScreen = true;
								 
								}else {
									JOptionPane.showMessageDialog(ui, "비밀번호가 틀렸습니다.");
								}
							}
						}
				}catch (ClassNotFoundException | IOException e) {
					System.out.println(e.toString());
			}
		}
	}
	
	// 방 만들기 
	public void sendCreateRoomRequest(String title, String pw, boolean twouser) {
		Room resp = null;
		synchronized (oos) {
			try {
				
				oos.writeObject("createroom#" + title + "#" + twouser + "#" + pw);
				resp = (Room) ois.readObject();;
				System.out.println(resp);

				if (resp == null) {
					JOptionPane.showMessageDialog(ui, "이미 모든 방이 생성되어있습니다.");
				} else if(!twouser){
					ui.setSize(1400, 730);
					ui.pnMusicChoice = new MusicChoicePanel(ui);
					ui.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
					ui.setTitle("Drop the beat!! - Room(" + title + ")");
					ui.setContentPane(ui.pnMusicChoice);
					ui.pnMusicChoice.btReady.setEnabled(false);
					ui.pnMusicChoice.btStart.setEnabled(true);
					ui.pnMusicChoice.tfusernick1.setText(resp.getJoiner().get(0).getNick());
					ui.pnMusicChoice.tfuserinfo1.setText(resp.getJoiner().get(0).tell());
					ui.pnMusicChoice.tfusernick2.setText("사용자 없음");
					ui.pnMusicChoice.lbCrownLeft.setIcon(new ImageIcon(getClass().getClassLoader().getResource("clientPanel/imge/crown1.png")));
					ui.pnMusicChoice.rightPanel.setVisible(false);
					ui.pnMusicChoice.p.leftButton.setEnabled(true);
					ui.pnMusicChoice.p.rightButton.setEnabled(true);
					ui.pnMusicChoice.btInvite.setEnabled(false);
					ui.pnMusicChoice.p.isRoomScreen = true;
				} else {
					ui.setSize(1400, 730);
					ui.pnMusicChoice = new MusicChoicePanel(ui);
					ui.setTitle("Drop the beat!! - Room(" + title + ")");
					ui.setContentPane(ui.pnMusicChoice);

					ui.pnMusicChoice.btReady.setEnabled(false);
					ui.pnMusicChoice.btStart.setEnabled(false);
					ui.pnMusicChoice.tfusernick1.setText(resp.getJoiner().get(0).getNick());
					ui.pnMusicChoice.tfuserinfo1.setText(resp.getJoiner().get(0).tell());
					ui.pnMusicChoice.tfusernick2.setText("사용자 없음");
					ui.pnMusicChoice.p.leftButton.setEnabled(true);
					ui.pnMusicChoice.p.rightButton.setEnabled(true);
					ui.pnMusicChoice.lbCrownLeft.setIcon(new ImageIcon(getClass().getClassLoader().getResource("clientPanel/imge/crown1.png")));
					ui.pnMusicChoice.btExpel.setEnabled(false);
					ui.pnMusicChoice.p.isRoomScreen = true;
				}
			} catch (ClassNotFoundException | IOException e) {
				System.out.println(e.toString());
			}
		}
	}
	
	// 채팅 보내기
	public void sendChatRequest(String txt) {
		String resp = null;
		synchronized (oos) {
			try {
				oos.writeObject("chat#" + txt);
				ui.pnMusicChoice.txInput.setText("");

			} catch (IOException e) {
				System.out.println(e.toString());
			}
		}
	}
	
	// 준비 상태 보내기
	public void sendReadyRequest() {
		synchronized (oos) {
			try {
				if (ui.pnMusicChoice.btReady.isSelected()) {
					oos.writeObject("ready");
					sendChatRequest("Ready");
				} else {
					oos.writeObject("readycancel");
					sendChatRequest("Ready Cancel");
				}
			} catch (IOException e) {
				System.out.println(e.toString());
			}
		}
	}
	
	// 방 입장시 상대방에게 정보 갱신 요청
	public void sendRoomUserInfoRequest() {
		synchronized (oos) {
			try {
				oos.writeObject("roomuserinfo");
			} catch (IOException e) {
				System.out.println(e.toString());
			}
		}
	}
	
	// 게임이 끝나고 유저 정보 갱신
	public void sendReRoomUserInfoRequest() {
		synchronized (oos) {
			try {
				oos.writeObject("reroomuserinfo");
				Room ac = (Room) ois.readObject();
				if (ac.getJoiner().get(0).getNick().equals(ui.pnMusicChoice.tfusernick1.getText())) {
					ui.pnMusicChoice.tfuserinfo1.setText(ac.getJoiner().get(0).tell());
				} else{
					ui.pnMusicChoice.tfuserinfo1.setText(ac.getJoiner().get(1).tell());
				}
				if (ac.getJoiner().get(0).getNick().equals(ui.pnMusicChoice.tfusernick2.getText())) {
					ui.pnMusicChoice.tfuserinfo2.setText(ac.getJoiner().get(0).tell());
					System.out.println("1 " + ac.getJoiner().get(0).tell());
				} else{
					ui.pnMusicChoice.tfuserinfo2.setText(ac.getJoiner().get(1).tell());
					System.out.println("2 " + ac.getJoiner().get(1).tell());
				}
			} catch (IOException | ClassNotFoundException e) {
				System.out.println(e.toString());
			}
		}
	}
	
	// 게임 시작했을 때
	public void sendStartGameRequest() {
		int num = ui.pnMusicChoice.p.nowSelected;
		synchronized (oos) {
			try {
				oos.writeObject("startgame#" + num);
			} catch (IOException e) {
				System.out.println(e.toString());
			}
		}
	}
	
	// 방에서 선택 뮤직 변경시
	public void sendChangeMusicRequest(String txt) {
		synchronized (oos) {
			try {
				System.out.println("곡 바뀜 보냄");
				oos.writeObject(txt);
			} catch (IOException e) {
				System.out.println(e.toString());
			}
		}
	}
	
	// 실시간 점수 갱신
	public void sendScoreRequest(int score) {
		synchronized (oos) {
			try {
				oos.writeObject("score#" + score);
			} catch (IOException e) {
				System.out.println(e.toString());
			}
		}
	}
	
	// 게임 결과 요청
	public void sendGameResultRequest(int result) {
		synchronized (oos) {
			try {
				oos.writeObject("gameresult#" + result);
			} catch (IOException e) {
				System.out.println(e.toString());
			}
		}
	}
	
	// 빠른 입장 요청
	public void sendQuickRoomRequest() {
		synchronized (oos) {
			try {
				oos.writeObject("QuickRoom");
				int resp = (Integer)ois.readObject();
				if(resp<0) {
					JOptionPane.showMessageDialog(ui, "현재 참가할 방이 없습니다.");
				} else {
					sendEnterRoomRequest(resp);
				}
			} catch (Exception e) {
				System.out.println(e.toString());
			}
		}
	}
	
	// 메세지 보내기 요청
	public void sendMessageRequest(String name, String ment) {
		synchronized (oos) {
			try {
				oos.writeObject("sendMessage#" + name + "#" + ment);
				
			} catch (Exception e) {
				System.out.println(e.toString());
			}
		}
	}
	
	// 메세지 갯수 갱신
	public void sendMessageStateRequest() {
		synchronized (oos) {
			try {
				oos.writeObject("notreadingmessage");
				int l = (Integer)ois.readObject();
				ui.pnRoom.pnInfo.btMessage.setText(String.valueOf(l));
			} catch (Exception e) {
				System.out.println(e.toString());
			}
		}
	}
	
	// 메세지 목록 요청
	public void sendShowMessageListRequest() {
		synchronized (oos) {
			try {
				oos.writeObject("messagelist");
				List<Message> resp = (List<Message>)ois.readObject();
				if(resp != null) {
					String[] ar = new String[resp.size()];
					int i = 0;
					for (Message a : resp) {
						ar[i++] = (a.isRead() ? "○" : "     ") + "  [ " + a.sendToString() + " " + (a.sendToString().equals("수신") ? "◀" : "▶") + " ] " + a.getName() + "                                      " + a.getDate();  
						
					}
					ui.m.messageList.setListData(ar);
					
				}
			} catch (Exception e) {
				System.out.println(e.toString());
				System.out.println(e.getStackTrace());
			}
		}
	}
	
	// 읽은 메세지 상태 변경
	public void sendCheckReadMessageRequest(int idx) {
		synchronized (oos) {
			try {
				oos.writeObject("checkreadmessage#" + idx);
				boolean l = (Boolean)ois.readObject();
				if(l) {
					sendMessageStateRequest();
				}
			} catch (Exception e) {
				System.out.println(e.toString());
			}
		}
	}
	
	// 읽을 메세지 불러옴
	public Message sendShowMessageRequest(int idx) {
		synchronized (oos) {
			try {
				oos.writeObject("readmessage#" + idx);
				Message m = (Message)ois.readObject();
				return m;
			} catch (Exception e) {
				System.out.println(e.toString());
				return null;
			}
		}
	}
	
	// 초대 가능한 유저 리스트 호출
	public void sendInviteUserListRequest() {
		synchronized (oos) {
			try {
				oos.writeObject("availableinviteuser");
				List<Account> resp = (List<Account>)ois.readObject();

				InviteUsetListFrame n = new InviteUsetListFrame();
				n.setSize(400, 500);
				n.setLocationRelativeTo(null);
				String[] ar = new String[resp.size()];
				int i = 0;
				for (Account a : resp) {
					ar[i++] = a.toString();
				}
				n.setVisible(true);
				n.userList.setListData(ar);
			} catch (Exception e) {
				System.out.println(e.toString());
			}
		}
	}
	
	// 유저에게 초대메세지 보내기
	public void sendinviteRequest(String user) {
		synchronized (oos) {
			try {
				oos.writeObject("inviteuser#" + user);
			} catch (Exception e) {
				System.out.println(e.toString());
			}
		}
	}
	
	// 강퇴하기
	public void sendExpelRequest() {
		synchronized (oos) {
			try {
				String name = ui.pnMusicChoice.tfusernick2.getText();
				oos.writeObject("expeluser#" + name);
				ui.pnMusicChoice.txArea.append("[시스템] " + name + "님을 강퇴하였습니다.\n");;
			} catch (Exception e) {
				System.out.println(e.toString());
			}
		}
	}
	
	// 1인용 게임 결과전달
	public void sendSingleGameResultRequest(int score) {
		synchronized (oos) {
			try {
				oos.writeObject("singlegameresult#" + score);
				Account b = (Account)ois.readObject();
				if(b!=null) {
					ui.pnMusicChoice.tfuserinfo1.setText(b.tell());
				} 
			} catch (Exception e) {
				System.out.println(e.toString());
			}
		}
	}
	
	// 방 설정 변경 요청
	public void sendChangeRoomSettingRequest(String title, String pw, boolean twouser) {
		synchronized (oos) {
			try {
				oos.writeObject("changeroomsetting#" + title + "#" + twouser + "#" + pw);
				if(twouser) {
					ui.pnMusicChoice.btReady.setEnabled(false);
					ui.pnMusicChoice.btStart.setEnabled(false);
					ui.pnMusicChoice.btInvite.setEnabled(true);
					ui.pnMusicChoice.rightPanel.setVisible(true);
					ui.setTitle("Drop the beat!! - Room(" + title + ")");
				} else {
					ui.pnMusicChoice.btReady.setEnabled(false);
					ui.pnMusicChoice.btStart.setEnabled(true);
					ui.pnMusicChoice.btInvite.setEnabled(false);
					ui.pnMusicChoice.rightPanel.setVisible(false);
					ui.setTitle("Drop the beat!! - Room(" + title + ")");
				}
			} catch (Exception e) {
				System.out.println(e.toString());
			}
		}
	}
	
}