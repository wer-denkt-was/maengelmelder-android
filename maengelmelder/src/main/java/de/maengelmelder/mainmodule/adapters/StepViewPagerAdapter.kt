package de.maengelmelder.mainmodule.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import de.maengelmelder.mainmodule.activities.MessageProcessActivity
import de.maengelmelder.mainmodule.fragments.BaseMessageStepFragment

class StepViewPagerAdapter(fm: FragmentManager, steps: Array<MessageProcessActivity.StepInfo>)
    : androidx.fragment.app.FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    private val mSteps = steps

    override fun getPageTitle(position: Int): CharSequence = mSteps[position].name

    override fun getItem(position: Int): Fragment = mSteps[position].fragment

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getCount(): Int = mSteps.size
}