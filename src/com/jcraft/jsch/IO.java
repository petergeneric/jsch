/* -*-mode:java; c-basic-offset:2; -*- */
/* JSch
 * Copyright (C) 2002 ymnk, JCraft,Inc.
 *  
 * Written by: 2002 ymnk<ymnk@jcaft.com>
 *   
 *   
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public License
 * as published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
   
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Library General Public License for more details.
 * 
 * You should have received a copy of the GNU Library General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package com.jcraft.jsch;

import java.io.*;

public class IO{
  InputStream in;
  OutputStream out;

  void setOutputStream(OutputStream out){
    this.out=out;
  }
  void setInputStream(InputStream in){
    this.in=in;
  }
  public void put(Packet p) throws IOException, java.net.SocketException {
    out.write(p.buffer.buffer, 0, p.buffer.index);
    out.flush();
  }
  void put(byte[] array, int begin, int length) throws IOException {
    out.write(array, begin, length);
    out.flush();
  }

  int getByte() throws IOException {
    return in.read()&0xff;
  }

  void getByte(byte[] array) throws IOException {
    getByte(array, 0, array.length);
  }

  void getByte(byte[] array, int begin, int length) throws IOException {
    do {
      int completed = in.read(array, begin, length);
      begin+=completed;
      length-=completed;
      }
    while (length>0);
  }
}
