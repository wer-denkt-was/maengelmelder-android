package de.maengelmelder.mainmodule.utils.showcases

import android.view.View

sealed class Target
class ViewTarget(val view: View) : Target()
class PointTarget(val x: Int, val y: Int) : Target()
