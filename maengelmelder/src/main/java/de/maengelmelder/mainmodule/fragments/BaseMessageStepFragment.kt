package de.maengelmelder.mainmodule.fragments

import android.content.Context
import android.os.Bundle
import android.os.Handler
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.maengelmelder.mainmodule.activities.MessageProcessActivity
import de.maengelmelder.mainmodule.objects.builder.MessageBuilder

/**
 * This fragment serves as the base class for any existing (or new) steps for adding information to messages
 */
abstract class BaseMessageStepFragment : androidx.fragment.app.Fragment() {

    /**
     * Context
     */
    protected var mContext: Context? = null

    /**
     * Generated view that contains the main view
     */
    private var mView : View? = null

    /**
     * Instance for handlers (delayed post, etc.)
     */
    private var mHandler: Handler? = null

    /**
     * View inflating. Also executing [onViewInflated] after the view is successfully inflated
     */

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mView = inflater.inflate(getLayoutId(), container, false)
        mContext = context
        onViewInflated(mView)
        return mView
    }

    /**
     * Callback when the fragment is attached to the activity
     */
    override fun onAttach(context: Context) {
        mContext = context
        super.onAttach(context)
    }

    /**
     * Whether this fragment inside a Viewpager can be swiped to move to the next one. One example usage is the [ChooseLocationStep] fragment
     * that can be navigated by swiping (thus, need to override the Viewpager default swipe functionality)
     */
    open fun canBeSwipedInsideViewPager(): Boolean = true

    /**
     * When this fragment is loaded again. The calling activity calls this method when the fragment is being re-displayed e.g. in a Viewpager
     */
    open fun onViewBroughtUp() { }

    /**
     * Returns the instance of the [MessageBuilder] using [Fragment.getActivity]
     */
    protected val builder: MessageBuilder?
        get() = (activity as MessageProcessActivity?)?.getBuilder()

    /**
     * Returns a singleton instance of [Handler]. Use this rather than creating new [Handler] object every time
     */
    protected fun getHandler(): Handler {
        if (mHandler == null) mHandler = Handler()
        return mHandler!!
    }

    /**
     * Returns the layout id to be inflated
     */
    abstract fun getLayoutId(): Int

    /**
     * Returns the title of the fragment
     */
    abstract fun getTitle(): String

    /**
     * Called when the view with provided layout id is inflated
     */
    abstract fun onViewInflated(v: View?)

    /**
     * Whether the necessary information for this step has been fulfilled
     */
    abstract fun isStepComplete(): Boolean

    /**
     * When it returns true, user will not be able to change in-between steps
     */
    abstract fun isLoading(): Boolean

    /**
     * Called when this fragment is swapped with a new one
     */
    abstract fun shouldPromptBeforeChange(): Boolean

    /**
     * Called every time before this fragment is swapped before promptBeforeChange() is called or
     * when you want to save the content of the message. The swapping of the fragment can happen when
     * another [BaseMessageStepFragment] is being swapped or when the activity hosting it is finished
     */
    abstract fun executeBeforeChange()

    /**
     * if shouldPromptBeforeChange() is true,
     *      this method should be executed to show prompt to the user before changing fragment
     *      if this returns true
     *          change fragment
     *      else
     *          stay
     */
    abstract fun promptBeforeChange(f: (Boolean) -> Unit)

}