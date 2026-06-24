package de.maengelmelder.mainmodule.network.responses

class BaseResponse(
        /**
         * HTTP code from responses from API execution + additional local error code. See [de.maengelmelder.mainmodule.network.MMAPI] constants
         */
        val code: Int,
        /**
         * The response body from API execution. Can be anything from JSON response, or HTML page.
         * Do check for exception when parsing
         */
        val body: String
) {
        fun isSuccess(): Boolean = code == 1 || (200..299).contains(code)
}

data class MessageSubsResponse(
        /**
         * true/false
         */
        val success: Boolean,
        /**
         * Subscribed message Id
         */
        val messageId: String,
        /**
         * user's email used for subscription
         */
        val email: String,
        /**
         * User's id used for subscription
         */
        val userId: String
)

data class LoginWithTokenResponse(
        /**
         * Response code from server
         */
        val code: Int,
        /**
         * ID of the user
         */
        val userId: String,
        /**
         * username
         */
        val username: String,
        /**
         * resulting token
         */
        val token: String,
        /**
         * Message (success/error)
         */
        val msg: String
)

data class MessageDetailErrorResponse(
        /**
         * error
         */
        val error: String,
        /**
         * message
         */
        val message: String
)

data class MessageUpdateResponse(
        /**
         * Code received from response after updating message.
         * */
        val code: Int,
        /**
         * The message ID which is being updated
         */
        val msgId: String,
        /**
         *  The local image path containing the image that is uploaded along with the update message, or null if you update without attaching images.
         *  This member will always contain the path, whether the upload fails or not
         */
        val localImagePath: String?,
        /**
         *  Print-ready response message.
         */
        val respMessage: String)

data class CreateMessageResponse(
        /**
         * Response code retrieved from the server, or -1 if there's something wrong with the the app
         */
        val code: Int,
        /**
         * Message retrieved from the server. Should be print-ready, can also be error message
         */
        val msg: String,
        /**
         * The server ID retrieved after successful upload, or empty if it fails
         */
        val msgId: String,
        /**
         * Domain ID of the uploaded message, or empty if it fails
         */
        val domainId: String)

data class LoginResponse(
        /**
         * True if the username and password are registered, false otherwise
         */
        val success: Boolean,
        /**
         * Print-ready message from the server indicating successful or failed login attempt
         */
        val msg: String)