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
 * 3. Neither the name Delphino CryptSecure nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * 4. Free or commercial forks of CryptSecure are permitted as long as
 *    both (a) and (b) are and stay fulfilled: 
 *    (a) This license is enclosed.
 *    (b) The protocol to communicate between CryptSecure servers
 *        and CryptSecure clients *MUST* must be fully conform with 
 *        the documentation and (possibly updated) reference 
 *        implementation from cryptsecure.org. This is to ensure 
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
package org.cryptsecure;

import android.annotation.SuppressLint;
import java.util.Collections;
import java.util.List;

/**
 * The UidListItem is an item for the extended UID list that needs to be sorted
 * by the timestamp of the last message for the main activity or the name for
 * composing activity (addressbook style).
 * 
 * @author Christian Motika
 * @since 1.2
 * @date 08/23/2015
 */
public class UidListItem implements Comparable<UidListItem> {

	/** The uid. */
	public int uid;

	/** The name. */
	public String name;
	
	/** The is group. */
	public boolean isGroup = false;

	/** The last message timestamp. */
	public Long lastMessageTimestamp;

	/** The last message. */
	public String lastMessage;

	/** The sort by name. */
	private static boolean sortByName = false;

	// ------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@SuppressLint("DefaultLocale")
	public int compareTo(UidListItem other) {
		if (sortByName) {
			// sort by name on request
			return (name.toLowerCase().compareTo(other.name.toLowerCase()));
		} else {
			// default sort by lastMessage
			return -(lastMessageTimestamp.compareTo(other.lastMessageTimestamp));
		}
	}

	// ------------------------------------------------------------------------

	/**
	 * Sort.
	 * 
	 * @param list
	 *            the list
	 * @param sortByName
	 *            the sort by name
	 */
	public static void sort(List<UidListItem> list, boolean sortByName) {
		UidListItem.sortByName = sortByName;
		Collections.sort(list);
	}

	// ------------------------------------------------------------------------

}
