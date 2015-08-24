/*
 * Copyright (c) 2015, Christian Motika.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 * all contributors, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, an acknowledgement to all contributors, this list of conditions
 * and the following disclaimer in the documentation and/or other materials
 * provided with the distribution.
 * 
 * 3. Neither the name Delphino Cryptocator nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS “AS IS” AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, 
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package org.cryptocator;

import android.content.Context;

public class ConversationItem {
	public int sendingid = -1;
	public int localid;
	public int mid;
	public boolean me;
	public int from;
	public int to;
	public String text;
	public long created;
	public long sent;
	public long received;
	public long read;
	public long withdraw;
	public boolean encrypted = false;
	public int transport = 0;                   // 0 = network, 1 = sms
	public boolean isKey = false;
	public boolean system = false;
	public int smsfailcnt = 0;
	public boolean smsfailed = false;

	public ConversationItem() {
		localid = -1;
		from = -1;
		to = -1;
		mid = -1;
		this.text = "";
		//this.me = false;
		created = -1;
		received = -1;
		sent = -1;
		read = -1;
	}

	public ConversationItem(String text, boolean me) {
		mid = -1;
		from = -1;
		to = -1;
		this.text = text;
		//this.me = me;
		created = -1;
		received = -1;
		sent = -1;
		read = -1;
	}
	
	
	public boolean me(Context context) {
		return (from == DB.myUid(context));
		
	}
}
