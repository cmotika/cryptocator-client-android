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

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ScrollView;

/**
 * The ScrollViewEx class is special scroll view that allows for detecting
 * scroll rest. It further allows to lock the scroll position and restore it at
 * a later point in time. It can even used to detect an overscroll event.
 * 
 * @author Christian Motika
 * @since 1.2
 * @date 08/23/2015
 * 
 */
public class ScrollViewEx extends ScrollView {

	/** The locked position. */
	int lockedPos = -1;

	/** The last overscroll. */
	long lastOverscroll = 0;

	/** The overscroll counter. */
	int overscrollCnt = 0;

	/** The overscrollmin minimum counter for triggering the overscroll even. */
	private static int OVERSCROLLMIN = 18;

	/**
	 * The time in ms in which the overscrolling must take place and the minimum
	 * time between two consicutive overscrollings.
	 */
	private static int LASTOVERSCROLLMINDIFF = 1000;

	/** The scroll view ex listener. */
	private ScrollViewExListener scrollViewExListener = null;

	/** The watch dog thread is necessary to detect the rest of the scrolling. */
	public WatchDogThread watchDogThread = null;

	/**
	 * The time in ms after which the scroll rest event is triggered after
	 * scrolling took place.
	 */
	private static int RESTTIMETRIGGER = 500;

	// -------------------------------------------------------------------------

	/**
	 * The listener interface for receiving scrollViewEx events. The class that
	 * is interested in processing a scrollViewEx event implements this
	 * interface, and the object created with that class is registered with a
	 * component using the component's
	 * <code>addScrollViewExListener<code> method. When
	 * the scrollViewEx event occurs, that object's appropriate
	 * method is invoked.
	 * 
	 * @see ScrollViewExEvent
	 */
	public interface ScrollViewExListener {

		/**
		 * On scroll changed.
		 * 
		 * @param scrollView
		 *            the scroll view
		 * @param x
		 *            the x
		 * @param y
		 *            the y
		 * @param oldx
		 *            the oldx
		 * @param oldy
		 *            the oldy
		 */
		void onScrollChanged(ScrollViewEx scrollView, int x, int y, int oldx,
				int oldy);

		/**
		 * On scroll rested.
		 * 
		 * @param scrollView
		 *            the scroll view
		 * @param x
		 *            the x
		 * @param y
		 *            the y
		 * @param oldx
		 *            the oldx
		 * @param oldy
		 *            the oldy
		 */
		void onScrollRested(ScrollViewEx scrollView, int x, int y, int oldx,
				int oldy);

		/**
		 * On oversroll.
		 * 
		 * @param up
		 *            the up
		 */
		void onOversroll(boolean up);

	}

	// -------------------------------------------------------------------------

	/**
	 * Instantiates a new scroll view ex.
	 * 
	 * @param context
	 *            the context
	 */
	public ScrollViewEx(Context context) {
		super(context);
	}

	// -------------------------------------------------------------------------

	/**
	 * Instantiates a new scroll view ex.
	 * 
	 * @param context
	 *            the context
	 * @param attrs
	 *            the attrs
	 * @param defStyle
	 *            the def style
	 */
	public ScrollViewEx(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	// -------------------------------------------------------------------------

	/**
	 * Instantiates a new scroll view ex.
	 * 
	 * @param context
	 *            the context
	 * @param attrs
	 *            the attrs
	 */
	public ScrollViewEx(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	// -------------------------------------------------------------------------

	/**
	 * Sets the scroll view listener.
	 * 
	 * @param scrollViewExListener
	 *            the new scroll view listener
	 */
	public void setScrollViewListener(ScrollViewExListener scrollViewExListener) {
		this.scrollViewExListener = scrollViewExListener;
	}

	// -------------------------------------------------------------------------

	/**
	 * Lock position.
	 */
	public void lockPosition() {
		lockedPos = this.getScrollY();
	}

	// -------------------------------------------------------------------------

	/**
	 * Checks if is locked.
	 * 
	 * @return true, if is locked
	 */
	public boolean isLocked() {
		return lockedPos != -1;
	}

	// -------------------------------------------------------------------------

	/**
	 * Restore locked position.
	 */
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

	// -------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.view.View#onScrollChanged(int, int, int, int)
	 */
	@Override
	protected void onScrollChanged(int l, int t, int oldl, int oldt) {
		super.onScrollChanged(l, t, oldl, oldt);

		if (scrollViewExListener != null && oldt == t) {
			overscrollCnt++;

			long tmp = System.currentTimeMillis();
			if (overscrollCnt > OVERSCROLLMIN) {
				if (tmp - lastOverscroll > LASTOVERSCROLLMINDIFF) {
					scrollViewExListener.onOversroll(t == 0);
				}
				lastOverscroll = tmp;
				overscrollCnt = 0;
			} else if (tmp - lastOverscroll <= LASTOVERSCROLLMINDIFF) {
				overscrollCnt = 0;
			}

		} else {
			overscrollCnt = 0;
		}

		if (scrollViewExListener != null) {
			scrollViewExListener.onScrollChanged(this, l, t, oldl, oldt);
		}
		if (watchDogThread == null) {
			startWatchDogThread(this.getContext(), RESTTIMETRIGGER,
					scrollViewExListener, this);
		}
		watchDogThread.xCurrent = l;
		watchDogThread.yCurrent = t;
		// Reset our watchdog
		watchDogThread.reset();
	}

	// -------------------------------------------------------------------------

	/**
	 * Start watch dog thread.
	 * 
	 * @param context
	 *            the context
	 * @param seconds
	 *            the seconds
	 * @param listener
	 *            the listener
	 * @param scrollView
	 *            the scroll view
	 */
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

	// -------------------------------------------------------------------------

	/**
	 * Stop watch dog thread.
	 */
	public void stopWatchDogThread() {
		if (watchDogThread != null) {
			watchDogThread.cancel();
		}
	}

	// -------------------------------------------------------------------------

	/**
	 * The internal class WatchDogThread is responsible for detecting a rest of
	 * the scrolling.
	 */
	class WatchDogThread extends Thread {

		/** The context. */
		Context context;

		/** The cancel. */
		boolean cancel = false;

		/** The start or reset time stamp. */
		long startOrResetTimeStamp = 0;

		/** The enabled. */
		boolean enabled = false;

		/** The milliseconds. */
		int milliseconds = 0;

		/** The listener. */
		ScrollViewExListener listener = null;

		/** The scroll view. */
		ScrollViewEx scrollView = null;

		/** The x last rest. */
		public int xLastRest;

		/** The y last rest. */
		public int yLastRest;

		/** The x current. */
		public int xCurrent;

		/** The y current. */
		public int yCurrent;

		// -------------------------------------------------

		/**
		 * Instantiates a new watch dog thread.
		 * 
		 * @param context
		 *            the context
		 * @param milliseconds
		 *            the milliseconds
		 * @param listener
		 *            the listener
		 * @param scrollView
		 *            the scroll view
		 */
		public WatchDogThread(Context context, int milliseconds,
				ScrollViewExListener listener, ScrollViewEx scrollView) {
			super();
			this.context = context;
			this.milliseconds = milliseconds;
			this.listener = listener;
			this.scrollView = scrollView;
		}

		// -------------------------------------------------

		/**
		 * Reset.
		 */
		public void reset() {
			startOrResetTimeStamp = System.currentTimeMillis();
			enabled = true;
		}

		// -------------------------------------------------

		/**
		 * Cancel.
		 */
		public void cancel() {
			cancel = true;
		}

		// -------------------------------------------------

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Thread#run()
		 */
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
						// Wait for the next scrolling even which does a reset()
						// call enabling the watchdog again
						enabled = false;
					}
				}
			}
			watchDogThread = null;
		}
	}

	// ------------------------------------------------------------------------

}
