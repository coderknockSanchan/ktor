/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.client.features

import io.ktor.client.*
import io.ktor.client.response.*
import kotlinx.coroutines.*
import kotlinx.coroutines.io.*
import kotlinx.coroutines.io.jvm.javaio.*
import kotlinx.io.streams.*
import java.io.*

internal actual fun HttpClient.platformDefaultTransformers() {
    responsePipeline.intercept(HttpResponsePipeline.Parse) { (info, body) ->
        if (body !is ByteReadChannel) return@intercept
        when (info.type) {
            InputStream::class -> {
                val stream = body.toInputStream(context.coroutineContext[Job])
                val response = object : InputStream() {
                    override fun read(): Int = stream.read()
                    override fun read(b: ByteArray, off: Int, len: Int): Int = stream.read(b, off, len)

                    override fun available(): Int = stream.available()

                    override fun close() {
                        super.close()
                        stream.close()
                        context.response.close()
                    }
                }
                proceedWith(HttpResponseContainer(info, response))
            }
        }
    }
}
