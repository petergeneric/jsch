/* -*-mode:java; c-basic-offset:2; -*- */
/*
Copyright (c) 2002,2003 ymnk, JCraft,Inc. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

  1. Redistributions of source code must retain the above copyright notice,
     this list of conditions and the following disclaimer.

  2. Redistributions in binary form must reproduce the above copyright 
     notice, this list of conditions and the following disclaimer in 
     the documentation and/or other materials provided with the distribution.

  3. The names of the authors may not be used to endorse or promote products
     derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JCRAFT,
INC. OR ANY CONTRIBUTORS TO THIS SOFTWARE BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.jcraft.jsch;

import java.net.*;
import java.io.*;

import java.util.Vector;

public class ChannelSftp extends ChannelSession{

  private static final byte SSH_FXP_INIT=               1;
  private static final byte SSH_FXP_VERSION=            2;
  private static final byte SSH_FXP_OPEN=               3;
  private static final byte SSH_FXP_CLOSE=              4;
  private static final byte SSH_FXP_READ=               5;
  private static final byte SSH_FXP_WRITE=              6;
  private static final byte SSH_FXP_LSTAT=              7;
  private static final byte SSH_FXP_FSTAT=              8;
  private static final byte SSH_FXP_SETSTAT=            9;
  private static final byte SSH_FXP_FSETSTAT=          10;
  private static final byte SSH_FXP_OPENDIR=           11;
  private static final byte SSH_FXP_READDIR=           12;
  private static final byte SSH_FXP_REMOVE=            13;
  private static final byte SSH_FXP_MKDIR=             14;
  private static final byte SSH_FXP_RMDIR=             15;
  private static final byte SSH_FXP_REALPATH=          16;
  private static final byte SSH_FXP_STAT=              17;
  private static final byte SSH_FXP_RENAME=            18;
  private static final byte SSH_FXP_READLINK=          19;
  private static final byte SSH_FXP_SYMLINK=           20;
  private static final byte SSH_FXP_STATUS=           101;
  private static final byte SSH_FXP_HANDLE=           102;
  private static final byte SSH_FXP_DATA=             103;
  private static final byte SSH_FXP_NAME=             104;
  private static final byte SSH_FXP_ATTRS=            105;
  private static final byte SSH_FXP_EXTENDED=         (byte)200;
  private static final byte SSH_FXP_EXTENDED_REPLY=   (byte)201;

  // pflags
  private static final int SSH_FXF_READ=           0x00000001;
  private static final int SSH_FXF_WRITE=          0x00000002;
  private static final int SSH_FXF_APPEND=         0x00000004;
  private static final int SSH_FXF_CREAT=          0x00000008;
  private static final int SSH_FXF_TRUNC=          0x00000010;
  private static final int SSH_FXF_EXCL=           0x00000020;

  private static final int SSH_FILEXFER_ATTR_SIZE=         0x00000001;
  private static final int SSH_FILEXFER_ATTR_UIDGID=       0x00000002;
  private static final int SSH_FILEXFER_ATTR_PERMISSIONS=  0x00000004;
  private static final int SSH_FILEXFER_ATTR_ACMODTIME=    0x00000008;
  private static final int SSH_FILEXFER_ATTR_EXTENDED=     0x80000000;

  public static final int SSH_FX_OK=                            0;
  public static final int SSH_FX_EOF=                           1;
  public static final int SSH_FX_NO_SUCH_FILE=                  2;
  public static final int SSH_FX_PERMISSION_DENIED=             3;
  public static final int SSH_FX_FAILURE=                       4;
  public static final int SSH_FX_BAD_MESSAGE=                   5;
  public static final int SSH_FX_NO_CONNECTION=                 6;
  public static final int SSH_FX_CONNECTION_LOST=               7;
  public static final int SSH_FX_OP_UNSUPPORTED=                8;
/*
   SSH_FX_OK
      Indicates successful completion of the operation.
   SSH_FX_EOF
     indicates end-of-file condition; for SSH_FX_READ it means that no
       more data is available in the file, and for SSH_FX_READDIR it
      indicates that no more files are contained in the directory.
   SSH_FX_NO_SUCH_FILE
      is returned when a reference is made to a file which should exist
      but doesn't.
   SSH_FX_PERMISSION_DENIED
      is returned when the authenticated user does not have sufficient
      permissions to perform the operation.
   SSH_FX_FAILURE
      is a generic catch-all error message; it should be returned if an
      error occurs for which there is no more specific error code
      defined.
   SSH_FX_BAD_MESSAGE
      may be returned if a badly formatted packet or protocol
      incompatibility is detected.
   SSH_FX_NO_CONNECTION
      is a pseudo-error which indicates that the client has no
      connection to the server (it can only be generated locally by the
      client, and MUST NOT be returned by servers).
   SSH_FX_CONNECTION_LOST
      is a pseudo-error which indicates that the connection to the
      server has been lost (it can only be generated locally by the
      client, and MUST NOT be returned by servers).
   SSH_FX_OP_UNSUPPORTED
      indicates that an attempt was made to perform an operation which
      is not supported for the server (it may be generated locally by
      the client if e.g.  the version number exchange indicates that a
      required feature is not supported by the server, or it may be
      returned by the server if the server does not implement an
      operation).
*/


//  private boolean interactive=true;
  private boolean interactive=false;
  private int count=1;
  private Buffer buf;
  private Packet packet=new Packet(buf);

  private String version="3";
  private String cwd;
  private String home;
  private String lcwd;

  ChannelSftp(){
    super();
    type="session".getBytes();
    io=new IO();
  }

  /*
  public void init(){
    io.setInputStream(session.in);
    io.setOutputStream(session.out);
  }
  */

  public void start(){
    try{

      PipedOutputStream pos=new PipedOutputStream();
      io.setOutputStream(pos);
      PipedInputStream pis=new PipedInputStream(pos);
      io.setInputStream(pis);

      Request request;
      request=new RequestSftp();
      request.request(session, this);

      thread=this;
      buf=new Buffer();
      packet=new Packet(buf);
      int i=0;
      int j=0;
      int length;
      int type;
      byte[] str;

      // send SSH_FXP_INIT
      sendINIT();

      // receive SSH_FXP_VERSION
      buf.rewind();
      i=io.in.read(buf.buffer, 0, buf.buffer.length);
      length=buf.getInt();
      type=buf.getByte();           // 2 -> SSH_FXP_VERSION

      // send SSH_FXP_REALPATH
      sendREALPATH(".".getBytes());

      // receive SSH_FXP_NAME
      buf.rewind();
      i=io.in.read(buf.buffer, 0, buf.buffer.length);
      length=buf.getInt();
      type=buf.getByte();          // 104 -> SSH_FXP_NAME
      buf.getInt();                //
      i=buf.getInt();              // count
      str=buf.getString();         // filename
      home=cwd=new String(str);
      str=buf.getString();         // logname
//    SftpATTRS.getATTR(buf);           // attrs

      lcwd=new File(".").getAbsolutePath();

    }
    catch(Exception e){
      //System.out.println(e);
    }
    //thread=null;
  }

  public void quit(){ disconnect();}
  public void exit(){ disconnect();}
  public void lcd(String path) throws SftpException{
    if(!path.startsWith("/")){ path=lcwd+"/"+path; }
    if((new File(path)).isDirectory()){
      lcwd=path;
      return;
    }
    throw new SftpException(SSH_FX_NO_SUCH_FILE, "No such directory");
  }

  /*
      cd /tmp
      c->s REALPATH
      s->c NAME
      c->s STAT
      s->c ATTR 
  */
  public void cd(String path) throws SftpException{
    try{
      if(!path.startsWith("/")){ path=cwd+"/"+path; }

      Vector v=glob_remote(path);
      if(v.size()!=1){
	throw new SftpException(SSH_FX_FAILURE, v.toString());
      }
      path=(String)(v.elementAt(0));

      sendREALPATH(path.getBytes());

      buf.rewind();
      int i=io.in.read(buf.buffer, 0, buf.buffer.length);
      int length=buf.getInt();
      int type=buf.getByte();
      if(type!=101 && type!=104){
	throw new SftpException(SSH_FX_FAILURE, "");
      }
      if(type==101){
	buf.getInt();
	i=buf.getInt();
	byte[] str=buf.getString();
	throw new SftpException(i, new String(str));
      }
      buf.getInt();
      i=buf.getInt();
      byte[] str=buf.getString();
      if(str!=null && str[0]!='/'){
        str=(cwd+"/"+new String(str)).getBytes();
      }
      cwd=new String(str);
      str=buf.getString();         // logname
      i=buf.getInt();              // attrs
    }
    catch(Exception e){
      if(e instanceof SftpException) throw (SftpException)e;
      throw new SftpException(SSH_FX_FAILURE, "");
    }
  }

  /*
      put foo
      c->s OPEN
      s->c HANDLE
      c->s WRITE
      s->c STATUS
      c->s CLOSE 
      s->c STATUS 
  */
  public void put(String src, String dst) throws SftpException{
    if(!src.startsWith("/")){ src=lcwd+"/"+src; } 
    if(!dst.startsWith("/")){ dst=cwd+"/"+dst; }
    try{
      Vector v=glob_remote(dst);
      if(v.size()!=1){
        throw new SftpException(SSH_FX_FAILURE, v.toString());
      }
      dst=(String)(v.elementAt(0));
      if(isRemoteDir(dst)){
        if(!dst.endsWith("/")){
	  dst+="/";
	}
	int i=src.lastIndexOf('/');
	if(i==-1) dst+=src;
	else dst+=src.substring(i+1);
      }
      FileInputStream fis=new FileInputStream(src);
      put(fis, dst);
      fis.close();
    }
    catch(Exception e){
      if(e instanceof SftpException) throw (SftpException)e;
      throw new SftpException(SSH_FX_FAILURE, "");
    }
  }
  public void put(InputStream src, String dst) throws SftpException{
    try{
      if(!dst.startsWith("/")){ dst=cwd+"/"+dst; } 
      Vector v=glob_remote(dst);
      if(v.size()!=1){
        throw new SftpException(SSH_FX_FAILURE, v.toString());
      }
      dst=(String)(v.elementAt(0));
      if(isRemoteDir(dst)){
	throw new SftpException(SSH_FX_FAILURE, dst+" is a directory");
      }
      sendOPENW(dst.getBytes());

      buf.rewind();
      int i=io.in.read(buf.buffer, 0, buf.buffer.length);
      int length=buf.getInt();
      int type=buf.getByte();
      if(type!=SSH_FXP_STATUS && type!=SSH_FXP_HANDLE){
	throw new SftpException(SSH_FX_FAILURE, "");
      }
      if(type==SSH_FXP_STATUS){
	buf.getInt();
	i=buf.getInt();
	byte[] str=buf.getString();
	throw new SftpException(i, new String(str));
      }
      buf.getInt();
      byte[] handle=buf.getString();         // filename

      byte[] data=new byte[1024];

      long offset=0;
      while(true){
        i=src.read(data, 0, 1024);
        if(i<=0)break;
        sendWRITE(handle, offset, data, 0, i);
        offset+=i;

        buf.rewind();
	i=io.in.read(buf.buffer, 0, buf.buffer.length);
	length=buf.getInt();
	type=buf.getByte();
	if(type!=SSH_FXP_STATUS){ break;}
        buf.getInt();
        if(buf.getInt()!=SSH_FX_OK){
          break;
	}
      }

      sendCLOSE(handle);

      buf.rewind();
      i=io.in.read(buf.buffer, 0, buf.buffer.length);
      length=buf.getInt();
      type=buf.getByte();
      if(type!=SSH_FXP_STATUS){
	throw new SftpException(SSH_FX_FAILURE, "");
      }
      buf.getInt();
      i=buf.getInt();
      if(i==SSH_FX_OK) return;
      byte[] str=buf.getString();
      throw new SftpException(i, new String(str));
    }
    catch(Exception e){
      if(e instanceof SftpException) throw (SftpException)e;
      throw new SftpException(SSH_FX_FAILURE, "");
    }
  }
  public OutputStream put(String dst) throws SftpException{
    if(!dst.startsWith("/")){ dst=cwd+"/"+dst; } 
    try{
      Vector v=glob_remote(dst);
      if(v.size()!=1){
	throw new SftpException(SSH_FX_FAILURE, v.toString());
      }
      dst=(String)(v.elementAt(0));
      if(isRemoteDir(dst)){
	throw new SftpException(SSH_FX_FAILURE, dst+" is a directory");
      }
      java.io.PipedOutputStream pos=new java.io.PipedOutputStream();

      final java.io.PipedInputStream pis=new java.io.PipedInputStream(pos);
      final ChannelSftp channel=this;
      final String _dst=dst;
      new Thread(new Runnable(){
	  public void run(){
	    try{ channel.put(pis, _dst); }
	    catch(Exception ee){
	      System.out.println("!!"+ee);
	    }
	    try{ pis.close(); }catch(Exception ee){}
	  }
	}).start();
      return pos;
    }
    catch(Exception e){
      if(e instanceof SftpException) throw (SftpException)e;
      throw new SftpException(SSH_FX_FAILURE, "");
    }
  }
  public void get(String src, String dst) throws SftpException{
    if(!dst.startsWith("/")){ dst=lcwd+"/"+dst; } 
    try{
      Vector v=glob_remote(src);
      for(int j=0; j<v.size(); j++){
	String _dst=dst;
	String _src=(String)(v.elementAt(j));
	if((new File(_dst)).isDirectory()){
	  if(!_dst.endsWith("/")){
	    _dst+="/";
	  }
	  int i=_src.lastIndexOf('/');
	  if(i==-1) _dst+=src;
	  else _dst+=_src.substring(i+1);
	}
	FileOutputStream fos=new FileOutputStream(_dst);
	get(_src, fos);
	fos.close();
      }
    }
    catch(Exception e){
      if(e instanceof SftpException) throw (SftpException)e;
      throw new SftpException(SSH_FX_FAILURE, "");
    }
  }
  public void get(String src, OutputStream dst) throws SftpException{
//System.out.println("get: "+src+", "+dst);
    try{
      if(!src.startsWith("/")){ src=cwd+"/"+src; } 
      Vector v=glob_remote(src);
      if(v.size()!=1){
        throw new SftpException(SSH_FX_FAILURE, v.toString());
      }
      src=(String)(v.elementAt(0));

      sendOPENR(src.getBytes());
      buf.rewind();
      int i=io.in.read(buf.buffer, 0, buf.buffer.length);
      int length=buf.getInt();
      int type=buf.getByte();
      if(type!=SSH_FXP_STATUS && type!=SSH_FXP_HANDLE){
	throw new SftpException(SSH_FX_FAILURE, "");
      }
      if(type==SSH_FXP_STATUS){
	buf.getInt();
	i=buf.getInt();
	byte[] str=buf.getString();
	throw new SftpException(i, new String(str));
      }
      buf.getInt();
      byte[] handle=buf.getString();         // filename

      byte[] data=null;

      long offset=0;
      while(true){
        sendREAD(handle, offset, 1000);
        buf.rewind();
	i=io.in.read(buf.buffer, 0, buf.buffer.length);
	length=buf.getInt();
	type=buf.getByte();
        buf.getInt();
	if(type!=SSH_FXP_STATUS && type!=SSH_FXP_DATA){ break;}
	if(type==SSH_FXP_STATUS){
          if(buf.getInt()==SSH_FX_EOF){
            break;
   	  }
//	  break;
	}
	data=buf.getString();
        dst.write(data, 0, data.length);
	dst.flush();
        offset+=data.length;
      }
      sendCLOSE(handle);
      buf.rewind();
      i=io.in.read(buf.buffer, 0, buf.buffer.length);
      length=buf.getInt();
      type=buf.getByte();
      if(type!=SSH_FXP_STATUS){
	throw new SftpException(SSH_FX_FAILURE, "");
      }
      buf.getInt();
      i=buf.getInt();
      if(i==SSH_FX_OK) return;
      byte[] str=buf.getString();
      throw new SftpException(i, new String(str));
    }
    catch(Exception e){
      if(e instanceof SftpException) throw (SftpException)e;
      throw new SftpException(SSH_FX_FAILURE, "");
    }
  }
  public InputStream get(String src) throws SftpException{
    if(!src.startsWith("/")){ src=cwd+"/"+src; } 
    try{
      Vector v=glob_remote(src);
      if(v.size()!=1){
        throw new SftpException(SSH_FX_FAILURE, v.toString());
      }
      src=(String)(v.elementAt(0));

      stat(src);

      java.io.PipedInputStream pis=new java.io.PipedInputStream();
      final java.io.PipedOutputStream pos=new java.io.PipedOutputStream(pis);
      final ChannelSftp channel=this;
      final String _src=src;
      new Thread(new Runnable(){
	  public void run(){
	    try{ channel.get(_src, pos); }
	    catch(Exception ee){
	      System.out.println("!!"+ee);
	    }
	    try{ pos.close(); }catch(Exception ee){}
	  }
	}).start();
      while(true){
	if(pis.available()!=0)break;
	Thread.sleep(1000);
      }
      return pis;
    }
    catch(Exception e){
      if(e instanceof SftpException) throw (SftpException)e;
      throw new SftpException(SSH_FX_FAILURE, "");
    }
  }
  public java.util.Vector ls(String path) throws SftpException{
    try{
      if(!path.startsWith("/")){ path=cwd+"/"+path; }

      String dir=path;
      byte[] pattern=null;
      if(!isRemoteDir(dir)){
	int foo=path.lastIndexOf('/');
	dir=path.substring(0, foo);
	pattern=path.substring(foo+1).getBytes();
      }

      sendOPENDIR(dir.getBytes());

      buf.rewind();
      int i=io.in.read(buf.buffer, 0, buf.buffer.length);
      int length=buf.getInt();
      int type=buf.getByte();
      if(type!=SSH_FXP_STATUS && type!=SSH_FXP_HANDLE){
	throw new SftpException(SSH_FX_FAILURE, "");
      }
      if(type==SSH_FXP_STATUS){
	buf.getInt();
	i=buf.getInt();
	byte[] str=buf.getString();
	throw new SftpException(i, new String(str));
      }
      buf.getInt();
      byte[] handle=buf.getString();         // filename

      java.util.Vector v=new java.util.Vector();
      while(true){
        sendREADDIR(handle);
        buf.rewind();
        i=io.in.read(buf.buffer, 0, buf.buffer.length);
        buf.index=i;
        length=buf.getInt();
        length=length-(i-4);
        type=buf.getByte();

        if(type!=SSH_FXP_STATUS && type!=SSH_FXP_NAME){
	  throw new SftpException(SSH_FX_FAILURE, "");
	}
        if(type==SSH_FXP_STATUS){ 
	  /*
	  buf.getInt();
	  i=buf.getInt();
	  System.out.println("i="+i);
	  if(i==SSH_FX_EOF) break;
	  byte[] str=buf.getString();
	  throw new SftpException(i, new String(str));
	  */
	  break;
	}

        buf.getInt();
        int count=buf.getInt();

        byte[] str;
        int flags;

        while(count>0){
          if(length>0){
            buf.shift();
            i=io.in.read(buf.buffer, buf.index, buf.buffer.length-buf.index);
  	    if(i<=0)break;
	    buf.index+=i;
            length-=i;
	  }

	  byte[] filename=buf.getString();
	  // System.out.println("filename: "+new String(filename));
	  str=buf.getString();
	  String longname=new String(str);
	  // System.out.println("longname: "+longname);

	  SftpATTRS attrs=SftpATTRS.getATTR(buf);
	  if(pattern==null || glob(pattern, filename)){
  	    v.addElement(longname);
//	    v.addElement(new Ssh_exp_name(new String(filename), longname, attrs));
	  }

	  count--; 
        }
      }

      sendCLOSE(handle);
      buf.rewind();
      i=io.in.read(buf.buffer, 0, buf.buffer.length);
      length=buf.getInt();
      type=buf.getByte();
      if(type!=SSH_FXP_STATUS){
	throw new SftpException(SSH_FX_FAILURE, "");
      }
      buf.getInt();
      i=buf.getInt();
      if(i==SSH_FX_OK) return v;
      byte[] str=buf.getString();
      throw new SftpException(i, new String(str));
    }
    catch(Exception e){
      if(e instanceof SftpException) throw (SftpException)e;
      throw new SftpException(SSH_FX_FAILURE, "");
    }
//    return null;
  }
  public void symlink(String oldpath, String newpath) throws SftpException{
    try{
      if(!oldpath.startsWith("/")){ oldpath=cwd+"/"+oldpath; } 
      if(!newpath.startsWith("/")){ newpath=cwd+"/"+newpath; } 

      Vector v=glob_remote(oldpath);
      if(v.size()!=1){
	throw new SftpException(SSH_FX_FAILURE, v.toString());
      }
      oldpath=(String)(v.elementAt(0));

      sendSYMLINK(oldpath.getBytes(), newpath.getBytes());
      buf.rewind();
      int i=io.in.read(buf.buffer, 0, buf.buffer.length);
      int length=buf.getInt();
      int type=buf.getByte();
      if(type!=SSH_FXP_STATUS){
	throw new SftpException(SSH_FX_FAILURE, "");
      }
      buf.getInt();
      i=buf.getInt();
      if(i==SSH_FX_OK) return;
      byte[] str=buf.getString();
      throw new SftpException(i, new String(str));
    }
    catch(Exception e){
      if(e instanceof SftpException) throw (SftpException)e;
      throw new SftpException(SSH_FX_FAILURE, "");
    }
  }
  public void rename(String oldpath, String newpath) throws SftpException{
    try{
      if(!oldpath.startsWith("/")){ oldpath=cwd+"/"+oldpath; } 
      if(!newpath.startsWith("/")){ newpath=cwd+"/"+newpath; } 

      Vector v=glob_remote(oldpath);
      if(v.size()!=1){
	throw new SftpException(SSH_FX_FAILURE, v.toString());
      }
      oldpath=(String)(v.elementAt(0));

      v=glob_remote(newpath);
      if(v.size()>=2){
	throw new SftpException(SSH_FX_FAILURE, v.toString());
      }
      if(v.size()==1){
        newpath=(String)(v.elementAt(0));
      }

      sendRENAME(oldpath.getBytes(), newpath.getBytes());
      buf.rewind();
      int i=io.in.read(buf.buffer, 0, buf.buffer.length);
      int length=buf.getInt();
      int type=buf.getByte();
      if(type!=SSH_FXP_STATUS){
	throw new SftpException(SSH_FX_FAILURE, "");
      }
      buf.getInt();
      i=buf.getInt();
      if(i==SSH_FX_OK) return;
      byte[] str=buf.getString();
      throw new SftpException(i, new String(str));
    }
    catch(Exception e){
      if(e instanceof SftpException) throw (SftpException)e;
      throw new SftpException(SSH_FX_FAILURE, "");
    }
  }
  public void rm(String path) throws SftpException{
    try{
      if(!path.startsWith("/")){ path=cwd+"/"+path; }
      Vector v=glob_remote(path);
      for(int j=0; j<v.size(); j++){
	path=(String)(v.elementAt(j));
        sendREMOVE(path.getBytes());
        buf.rewind();
        int i=io.in.read(buf.buffer, 0, buf.buffer.length);
        int length=buf.getInt();
        int type=buf.getByte();
        if(type!=SSH_FXP_STATUS){
	  throw new SftpException(SSH_FX_FAILURE, "");
        }
        buf.getInt();
        i=buf.getInt();
        if(i!=SSH_FX_OK){
	  byte[] str=buf.getString();
	  throw new SftpException(i, new String(str));
	}
      }
    }
    catch(Exception e){
      if(e instanceof SftpException) throw (SftpException)e;
      throw new SftpException(SSH_FX_FAILURE, "");
    }
  }
  boolean isRemoteDir(String path){
    try{
      sendSTAT(path.getBytes());
      buf.rewind();
      int i=io.in.read(buf.buffer, 0, buf.buffer.length);
      int length=buf.getInt();
      int type=buf.getByte();
      if(type!=SSH_FXP_ATTRS){ return false; }
      buf.getInt();
      SftpATTRS attr=SftpATTRS.getATTR(buf);
      return attr.isDir();
    }
    catch(Exception e){}
    return false;
  }
  public void chgrp(int gid, String path) throws SftpException{
    try{
      if(!path.startsWith("/")){ path=cwd+"/"+path; }

      Vector v=glob_remote(path);
      for(int j=0; j<v.size(); j++){
	path=(String)(v.elementAt(j));
        sendSTAT(path.getBytes());
 
        buf.rewind();
        int i=io.in.read(buf.buffer, 0, buf.buffer.length);
        int length=buf.getInt();
        int type=buf.getByte();
        if(type!=SSH_FXP_ATTRS){
	  throw new SftpException(SSH_FX_FAILURE, "");
	}
	buf.getInt();
	SftpATTRS attr=SftpATTRS.getATTR(buf);
	attr.setUIDGID(attr.uid, gid); 

	sendSETSTAT(path.getBytes(), attr);

	buf.rewind();
	i=io.in.read(buf.buffer, 0, buf.buffer.length);
	length=buf.getInt();
	type=buf.getByte();
	if(type!=SSH_FXP_STATUS){
	  throw new SftpException(SSH_FX_FAILURE, "");
	}
	buf.getInt();
	i=buf.getInt();
	if(i!=SSH_FX_OK){
	  byte[] str=buf.getString();
	  throw new SftpException(i, new String(str));
	}
      }
    }
    catch(Exception e){
      if(e instanceof SftpException) throw (SftpException)e;
      throw new SftpException(SSH_FX_FAILURE, "");
    }
  }
  public void chown(int uid, String path) throws SftpException{
    try{
      if(!path.startsWith("/")){ path=cwd+"/"+path; }

      Vector v=glob_remote(path);
      for(int j=0; j<v.size(); j++){
	path=(String)(v.elementAt(j));

	sendSTAT(path.getBytes());
 
	buf.rewind();
	int i=io.in.read(buf.buffer, 0, buf.buffer.length);
	int length=buf.getInt();
	int type=buf.getByte();
	if(type!=SSH_FXP_ATTRS){
	  throw new SftpException(SSH_FX_FAILURE, "");
	}
	buf.getInt();
	SftpATTRS attr=SftpATTRS.getATTR(buf);
	attr.setUIDGID(uid, attr.gid); 

	sendSETSTAT(path.getBytes(), attr);

	buf.rewind();
	i=io.in.read(buf.buffer, 0, buf.buffer.length);
	length=buf.getInt();
	type=buf.getByte();
	if(type!=SSH_FXP_STATUS){
	  throw new SftpException(SSH_FX_FAILURE, "");
	}
	buf.getInt();
	i=buf.getInt();
	if(i!=SSH_FX_OK){
	  byte[] str=buf.getString();
	  throw new SftpException(i, new String(str));
	}
      }
    }
    catch(Exception e){
      if(e instanceof SftpException) throw (SftpException)e;
      throw new SftpException(SSH_FX_FAILURE, "");
    }
  }
  public void chmod(int permissions, String path) throws SftpException{
    try{
      if(!path.startsWith("/")){ path=cwd+"/"+path; }

      Vector v=glob_remote(path);
      for(int j=0; j<v.size(); j++){
	path=(String)(v.elementAt(j));

	sendSTAT(path.getBytes());
 
	buf.rewind();
	int i=io.in.read(buf.buffer, 0, buf.buffer.length);
	int length=buf.getInt();
	int type=buf.getByte();
	if(type!=SSH_FXP_ATTRS){
	  throw new SftpException(SSH_FX_FAILURE, "");
	}
	buf.getInt();
	SftpATTRS attr=SftpATTRS.getATTR(buf);
	attr.setPERMISSIONS(permissions); 

	sendSETSTAT(path.getBytes(), attr);

	buf.rewind();
	i=io.in.read(buf.buffer, 0, buf.buffer.length);
	length=buf.getInt();
	type=buf.getByte();
	if(type!=SSH_FXP_STATUS){
	  throw new SftpException(SSH_FX_FAILURE, "");
	}
	buf.getInt();
	i=buf.getInt();
	if(i!=SSH_FX_OK){
	  byte[] str=buf.getString();
	  throw new SftpException(i, new String(str));
	}
      }
    }
    catch(Exception e){
      if(e instanceof SftpException) throw (SftpException)e;
      throw new SftpException(SSH_FX_FAILURE, "");
    }
  }
  public void rmdir(String path) throws SftpException{
    try{
      if(!path.startsWith("/")){ path=cwd+"/"+path; }
      Vector v=glob_remote(path);
      for(int j=0; j<v.size(); j++){
	path=(String)(v.elementAt(j));

	sendRMDIR(path.getBytes());
	buf.rewind();
	int i=io.in.read(buf.buffer, 0, buf.buffer.length);
	int length=buf.getInt();
	int type=buf.getByte();
	if(type!=SSH_FXP_STATUS){
	  throw new SftpException(SSH_FX_FAILURE, "");
	}
	buf.getInt();
	i=buf.getInt();
	if(i!=SSH_FX_OK){
	  byte[] str=buf.getString();
	  throw new SftpException(i, new String(str));
	}
      }
    }
    catch(Exception e){
      if(e instanceof SftpException) throw (SftpException)e;
      throw new SftpException(SSH_FX_FAILURE, "");
    }
  }

  public void mkdir(String path) throws SftpException{
    try{
     if(!path.startsWith("/")){ path=cwd+"/"+path; }
      sendMKDIR(path.getBytes(), null);
      buf.rewind();
      int i=io.in.read(buf.buffer, 0, buf.buffer.length);
      int length=buf.getInt();
      int type=buf.getByte();
      if(type!=SSH_FXP_STATUS){
	throw new SftpException(SSH_FX_FAILURE, "");
      }
      buf.getInt();
      i=buf.getInt();
      if(i==SSH_FX_OK) return;
      byte[] str=buf.getString();
      throw new SftpException(i, new String(str));
    }
    catch(Exception e){
      if(e instanceof SftpException) throw (SftpException)e;
      throw new SftpException(SSH_FX_FAILURE, "");
    }
  }

  public SftpATTRS stat(String path) throws SftpException{
    try{
     if(!path.startsWith("/")){ path=cwd+"/"+path; }
      sendSTAT(path.getBytes());
      buf.rewind();
      int i=io.in.read(buf.buffer, 0, buf.buffer.length);
      int length=buf.getInt();
      int type=buf.getByte();
      if(type!=SSH_FXP_ATTRS){
	if(type==SSH_FXP_STATUS){
	  buf.getInt();
	  i=buf.getInt();
	  byte[] str=buf.getString();
	  throw new SftpException(i, new String(str));
	}
	throw new SftpException(SSH_FX_FAILURE, "");
      }
      buf.getInt();
      SftpATTRS attr=SftpATTRS.getATTR(buf);
      return attr;
    }
    catch(Exception e){
      if(e instanceof SftpException) throw (SftpException)e;
      throw new SftpException(SSH_FX_FAILURE, "");
    }
    //return null;
  }
  public SftpATTRS lstat(String path) throws SftpException{
    try{
     if(!path.startsWith("/")){ path=cwd+"/"+path; }
      sendLSTAT(path.getBytes());
      buf.rewind();
      int i=io.in.read(buf.buffer, 0, buf.buffer.length);
      int length=buf.getInt();
      int type=buf.getByte();
      if(type!=SSH_FXP_ATTRS){
	if(type==SSH_FXP_STATUS){
	  buf.getInt();
	  i=buf.getInt();
	  byte[] str=buf.getString();
	  throw new SftpException(i, new String(str));
	}
	throw new SftpException(SSH_FX_FAILURE, "");
      }
      buf.getInt();
      SftpATTRS attr=SftpATTRS.getATTR(buf);
      return attr;
    }
    catch(Exception e){
      if(e instanceof SftpException) throw (SftpException)e;
      throw new SftpException(SSH_FX_FAILURE, "");
    }
  }

  public String pwd(){ return cwd; }
  public String lpwd(){ return lcwd; }
  public String version(){ return version; }

  private void sendINIT() throws Exception{
    packet.reset();
    putHEAD(SSH_FXP_INIT, 5);
    buf.putInt(3);                // version 3
   session.write(packet, this, 5+4);
  }

  private void sendREALPATH(byte[] path) throws Exception{
    sendPacketPath(SSH_FXP_REALPATH, path);
  }
  private void sendSTAT(byte[] path) throws Exception{
    sendPacketPath(SSH_FXP_STAT, path);
  }
  private void sendLSTAT(byte[] path) throws Exception{
    sendPacketPath(SSH_FXP_LSTAT, path);
  }
  private void sendFSTAT(byte[] handle) throws Exception{
    sendPacketPath(SSH_FXP_FSTAT, handle);
  }
  private void sendSETSTAT(byte[] path, SftpATTRS attr) throws Exception{
    packet.reset();
    putHEAD(SSH_FXP_SETSTAT, 9+path.length+attr.length());
    buf.putInt(count++);
    buf.putString(path);             // path
    attr.dump(buf);
    session.write(packet, this, 9+path.length+attr.length()+4);
  }
  private void sendREMOVE(byte[] path) throws Exception{
    sendPacketPath(SSH_FXP_REMOVE, path);
  }
  private void sendMKDIR(byte[] path, SftpATTRS attr) throws Exception{
    packet.reset();
    putHEAD(SSH_FXP_MKDIR, 9+path.length+(attr!=null?attr.length():4));
    buf.putInt(count++);
    buf.putString(path);             // path
    if(attr!=null) attr.dump(buf);
    else buf.putInt(0);
    session.write(packet, this, 9+path.length+(attr!=null?attr.length():4)+4);
  }
  private void sendRMDIR(byte[] path) throws Exception{
    sendPacketPath(SSH_FXP_RMDIR, path);
  }
  private void sendSYMLINK(byte[] p1, byte[] p2) throws Exception{
    sendPacketPath(SSH_FXP_SYMLINK, p1, p2);
  }
  private void sendREADLINK(byte[] path) throws Exception{
    sendPacketPath(SSH_FXP_READLINK, path);
  }
  private void sendOPENDIR(byte[] path) throws Exception{
    sendPacketPath(SSH_FXP_OPENDIR, path);
  }
  private void sendREADDIR(byte[] path) throws Exception{
    sendPacketPath(SSH_FXP_READDIR, path);
  }
  private void sendRENAME(byte[] p1, byte[] p2) throws Exception{
    sendPacketPath(SSH_FXP_RENAME, p1, p2);
  }
  private void sendCLOSE(byte[] path) throws Exception{
    sendPacketPath(SSH_FXP_CLOSE, path);
  }
  private void sendOPENR(byte[] path) throws Exception{
    sendOPEN(path, SSH_FXF_READ);
  }
  private void sendOPENW(byte[] path) throws Exception{
    sendOPEN(path, SSH_FXF_WRITE|SSH_FXF_CREAT|SSH_FXF_TRUNC);
  }
  private void sendOPEN(byte[] path, int mode) throws Exception{
    packet.reset();
    putHEAD(SSH_FXP_OPEN, 17+path.length);
    buf.putInt(count++);
    buf.putString(path);
    buf.putInt(mode);
    buf.putInt(0);           // attrs
    session.write(packet, this, 17+path.length+4);
  }
  private void sendPacketPath(byte fxp, byte[] path) throws Exception{
    packet.reset();
    putHEAD(fxp, 9+path.length);
    buf.putInt(count++);
    buf.putString(path);             // path
    session.write(packet, this, 9+path.length+4);
  }
  private void sendPacketPath(byte fxp, byte[] p1, byte[] p2) throws Exception{
    packet.reset();
    putHEAD(fxp, 13+p1.length+p2.length);
    buf.putInt(count++);
    buf.putString(p1);
    buf.putString(p2);
    session.write(packet, this, 13+p1.length+p2.length+4);
  }

  private void sendWRITE(byte[] handle, long offset, 
			 byte[] data, int start, int length) throws Exception{
    packet.reset();
    putHEAD(SSH_FXP_WRITE, 21+handle.length+length);
    buf.putInt(count++);
    buf.putString(handle);
    buf.putLong(offset);
    buf.putString(data, start, length);
    session.write(packet, this, 21+handle.length+length+4);
  }

  private void sendREAD(byte[] handle, long offset, int length) throws Exception{
    packet.reset();
    putHEAD(SSH_FXP_READ, 21+handle.length);
    buf.putInt(count++);
    buf.putString(handle);
    buf.putLong(offset);
    buf.putInt(length);
    session.write(packet, this, 21+handle.length+4);
  }

  private void putHEAD(byte type, int length) throws Exception{
    buf.putByte((byte)Session.SSH_MSG_CHANNEL_DATA);
    buf.putInt(recipient);
    buf.putInt(length+4);
    buf.putInt(length);
    buf.putByte(type);
  }
  private Vector glob_remote(String _path) throws Exception{
//System.out.println("glob_remote: "+_path);
    Vector v=new Vector();
    byte[] path=_path.getBytes();
    int i=path.length-1;
    while(i>=0){if(path[i]=='*' || path[i]=='?')break;i--;}
    if(i<0){ v.addElement(_path); return v;}
    while(i>=0){if(path[i]=='/')break;i--;}
    if(i<0){ v.addElement(_path); return v;}
    byte[] dir;
    if(i==0){dir=new byte[]{(byte)'/'};}
    else{ 
      dir=new byte[i];
      System.arraycopy(path, 0, dir, 0, i);
    }
//System.out.println("dir: "+new String(dir));
    byte[] pattern=new byte[path.length-i-1];
    System.arraycopy(path, i+1, pattern, 0, pattern.length);
//System.out.println("file: "+new String(pattern));

    sendOPENDIR(dir);

    buf.rewind();
    i=io.in.read(buf.buffer, 0, buf.buffer.length);
    int length=buf.getInt();
    int type=buf.getByte();
    if(type!=SSH_FXP_STATUS && type!=SSH_FXP_HANDLE){
      throw new SftpException(SSH_FX_FAILURE, "");
    }
    if(type==SSH_FXP_STATUS){
      buf.getInt();
      i=buf.getInt();
      byte[] str=buf.getString();
      throw new SftpException(i, new String(str));
    }
    buf.getInt();
    byte[] handle=buf.getString();         // filename

    while(true){
      sendREADDIR(handle);
      buf.rewind();
      i=io.in.read(buf.buffer, 0, buf.buffer.length);
      buf.index=i;
      length=buf.getInt();
      length=length-(i-4);
      type=buf.getByte();

      if(type!=SSH_FXP_STATUS && type!=SSH_FXP_NAME){
	throw new SftpException(SSH_FX_FAILURE, "");
      }
      if(type==SSH_FXP_STATUS){ 
	break;
      }

      buf.getInt();
      int count=buf.getInt();

      byte[] str;
      int flags;

      while(count>0){
	if(length>0){
	  buf.shift();
	  i=io.in.read(buf.buffer, buf.index, buf.buffer.length-buf.index);
	  if(i<=0)break;
	  buf.index+=i;
	  length-=i;
	}

	byte[] filename=buf.getString();
	//System.out.println("filename: "+new String(filename));
	str=buf.getString();
	SftpATTRS attrs=SftpATTRS.getATTR(buf);

	if(glob(pattern, filename)){
	  v.addElement(new String(dir)+"/"+new String(filename));
	}

	count--; 
      }
    }

    sendCLOSE(handle);
    buf.rewind();
    i=io.in.read(buf.buffer, 0, buf.buffer.length);
    length=buf.getInt();
    type=buf.getByte();
    if(type!=SSH_FXP_STATUS){
      throw new SftpException(SSH_FX_FAILURE, "");
    }
    buf.getInt();
    i=buf.getInt();
    if(i==SSH_FX_OK) return v;

    return null;
  }
  private boolean glob(byte[] pattern, byte[] name){
    int patternlen=pattern.length;
    if(patternlen==0)
      return false;
    int namelen=name.length;
    int i=0;
    int j=0;
    while(i<patternlen && j<namelen){
      if(pattern[i]=='\\'){
	if(i+1==patternlen)
	  return false;
	i++;
	if(pattern[i]!=name[j]) return false;
	i++; j++;
	continue;
      }
      if(pattern[i]=='*'){
	if(patternlen==i+1) return true;
	i++;
	byte foo=pattern[i];
	while(j<namelen){
	  if(foo==name[j])break;
	  j++;
	}
	if(j==namelen) return false;
	i++; j++;
	continue;
      }
      if(pattern[i]=='?'){
	i++; j++;
	continue;
      }
      if(pattern[i]!=name[j]) return false;
      i++; j++;
      continue;
    }
    if(i==patternlen && j==namelen) return true;
    return false;
  }
  /*
   * Class: Ssh_exp_name
   *
   * Represents the result of a query about filenames (e.g. FXP_OPENDIR+ FXP_READDIR )
   */
  public static class Ssh_exp_name {
    private  String filename;
    private  String longname;
    private  SftpATTRS attrs;
    Ssh_exp_name(String filename,
		 String longname,
		 SftpATTRS attrs){
      setFilename(filename);
      setLongname(longname);
      setAttrs(attrs);
    }
    public String getFilename(){return filename;};
    public void setFilename(String filename){this.filename = filename;};
    public String getLongname(){return longname;};
    public void setLongname(String longname){this.longname = longname;};
    public SftpATTRS getAttrs(){return attrs;};
    public void setAttrs(SftpATTRS attrs) {this.attrs = attrs;};
    public String toString(){
      return (attrs.toString()+" "+filename);
    }
  }
}
