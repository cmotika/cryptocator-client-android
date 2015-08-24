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
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.EditText;

public class AddUserEditText extends EditText {
	
	private AddUserEditTextKeyListener listener;
	
	public void setKeyListener(AddUserEditTextKeyListener listener) {
		this.listener = listener;
	}
	
	public interface AddUserEditTextKeyListener {
		public boolean keyEvent(int keyCode, KeyEvent event);
	}
	
	public AddUserEditText(Context context) {
		super(context);
	}
	
    public AddUserEditText(Context context, AttributeSet attrs) {
    	super(context, attrs);
    }

    public AddUserEditText(Context context, AttributeSet attrs, int defStyle) {
    	super(context, attrs, defStyle);
    }


	public boolean onKeyPreIme(int keyCode, KeyEvent event) {
		boolean superHandling = true;
		if (listener != null) {
			 superHandling = listener.keyEvent(keyCode, event);
		}
//	    if (keyCode == KeyEvent.KEYCODE_BACK && 
//	        event.getAction() == KeyEvent.ACTION_UP) {
//	            // do your stuff
//	            return false;
//	    }
		if (superHandling) {
			return super.dispatchKeyEvent(event);
		} else {
			return false;
		}
	}
}
