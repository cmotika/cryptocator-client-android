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
 * notice, an acknowledgment to all contributors, this list of conditions
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

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
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
 * SelectionListener presents the user entered return text as a String.<BR>
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

	/** The BUTTONO k0. */
	public static int BUTTONOK0 = 0;
	
	/** The BUTTONO k1. */
	public static int BUTTONOK1 = 1;
	
	/** The buttoncancel. */
	public static int BUTTONCANCEL = 2;

	/**
	 * The listener interface for receiving onSelection events.
	 * The class that is interested in processing a onSelection
	 * event implements this interface, and the object created
	 * with that class is registered with a component using the
	 * component's <code>addOnSelectionListener<code> method. When
	 * the onSelection event occurs, that object's appropriate
	 * method is invoked.
	 *
	 * @see OnSelectionEvent
	 */
	public interface OnSelectionListener {
		
		/**
		 * Selected.
		 *
		 * @param dialog the dialog
		 * @param button the button
		 * @param cancel the cancel
		 * @param returnText the return text
		 */
		void selected(MessageInputDialog dialog, int button, boolean cancel,
				String returnText);
	}

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

	/** The handled. */
	boolean handled = false;

	/** The cancel. */
	boolean cancel = true;

	/** The context. */
	Context context = null;
	
	/** The selection listener. */
	OnSelectionListener selectionListener;

	// -------------------------------------------------------------------------

	/**
	 * Instantiates a new message input dialog.
	 *
	 * @param context the context
	 * @param titleMessage the title message
	 * @param textMessage the text message
	 * @param okButton0 the ok button0
	 * @param okButton1 the ok button1
	 * @param cancelButton the cancel button
	 * @param defaultText the default text
	 * @param selectionListener the selection listener
	 */
	public MessageInputDialog(Context context, String titleMessage,
			String textMessage, String okButton0, String okButton1,
			String cancelButton, String defaultText,
			OnSelectionListener selectionListener) {
		super(context, R.style.AlertDialogCustom);
		this.context = context;
		this.titleMessage = titleMessage;
		this.textMessage = textMessage;
		this.trueButton = okButton0;
		this.neutralButton = okButton1;
		this.falseButton = cancelButton;
		this.selectionListener = selectionListener;
		this.returnText = defaultText;
	}

	// -------------------------------------------------------------------------

	/**
	 * Sets the text.
	 *
	 * @param newText the new text
	 * @param showKeyboard the show keyboard
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

	/* (non-Javadoc)
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

		// outerLayout.addView(buttonLayout);

		LinearLayout.LayoutParams lpScrollView = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.FILL_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT, .8f);
		// lpScrollView.setMargins(20, 5, 0, 15);
		ScrollView scrollView = new ScrollView(context);
		scrollView.setLayoutParams(lpScrollView);
		scrollView.addView(outerLayout);

		final LinearLayout dialogLayout = new LinearLayout(context);
		dialogLayout.setOrientation(LinearLayout.VERTICAL);
		dialogLayout.addView(scrollView);
		dialogLayout.addView(buttonLayout);

		setContentView(dialogLayout); // do this for Dialog (not AlertDialog)
		// setView(outerLayout);
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

		// View decor = this.getWindow().getDecorView();
		// decor.get
		// .setBackgroundColor(Color.rgb(255, 255, 255));

		// (new ContextThemeWrapper(this, R.style.AlertDialogCustom));

		// setContentView(com.android.internal.R.style.Theme_Dialog_Alert);
		//

		// super.setTheme(android.R.style.Theme_Dialog);
		// ContextThemeWrapper wrapper = new ContextThemeWrapper(context,
		// R.style.AlertDialogCustom);
		// wrapper.

		// this.getLayoutInflater().inflate(R.style.AlertDialogCustom,
		// this.getWindow().getDecorView());
		// setContentView(R.style.AlertDialogCustom);

		// set dolphin
		Utility.setBackground(context, outerLayout, R.drawable.dolphins3);

		// resolveDialogScheme("com.android.internal.R.style.Theme_Dialog_Alert")
		// getWindow().setBackgroundDrawable(new ColorDrawable(Color.WHITE));

		// SHOW DIALOG AT THE TOP POSITION BECAUSE OTHERWISE IT MIGHT GET
		// PROBLEMS WITH KEYBOARD
		// SHOWING AT THE BOTTOM

		WindowManager.LayoutParams wmlp = dialog.getWindow().getAttributes();
		wmlp.gravity = Gravity.TOP;
		wmlp.y = 10; // y position
		// wmlp.gravity = Gravity.TOP | Gravity.LEFT;
		// wmlp.x = 100; //x position

		if (returnText != null && returnText.length() > 0) {
			inputText.setText(returnText);
			inputText.setSelection(0, returnText.length());
		} else {
			scrollView.postDelayed(new Runnable() {
				public void run() {
					inputText.requestFocus();
					Utility.showKeyboardExplicit(inputText);
				}
			}, 100);
		}

		// dialogLayout.getViewTreeObserver().addOnGlobalLayoutListener(
		// new ViewTreeObserver.OnGlobalLayoutListener() {
		// public void onGlobalLayout() {
		// c++;
		// if (isKeyboardVisible(dialogLayout)) {
		// dialog.setTitle("KEYBOARD ON " + c);
		// dialogLayout.setPadding(0, 0, 0, 200);
		// } else {
		// dialog.setTitle("KEYBOARD OFF " + c);
		// dialogLayout.setPadding(0, 0, 0, 0);
		// }
		// }
		// });

		// // Make sure the dialog is shifted up when the keyboard is shown!
		// // e.g. in the inner view!
		// this.getWindow().setSoftInputMode(
		// WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
	}

	// int c = 0;
	//
	// //
	// -------------------------------------------------------------------------
	// private static int minWidthDiff = 100;
	// private static int mLastHeightDifferece;
	// private static boolean keyboardVisible = false;
	//
	// private boolean isKeyboardVisible(View rootView) {
	// // get screen frame rectangle
	// Rect r = new Rect();
	// rootView.getWindowVisibleDisplayFrame(r);
	// // get screen height
	// int screenHeight = rootView.getRootView().getHeight();
	// // calculate the height difference
	// int heightDifference = screenHeight - (r.bottom - r.top);
	//
	// // Log.d("communicator",
	// // "@@@@ heightDifference =" + heightDifference +
	// // ", mLastHeightDifferece = "+mLastHeightDifferece+" , screenHeight = "
	// // + screenHeight);
	//
	// // if height difference is different then the last height difference and
	// // is bigger then a third of the screen we can assume the keyboard is
	// // open
	// if (heightDifference != mLastHeightDifferece) {
	// if (heightDifference > screenHeight / 4
	// && ((heightDifference > mLastHeightDifferece + minWidthDiff) ||
	// (heightDifference < mLastHeightDifferece
	// - minWidthDiff))) {
	// // keyboard visiblevisible
	// // Log.d("communicator", "@@@@@@ CHANGE TO VISIBLE=TRUE");
	// mLastHeightDifferece = heightDifference;
	// keyboardVisible = true;
	// } else if (heightDifference < screenHeight / 4) {
	// // Log.d("communicator", "@@@@@@ CHANGE TO VISIBLE=FALSE");
	// // keyboard hidden
	// mLastHeightDifferece = heightDifference;
	// keyboardVisible = false;
	// }
	// }
	// return keyboardVisible;
	// }
}
