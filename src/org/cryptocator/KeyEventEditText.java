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

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.EditText;

/**
 * The KeyEventEditText class is a specialized EditText that has an
 * KeyEventEditTextTextKeyListener to listen for keyboard events.
 * 
 * @author Christian Motika
 * @since 2.1
 * @date 08/23/2015
 */
public class KeyEventEditText extends EditText {

	/** The listener for keyboard events. */
	private KeyEventEditTextEditTextKeyListener listener;

	// ------------------------------------------------------------------------

	/**
	 * Sets the key listener for this KeyEventEditText.
	 * 
	 * @param listener
	 *            the new key listener
	 */
	public void setKeyListener(KeyEventEditTextEditTextKeyListener listener) {
		this.listener = listener;
	}

	// ------------------------------------------------------------------------

	/**
	 * The listener interface for receiving keyEventEditTextEditTextKey events.
	 * The class that is interested in processing a keyEventEditTextEditTextKey
	 * event implements this interface, and the object created with that class
	 * is registered with a component using the component's
	 * <code>addKeyEventEditTextEditTextKeyListener<code> method. When
	 * the keyEventEditTextEditTextKey event occurs, that object's appropriate
	 * method is invoked.
	 * 
	 * @see KeyEventEditTextEditTextKeyEvent
	 */
	public interface KeyEventEditTextEditTextKeyListener {

		/**
		 * Key event. Returns true if the event was handled and false if it was
		 * not handled.
		 * 
		 * @param keyCode
		 *            the key code
		 * @param event
		 *            the event
		 * @return true, if successful
		 */
		public boolean keyEvent(int keyCode, KeyEvent event);
	}

	// ------------------------------------------------------------------------

	/**
	 * Instantiates a new key event edit text.
	 * 
	 * @param context
	 *            the context
	 */
	public KeyEventEditText(Context context) {
		super(context);
	}

	// ------------------------------------------------------------------------

	/**
	 * Instantiates a new key event edit text.
	 * 
	 * @param context
	 *            the context
	 * @param attrs
	 *            the attrs
	 */
	public KeyEventEditText(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	// ------------------------------------------------------------------------

	/**
	 * Instantiates a new key event edit text.
	 * 
	 * @param context
	 *            the context
	 * @param attrs
	 *            the attrs
	 * @param defStyle
	 *            the def style
	 */
	public KeyEventEditText(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	// ------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.TextView#onKeyPreIme(int, android.view.KeyEvent)
	 */
	public boolean onKeyPreIme(int keyCode, KeyEvent event) {
		boolean superHandling = true;
		if (listener != null) {
			// If a key listener is set then inform it about the event.
			superHandling = listener.keyEvent(keyCode, event);
		}
		if (superHandling) {
			return super.dispatchKeyEvent(event);
		} else {
			return false;
		}
	}

	// ------------------------------------------------------------------------

}
