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
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageButton;

public class ImageLabelButton extends ImageButton {

	String text = "";
	int textColor = Color.WHITE;
	int textSize = 22;
	int height = 0;

	public ImageLabelButton(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

	public ImageLabelButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	public ImageLabelButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
	}

	public void setTextAndImageResource(String text, int resId) {
		setTextAndImageResource( text, 22,  resId);
	}

	public void setTextAndImageResource(String text, int textSize, int resId) {
		this.text = text;
		setImageResourceInternal(resId); 
	}

	public void setTextSize(int size) {
		this.textSize = size;
	}

	public void setTextColor(int color) {
		this.textColor = color;
	}

//	@Override
//	public void setImageDrawable(Drawable drawable)  {
//		throw (new RuntimeException("Use setTextAndImageResource for setting an image."));
//	}

	@Override
	public void setImageURI(Uri uri) {
		throw (new RuntimeException("Use setTextAndImageResource for setting an image."));
	}

	
//	@Override
//	public void setImageBitmap(Bitmap bm) {
//		throw (new RuntimeException("Use setTextAndImageResource for setting an image."));
//	}

	@Override
	public void setImageResource(int resId) {
		throw (new RuntimeException("Use setTextAndImageResource for setting an image."));
	}

	
	private void setImageResourceInternal(int resId) {
		Paint paint = new Paint();
		paint.setColor(this.textColor);
		paint.setTextSize(this.textSize);
		Rect bounds = new Rect();
		paint.getTextBounds("XXX", 0, "XXX".length(), bounds);
		height = bounds.height();

		Log.d("communicator",
				"# # # TEXT '"+text+"' BOUNDS w=" +bounds.width() + ", h=" + bounds.height());

		Bitmap icon = BitmapFactory.decodeResource(this.getResources(), resId);

		Log.d("communicator",
				"# # #  ICON  BOUNDS w=" +icon.getWidth() + ", h=" + icon.getHeight());

        Bitmap icon2 = Bitmap.createBitmap(icon.getWidth(), icon.getHeight() + height +   height, Bitmap.Config.ARGB_8888);

		Log.d("communicator",
				"# # #  ICON2  BOUNDS w=" +icon.getWidth() + ", h=" + icon.getHeight() + height +  height);

        Canvas canvas = new Canvas(icon2);
        // draw the source image centered
        canvas.drawBitmap(icon, 0, 0, new Paint());
		
		super.setImageBitmap(icon2);
	}
	
	
	

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		// Calc text size
		Paint paint = new Paint();
		paint.setColor(this.textColor);
		paint.setTextSize(this.textSize);
		Rect bounds = new Rect();
		paint.getTextBounds(this.text, 0, this.text.length(), bounds);
		
		
		// Draw the Text
		float buttonWitdh = this.getWidth();
		float buttonHeight = this.getHeight();
		canvas.drawText(this.text, (buttonWitdh - bounds.width()) / 2,
				buttonHeight - height - height/2, paint);
	}

}
