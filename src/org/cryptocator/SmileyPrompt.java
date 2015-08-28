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

import android.app.Dialog;
import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;

/**
 * The SmileyPrompt class prompts for inserting smileys as defined in
 * SmileyEditText.
 * 
 * @author Christian Motika
 * @date 08/23/2015
 * @since 1.2
 * 
 */
public class SmileyPrompt {

	/** The maximal number of smileys per row in landscape mode. */
	final int MAXSMILEYCOLUMNSLANDSCAPE = 5;

	/** The maximal number of smileys per row in non-landscape mode. */
	final int MAXSMILEYCOLUMNSPORTRAIT = 3;

	/** The smiley selected listener. */
	OnSmileySelectedListener onSmileySelectedListener = null;

	// ------------------------------------------------------------------------

	public interface OnSmileySelectedListener {

		/**
		 * If a smiley is selected the textualSmiley String holds the textual
		 * representation of the smiley. If no smiley is selected the parameter
		 * is null.
		 *
		 * @param textualSmiley the textual smiley
		 */
		void onSelect(String textualSmiley);
	}

	// ------------------------------------------------------------------------

	/**
	 * Sets the on smiley selected listener.
	 * 
	 * @param onSmileySelectedListener
	 *            the new on smiley selected listener
	 */
	public void setOnSmileySelectedListener(
			OnSmileySelectedListener onSmileySelectedListener) {
		this.onSmileySelectedListener = onSmileySelectedListener;
	}

	// ------------------------------------------------------------------------

	/**
	 * Send message prompt.
	 * 
	 * @param context
	 *            the context
	 * @param sms
	 *            the sms
	 * @param encryption
	 *            the encryption
	 */
	public void promptSmileys(final Context context) {
		String title = "Insert Smiley";
		String text = null;

		new MessageAlertDialog(context, title, text, null, null, " Cancel ",
				new MessageAlertDialog.OnSelectionListener() {
					public void selected(int button, boolean cancel) {
						// nothing
					}
				}, new MessageAlertDialog.OnInnerViewProvider() {
					public View provide(final MessageAlertDialog dialog) {
						View smileyLayout = buildSmileyTable(context, dialog);
						return smileyLayout;
					}
				}).show();
	}

	// ------------------------------------------------------------------------

	/**
	 * Builds the smiley table layout.
	 * 
	 * @param context
	 *            the context
	 * @return the table layout
	 */
	private LinearLayout buildSmileyTable(final Context context,
			final Dialog dialog) {
		int smileys = ImageSmileyEditText.smileyText.length;
		// int maxrowsandcolumns = (int) Math.ceil(Math.sqrt(smileys));

		int maxcolumns = MAXSMILEYCOLUMNSPORTRAIT;
		if (Utility.isOrientationLandscape(context)) {
			maxcolumns = MAXSMILEYCOLUMNSLANDSCAPE;
		}

		int maxrows = (int) Math.ceil(((double) smileys)
				/ ((double) maxcolumns));

		Log.d("communicator", "SMILEY: smileys=" + smileys);
		Log.d("communicator", "SMILEY: maxrows=" + maxrows);

		LinearLayout smileyLayout = new LinearLayout(context);
		smileyLayout.setOrientation(LinearLayout.VERTICAL);
		smileyLayout.setGravity(Gravity.CENTER_HORIZONTAL);
		TableLayout tableLayout = new TableLayout(context);
		smileyLayout.addView(tableLayout);
		tableLayout.setGravity(Gravity.CENTER_HORIZONTAL);

		TableLayout.LayoutParams lpTable = new TableLayout.LayoutParams(
				TableLayout.LayoutParams.WRAP_CONTENT,
				TableLayout.LayoutParams.WRAP_CONTENT);
		lpTable.setMargins(5, 10, 5, 10);
		tableLayout.setLayoutParams(lpTable);

		TableRow.LayoutParams lpRows = new TableRow.LayoutParams(
				TableRow.LayoutParams.WRAP_CONTENT,
				TableRow.LayoutParams.WRAP_CONTENT);

		TableRow.LayoutParams lpButtons = new TableRow.LayoutParams(120, 120);
		lpButtons.setMargins(0, 0, 0, 0);

		// Initialize index to -1 in order to start with 0
		int index = 0;
		for (int row = 0; row < maxrows; row++) {
			TableRow tableRow = new TableRow(context);
			tableRow.setLayoutParams(lpRows);
			for (int column = 0; column < maxcolumns; column++) {
				if (index < smileys) {

					final int orderNumber = ImageSmileyEditText.smileyOrder[index];

					ImageLabelButton smileButton = new ImageLabelButton(context);
					String smileyImg = "s" + orderNumber;
					//Log.d("communicator", "SMILEY["+index+"] " + smileyImg);
					int id = context.getResources().getIdentifier(smileyImg,
							"drawable", context.getPackageName());
					//Log.d("communicator", "SMILEY["+index+"] id=" + id);
					smileButton.setTextAndImageResource(
							ImageSmileyEditText.smileyLabel[orderNumber], id);
					smileButton.setLayoutParams(lpButtons);
					smileButton.setOnClickListener(new View.OnClickListener() {
						public void onClick(View v) {
							if (onSmileySelectedListener != null) {
								onSmileySelectedListener
										.onSelect(ImageSmileyEditText.smileyText[orderNumber]);
							}
							dialog.dismiss();
						}
					});
					tableRow.addView(smileButton);
				}
				index++;
			}
			tableLayout.addView(tableRow);
		}

		return smileyLayout;
	}

	// ------------------------------------------------------------------------

}
