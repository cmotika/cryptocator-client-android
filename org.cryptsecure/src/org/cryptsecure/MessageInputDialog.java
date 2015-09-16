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

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * The MessageInputDialog class is responsible for displaying a dialog with a
 * title and text information and up to three buttons: At most two OK buttons
 * and a CANCEL button. Additionally it has an input text field. The
 * SelectionListener presents the user entered return text as a String. The
 * dialog will be presented at the top of the screen (y axis) to prevent the
 * keyboard might from hiding parts of it.<BR>
 * <BR>
 * The MessageInputDialog needs the following permission: <uses-permission
 * android:name="android.permission.SYSTEM_ALERT_WINDOW" />
 * 
 * @author Christian Motika
 * @since 1.2
 * @date 08/23/2015
 * 
 */
public class MessageInputDialog extends Dialog {

	/** The fist OK BUTTONO. */
	public static int BUTTONOK0 = 0;

	/** The second OK BUTTON. */
	public static int BUTTONOK1 = 1;

	/** The CANCEL button. */
	public static int BUTTONCANCEL = 2;

	// ------------------------------------------------------------------------

	/**
	 * The listener interface for receiving onSelection events. The class that
	 * is interested in processing a onSelection event implements this
	 * interface, and the object created with that class is registered with a
	 * component using the component's
	 * <code>addOnSelectionListener<code> method. When
	 * the onSelection event occurs, that object's appropriate
	 * method is invoked.
	 * 
	 * @see OnSelectionEvent
	 */
	public interface OnSelectionListener {

		/**
		 * Selected.
		 * 
		 * @param dialog
		 *            the dialog
		 * @param button
		 *            the button
		 * @param cancel
		 *            the cancel
		 * @param returnText
		 *            the return text
		 */
		void selected(MessageInputDialog dialog, int button, boolean cancel,
				String returnText);
	}

	// ------------------------------------------------------------------------

	/** The title message. */
	String titleMessage;

	/** The text message. */
	String textMessage;

	/** The true button. */
	String trueButton;

	/** The neutral button. */
	String neutralButton;

	/** The false button. */
	String falseButton;

	/** The return text. */
	String returnText;

	/** The input text. */
	EditText inputText = null;

	/** The input type of the text field. */
	int inputType = InputType.TYPE_CLASS_TEXT;

	/** The handled. */
	boolean handled = false;

	/** The cancel. */
	boolean cancel = true;

	/** The context. */
	Context context = null;

	/** The selection listener. */
	OnSelectionListener selectionListener;

	/** The show the keyboard when showing dialog. */
	boolean showKeyboardOnShow;
	
	/** The select the text when showing dialog. */
	boolean selectOnShow;
	
	// -------------------------------------------------------------------------

	/**
	 * Instantiates a new message input dialog with input type text.
	 * 
	 * @param context
	 *            the context
	 * @param titleMessage
	 *            the title message
	 * @param textMessage
	 *            the text message
	 * @param okButton0
	 *            the ok button0
	 * @param okButton1
	 *            the ok button1
	 * @param cancelButton
	 *            the cancel button
	 * @param defaultText
	 *            the default text
	 * @param selectionListener
	 *            the selection listener
	 */
	public MessageInputDialog(Context context, String titleMessage,
			String textMessage, String okButton0, String okButton1,
			String cancelButton, String defaultText,
			OnSelectionListener selectionListener) {
		this(context, titleMessage, textMessage, okButton0, okButton1,
				cancelButton, defaultText, selectionListener,
				InputType.TYPE_CLASS_TEXT, true, true);
	}

	// -------------------------------------------------------------------------

	/**
	 * Instantiates a new message input dialog with user defined input type.
	 * 
	 * @param context
	 *            the context
	 * @param titleMessage
	 *            the title message
	 * @param textMessage
	 *            the text message
	 * @param okButton0
	 *            the ok button0
	 * @param okButton1
	 *            the ok button1
	 * @param cancelButton
	 *            the cancel button
	 * @param defaultText
	 *            the default text
	 * @param selectionListener
	 *            the selection listener
	 */
	public MessageInputDialog(Context context, String titleMessage,
			String textMessage, String okButton0, String okButton1,
			String cancelButton, String defaultText,
			OnSelectionListener selectionListener, int inputType, boolean selectOnShow, boolean showKeyboardOnShow) {
		super(context, R.style.AlertDialogCustom);
		this.context = context;
		this.titleMessage = titleMessage;
		this.textMessage = textMessage;
		this.trueButton = okButton0;
		this.neutralButton = okButton1;
		this.falseButton = cancelButton;
		this.selectionListener = selectionListener;
		this.returnText = defaultText;
		this.inputType = inputType;
		this.selectOnShow = selectOnShow;
		this.showKeyboardOnShow = showKeyboardOnShow;
	}

	// -------------------------------------------------------------------------

	/**
	 * Sets the text.
	 * 
	 * @param newText
	 *            the new text
	 * @param showKeyboard
	 *            the show keyboard
	 */
	public void setText(String newText, boolean showKeyboard) {
		if (newText != null) {
			inputText.setText(newText);
			inputText.setSelection(0, newText.length());
		}
		if (showKeyboard) {
			inputText.postDelayed(new Runnable() {
				public void run() {
					inputText.requestFocus();
					Utility.showKeyboardExplicit(inputText);
				}
			}, 100);
		}
	}

	// -------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Dialog#onCreate(android.os.Bundle)
	 */
	// @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		handled = false;

		final MessageInputDialog dialog = this;
		Context context = this.getContext();
		// NEEDS : <uses-permission
		// android:name="android.permission.SYSTEM_ALERT_WINDOW" />
		this.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);

		dialog.getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

		LinearLayout outerLayout = new LinearLayout(context);
		outerLayout.setOrientation(LinearLayout.VERTICAL);
		outerLayout.setGravity(Gravity.CENTER_HORIZONTAL);

		if (textMessage != null) {
			TextView textView = new TextView(context);
			textView.setTextSize(17);
			LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT,
					LinearLayout.LayoutParams.WRAP_CONTENT);
			lp2.setMargins(25, 25, 25, 0);
			textView.setLayoutParams(lp2);
			textView.setText(textMessage);
			outerLayout.addView(textView);
		}

		inputText = new EditText(context);
		inputText.setInputType(inputType);
		LinearLayout.LayoutParams lpEditText = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		lpEditText.setMargins(25, 25, 25, 25);
		inputText.setLayoutParams(lpEditText);
		outerLayout.addView(inputText);

		LinearLayout buttonLayout = new LinearLayout(context);
		buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
		buttonLayout.setGravity(Gravity.CENTER_HORIZONTAL);
		buttonLayout.setBackgroundColor(Color.rgb(150, 150, 150));

		Button okButton = new Button(context);
		okButton.setVisibility(View.GONE);
		buttonLayout.addView(okButton);
		if (trueButton != null) {
			okButton.setVisibility(View.VISIBLE);
			okButton.setText(trueButton);
			okButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					handled = true;
					returnText = inputText.getText().toString();
					selectionListener.selected(dialog, BUTTONOK0, false,
							returnText);
				}
			});
		}

		Button okButton2 = new Button(context);
		okButton2.setVisibility(View.GONE);
		buttonLayout.addView(okButton2);
		if (neutralButton != null) {
			okButton2.setVisibility(View.VISIBLE);
			okButton2.setText(neutralButton);
			okButton2.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					handled = true;
					returnText = inputText.getText().toString();
					selectionListener.selected(dialog, BUTTONOK1, false,
							returnText);
				}
			});
		}

		Button cancelButton = new Button(context);
		cancelButton.setVisibility(View.GONE);
		buttonLayout.addView(cancelButton);
		if (falseButton != null) {
			cancelButton.setVisibility(View.VISIBLE);
			cancelButton.setText(falseButton);
			cancelButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					handled = true;
					returnText = inputText.getText().toString();
					selectionListener.selected(dialog, BUTTONCANCEL, true,
							returnText);
				}
			});
		}

		this.setOnDismissListener(new OnDismissListener() {
			public void onDismiss(DialogInterface arg0) {
				try {
					if (!handled) {
						selectionListener.selected(null, -1, true, inputText
								.getText().toString());
					}
				} catch (Exception e) {
					// not crash here!
				}
				try {
					dismiss();
				} catch (Exception e) {
					// not crash here!
				}
			}
		});

		LinearLayout.LayoutParams lpScrollView = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT, .8f);
		ScrollView scrollView = new ScrollView(context);
		scrollView.setLayoutParams(lpScrollView);
		scrollView.addView(outerLayout);

		final LinearLayout dialogLayout = new LinearLayout(context);
		dialogLayout.setOrientation(LinearLayout.VERTICAL);
		dialogLayout.addView(scrollView);
		dialogLayout.addView(buttonLayout);

		setContentView(dialogLayout);
		setTitle(titleMessage);

		LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT, 1);
		lp1.setMargins(10, 10, 5, 10);
		LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT, 1);
		lp2.setMargins(5, 10, 5, 10);
		LinearLayout.LayoutParams lp3 = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT, 1);
		lp3.setMargins(5, 10, 10, 10);
		okButton.setLayoutParams(lp1);
		okButton2.setLayoutParams(lp2);
		cancelButton.setLayoutParams(lp3);

		LayoutParams params = okButton.getLayoutParams();
		params.width = 0;
		params = okButton2.getLayoutParams();
		params.width = 0;
		params = cancelButton.getLayoutParams();
		params.width = 0;

		int buttons = 0;
		if (trueButton != null) {
			buttons++;
		}
		if (neutralButton != null) {
			buttons++;
		}
		if (falseButton != null) {
			buttons++;
		}
		int minWidth = buttons * 200;
		if (minWidth < 450) {
			minWidth = 450;
		}
		outerLayout.setMinimumWidth(minWidth);

		// Set dolphin background
		Utility.setBackground(context, outerLayout, R.drawable.dolphins3);

		// SHOW DIALOG AT THE TOP POSITION BECAUSE OTHERWISE IT MIGHT GET
		// PROBLEMS WITH KEYBOARD
		// SHOWING AT THE BOTTOM

		WindowManager.LayoutParams wmlp = dialog.getWindow().getAttributes();
		wmlp.gravity = Gravity.TOP;
		wmlp.y = 10; // y position
		// wmlp.gravity = Gravity.TOP | Gravity.LEFT;
		// wmlp.x = 100; //x position

		
		inputText.setText(returnText);
		if (selectOnShow) {
			inputText.setSelection(0, returnText.length());
		}
		
		if (showKeyboardOnShow) {
			scrollView.postDelayed(new Runnable() {
				public void run() {
					inputText.requestFocus();
					Utility.showKeyboardExplicit(inputText);
				}
			}, 100);
		} else {
			Utility.hideKeyboardExplicit(inputText);
			scrollView.postDelayed(new Runnable() {
				public void run() {
					Utility.hideKeyboardExplicit(inputText);
				}
			}, 50);
			scrollView.postDelayed(new Runnable() {
				public void run() {
					Utility.hideKeyboardExplicit(inputText);
				}
			}, 100);
		}
	}

	// ------------------------------------------------------------------------

}
