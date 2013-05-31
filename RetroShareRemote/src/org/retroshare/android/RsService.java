package com.example.retroshare.remote;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.retroshare.remote.RsCtrlService.ConnectionError;
import com.example.retroshare.remote.RsCtrlService.RsCtrlServiceListener;
import com.example.retroshare.remote.RsCtrlService.RsMessage;

import rsctrl.chat.Chat.ResponseMsgIds;
import rsctrl.core.Core;

import net.lag.jaramiko.Channel;
import net.lag.jaramiko.ClientTransport;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;


public class RsService extends Service implements RsCtrlServiceListener{
	private static final String TAG="RsService";
	private static final int MAGIC_CODE = 0x137f0001;
	
	
	private static class Datapack implements Serializable{
		static final long serialVersionUID = 1L;
		// index: server name
		Map<String,RsServerData> serverDataMap=new HashMap<String,RsServerData>();
	}
	private Datapack mDatapack;
	private NotifyService mNotifyService;
	
	//private Handler mHandler;
	@Override
	public void onCreate(){
		try {
			ObjectInputStream i=new ObjectInputStream(openFileInput("RsService"+Long.toString(Datapack.serialVersionUID)));
			mDatapack=(Datapack) i.readObject();
			
			Log.v(TAG, "read Datapack, Datapack.serverDataMap="+mDatapack.serverDataMap);
		} catch (StreamCorruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(mDatapack==null){mDatapack=new Datapack();}
		
		mRsCtrlService=new RsCtrlService(new UiThreadHandler());
		mRsCtrlService.registerListener(this);
		
		mNotifyService=new NotifyService(mRsCtrlService.chatService,this);
		
		// update own State Notification
		updateNotification();
		
		int RESPONSE=(0x01<<24);
		final int MsgId_EventChatMessage=(RESPONSE|(Core.PackageId.CHAT_VALUE<<8)|ResponseMsgIds.MsgId_EventChatMessage_VALUE);
		//mRsCtrlService.registerMsgHandler(MsgId_EventChatMessage, new ChatlobbyChatActivity.ChatHandler());
		
		
	}
	
	@Override
	public void onDestroy(){
		Log.v(TAG, "onDestroy()");
		mNotifyService.cancelAll();
		mRsCtrlService.disconnect();
		mRsCtrlService.destroy();
		mRsCtrlService=null;
	}
	
	/*
	@Override
	public boolean onUnbind(Intent intent){
		Log.v(TAG, "onUnbind()");
		
		// don't use rebind
		return false;
	}
	*/
	
	public void saveData(){
		try {
			RsServerData sd=mRsCtrlService.getServerData();
			mDatapack.serverDataMap.put(sd.name, sd);
			Log.v(TAG, "trying to save Datapack, Datapack.serverDataMapt="+mDatapack.serverDataMap);
			ObjectOutputStream o=new ObjectOutputStream(openFileOutput("RsService"+Long.toString(Datapack.serialVersionUID), 0));
			o.writeObject(mDatapack);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public Map<String,RsServerData> getServers(){
		return mDatapack.serverDataMap;
	}
	
	//---------------------------------------------
	// Binder
	private final IBinder mBinder=new RsBinder();
	@Override
	public IBinder onBind(Intent arg0) {
		/*
		// tut
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
		
		int icon = R.drawable.ic_launcher;
		CharSequence tickerText = "Hello";
		long when = System.currentTimeMillis();

		Notification notification = new Notification(icon, tickerText, when);
		
		Context context = getApplicationContext();
		CharSequence contentTitle = "My notification";
		CharSequence contentText = "Hello World!";
		Intent notificationIntent = new Intent(this, RsService.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

		notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
		
		int HELLO_ID = 1;

		mNotificationManager.notify(HELLO_ID, notification);
		*/
		
		/*
		// tut auch, macht die benachrichtigung aber wegen startForeground() unl�schbar
		Notification notification = new Notification(R.drawable.ic_launcher, "blubber",System.currentTimeMillis());
		
		Intent notificationIntent = new Intent(this, RsService.class);
		
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		
		notification.setLatestEventInfo(this, "rsremote","rs remote wurde gestartet", pendingIntent);
		
		//first param has to be greater 0, dont know why
		startForeground(1, notification);
		*/
		
		return mBinder;
	}
	
	public class RsBinder extends Binder {
		RsService getService(){
			return RsService.this;
		}
	}
	
	
	// neu neu nicht l�schen
	private static class UiThreadHandler extends Handler implements UiThreadHandlerInterface{
		@Override
		public void postToUiThread(Runnable r) {
			post(r);
		}
	}
	
	public RsCtrlService mRsCtrlService;

	@Override
	public void onConnectionStateChanged(RsCtrlService.ConnectionEvent ce) {
		saveData();
		updateNotification();
	}
	
	private void updateNotification(){
		int icon;
		String tickerText;
		String contentTitle=(String) getResources().getText(R.string.app_name);
		String contentMessage;
		
		//=(String) getResources().getText(R.string.error)
		
		if(mRsCtrlService.isOnline()){
			icon=R.drawable.rstray3;
			tickerText=(String) getResources().getText(R.string.connected);
			//contentTitle=(String) getResources().getText(R.string.app_name);
			contentMessage=(String) getResources().getText(R.string.connected);
		}else{
			if(mRsCtrlService.getLastConnectionError()==ConnectionError.NONE){
				icon=R.drawable.rstray0;				
				tickerText=(String) getResources().getText(R.string.not_connected);
				//contentTitle="RetroShare Remote";
				contentMessage=(String) getResources().getText(R.string.not_connected);
			}else{
				icon=R.drawable.rstray0_err2;
				tickerText=(String) getResources().getText(R.string.connection_error);
				//contentTitle="RetroShare Remote";
				contentMessage=(String) getResources().getText(R.string.connection_error);
			}
		}

		
		Notification notification = new Notification(icon, tickerText,System.currentTimeMillis());
		
		Intent notificationIntent = new Intent(this, MainActivity.class);
		
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		
		notification.setLatestEventInfo(this, contentTitle,contentMessage, pendingIntent);
		
		startForeground(NotificationIds.RS_SERVICE, notification);
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	//--------------------------------------------
	
	/*
	private Socket socket;
	private ClientTransport transport;
	private Channel channel;
	private InputStream mInputStream;
	private OutputStream outputStream;
	
	private enum ConnectState{
		ONLINE,OFFLINE
	}
	private enum ConnectAction{
		CONNECT,DISCONNECT,NONE
	}
	*/
	
	// use with synchronized
	/*
	private ConnectState connectState=ConnectState.OFFLINE;
	private ConnectAction connectAction=ConnectAction.NONE;
	
	public void connect(){
		Log.v(TAG, "connect");
		synchronized(connectAction){
			connectAction=ConnectAction.CONNECT;
		}
	}
	
	
	// use with synchronized
	
	//is totaler quatsch, weil das object auch au�erhalb ver�ndert werden k�nnte
	// l�sung: serverliste innerhalb RsService verwalten -> ist auch gut wegen PKey in serverdata
	// addServer
	// deleteServer
	// bzw setServerData(hostname,user,passwd ...)
	
	// init, because of lock, without init, locking is impossible
	private RsServerData mServerData=new RsServerData();;
	public void setServerData(RsServerData d){
		Log.v(TAG, "setServerData");
		synchronized(mServerData){
			mServerData=d;
		}
	}
	public RsServerData getServerData(){
		synchronized(mServerData){
			return mServerData;
		}
	}
	
	private RsServerData[] mServers;
	public RsServerData[] getServers(){
		synchronized(mServers){
			return mServers;
		}
	}
	

	
	private void _connect(){
		try {
			synchronized(mServerData){
				Log.v(TAG, "_connect()");
				socket=new Socket();
				socket.connect(new InetSocketAddress(mServerData.hostname,mServerData.port), 2000);
				transport=new ClientTransport(socket);
				transport.start(mServerData.hostkey, 2000);
				transport.authPassword(mServerData.user, mServerData.password, 2000);
				channel=transport.openSession(2000);
				channel.invokeShell(2000);
				mInputStream=channel.getInputStream();
				outputStream=channel.getOutputStream();
				
				connectState=ConnectState.ONLINE;
				connectAction=ConnectAction.NONE;
								Log.v(TAG, "_connect(): success");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Log.e(TAG,Log.getStackTraceString(e));
			connectState=ConnectState.OFFLINE;
			connectAction=ConnectAction.NONE;
			
			//noch zu h�ndeln: Java.net.SocketEception Network unreachable
		}
	}
	
	public class RsMessage{
		public int msgId;
		public int reqId;
		public byte[] body;
	}
	
	// use with synchronized()
	private ArrayList<RsMessage> outMsgList=new ArrayList<RsMessage>();
	private int msgCounter=0;
	
	//holds pairs of <reqId,Handler>
	private HashMap<Integer,RsMessageHandler> msgHandlers= new HashMap<Integer,RsMessageHandler>();
	//holds pairs of <msgId,handler>
	private HashMap<Integer,RsMessageHandler> msgHandlersById= new HashMap<Integer,RsMessageHandler>();
	
	public int sendMsg(RsMessage msg, RsMessageHandler h){
		int reqId=0;
		synchronized(outMsgList){
			msgCounter++;
			msg.reqId=msgCounter;
			outMsgList.add(msg);
			reqId=msg.reqId;
		}
		synchronized(msgHandlers){
			msgHandlers.put(reqId, h);
		}
		return reqId;
	}
	
	public void registerMsgHandler(int msgId,RsMessageHandler h){
		synchronized(msgHandlersById){
			msgHandlersById.put(msgId, h);
		}
	}
	
	public RsMessageHandler getHandler(int msgId){
		return msgHandlersById.get(msgId);
	}
	
	volatile static boolean running=false;
	public void startThread(){
		if(running==false){
			Log.v(TAG,"startThread: starting Thread");
			Thread t=new Thread(this);
			t.start();
			running=true;
		}
		else{
			Log.v(TAG,"startThread: already running");
		}
	}
	@Override
	public void run() {
		while(running){
			
			try {
				Thread.sleep(50);
				//Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				Log.v(TAG,Log.getStackTraceString(e));
			}
			//Log.v(TAG, "run()");
			
			// check if we have to connect
			boolean connect=false;
			synchronized(connectAction){
				//if(connectState==ConnectState.OFFLINE){
					if(connectAction==ConnectAction.CONNECT){
						connect=true;
					}
				//}
			}
			if(connect){
				_connect();
			}
			if(connectState==ConnectState.ONLINE){
				// handle outgoing
				{
					RsMessage msg=null;
					synchronized(outMsgList){
						if(!outMsgList.isEmpty()){
							msg=outMsgList.get(0);
							outMsgList.remove(0);
						}
					}
					if(msg != null){
						_sendMsg(msg);
					}
				}
				// handle incoming
				{
					int msgType=_recvMsg();
					if(msgType != -1){
						RsMessage msg=new RsMessage();
						msg.msgId=curMsgId;
						msg.reqId=curReqId;
						msg.body=curBody;
						RsMessageHandler h=null;
						synchronized(msgHandlers){
							h=msgHandlers.remove(msg.reqId);
						}
						if(h!=null){
							Log.v(TAG,"run(): received Msg with reqId Handler, will now post Msg to UI Thread");
							h.setMsg(msg);
							// post to ui thread
							h.post(h);
						}
						else{
							synchronized(msgHandlersById){
								h=msgHandlersById.get(msg.msgId);
							}
							if(h!=null){
								Log.v(TAG,"run(): received Msg with msgId Handler, will now post Msg to UI Thread");
								h.setMsg(msg);
								// post to ui thread
								h.post(h);
							}
							else{
								System.err.print("Error: msgHandler not found");
							}
						}
					}
				}
				
			}
		}
	}
	
	// blockiert nicht
	// aber: nicblockierend in einem eigenen thread ist schwachsinn,
	// deswegen auf blockierend umstellen, oder den thread weglassen und nichtblockierend pollen
	// problem: rausfinden wie das timeout f�r die blockierung in jaramiko ist
	// nachteil: blockierend blockiert auch alle anderen aktionen im thread
	private void _sendMsg(RsMessage msg) {
		//allocate memory
		// 16 byte header + body
		ByteBuffer bb=ByteBuffer.allocate(16+msg.body.length);
		bb.putInt(MAGIC_CODE);
		bb.putInt(msg.msgId);
		bb.putInt(msg.reqId);
		bb.putInt(msg.body.length);
		bb.put(msg.body);
		try {
			Log.v(TAG,"_sendMsg()");
			outputStream.write(bb.array());
			System.out.println("sendRpc:");
			System.out.println(util.byteArrayToHexString(bb.array()));
			Log.v(TAG,"_sendMsg: success");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Log.e(TAG,Log.getStackTraceString(e));
			//Log.e(TAG,"Stopping Thread");
			//running=false;
			//Thread.currentThread().interrupt();
		}
	}
	
	// allocate 4 Bytes for Magic Code
	private ByteBuffer inbuf=ByteBuffer.allocate(4);
	
	private enum InputState{
		BEGIN,HAVE_MAGIC_CODE,HAVE_MSG_ID,HAVE_REQ_ID,HAVE_BODY_SIZE
	}
	private InputState inputState=InputState.BEGIN;
	private int curMsgId;
	private int curReqId;
	private int curBodySize;
	private byte[] curBody;
	
	private int _recvMsg(){
		try {
			switch(inputState){
				case BEGIN:
					while(mInputStream.available()>0 && inbuf.position()<4)
					{
						inbuf.put((byte) mInputStream.read());
					}
					if(inbuf.position()==4)
					{
						inbuf.rewind();
						if(inbuf.getInt()==MAGIC_CODE){
							inbuf=ByteBuffer.allocate(4);
							inputState=InputState.HAVE_MAGIC_CODE;
							System.out.println("received MAGIC_CODE");
						}
						else{
							System.out.println("Error: no MAGIC_CODE");
						}
					}
					break;
				case HAVE_MAGIC_CODE :
					while(mInputStream.available()>0 && inbuf.position()<4)
					{
						inbuf.put((byte) mInputStream.read());
					}
					if(inbuf.position()==4)
					{
						inbuf.rewind();
						curMsgId=inbuf.getInt();
						inbuf=ByteBuffer.allocate(4);
						inputState=InputState.HAVE_MSG_ID;
						System.out.print("received MSG_ID: ");
						System.out.print(curMsgId);
						System.out.println();
					}
					break;
				case HAVE_MSG_ID:
					while(mInputStream.available()>0 && inbuf.position()<4)
					{
						inbuf.put((byte) mInputStream.read());
					}
					if(inbuf.position()==4)
					{
						inbuf.rewind();
						curReqId=inbuf.getInt();
						inbuf=ByteBuffer.allocate(4);
						inputState=InputState.HAVE_REQ_ID;
						System.out.print("received REQ_ID: ");
						System.out.print(curReqId);
						System.out.println();
					}
					break;
				case HAVE_REQ_ID:
					while(mInputStream.available()>0 && inbuf.position()<4)
					{
						inbuf.put((byte) mInputStream.read());
					}
					if(inbuf.position()==4)
					{
						inbuf.rewind();
						curBodySize=inbuf.getInt();
						inbuf=ByteBuffer.allocate(curBodySize);
						inputState=InputState.HAVE_BODY_SIZE;
						System.out.print("received BODY_SIZE: ");
						System.out.print(curBodySize);
						System.out.println();
					}
					break;
				case HAVE_BODY_SIZE:
					while(mInputStream.available()>0 && inbuf.position()<curBodySize)
					{
						inbuf.put((byte) mInputStream.read());
					}
					if(inbuf.position()==curBodySize)
					{
						inbuf.rewind();
						curBody=inbuf.array();
						inbuf=ByteBuffer.allocate(4);
						inputState=InputState.BEGIN;
						System.out.println("received complete Body:");
						System.out.print(util.byteArrayToHexString(curBody));
						System.out.println();
						return curMsgId;
					}
					break;
				default:
					break;
				
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Log.e(TAG,Log.getStackTraceString(e));
		}
		return -1;
	}
	
	*/

}
