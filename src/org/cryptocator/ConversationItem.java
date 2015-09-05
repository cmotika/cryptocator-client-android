/*
 * Copyright (c) 2015, Christian Motika. Dedicated to Sara.
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
 * notice, an acknowledgment to all contributors, this list of conditions
 * and the following disclaimer in the documentation and/or other materials
 * provided with the distribution.
 * 
 * 3. Neither the name Delphino Cryptocator nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * 4. Free or commercial forks of Cryptocator are permitted as long as
 *    both (a) and (b) are and stay fulfilled. 
 *    (a) this license is enclosed
 *    (b) the protocol to communicate between Cryptocator servers
 *        and Cryptocator clients *MUST* must be fully conform with 
 *        the documentation and (possibly updated) reference 
 *        implementation from cryptocator.org. This is to ensure 
 *        interconnectivity between all clients and servers. 
 * 
 * THIS SOFTWARE IS PROVIDED BY THE CONTRIBUTORS “AS IS” AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE CONTRIBUTORS
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


/**
 * The ConversationItem class is a fast data structure to represent an item in a
 * conversation as read from the database or send/received to/from the server.
 * It is more a struct than a class and for fast processing the objects are made
 * public without any getter/setter methods.
 * 
 * @author Christian Motika
 * @since 1.2
 * @date 08/23/2015
 * 
 */
public class ConversationItem {

	/**
	 * The sending id is the id of this conversation item in the SENDING
	 * table/database.
	 */
	public int sendingid = -1;

	/**
	 * The local id is the local id of this conversation item in the MESSAGES
	 * table/database for the particular user.
	 */
	public int localid;

	/**
	 * The mid is the global mid as uniquely determined by the server. It is -1
	 * if there is no such id yet.
	 */
	public int mid;

	/** The from uid of the sender. */
	public int from;

	/** The to uid of the recipient. */
	public int to;

	/** The message text. */
	public String text;

	/** The created timestamp. */
	public long created;

	/** The sent timestamp. */
	public long sent;

	/** The received timestamp. */
	public long received;

	/** The read timestamp. */
	public long read;

	/** The withdraw timestamp. */
	public long withdraw;

	/** The encrypted flag. */
	public boolean encrypted = false;

	/** The (desired) transport, i.e., 0 = Internet or 1 = SMS. */
	public int transport = 0;

	/** The flag for a session key message. */
	public boolean isKey = false;

	/**
	 * The flag for a system message, e.g., read confirmations or withdraw
	 * requests.
	 */
	public boolean system = false;

	/**
	 * The fail counter for sms that could not be sent because of non-network
	 * errors. E.g., invalid phone number.
	 */
	public int smsfailcnt = 0;

	/** The flag for telling that the SMS permanently failed to be sent. */
	public boolean smsfailed = false;

	/** The lasttry timestamp. */
	public long lasttry;

	/** The number of tries for sending this message. */
	public int tries = 0;

	/**
	 * The total number of parts. If this is a multipart message this is > 1,
	 * otherwise it is 1.
	 */
	public int parts = 1;

	/**
	 * The part of this message. If this is a multipart message this can be > 0,
	 * otherwise it is 0.
	 */
	public int part = DB.DEFAULT_MESSAGEPART;

	/**
	 * The unique id for a multipart message. empty string if no multipart
	 * message
	 */
	public String multipartid = DB.NO_MULTIPART_ID;

	// ------------------------------------------------------------------------

	/**
	 * Instantiates a new conversation item.
	 */
	public ConversationItem() {
		localid = -1;
		from = -1;
		to = -1;
		mid = -1;
		this.text = "";
		created = -1;
		received = -1;
		sent = -1;
		read = -1;
	}

	// ------------------------------------------------------------------------

	/**
	 * Instantiates a new conversation item.
	 * 
	 * @param text
	 *            the text
	 * @param me
	 *            the me
	 */
	public ConversationItem(String text, boolean me) {
		mid = -1;
		from = -1;
		to = -1;
		this.text = text;
		created = -1;
		received = -1;
		sent = -1;
		read = -1;
	}

	// ------------------------------------------------------------------------

	/**
	 * Me tells if the sender of this conversation item is the current user of
	 * the device. This is a new convention since Version 1.3. Do not save a
	 * "real" uid for "me" any more because for several servers we could have
	 * different uids which makes it hard to compare.
	 * 
	 * @param context
	 *            the context
	 * @return true, if successful
	 */
	public boolean me() {
		return (from == DB.DEFAULT_MYSELF_UID);
	}

	// ------------------------------------------------------------------------

	/**
	 * A conversationitem is ready to be processed if it is NON a multipart
	 * message or if it is a multipart message that has been combined. Combined
	 * multipart messages only have set parts == 1.
	 * 
	 * @return true, if successful
	 */
	public boolean readyToProcess() {
		return (parts == 1);
	}

	// ------------------------------------------------------------------------

}
