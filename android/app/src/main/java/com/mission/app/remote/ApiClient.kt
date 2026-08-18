package com.mission.app.remote

import com.mission.app.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayInputStream
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * 构建 OkHttp 客户端：支持打包/导入的自签证书（方案 A），
 * 同时保留系统 CA 信任（公网域名证书场景，方案 B）。
 */
object ApiClient {

    fun create(baseUrl: String, token: String?, certBytes: ByteArray?): ApiService {
        val builder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)

        if (certBytes != null && certBytes.isNotEmpty()) {
            val tm = compositeTrustManager(certBytes)
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, arrayOf<TrustManager>(tm), SecureRandom())
            builder.sslSocketFactory(sslContext.socketFactory, tm)
        }

        builder.addInterceptor { chain ->
            val original = chain.request()
            val request = original.newBuilder()
                .apply {
                    if (!token.isNullOrBlank()) addHeader("Authorization", "Bearer $token")
                }
                .build()
            chain.proceed(request)
        }

        if (BuildConfig.DEBUG) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            builder.addInterceptor(logging)
        }

        val okHttp = builder.build()
        return Retrofit.Builder()
            .baseUrl(normalizeBaseUrl(baseUrl))
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    private fun normalizeBaseUrl(raw: String): String {
        var trimmed = raw.trim()
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            trimmed = "https://$trimmed"
        }
        val url = java.net.URI(trimmed)
        val scheme = url.scheme ?: "https"
        val host = url.host ?: return "$trimmed/"
        val port = if (url.port != -1) ":${url.port}" else ""
        return "$scheme://$host$port/"
    }

    private fun compositeTrustManager(certPem: ByteArray): X509TrustManager {
        val factory = CertificateFactory.getInstance("X.509")
        val cert = factory.generateCertificate(ByteArrayInputStream(certPem)) as X509Certificate

        // 用内置证书构建一个仅信任该证书的 TrustManager
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        keyStore.load(null, null)
        keyStore.setCertificateEntry("sync", cert)
        val extraFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        extraFactory.init(keyStore)
        val extra = extraFactory.trustManagers.first { it is X509TrustManager } as X509TrustManager

        // 系统默认 TrustManager（信任公网 CA）
        val systemFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        systemFactory.init(null as KeyStore?)
        val system = systemFactory.trustManagers.first { it is X509TrustManager } as X509TrustManager

        return object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
                system.checkClientTrusted(chain, authType)
            }

            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                try {
                    system.checkServerTrusted(chain, authType)
                } catch (e: Exception) {
                    // 自签证书场景：回退到内置证书信任
                    extra.checkServerTrusted(chain, authType)
                }
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> =
                (system.acceptedIssuers + extra.acceptedIssuers).distinct().toTypedArray()
        }
    }
}