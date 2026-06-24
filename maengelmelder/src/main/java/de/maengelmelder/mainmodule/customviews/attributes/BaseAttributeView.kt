package de.maengelmelder.mainmodule.customviews.attributes

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import de.maengelmelder.mainmodule.objects.Attribute

/**
 * Declaration-site invariant. Allows adding subclass of [BaseAttributeView] with subtype of Any (lower bounds)
 * This class acts as a base class of any views associated to the MM attributes (e.g. text, checkboxes, etc)
 */
abstract class BaseAttributeView<out T> where T: Any {

    /**
     * Context
     */
    protected var context: Context

    /**
     * Inflated view
     */
    protected var view: View? = null

    /**
     * Associated attribute
     */
    protected var attribute: Attribute? = null

    /**
     * Layout ID of the inflated view
     */
    private var mLayoutResId: Int

    constructor(c: Context, layoutResId: Int, attr: Attribute){
        context = c
        mLayoutResId = layoutResId
        attribute = attr
        view = LayoutInflater.from(context).inflate(mLayoutResId, null)
    }

    /**
     * Returns the inflated view
     */
    fun getInflatedView(): View? = view

    /**
     * Returns the associated attribute
     */
    fun getAttrib(): Attribute? = attribute

    /**
     * Whether this value for this attribute is needed
     */
    protected fun isRequired(): Boolean = attribute?.required?: false

    /**
     * Set the title of the attribute view
     */
    abstract fun setTitle(title: String)

    /**
     * Set the hint
     */
    abstract fun setHint(hint: String)

    /**
     * Get the attribute type (text, checkbox, etc)
     */
    abstract fun getType(): String

    /**
     * Returns the input value for this attribute
     */
    abstract fun getValue(): T?

    /**
     * Should return true if there is a valid value assigned, false otherwise
     */
    abstract fun hasValue(): Boolean

    /**
     * Set the value for this attribute
     */
    abstract fun setValue(value: Any)

    /**
     * Whether the attribute is filled out by the user
     */
    abstract fun isAttributeFilledOut(): Boolean

    /**
     * Set whether the view should be readonly or not
     */
    abstract fun toggleReadOnly(toggle: Boolean)

    /**
     * Toggle error regarding the input
     */
    abstract fun toggleError(toggle: Boolean, errText: String = "")
}