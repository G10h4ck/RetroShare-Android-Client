package com.example.retroshare.remote;

import java.io.IOException;
import java.io.NotActiveException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import net.lag.jaramiko.PKey;
import net.lag.jaramiko.SSHException;

// TODO: test serialization and clone()
// http://java.sun.com/developer/technicalArticles/Programming/serialization/
public class RsServerData implements Serializable, Cloneable{
	private static final boolean DEBUG=false;
	private static final long serialVersionUID = 0;
	
	// name to identify this server
	public String name;
	
	public String user;
	public String password;
	public boolean savePassword=false;
	public String hostname;
	public String dhtKey;
	public int port;
	public transient PKey hostkey;
	
	@Override
	public String toString(){
		//if(hostkey!=null){
			try{
				return "Servername:"+name+" \""+user+":"+password+"@"+hostname+":"+Integer.toString(port)+" key="+hostkey+"\"";
			} catch(NullPointerException e){
				//System.err.println("NullPointerException in RsServerData.toString()");
				return "Servername:"+name+" \""+user+":"+password+"@"+hostname+":"+Integer.toString(port)+" key=Error in RsServerData.toString() \"";
			}/*
		}else{
			return "\""+user+":"+password+"@"+hostname+":"+Integer.toString(port)+" key=RsServerData::toString: hostkey=null\"";
		}*/
	}
	
	public String getHostkeyFingerprint(){
		String s=hostkey.getSSHName()+" "+Integer.toString(hostkey.getBits())+" ";
		
		boolean firstbyte=true;
		for(byte b:hostkey.getFingerprint()){
			if(firstbyte==true){
				firstbyte=false;
			}else{
				s+=":";
			}
			byte[] a=new byte[2];
			int c=(b>>4) & 0x0f;
			a[0]=(byte) ((c<10)?(c+'0'):(c-10+'a'));
			c=b & 0x0f;
			a[1]=(byte) ((c<10)?(c+'0'):(c-10+'a'));
			
			s+=new String(a);
		}
		
		return s;
	}
	
	private void writeObject(ObjectOutputStream out){
		if(DEBUG){System.err.println("RsServerData::writeObject: "+this);}
		try {
			out.defaultWriteObject();
			if(hostkey!=null){
				out.writeBoolean(true);
				out.writeObject(hostkey.toByteArray());
			}
			else{
				out.writeBoolean(false);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
	}
	
	private void readObject(ObjectInputStream in) throws NotActiveException, IOException, ClassNotFoundException{
		if(DEBUG){System.err.println("RsServerData::readObject begin: ");}
		in.defaultReadObject();
		if(in.readBoolean()){
			if(DEBUG){System.err.println("RsServerData::readObject: PKey!=null");}
			byte[] b=(byte[]) in.readObject();
			hostkey= PKey.createFromData(b);
		}
		else{
			if(DEBUG){System.err.println("RsServerData::readObject: PKey==null");}
			hostkey=null;
		}
		if(DEBUG){System.err.println("RsServerData::readObject end: "+this);}
	}
	
	protected RsServerData clone(){
		RsServerData d=new RsServerData();
		d.name=name;
		d.user=user;
		d.password=password;
		d.savePassword=savePassword;
		d.hostname=hostname;
		d.dhtKey=dhtKey;
		d.port=port;
		if(hostkey!=null){
			try {
				d.hostkey=PKey.createFromData(hostkey.toByteArray());
			} catch (SSHException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else{
			d.hostkey=null;
		}
		return d;
	}
}
