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
 *    both (a) and (b) are and stay fulfilled: 
 *    (a) This license is enclosed.
 *    (b) The protocol to communicate between Cryptocator servers
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;

/**
 * The ImageSmileyEditText class is a EditText component for displaying messages
 * in the conversation class. It is capable of parsing and substituting textual
 * smileys with graphical ones. The copied text will still have the textual
 * smileys in it for convenience. It registeres all images in
 * Conversation.images (so that we can later swipe between them in fullscreen
 * mode) and it registers a clickable span image context menu from conversation.
 * 
 * Special thanks and acknowledgments to user
 * http://stackoverflow.com/users/755804/18446744073709551615 and his answer in
 * Stackoverflow
 * stackoverflow.com/questions/15352496/how-to-add-image-in-a-textview-text This
 * class is based on his answer.
 * 
 * @author Christian Motika
 * @date 08/23/2015
 * @since 1.2
 * 
 */
public class ImageSmileyEditText extends EditText {

	// /** The constant for mapping order from smiley index number. */
	// public static final int[] smileyOrder = { 5, 0, 1, 3, 7, 6, 2, 4 };
	//
	// /** The constant for mapping label from smiley index number. */
	// public static final String[] smileyLabel = { "biggrin", "frown", "mad",
	// "pleased", "rolleyes", "smile", "tongue", "wink" };
	//
	// /** The constant for mapping text from smiley index number. */
	// public static final String[] smileyText = { "=)", ":(", ">:|", ":]",
	// "^^",
	// ":)", ":P", ";)" };

	private String description = null;

	private String titleAddition = null;

	/**
	 * The is input text field then reduce images to tumbnails and doe not
	 * resize!
	 */
	private boolean isInputTextField = false;

	/** The cut/copy/paste listener. */
	OnCutCopyPasteListener onCutCopyPasteListener = null;

	/** The constant for mapping order from smiley index number. */
	public static final int[] smileyOrder = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
			11, 12, 13, 14 };

	/** The constant for mapping label from smiley index number. */
	public static final String[] smileyLabel = { "smile", "frown", "drained",
			"desperate", "tongue", "wink", "cheer", "biggrin", "loving",
			"no clue", "marveled", "tired", "faithful", "sobbing", "rolleyes" };

	/** The constant for mapping text from smiley index number. */
	public static final String[] smileyText = { ":-)", ":-(", ":-B", ":-/",
			":-Z", ";-)", ":~f", ":-D", ":-K", ":~b", ":-8", ":-M", ":-A",
			":~C", ":~d" };

	/** The constant spannableFactory. */
	private static final Spannable.Factory spannableFactory = Spannable.Factory
			.getInstance();

	/** The max withd of the text field. */
	private int maxWidth = 100;

	// ------------------------------------------------------------------------

	/**
	 * Sets the possibly linked conversation item. This is used when calling the
	 * context menu click action of an image to generate a proper name and
	 * title.
	 * 
	 * @param conversationItem
	 *            the new conversation item
	 */
	public void setConversationItem(ConversationItem conversationItem) {
		titleAddition = Conversation.buildImageTitleAddition(this.getContext(),
				conversationItem);
		description = Conversation.buildImageDescription(this.getContext(),
				conversationItem);
	}

	// ------------------------------------------------------------------------

	/**
	 * The constant for mapping regexp parsing rules from smiley index number.
	 * Do this just once and statically for faster performance.
	 */
	// Java characters that have to be escaped in regular expressions:
	// \.[]{}()*+-?^$|
	// private static final Pattern[] refSmileys = { Pattern.compile("=\\)"),
	// Pattern.compile(":\\("), Pattern.compile(">:\\|"),
	// Pattern.compile(":\\]"), Pattern.compile("\\^\\^"),
	// Pattern.compile(":\\)"), Pattern.compile(":P"),
	// Pattern.compile(";\\)") };

	private static final Pattern[] refSmileys = { Pattern.compile(":\\-\\)"),
			Pattern.compile(":\\-\\("), Pattern.compile(":\\-B"),
			Pattern.compile(":\\-\\/"), Pattern.compile(":\\-Z"),
			Pattern.compile(";\\-\\)"), Pattern.compile(":~f"),
			Pattern.compile(":\\-D"), Pattern.compile(":\\-K"),
			Pattern.compile(":~b"), Pattern.compile(":\\-8"),
			Pattern.compile(":\\-M"), Pattern.compile(":\\-A"),
			Pattern.compile(":~C"), Pattern.compile(":~d") };

	// ------------------------------------------------------------------------

	public interface OnCutCopyPasteListener {

		/**
		 * If the cut action was triggered this method will fire.
		 * 
		 */
		void onCut();

		/**
		 * If the copy action was triggered this method will fire.
		 * 
		 */
		void onCopy();

		/**
		 * If the paste action was triggered this method will fire.
		 * 
		 */
		void onPaste();
	}

	// ------------------------------------------------------------------------

	/**
	 * Sets the cut/copy/paste listener.
	 * 
	 * @param onCutCopyPasteListener
	 *            the new on cut copy paste listener
	 */
	public void setOnCutCopyPasteListener(
			OnCutCopyPasteListener onCutCopyPasteListener) {
		this.onCutCopyPasteListener = onCutCopyPasteListener;
	}

	// ------------------------------------------------------------------------

	/**
	 * Instantiates a new smiley edit text.
	 * 
	 * @param context
	 *            the context
	 * @param attrs
	 *            the attrs
	 * @param defStyle
	 *            the def style
	 */
	public ImageSmileyEditText(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	// ------------------------------------------------------------------------

	/**
	 * Instantiates a new smilie edit text.
	 * 
	 * @param context
	 *            the context
	 * @param attrs
	 *            the attrs
	 */
	public ImageSmileyEditText(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	// ------------------------------------------------------------------------

	/**
	 * Instantiates a new smilie edit text.
	 * 
	 * @param context
	 *            the context
	 */
	public ImageSmileyEditText(Context context) {
		super(context);
	}

	// ------------------------------------------------------------------------

	/**
	 * Update max width.
	 * 
	 * @param maxWidth
	 *            the max width
	 */
	public void updateMaxWidth(int width) {
		if (!isInputTextField) { // && containsImages) {
			int newMaxWidth = (width * 60) / 100;
			if (newMaxWidth < 100) {
				newMaxWidth = 100;
			}

			String text = this.getText().toString();
			if (text.length() > 20) {
				text = text.substring(0, 20) + "...";
			}

			// Log.d("communicator", "IMAGESMILEY updateMaxWidth(" + text
			// + ")  maxWidth=" + maxWidth + ", newMaxWidth="
			// + newMaxWidth + ", containsImages=" + containsImages);

			if (newMaxWidth != maxWidth) {
				maxWidth = newMaxWidth;
				// Log.d("communicator", "IMAGESMILEY REDRAW!!!");

				// Only in this case recompute layout
				this.setText(this.getText().toString());
			}
		} else {
			maxWidth = 100;
		}
	}

	// ------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.EditText#setText(java.lang.CharSequence,
	 * android.widget.TextView.BufferType)
	 */
	@Override
	public void setText(CharSequence text, BufferType type) {
		try {
			// We instantiate the spannable with the height of a line
			Spannable spannable = getTextWithImages(getContext(), text,
					this.getLineHeight(), this, isInputTextField);
			super.setText(spannable, BufferType.SPANNABLE);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	// ------------------------------------------------------------------------

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {

		if (Conversation.isTypingFast() && !isInputTextField) {
			return;
		}

		super.onLayout(changed, l, t, r, b);
	}

	// ------------------------------------------------------------------------

	/**
	 * Adds the images.
	 * 
	 * @param context
	 *            the context
	 * @param spannable
	 *            the spannable
	 * @param textHeight
	 *            the text height
	 * @return true, if successful
	 */
	private static boolean addImages(final Context context,
			Spannable spannable, float textHeight,
			final ImageSmileyEditText editText, boolean isInputTextField) {
		
		// editText.containsImages = false;

		// Taken from the acknowledged article:
		//
		// Pattern refImg = Pattern
		// .compile("\\Q[img \\E([a-zA-Z0-9_]+?)\\Q/]\\E");
		// String resname = spannable
		// .subSequence(matcher.start(1), matcher.end(1)).toString()
		// .trim();

		boolean hasChanges = false;

		// Replace smileys: Only if the user has turned on this feature
		if (Utility.loadBooleanSetting(context, Setup.OPTION_SMILEYS,
				Setup.DEFAULT_SMILEYS)) {
			// For every available smiley we have to parse the text...
			for (int smileyIndex = 0; smileyIndex < refSmileys.length; smileyIndex++) {
				Pattern smileyPattern = refSmileys[smileyIndex];
				String smileyImg = "s" + smileyIndex;

				Matcher matcher = smileyPattern.matcher(spannable);
				while (matcher.find()) {
					boolean set = true;
					for (ImageSpan span : spannable.getSpans(matcher.start(),
							matcher.end(), ImageSpan.class)) {
						if (spannable.getSpanStart(span) >= matcher.start()
								&& spannable.getSpanEnd(span) <= matcher.end()) {
							spannable.removeSpan(span);
						} else {
							set = false;
							break;
						}
					}
					// Get the smiley image from drawable folder
					int id = context.getResources().getIdentifier(smileyImg,
							"drawable", context.getPackageName());
					Drawable drawable = context.getResources().getDrawable(id);
					drawable.setBounds(0, 0, (int) textHeight, (int) textHeight);
					if (set) {
						hasChanges = true;
						ImageSpan imageSpan = new ImageSpan(context, id);
						spannable.setSpan(imageSpan, matcher.start(),
								matcher.end(),
								Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					}
				}
			}
		}

		// Replace images
		Pattern refImg = Pattern
				.compile("\\Q[img \\E([a-zA-Z0-9_+/=]+?)\\Q]\\E");
		// BASE64 encoded image
		Matcher matcher = refImg.matcher(spannable);
		while (matcher.find()) {
			boolean set = true;
			// editText.containsImages = true;
			for (ImageSpan span : spannable.getSpans(matcher.start(),
					matcher.end(), ImageSpan.class)) {
				if (spannable.getSpanStart(span) >= matcher.start()
						&& spannable.getSpanEnd(span) <= matcher.end()) {
					spannable.removeSpan(span);
				} else {
					set = false;
					break;
				}
			}
			final String encodedImg = spannable
					.subSequence(matcher.start(1), matcher.end(1)).toString()
					.trim();

			// Log.d("communicator", "IMAGE1:" + spannable.toString());
			// Log.d("communicator", "IMAGE2:" + encodedImg);

			Bitmap bitmap = null;
			Drawable drawable = null;
			try {
				bitmap = Utility.loadImageFromBASE64String(context, encodedImg);
				drawable = Utility.getDrawableFromBitmap(context, bitmap);
				int h = bitmap.getHeight();
				int w = bitmap.getWidth();

				// Log.d("communicator", "IMAGESMILEY addImages()  maxWidth="
				// + editText.maxWidth);

				if (w > editText.maxWidth) {
					float scale = ((float) w) / ((float) editText.maxWidth);
					// Log.d("communicator", "RESIZE: (1) scale=" + scale);
					w = editText.maxWidth;
					h = (int) ((float) h / scale);
				}

				// Log.d("communicator", "IMAGE3: h=" + h + ", w=" + w);
				// Must set the original bytes, otherwise NPE
				drawable.setBounds(0, 0, w, h);
			} catch (Exception e) {
				set = false;
				e.printStackTrace();
			}
			if (set && drawable != null) {
				hasChanges = true;
				int start = matcher.start();
				int end = matcher.end();
				// Log.d("communicator", "IMAGE4: start=" + start + ", end=" +
				// end);
				spannable.setSpan(new ImageSpan(drawable), start, end,
						Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

				int imageIndex = -1;
				if (Conversation.isAlive() && !isInputTextField) {
					imageIndex = Conversation.getInstance().registerImage(
							bitmap, 1);
//					 Log.d("communicator", "IMAGE[" + imageIndex
//					 + "] ADD: start=" + start + ", end=" + end);
				}
				final int imageIndex2 = imageIndex;
				final Bitmap bitmap2 = bitmap;
				ClickableSpan clickableSpan = new ClickableSpan() {
					@Override
					public void onClick(View widget) {
						if (Conversation.isAlive()) {
							Conversation conversation = Conversation
									.getInstance();
							ImageContextMenu.show(context, conversation
									.createImageContextMenu(conversation,
											bitmap2, encodedImg,
											editText.titleAddition,
											editText.description, imageIndex2));
						}
					}
				};
				spannable.setSpan(clickableSpan, start, end,
						Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

			}
		}
		return hasChanges;
	}

	// ------------------------------------------------------------------------

	/**
	 * Gets the text with images.
	 * 
	 * @param context
	 *            the context
	 * @param text
	 *            the text
	 * @param textHeight
	 *            the text height
	 * @return the text with images
	 */
	private static Spannable getTextWithImages(Context context,
			CharSequence text, float textHeight, ImageSmileyEditText editText,
			boolean isInputTextField) {
		Spannable spannable = spannableFactory.newSpannable(text);
		// Log.d("communicator", "TEXTEDIT getTextWithImages()");
		addImages(context, spannable, textHeight, editText, isInputTextField);
		return spannable;
	}

	// ------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.TextView#onTextContextMenuItem(int)
	 */
	@Override
	public boolean onTextContextMenuItem(int id) {
		// Log.d("communicator", "TEXTEDIT onTextContextMenuItem()");
		boolean consumed = super.onTextContextMenuItem(id);
		switch (id) {
		case android.R.id.cut:
			if (onCutCopyPasteListener != null) {
				onCutCopyPasteListener.onCut();
			}
			break;
		case android.R.id.copy:
			if (onCutCopyPasteListener != null) {
				onCutCopyPasteListener.onCopy();
			}
		case android.R.id.paste:
			if (onCutCopyPasteListener != null) {
				onCutCopyPasteListener.onPaste();
			}
			break;
		}
		return consumed;
	}

	// ------------------------------------------------------------------------

	/**
	 * Sets the input text field to true or false. If set to true the text field
	 * will not redraw itself and show thumbnail images of width 100 only.
	 * 
	 * @param isInputTextField
	 *            the new input text field
	 */
	public void setInputTextField(boolean isInputTextField) {
		this.isInputTextField = isInputTextField;
	}

	// ------------------------------------------------------------------------

}
