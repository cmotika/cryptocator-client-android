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

import org.cryptocator.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MessageAlertDialog extends Dialog {

	public interface OnSelectionListener {
		void selected(int button, boolean cancel);
	}

	public interface OnInnerViewProvider {
		View provide(MessageAlertDialog dialog);
	}

	String titleMessage;
	String textMessage;
	String trueButton;
	String neutralButton;
	String falseButton;

	boolean handled = false;

	boolean cancel = true;

	Context context = null;
	OnSelectionListener selectionListener;
	OnInnerViewProvider innerViewProvider;

	View innerView = null;

	// -------------------------------------------------------------------------

	// -------------------------------------------------------------------------

	public MessageAlertDialog(Context context, String titleMessage,
			String textMessage, String okButton0, String okButton1,
			String cancelButton, OnSelectionListener selectionListener) {
		this(context, titleMessage, textMessage, okButton0, okButton1,
				cancelButton, selectionListener, null);
	}

	// -------------------------------------------------------------------------

	public MessageAlertDialog(Context context, String titleMessage,
			String textMessage, String okButton0, String okButton1,
			String cancelButton, OnSelectionListener selectionListener,
			OnInnerViewProvider innerViewProvider) {
		super(context, R.style.AlertDialogCustom);
		this.context = context;
		this.titleMessage = titleMessage;
		this.textMessage = textMessage;
		this.trueButton = okButton0;
		this.neutralButton = okButton1;
		this.falseButton = cancelButton;
		this.selectionListener = selectionListener;
		this.innerViewProvider = innerViewProvider;
	}

	// -------------------------------------------------------------------------

	// @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		handled = false;

		Context context = this.getContext();
		// NEEDS : <uses-permission
		// android:name="android.permission.SYSTEM_ALERT_WINDOW" />
		this.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);

		LinearLayout outerLayout = new LinearLayout(context);
		outerLayout.setOrientation(LinearLayout.VERTICAL);
		outerLayout.setGravity(Gravity.CENTER_HORIZONTAL);

		if (textMessage != null) {
			TextView textView = new TextView(context);
			textView.setTextSize(17);
			LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT,
					LinearLayout.LayoutParams.WRAP_CONTENT);
			lp2.setMargins(25, 25, 25, 25);
			textView.setLayoutParams(lp2);
			textView.setText(textMessage);
			outerLayout.addView(textView);
		}

		if (innerViewProvider != null) {
			View innerView = innerViewProvider.provide(this);
			if (innerView != null) {
				outerLayout.addView(innerView);
			}
		}

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
					selectionListener.selected(0, false);
					dismiss();
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
					selectionListener.selected(1, false);
					dismiss();
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
					selectionListener.selected(2, true);
					dismiss();
				}
			});
		}

		this.setOnDismissListener(new OnDismissListener() {
			public void onDismiss(DialogInterface arg0) {
				if (!handled) {
					selectionListener.selected(-1, true);
				}
				dismiss();
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

		LinearLayout dialogLayout = new LinearLayout(context);
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

//		// Make sure the dialog is shifted up when the keyboard is shown!
//		this.getWindow().setSoftInputMode(
//			    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
	}

	// -------------------------------------------------------------------------
}
