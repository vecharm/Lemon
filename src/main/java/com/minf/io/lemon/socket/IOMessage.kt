package com.minf.io.lemon.socket

/**
 * The Class IOMessage.
 */
internal class IOMessage {

    /** The field values  */
    private val fields = arrayOfNulls<String>(NUM_FIELDS)

    /** Type  */
    /**
     * Returns the type of this IOMessage.
     *
     * @return the type
     */
    var type: Int = 0
        private set

    /**
     * Returns the id of this IOMessage.
     *
     * @return the id
     */
    /**
     * Sets the id of this IOMessage
     *
     * @param id
     */
    var id: String?
        get() = fields[FIELD_ID]
        set(id) {
            fields[FIELD_ID] = id
        }

    /**
     * Returns the endpoint of this IOMessage.
     *
     * @return the endpoint
     */
    val endpoint: String?
        get() = fields[FIELD_ENDPOINT]


    /**
     * Returns the data of this IOMessage.
     *
     * @return the data
     */
    val data: String?
        get() = fields[FIELD_DATA]

    /**
     * Instantiates a new IOMessage by given data.
     *
     * @param type
     * the type
     * @param id
     * the id
     * @param namespace
     * the namespace
     * @param data
     * the data
     */
    constructor(type: Int, id: String?, namespace: String, data: String) {
        this.type = type
        this.fields[FIELD_ID] = id
        this.fields[FIELD_TYPE] = "" + type
        this.fields[FIELD_ENDPOINT] = namespace
        this.fields[FIELD_DATA] = data
    }

    /**
     * Instantiates a new IOMessage by given data.
     *
     * @param type
     * the type
     * @param namespace
     * the name space
     * @param data
     * the data
     */
    constructor(type: Int, namespace: String, data: String) : this(type, null, namespace, data)

    /**
     * Instantiates a new IOMessage from a String representation. If the String
     * is not well formated, the result is undefined.
     *
     * @param message
     * the message
     */
    constructor(message: String) {
        val fields = message.split(":".toRegex(), NUM_FIELDS).toTypedArray()
        for (i in fields.indices) {
            this.fields[i] = fields[i]
            if (i == FIELD_TYPE)
                this.type = Integer.parseInt(fields[i])
        }

    }

    /**
     * Generates a String representation of this object.
     */
    override fun toString(): String {
        val builder = StringBuilder()

        for (i in this.fields.indices) {
            builder.append(':')
            if (this.fields[i] != null) {
                builder.append(this.fields[i])
            }
        }

        return builder.substring(1)
    }

    companion object {

        /** Message type disconnect  */
        const val TYPE_DISCONNECT = 0

        /** Message type connect  */
        const val TYPE_CONNECT = 1

        /** Message type heartbeat  */
        const val TYPE_HEARTBEAT = 2

        /** Message type message  */
        const val TYPE_MESSAGE = 3

        /** Message type JSON message  */
        const val TYPE_JSON_MESSAGE = 4

        /** Message type event  */
        const val TYPE_EVENT = 5

        /** Message type acknowledge  */
        const val TYPE_ACK = 6

        /** Message type error  */
        const val TYPE_ERROR = 7

        /** Message type noop  */
        const val TYPE_NOOP = 8

        /** Index of the type field in a message  */
        const val FIELD_TYPE = 0

        /** Index of the id field in a message  */
        const val FIELD_ID = 1

        /** Index of the end point field in a message  */
        const val FIELD_ENDPOINT = 2

        /** Index of the data field in a message  */
        const val FIELD_DATA = 3

        /** Number of fields in a message.  */
        const val NUM_FIELDS = 4
    }

}