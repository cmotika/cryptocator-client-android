package org.cryptocator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.style.ImageSpan;
import android.util.AttributeSet;
import android.widget.EditText;

/**
 * The SmileyEditText class is a EditText component for displaying messages in
 * the conversation class. It is capable of parsing and substituting textual
 * smileys with graphical ones. The copied text will still have the textual
 * smileys in it for convenience.
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
public class SmileyEditText extends EditText {

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

	/** The constant for mapping order from smiley index number. */
	public static final int[] smileyOrder = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
			11, 12, 13, 14};

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
	public SmileyEditText(Context context, AttributeSet attrs, int defStyle) {
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
	public SmileyEditText(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	// ------------------------------------------------------------------------

	/**
	 * Instantiates a new smilie edit text.
	 * 
	 * @param context
	 *            the context
	 */
	public SmileyEditText(Context context) {
		super(context);
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
		// We instantiate the spannable with the height of a line
		Spannable spannable = getTextWithImages(getContext(), text,
				this.getLineHeight());
		super.setText(spannable, BufferType.SPANNABLE);
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
	private static boolean addImages(Context context, Spannable spannable,
			float textHeight) {

		// Taken from the acknowledged article:
		//
		// Pattern refImg = Pattern
		// .compile("\\Q[img \\E([a-zA-Z0-9_]+?)\\Q/]\\E");
		// String resname = spannable
		// .subSequence(matcher.start(1), matcher.end(1)).toString()
		// .trim();

		boolean hasChanges = false;

		// Only if the user has turned on this feature
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
						spannable.setSpan(new ImageSpan(context, id),
								matcher.start(), matcher.end(),
								Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					}
				}
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
			CharSequence text, float textHeight) {
		Spannable spannable = spannableFactory.newSpannable(text);
		addImages(context, spannable, textHeight);
		return spannable;
	}

	// ------------------------------------------------------------------------

}
