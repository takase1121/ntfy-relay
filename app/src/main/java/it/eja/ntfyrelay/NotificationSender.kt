// Copyright (C) 2024 by Ubaldo Porcheddu <ubaldo@eja.it>
// Copyright (C) 2025 by Takase

package it.eja.ntfyrelay

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Icon
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import kotlin.random.Random

private const val RETRY_TIMEOUT = 5000
private const val ICON_DIM = 32

class NotificationSender(private val context: Context) {
    private val threadPool = Executors.newSingleThreadExecutor()

    fun sendNotification(
        config: NotificationServerConfig,
        title: String,
        message: String,
        icon: Icon
    ) {
        if (title.isEmpty() && message.isEmpty()) return

        threadPool.submit {
            do {
                val url = URL(config.url)
                val connection = url.openConnection() as HttpURLConnection
                val username = config.username
                val password = config.password

                when (config.authType) {
                    NotificationServerConfig.AuthType.HTTP_BASIC -> connection.setRequestProperty(
                        "Authorization", "Basic ${
                            Base64.encodeToString(
                                "$username:$password".toByteArray(),
                                Base64.NO_WRAP
                            )
                        }"
                    )

                    NotificationServerConfig.AuthType.HTTP_TOKEN -> connection.setRequestProperty(
                        "Authorization",
                        "Bearer $password"
                    )

                    NotificationServerConfig.AuthType.HTTP_NONE -> Unit
                }

                connection.requestMethod = "POST"
                connection.setRequestProperty("X-Title", title)
                connection.doOutput = true

                val outputStream = connection.outputStream
                outputStream.write(message.toByteArray())
                outputStream.flush()
                outputStream.close()

                val responseCode = connection.responseCode
                if (responseCode == 429) {
                    // sleep for a random amount of time, then retry
                    Thread.sleep(
                        RETRY_TIMEOUT + Random.Default.nextLong(
                            (-0.1 * RETRY_TIMEOUT).toLong(),
                            (0.1 * RETRY_TIMEOUT).toLong()
                        )
                    )
                } else if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw RuntimeException("Unexpected response code: $responseCode")
                }
            } while (responseCode == 429)
        }
    }

    // when this is supported :)
    private fun getBitmapFromIcon(icon: Icon): String? {
        val drawable = icon.loadDrawable(context) ?: return null

        val bitmap = if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
            Bitmap.createBitmap(
                1,
                1,
                Bitmap.Config.ARGB_8888
            ); // Single color bitmap will be created of 1x1 pixel
        } else {
            Bitmap.createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            );
        }

        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        val bitmapStream = ByteArrayOutputStream()
        Bitmap.createScaledBitmap(bitmap, ICON_DIM, ICON_DIM, true)
            .compress(Bitmap.CompressFormat.PNG, 100, bitmapStream)
        return "data:image/png;base64,${
            Base64.encodeToString(
                bitmapStream.toByteArray(),
                Base64.NO_WRAP
            )
        }"
    }
}