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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;

/**
 * The ImageFullscreenActivity displays the image in full screen mode either
 * landscape or portrait with maximum width.
 * 
 * @author Christian Motika
 * @since 1.3
 * @date 09/08/2015
 */
public class ImageFullscreenActivity extends Activity {

	/** The bitmap to displaye. */
	public static Bitmap bitmap = null;

	/** The image view. */
	ImageView imageView;

	/** The activity. */
	Activity activity = null;

	/** The context. */
	Context context = null;

	/**
	 * The toggle zoomed mode. In the zoomed mode the height or with is
	 * maximized.
	 */
	private static boolean toggleZoomToFit = true;

	/** The lp image. */
	private LinearLayout.LayoutParams lpImage = null;

	/** The lp image zoomed. */
	private LinearLayout.LayoutParams lpImageZoomed = null;
	
	/** The visible. */
	public static boolean visible = false;

	// ------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		visible = true;

		context = this;

		// remove title & softkeys
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().clearFlags(
				WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);

		Utility.hideSoftkeys(this);

		LinearLayout.LayoutParams lpOuter = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.MATCH_PARENT);
		lpOuter.setMargins(0, 0, 0, 0);

		lpImage = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);

		lpImageZoomed = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.MATCH_PARENT);

		imageView = new ImageView(context);
		lpImage.gravity = Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL;
		imageView.setImageBitmap(bitmap);

		// Start in zoomed mode
		if (!toggleZoomToFit) {
			imageView.setScaleType(ScaleType.CENTER_INSIDE);
			imageView.setLayoutParams(lpImage);
		} else {
			imageView.setScaleType(ScaleType.FIT_CENTER);
			imageView.setLayoutParams(lpImageZoomed);
		}

		LinearLayout outerLayout = new LinearLayout(context);
		outerLayout.setBackgroundColor(Color.parseColor("#FF000000"));
		outerLayout.setGravity(Gravity.CENTER_HORIZONTAL
				| Gravity.CENTER_VERTICAL);
		outerLayout.addView(imageView);
		outerLayout.setLayoutParams(lpOuter);

		setContentView(outerLayout);

		outerLayout.setClickable(true);
		outerLayout.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				toggleZoomToFit = !toggleZoomToFit;
				if (!toggleZoomToFit) {
					imageView.setScaleType(ScaleType.CENTER_INSIDE);
					imageView.setLayoutParams(lpImage);
				} else {
					imageView.setScaleType(ScaleType.FIT_CENTER);
					imageView.setLayoutParams(lpImageZoomed);
				}
			}
		});
		outerLayout.setOnLongClickListener(new View.OnLongClickListener() {
			public boolean onLongClick(View v) {
				Conversation.getInstance().fastScrollView
						.restoreLockedPosition();
				finish();
				return true;
			}
		});
	}

	// ------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onStop()
	 */
	@Override
	public void onStop() {
		super.onStop();
		visible = false;
	}

	// ------------------------------------------------------------------------

	/**
	 * Show fullscreen image.
	 * 
	 * @param context
	 *            the context
	 * @param bitmap
	 *            the bitmap
	 */
	public static void showFullscreenImage(Context context, Bitmap bitmap) {
		Intent dialogIntent = new Intent(context, ImageFullscreenActivity.class);
		ImageFullscreenActivity.bitmap = bitmap;
		dialogIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(dialogIntent);

	}

	// ------------------------------------------------------------------------

}