/*
 * Copyright 2017 The Hyve
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.radarbase.util

import java.io.IOException
import java.nio.ByteBuffer

/**
 * Header for a [QueueFile].
 *
 * This class is an adaptation of com.squareup.tape2, allowing multi-element writes. It also
 * removes legacy support.
 *
 */
class QueueFileHeader
/**
 * QueueFileHeader that matches storage. If the storage already existed, the header is read from
 * the file. Otherwise, the header is initialized and written to file.
 * @param storage medium to write to.
 * @throws IOException if the storage cannot be read or contains invalid data.
 */
@Throws(IOException::class)
constructor(
        /** Storage to read and write the header.  */
        private val storage: QueueStorage) {

    /** Buffer to read and store the header with.  */
    private val headerBuffer = ByteBuffer.allocate(QUEUE_HEADER_LENGTH.toInt())

    /**
     * Cached file length. Always a power of 2.
     * Setting the stored length does not modify the storage length
     * itself.
     */
    var length: Long = 0

    /** Number of elements.  */
    /** Get the number of elements in the QueueFile.  */
    var count: Int = 0

    val dataLength: Long
        get() = length - QUEUE_HEADER_LENGTH

    /** Version number. Currently fixed to [.VERSIONED_HEADER].  */
    private val version: Int

    /** Position of the first (front-most) element in the queue.  */
    /** Get the position of the first element in the QueueFile.  */
    /** Set the position of the first element in the QueueFile.  */
    var firstPosition: Long = 0

    /** Position of the last (back-most) element in the queue.  */
    /** Get the position of the last element in the QueueFile.  */
    /** Set the position of the last element in the QueueFile.  */
    var lastPosition: Long = 0

    init {
        version = VERSIONED_HEADER
        if (this.storage.isPreExisting) {
            read()
        } else {
            length = this.storage.length
            if (dataLength < 0) {
                throw IOException("Storage does not contain header.")
            }
            count = 0
            firstPosition = 0L
            lastPosition = 0L
            write()
        }
    }

    /** To initialize the header, read it from file.  */
    @Throws(IOException::class)
    private fun read() {
        headerBuffer.rewind()
        storage.readFully(0L, headerBuffer)
        headerBuffer.flip()

        val version = headerBuffer.int
        if (version != VERSIONED_HEADER) {
            throw IOException("Storage $storage is not recognized as a queue file.")
        }
        length = headerBuffer.long
        if (length > storage.length) {
            throw IOException("File is truncated. Expected length: " + length
                    + ", Actual length: " + storage.length)
        }
        count = headerBuffer.int
        firstPosition = headerBuffer.long
        lastPosition = headerBuffer.long

        if (dataLength < 0) {
            throw IOException("File length in $storage header too small")
        }
        if (firstPosition < 0 || firstPosition > length
                || lastPosition < 0 || lastPosition > length) {
            throw IOException("Element offsets point outside of storage $storage")
        }
        if (count < 0 || count > 0 && (firstPosition == 0L || lastPosition == 0L)) {
            throw IOException("Number of elements not correct in storage $storage")
        }
        val crc = headerBuffer.int
        if (crc != hashCode()) {
            throw IOException("Queue storage $storage was corrupted.")
        }
    }

    /**
     * Writes the header to file in a single write operation. This will flush the storage.
     * @throws IOException if the header could not be written
     */
    @Throws(IOException::class)
    fun write() {
        headerBuffer.apply {
            rewind()
            putInt(VERSIONED_HEADER)
            putLong(length)
            putInt(count)
            putLong(firstPosition)
            putLong(lastPosition)
            putInt(this@QueueFileHeader.hashCode())

            // then write the byte buffer out in one go
            flip()
        }
        storage.writeFully(0L, headerBuffer)
        storage.flush()
    }

    /**
     * Hash function for the header, so that it can be verified.
     */
    override fun hashCode(): Int {
        var result = version
        result = 31 * result + (length shr 32 xor length).toInt()
        result = 31 * result + count
        result = 31 * result + (firstPosition shr 32 xor firstPosition).toInt()
        result = 31 * result + (lastPosition shr 32 xor lastPosition).toInt()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val otherHeader = other as QueueFileHeader
        return version == otherHeader.version
                && length == otherHeader.length
                && count == otherHeader.count
                && firstPosition == otherHeader.firstPosition
                && lastPosition == otherHeader.lastPosition
    }

    override fun toString() = "QueueFileHeader[length=$length, size=$count, first=$firstPosition, last=$lastPosition]"

    /** Clear the positions and count. This does not change the stored file length.  */
    fun clear() {
        count = 0
        firstPosition = 0L
        lastPosition = 0L
    }

    companion object {
        /** The header length in bytes.  */
        const val QUEUE_HEADER_LENGTH = 36L

        /** Leading bit set to 1 indicating a versioned header and the version of 1.  */
        private const val VERSIONED_HEADER = 0x00000001
    }
}