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
FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
VISIGOTH SOFTWARE SOCIETY OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
