package de.maengelmelder.mainmodule.service.receivers

/**
 *
 * Put the constants for filtering broadcast messages here.
 * Note that the value of the constants should match the value given in manifest, if the broadcast receiver is registered in manifest instead of initiating it on activity
 */
object BroadcastFilterList {

    /**
     * Broadcast the status of message upload, along with accompanying images
     */
    val MESSAGE_UPLOAD = "de.maengelmelder2.app.receiver.MESSAGE_UPLOAD"

}