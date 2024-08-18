package com.example.bygm

import android.app.Activity
import android.app.ProgressDialog
import android.os.AsyncTask
import android.util.Log
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class OCRAsyncTask(
    private val activity: Activity,
    private val apiKey: String,
    private val isOverlayRequired: Boolean,
    private val inputStream: InputStream,
    private val language: String,
    private val callback: IOCRCallBack
) : AsyncTask<Void, Void, String>() {

    private val url = "https://api.ocr.space/parse/image"
    private val TAG = "OCRAsyncTask"

    override fun onPreExecute() {
        super.onPreExecute()
        // Show progress dialog or loading indicator here
    }

    override fun doInBackground(vararg params: Void?): String? {
        return try {
            val response = sendPost(apiKey, isOverlayRequired, inputStream, language)
            parseOCRResponse(response)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun sendPost(apiKey: String, isOverlayRequired: Boolean, inputStream: InputStream, language: String): String {
        val connection = URL(url).openConnection() as HttpsURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("User-Agent", "Mozilla/5.0")
        connection.setRequestProperty("Accept-Language", "en-US,en;q=0.5")
        connection.doOutput = true

        val boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW"
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")

        val outputStream = DataOutputStream(connection.outputStream)

        outputStream.writeBytes("--$boundary\r\n")
        outputStream.writeBytes("Content-Disposition: form-data; name=\"apikey\"\r\n\r\n")
        outputStream.writeBytes("$apiKey\r\n")

        outputStream.writeBytes("--$boundary\r\n")
        outputStream.writeBytes("Content-Disposition: form-data; name=\"isOverlayRequired\"\r\n\r\n")
        outputStream.writeBytes("$isOverlayRequired\r\n")

        outputStream.writeBytes("--$boundary\r\n")
        outputStream.writeBytes("Content-Disposition: form-data; name=\"language\"\r\n\r\n")
        outputStream.writeBytes("$language\r\n")

        outputStream.writeBytes("--$boundary\r\n")
        outputStream.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"image.jpg\"\r\n")
        outputStream.writeBytes("Content-Type: image/jpeg\r\n\r\n")

        val buffer = ByteArray(4096)
        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            outputStream.write(buffer, 0, bytesRead)
        }

        outputStream.writeBytes("\r\n")
        outputStream.writeBytes("--$boundary--\r\n")
        outputStream.flush()
        outputStream.close()
        inputStream.close()

        val response = StringBuilder()
        BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                response.append(line)
            }
        }

        return response.toString()
    }

    private fun parseOCRResponse(response: String): String {
        return try {
            val jsonResponse = JSONObject(response)
            val parsedResults = jsonResponse.getJSONArray("ParsedResults")
            val firstResult = parsedResults.getJSONObject(0)
            val parsedText = firstResult.getString("ParsedText")
            parsedText
        } catch (e: Exception) {
            e.printStackTrace()
            "Error parsing response"
        }
    }

    override fun onPostExecute(result: String?) {
        super.onPostExecute(result)
        // Dismiss progress dialog or loading indicator here
        callback.getOCRCallBackResult(result)
        Log.d(TAG, result.toString())
    }
}