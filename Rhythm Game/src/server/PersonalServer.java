package server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import Common.Account;
import Common.Message;
import Common.Room;

public class PersonalServer extends Thread {
	
	// static ================================================================
	static UserAccountPool accountPool; 	// 전체 유저와 현재 접속중인 유저 관리
	static DatagramSocket ds;				// UDP를 위한 소켓
	static List<Room> rooms;				// 생성된 방을 관리
	 
	static {
		accountPool = new UserAccountPool();
		rooms = new ArrayList<>();

		try {
			ds = new DatagramSocket(56789);
		}catch(IOException e) {
			System.out.println("alramSocket create failed.. " + e.toString());
		}
	}
	
	static void sendAlramToAll(String alram) {			// 모든 유저에게 UDP전송
		DatagramPacket dp = new DatagramPacket(alram.getBytes(), alram.getBytes().length);
		for(Account a : accountPool.getCurrentUser()) {
			dp.setSocketAddress(a.getSocketAddress());
			try {
				System.out.println("server UDP send");
				
				ds.send(dp);
			}catch(IOException e) {
				System.out.println("[server-Thread] send alarm failed .. " + e.toString());
			}
		}
	}	
	
	static void sendAlramToUser(SocketAddress sa, String alram) {		// 특정 유저에게 UDP 전송
		DatagramPacket dp = new DatagramPacket(alram.getBytes(), alram.getBytes().length);
		dp.setSocketAddress(sa);
		try {
			System.out.println("server UDP send");
			ds.send(dp);
		}catch(IOException e) {
			System.out.println("[server-Thread] send alarm failed .. " + e.toString());
		}
		
	}
	
	static void sendAlramToUsers(Room r, String alram) {			// 방에 있는 사람에게 UDP 전송
		DatagramPacket dp = new DatagramPacket(alram.getBytes(), alram.getBytes().length);
	
		for(Account a : r.getJoiner()) {
			SocketAddress sa = a.getSocketAddress();
			dp.setSocketAddress(sa);
			try {
				System.out.println("server UDP send");
				System.out.println("txt"+alram);
				ds.send(dp);
			}catch(IOException e) {
				System.out.println("[server-Thread] send alarm failed .. " + e.toString());
			}
		}
	}
	
	//=========================================================================================
	
	private Socket socket;
	private ObjectOutputStream oos;
	private ObjectInputStream ois;
	private Account user;		//  현재 계정 객체 저장
	
	// 생성자 ================================
	public PersonalServer(Socket socket) {
		this.socket = socket;
		try {
			oos = new ObjectOutputStream(socket.getOutputStream());
			ois = new ObjectInputStream(socket.getInputStream());
		}catch(IOException e) {}
	}
	//========================================
	@Override
	public void run() {
		String[] command = null;
		while(socket.isConnected()) {
			String received;
			try {
				received = (String)ois.readObject();
			}catch(IOException | ClassNotFoundException e) {	// 비정상 종료시
				user.setSocketAddress(null);	// 해당 아이디의 ip null
				accountPool.getCurrentUser().remove(user);	// 현재 로그인 유저 제거
				if(user.getJoinRoomIndex() != -1) {		// 유저가 방에 있었을 때 처리
					if(rooms.get(user.getJoinRoomIndex()).getJoiner().size() == 2) {
						sendAlramToUser(getOtherRoomUserSocketAddress(), "reroominfo");
					} else {
						
					}
					rooms.get(user.getJoinRoomIndex()).getJoiner().remove(user);
					if(rooms.get(user.getJoinRoomIndex()).getJoiner().size()==0) {
						rooms.remove(user.getJoinRoomIndex());
						for(Room r : rooms) {
							for(Account a : r.getJoiner()) {
								if(a.getJoinRoomIndex() > user.getJoinRoomIndex()) {
									a.setJoinRoomIndex(a.getJoinRoomIndex()-1);
								}
							}
						}
					}
					user.setJoinRoomIndex(user.getJoinRoomIndex()-1);
				}
				accountPool.fileOutput();
				sendAlramToAll("userListChange");
				sendAlramToAll("changerooms");
				break;
			}
			
			System.out.println("[server] received : " + received);
			command = received.split("#");
			Object resp = null;
			System.out.println(command[0]);
			switch(command[0]) {
				
				case "create":			// 회원가입
					resp = accountPool.create(command[1], command[2], command[3]);
					sendToClient(resp);
					break;	
				case "join":			// 로그인
					String result = accountPool.login(command[1], command[2], socket.getRemoteSocketAddress());
					user = accountPool.getAccountMap().get(command[1]);
					sendToClient(result);
					if(result.equals("true")) {
						sendAlramToAll("userListChange");
					}
					break;
				case "get":				// RoomPanel 유저 목록
					resp = accountPool.getCurrentUser();
					sendToClient(resp);
					break;
				case "logout":			// 로그아웃
					boolean logOutResult = accountPool.logOut(user);
					if(logOutResult) {
						sendAlramToAll("userListChange");
					}
					sendToClient(logOutResult);
					break;
				case "enterroom":		// 방 입장
					System.out.println("enterroom");
					user.setJoinRoomIndex(Integer.valueOf(command[1]));
					int roomIndex = user.getJoinRoomIndex();
					if(rooms.get(roomIndex).getJoiner().size() != 2 && rooms.get(roomIndex).getPass().equals("")) {
						rooms.get(roomIndex).enterAccount(user);
						resp = rooms.get(roomIndex);
						sendToClient(resp);
						sendAlramToAll("changerooms");
					} else {
						resp = null;
						sendToClient(resp);
					}
					break;
				case "secretroom":	// 비번방 입장시
					if(command[1].equals((rooms.get(user.getJoinRoomIndex()).getPass()))) {
						rooms.get(user.getJoinRoomIndex()).enterAccount(user);
						resp = rooms.get(user.getJoinRoomIndex());
						sendAlramToAll("changerooms");
					} else {
						resp = null;
					}
					sendToClient(resp);
					break;
				case "createroom":	// 방 생성
					if(rooms.size() >=8) {
						sendToClient(null);
						sendToClient(0);
					} else {
						user.setJoinRoomIndex(rooms.size());
						// command의 길이가 3이라면 비번방이 아니고 1인용이나 2인용
						// command의 길이가 4라면 비번방이기 때문에 무조건 2인용
						if(command.length == 3) {
							if(command[2].equals("true")) {
								rooms.add(new Room(user,command[1],true,""));
							} else {
								rooms.add(new Room(user,command[1],false,""));
							}
						} else {
							rooms.add(new Room(user,command[1],true,command[2]));
						}
						resp = rooms.get(rooms.size()-1);
						sendToClient(resp);
						sendAlramToAll("changerooms");
					}
					System.out.println("createroom");
					break;
				case "roomlist":	// 방 목록 불러오기
					sendToClient(rooms);
					break;	
				case "leave":		// 방에서 나감
					if(rooms.get(user.getJoinRoomIndex()).getJoiner().size() == 2) {
						sendAlramToUser(getOtherRoomUserSocketAddress(), "reroominfo");
					} 
					rooms.get(user.getJoinRoomIndex()).getJoiner().remove(user);
					if(rooms.get(user.getJoinRoomIndex()).getJoiner().size()==0) {
						rooms.remove(user.getJoinRoomIndex());
						for(Room r : rooms) {
							for(Account a : r.getJoiner()) {
								if(a.getJoinRoomIndex() > user.getJoinRoomIndex()) {
									a.setJoinRoomIndex(a.getJoinRoomIndex() -1);
								}
							}
						}
					}
					sendToClient(null);
					sendAlramToAll("changerooms");
					System.out.println("??");
					user.setJoinRoomIndex(-1);
					break;
				case "chat":		// 방유저에게 채팅 뿌려줌
					Room r = rooms.get(user.getJoinRoomIndex());
					String str = "["+user.getNick()+"] : ";
					sendAlramToUsers(r, "changechat#"+str+command[1]);
					break;
				case "ready":	// 준비완료
					sendAlramToUser(getOtherRoomUserSocketAddress(), "readysuccess");
					System.out.println("보냄?");
					break;
				case "readycancel":	// 준비취소
					sendAlramToUser(getOtherRoomUserSocketAddress(), "readycancel");
					break;
				case "roomuserinfo":	// 방 입장시 방장에게 방 유저 목록 갱신 요청
					String req = "roominfo#"+user.getNick()+"#"+user.tell();
					sendAlramToUser(getOtherRoomUserSocketAddress(), req);
					break;
				case "startgame":	// 방 유저들에게 게임 시작 알림
					Room r1 = rooms.get(user.getJoinRoomIndex());
					r1.setGameStart(true);
					sendAlramToAll("changerooms");
					sendAlramToUsers(r1, received +"#" + r1.getJoiner().size());
					break;
				case "right":	// 곡 선택 오른쪽 이동
					Room r2 = rooms.get(user.getJoinRoomIndex());
					if(r2.getSelectMusic() == 3) {
						r2.setSelectMusic(0);
					} else {
						r2.setSelectMusic(r2.getSelectMusic()+1);
					}
					sendAlramToUsers(r2, received);
					break;
				case "left":	// 곡 선택 왼쪽 이동
					Room r3 = rooms.get(user.getJoinRoomIndex());
					if(r3.getSelectMusic() == 0) {
						r3.setSelectMusic(3);
					} else {
						r3.setSelectMusic(r3.getSelectMusic()-1);
					}
					sendAlramToUsers(r3, received);
					break;
				case "score":	// 점수 받아서 상대방에게 전송
					sendAlramToUser(getOtherRoomUserSocketAddress(), received);
					break;
				case "gameresult":	// 2인용 게임 결과 유저에게 전송하고 대기방으로 변경
					if(command[1].equals("0")) {
						user.setWin(user.getWin()+1);
					} else if(command[1].equals("2")) {
						user.setDraw(user.getDraw()+1);
					} else if(command[1].equals("1")) {
						user.setLose(user.getLose()+1);
					}
					accountPool.fileOutput();
					Room r4 = rooms.get(user.getJoinRoomIndex());
					if(r4.isResultSetting()) {
						sendAlramToUsers(r4, "gameresultSetted");
						r4.setGameStart(false);
						r4.setResultSetting(false);
					} else {
						r4.setResultSetting(true);
					}
					break;
				case "reroomuserinfo":	// 게임 끝나고 유저 정보 갱신
					Room r5 = rooms.get(user.getJoinRoomIndex());
					sendToClient(r5);
					System.out.println("보냄?");
					break;
				case "QuickRoom":	// 빠른 입장
					List<Room> cur = new ArrayList<>(); 
					for(Room s : rooms) {
						if(s.getPass().equals("") && s.getJoiner().size() != 2 && s.isTwoUserRoom()) {
							cur.add(s);
						}
					}
					if(cur.isEmpty()) {
						resp = -1;
						sendToClient(resp);
					} else {
						int ran = (int)(Math.random()*cur.size());
						int d = rooms.indexOf(cur.get(ran));
						sendToClient(d);
					}
					break;
				case "sendMessage":	// 쪽지 보내기
					String date = new SimpleDateFormat("yyyy-MM-dd aaa hh:mm ").format(new Date());
					Account rec = accountPool.getAccountMap().get(command[1]);
					rec.getMessagelist().add(new Message(false, user.getNick(), command[2], false, date));
					Message m = new Message(true, command[1], command[2], true,  date);
					user.getMessagelist().add(m);
					accountPool.fileOutput();
					sendAlramToUser(rec.getSocketAddress(), "resetmessage");
					sendAlramToUser(rec.getSocketAddress(), "sendmessage");
					break;
				case "messagelist": // 쪽지 리스트 띄우기
					sendToClient(user.getMessagelist());
					break;
				case "notreadingmessage":	// 읽지 않은 쪽지 갯수
					int num = 0;
					for(Message m1 : user.getMessagelist()) {
						if(m1.isRead() == false ) {
							num++;
						}
					}
					sendToClient(num);
					break;
				case "checkreadmessage":	// 쪽지 읽었을 때 처리
					if(user.getMessagelist().get(Integer.valueOf(command[1])).isRead() == false) {
						user.getMessagelist().get(Integer.valueOf(command[1])).setRead(true);
						accountPool.fileOutput();
						sendToClient(true);
					} else {
						sendToClient(false);
					}
					break;
				case "readmessage":			// 특정 쪽지 내용
					resp = user.getMessagelist().get(Integer.valueOf(command[1]));
					sendToClient(resp);
					break;
				case "availableinviteuser":	// 초대 가능한 사람 띄우기
					List<Account> li = new ArrayList<>();
					for(Account b : accountPool.getCurrentUser()) {
						if(b.getJoinRoomIndex() == -1) {
							li.add(b);
						}
					}
					sendToClient(li);
					break;
				case "inviteuser":	// 유저 초대
					SocketAddress s1 = accountPool.getAccountMap().get(command[1]).getSocketAddress();
					sendAlramToUser(s1, "invite#" + user.getNick() + "#" + user.getJoinRoomIndex());
					break;
				case "expeluser":	// 유저 강퇴
					Account a4 = rooms.get(user.getJoinRoomIndex()).getJoiner().get(1);
					SocketAddress s2 = a4.getSocketAddress();
					sendAlramToUser(s2, "expel");
					break;
				case "singlegameresult":	// 1인용 게임 결과 처리
					if(user.getMaxScore() < Integer.valueOf(command[1])){
						user.setMaxScore(Integer.valueOf(command[1]));
						sendAlramToAll("userListChange");
						resp = user;
					} else {
						resp = null;
					}
					sendToClient(resp);
					break;
				case "changeroomsetting":	// 방 설정 변경
					Room t = rooms.get(user.getJoinRoomIndex());
					t.setTitle(command[1]);
					t.setTwoUserRoom(Boolean.valueOf(command[2]));
					if(command.length == 4) {
						t.setPass(command[3]);
					} else {
						t.setPass("");
					}
					sendAlramToAll("changerooms");
					break;
					
			}
		}
	}

	private void sendToClient(Object resp) {
		try {
			oos.reset();	
			oos.writeObject(resp);
			System.out.println(resp);
		}catch(IOException e) {
			System.out.println("server write fail.. " + e.toString());
		}
	}
	
	private SocketAddress getOtherRoomUserSocketAddress() {
		Room r = rooms.get(user.getJoinRoomIndex());
		int idx = 0;
		if(r.isTwoUserRoom()) {
			idx = r.getJoiner().indexOf(user) == 0 ? 1 :0;
		}
		return r.getJoiner().get(idx).getSocketAddress();
	}
	
}
