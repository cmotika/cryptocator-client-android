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
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ScrollView;

public class ScrollViewEx extends ScrollView {
	int lockedPos = -1;

	long lastOverscroll = 0;
	int overscrollCnt = 0;

	private static int OVERSCROLLMIN = 18;
	private static int LASTOVERSCROLLMINDIFF = 1000;

	// -------------------------------------------------------------------------

	public void lockPosition() {
		lockedPos = this.getScrollY();
	}

	public boolean isLocked() {
		return lockedPos != -1;
	}

	public void restoreLockedPosition() {
		final int lockedPosCopy = lockedPos;
		lockedPos = -1;
		final ScrollView me = this;
		this.postDelayed(new Runnable() {
			public void run() {
				me.scrollTo(0, lockedPosCopy);
			}
		}, 200);
	}

	public interface ScrollViewExListener {
		void onScrollChanged(ScrollViewEx scrollView, int x, int y, int oldx,
				int oldy);

		void onScrollRested(ScrollViewEx scrollView, int x, int y, int oldx,
				int oldy);

		void onOversroll(boolean up);

	}

	private ScrollViewExListener scrollViewExListener = null;

	public ScrollViewEx(Context context) {
		super(context);
	}

	public ScrollViewEx(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public ScrollViewEx(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public void setScrollViewListener(ScrollViewExListener scrollViewExListener) {
		this.scrollViewExListener = scrollViewExListener;
	}

	@Override
	protected void onScrollChanged(int l, int t, int oldl, int oldt) {
		super.onScrollChanged(l, t, oldl, oldt);

		if (scrollViewExListener != null && oldt == t) {
			overscrollCnt++;

//			Log.d("communicator", "######## INC overscrollCnt = "
//					+ overscrollCnt);

			long tmp = System.currentTimeMillis();
			if (overscrollCnt > OVERSCROLLMIN) {
				if (tmp - lastOverscroll > LASTOVERSCROLLMINDIFF) {
					scrollViewExListener.onOversroll(t == 0);
				}
				lastOverscroll = tmp;
				overscrollCnt = 0;
//				Log.d("communicator", "######## RST1 overscrollCnt = "
//						+ overscrollCnt);
			} else if (tmp - lastOverscroll <= LASTOVERSCROLLMINDIFF) {
				overscrollCnt = 0;
			}

		} else {
//			Log.d("communicator", "######## RST2 overscrollCnt = "
//					+ overscrollCnt);
			overscrollCnt = 0;
		}

		if (scrollViewExListener != null) {
			scrollViewExListener.onScrollChanged(this, l, t, oldl, oldt);
		}
		if (watchDogThread == null) {
			startWatchDogThread(this.getContext(), 500, scrollViewExListener,
					this);
		}
		watchDogThread.xCurrent = l;
		// Log.d("communicator", "@@@@ internal onScrollChanged -> " + t);
		watchDogThread.yCurrent = t;
		// reset our watchdog
		watchDogThread.reset();
	}

	// ---

	public WatchDogThread watchDogThread = null;

	public void startWatchDogThread(Context context, int seconds,
			ScrollViewExListener listener, ScrollViewEx scrollView) {
		if (watchDogThread == null) {
			watchDogThread = new WatchDogThread(context, seconds, listener,
					scrollView);
			watchDogThread.start();
		} else {
			watchDogThread.cancel();
			watchDogThread = new WatchDogThread(context, seconds, listener,
					scrollView);
		}
	}

	public void stopWatchDogThread() {
		if (watchDogThread != null) {
			watchDogThread.cancel();
		}
	}

	class WatchDogThread extends Thread {
		Context context;
		boolean cancel = false;
		long startOrResetTimeStamp = 0;
		boolean enabled = false;
		int milliseconds = 0;
		ScrollViewExListener listener = null;
		ScrollViewEx scrollView = null;

		public int xLastRest;
		public int yLastRest;
		public int xCurrent;
		public int yCurrent;

		public WatchDogThread(Context context, int milliseconds,
				ScrollViewExListener listener, ScrollViewEx scrollView) {
			super();
			this.context = context;
			this.milliseconds = milliseconds;
			this.listener = listener;
			this.scrollView = scrollView;
		}

		public void reset() {
			startOrResetTimeStamp = System.currentTimeMillis();
			enabled = true;
		}

		public void cancel() {
			cancel = true;
		}

		public void run() {
			while (!cancel) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if (enabled) {
					if (startOrResetTimeStamp <= (System.currentTimeMillis() - milliseconds)) {
						// WATCH DOG TRIGGERS // REST REACHED !!!
						if (listener != null) {
							listener.onScrollRested(scrollView, xCurrent,
									yCurrent, xLastRest, yLastRest);
							xLastRest = xCurrent;
							yLastRest = yCurrent;
						}
						scrollView.overscrollCnt = 0;
						Log.d("communicator", "######## RST3 overscrollCnt = "
								+ overscrollCnt);
						// wait for the next scrolling even which does a reset()
						// call enabling the watchdog again
						enabled = false;
					}
				}
			}
			watchDogThread = null;
		}
	}

}
