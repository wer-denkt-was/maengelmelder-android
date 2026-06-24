package de.maengelmelder.mainmodule.customviews.attributes

import android.content.Context
import android.os.Build
import android.text.Html
import android.text.InputType
import android.text.method.LinkMovementMethod
import android.util.Patterns
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import de.maengelmelder.mainmodule.R
import de.maengelmelder.mainmodule.objects.Attribute

/**
 * Textarea attribute field
 */
class TextAreaAttributeView(c: Context, attr: Attribute) : TextAttributeView(c, attr, R.layout.mm_attribute_textarea) {

    override fun getType(): String = Attribute.TYPE_TEXTAREA

    fun setMinLines(minLines: Int): TextAreaAttributeView {
        mContent?.minLines = minLines
        return this
    }

}