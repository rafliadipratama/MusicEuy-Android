package com.rafliadipratama.musiceuy.data.network

import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

class NewPipeDownloader private constructor(private val client: OkHttpClient) : Downloader() {

    companion object {
        fun create(): NewPipeDownloader {
            val client = OkHttpClient.Builder()
                .readTimeout(20, TimeUnit.SECONDS)
                .connectTimeout(20, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
            return NewPipeDownloader(client)
        }
    }

    @Throws(IOException::class)
    override fun execute(request: Request): Response {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val okHeadersBuilder = Headers.Builder()
        for ((key, values) in headers) {
            for (value in values) {
                okHeadersBuilder.add(key, value)
            }
        }
        val okHeaders = okHeadersBuilder.build()

        val body = dataToSend?.toRequestBody(null)

        val okHttpRequest = okhttp3.Request.Builder()
            .url(url)
            .headers(okHeaders)
            .method(httpMethod, body)
            .build()

        val okHttpResponse = client.newCall(okHttpRequest).execute()

        val responseCode = okHttpResponse.code
        val responseMessage = okHttpResponse.message
        val responseBody = okHttpResponse.body?.string()
        val responseHeaderMap = okHttpResponse.headers.toMultimap()
        val latestUrl = okHttpResponse.request.url.toString()

        return Response(
            responseCode,
            responseMessage,
            responseHeaderMap,
            responseBody,
            latestUrl
        )
    }
}
