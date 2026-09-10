@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
package com.kelvinsaputra.promptstudio.platform

import kotlinx.cinterop.*
import platform.Foundation.*
import platform.posix.memcpy

internal fun appDirectory(): String {
    val base = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true).first() as String
    val path = "$base/Prompt Studio"
    check(NSFileManager.defaultManager.createDirectoryAtPath(path, true, null, null))
    return path
}
internal fun ByteArray.asData(): NSData = if (isEmpty()) NSData() else usePinned { NSData.create(bytes = it.addressOf(0), length = size.toULong()) }
internal fun NSData.asBytes(): ByteArray = ByteArray(length.toInt()).also { array ->
    if (array.isNotEmpty()) array.usePinned { memcpy(it.addressOf(0), bytes, length) }
}
internal fun readBytes(path: String): ByteArray? {
    if (!NSFileManager.defaultManager.fileExistsAtPath(path)) return null
    return checkNotNull(NSData.dataWithContentsOfFile(path)).asBytes()
}
internal fun atomicWrite(path: String, bytes: ByteArray) { check(bytes.asData().writeToFile(path, true)) }
internal fun removeFile(path: String) {
    if (NSFileManager.defaultManager.fileExistsAtPath(path)) check(NSFileManager.defaultManager.removeItemAtPath(path, null))
}
