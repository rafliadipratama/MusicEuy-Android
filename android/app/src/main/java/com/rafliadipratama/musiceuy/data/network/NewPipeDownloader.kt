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
        var hasUserAgent = false
        var hasCookie = false
        var hasAcceptLanguage = false

        for ((key, values) in headers) {
            if (key.equals("User-Agent", ignoreCase = true)) hasUserAgent = true
            if (key.equals("Cookie", ignoreCase = true)) hasCookie = true
            if (key.equals("Accept-Language", ignoreCase = true)) hasAcceptLanguage = true
            for (value in values) {
                okHeadersBuilder.add(key, value)
            }
        }

        if (!hasUserAgent) {
            okHeadersBuilder.add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36")
        }
        if (!hasAcceptLanguage) {
            okHeadersBuilder.add("Accept-Language", "id-ID,id;q=0.9,en-US;q=0.8,en;q=0.7")
        }
        if (!hasCookie) {
            okHeadersBuilder.add("Cookie", "SOCS=CAESEwgDEgk2OTg1NTA0NjEaAmVuIAEaBgiA_LyaBg; CONSENT=PENDING+999; PREF=hl=id&gl=ID")
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
