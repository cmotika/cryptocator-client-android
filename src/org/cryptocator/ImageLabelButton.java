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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.util.AttributeSet;
import android.widget.ImageButton;

/**
 * The ImageLabelButton class is an ImageButton that has an additional label
 * below the image icon. To use the ImageButton setTextAndImageResource() must
 * be called programmatically.
 * 
 * @author Christian Motika
 * @since 1.2
 * @date 08/23/2015
 * 
 */
public class ImageLabelButton extends ImageButton {

	/** The label text. */
	String text = "";

	/** The default label text color. */
	final int DEFAULTTEXTCOLOR = Color.WHITE;

	/** The label text size. */
	int textSize = 22;

	/** The height of the text label. */
	int height = 0;

	/** The width of the text label. */
	int width = 0;

	/** The paint object for painting the additional label text. */
	Paint paint = null;

	// ------------------------------------------------------------------------

	/**
	 * Instantiates a new image label button.
	 * 
	 * @param context
	 *            the context
	 */
	public ImageLabelButton(Context context) {
		super(context);
	}

	// ------------------------------------------------------------------------

	/**
	 * Instantiates a new image label button.
	 * 
	 * @param context
	 *            the context
	 * @param attrs
	 *            the attrs
	 */
	public ImageLabelButton(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	// ------------------------------------------------------------------------

	/**
	 * Instantiates a new image label button.
	 * 
	 * @param context
	 *            the context
	 * @param attrs
	 *            the attrs
	 * @param defStyle
	 *            the def style
	 */
	public ImageLabelButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	// ------------------------------------------------------------------------

	/**
	 * Sets the text and image resource for the ImageLabelButton with a default
	 * font size of 22.
	 * 
	 * @param text
	 *            the text
	 * @param resId
	 *            the res id
	 */
	public void setTextAndImageResource(String text, int resId) {
		recalculateTextSize(text, textSize);
		setTextAndImageResource(text, 22, resId);
	}

	// ------------------------------------------------------------------------

	/**
	 * Sets the text and image resource for the ImageLabelButton.
	 * 
	 * @param text
	 *            the text
	 * @param textSize
	 *            the text size
	 * @param resId
	 *            the res id
	 */
	public void setTextAndImageResource(String text, int textSize, int resId) {
		this.text = text;
		this.textSize = textSize;
		recalculateTextSize(text, textSize);
		setImageResourceInternal(resId);
	}

	// ------------------------------------------------------------------------

	private void recalculateTextSize(String text, int textSize) {
		if (paint == null) {
			paint = new Paint();
			paint.setColor(DEFAULTTEXTCOLOR);
		}
		// Calculate the text size
		paint.setTextSize(this.textSize);
		Rect bounds = new Rect();
		paint.getTextBounds(this.text, 0, this.text.length(), bounds);
		width = bounds.width();
	}

	// ------------------------------------------------------------------------

	/**
	 * Set the label text color.
	 * 
	 * @param color
	 *            the new text color
	 */
	public void setTextColor(int textColor) {
		if (paint == null) {
			paint = new Paint();
		}
		paint.setColor(textColor);
	}

	// ------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.ImageView#setImageURI(android.net.Uri)
	 */
	@Override
	public void setImageURI(Uri uri) {
		throw (new RuntimeException(
				"Use setTextAndImageResource for setting an image."));
	}

	// ------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.ImageView#setImageResource(int)
	 */
	@Override
	public void setImageResource(int resId) {
		throw (new RuntimeException(
				"Use setTextAndImageResource for setting an image."));
	}

	// ------------------------------------------------------------------------

	/**
	 * Internally prepare the drawing of the ImageLabelButton by creating a new
	 * internal bitmap from the original bitmap but with space for the
	 * additional text label which is later drawn centered below the original
	 * icon image.
	 * 
	 * @param resId
	 *            the new image resource internal
	 */
	private void setImageResourceInternal(int resId) {
		Paint paint = new Paint();
		paint.setTextSize(this.textSize);
		Rect bounds = new Rect();
		// Get dummy bounds for the height. Use a dummy text here because
		// otherwise we get different values for different characters.
		paint.getTextBounds("XXX", 0, "XXX".length(), bounds);
		height = bounds.height();

		Bitmap icon = BitmapFactory.decodeResource(this.getResources(), resId);

		Bitmap icon2 = Bitmap.createBitmap(icon.getWidth(), icon.getHeight()
				+ height + height, Bitmap.Config.ARGB_8888);

		Canvas canvas = new Canvas(icon2);

		// Draw the source image centered
		canvas.drawBitmap(icon, 0, 0, new Paint());

		super.setImageBitmap(icon2);
	}

	// ------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.ImageView#onDraw(android.graphics.Canvas)
	 */
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		// Draw the Text. The space was created in setImageResourceInternal()
		// already.
		float buttonWitdh = this.getWidth();
		float buttonHeight = this.getHeight();
		canvas.drawText(this.text, (buttonWitdh - width) / 2, buttonHeight
				- height - height / 2, paint);
	}

	// ------------------------------------------------------------------------

}
