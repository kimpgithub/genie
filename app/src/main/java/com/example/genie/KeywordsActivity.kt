package com.example.genie

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import okhttp3.ResponseBody
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.io.IOException

class KeywordsActivity : AppCompatActivity() {
    private lateinit var linearLayout: LinearLayout
    private lateinit var imageView: ImageView

    private val retrofitClient: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("http://54.206.68.169:8080/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val apiService: ApiService by lazy {
        retrofitClient.create(ApiService::class.java)
    }

    interface ApiService {
        @GET("texts/{fileName}")
        fun getKeywordsJson(@Path("fileName") fileName: String): Call<ResponseBody>
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        linearLayout = LinearLayout(this)
        linearLayout.orientation = LinearLayout.VERTICAL

        imageView = ImageView(this)
        linearLayout.addView(imageView)

        setContentView(linearLayout)

        Log.d("KeywordsActivity", "Fetching keywords JSON...")

        fetchKeywordsJson()
    }

    private fun fetchKeywordsJson() {
        val fileName = "image.json" // 키워드 추출 JSON 파일 이름

        apiService.getKeywordsJson(fileName).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody != null) {
                        parseJsonAndDisplayKeywords(responseBody.string())
                    } else {
                        Log.e("KeywordsActivity", "Response body is null")
                    }
                } else {
                    Log.e("KeywordsActivity", "Failed to fetch keywords JSON")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("KeywordsActivity", "Error fetching keywords JSON: ${t.message}", t)
            }
        })
    }

    private fun parseJsonAndDisplayKeywords(jsonString: String) {
        try {
            Log.d("KeywordsActivity", "Parsing keywords JSON...")
            val jsonObject = JSONObject(jsonString)
            val textArray = jsonObject.getJSONArray("text")
            val keywords = mutableListOf<String>()

            for (i in 0 until textArray.length()) {
                keywords.add(textArray.getString(i))
            }

            if (keywords.isEmpty()) {
                Log.e("KeywordsActivity", "No text found in JSON response")
            } else {
                displayKeywords(keywords)
            }
        } catch (e: JSONException) {
            Log.e("KeywordsActivity", "Error parsing keywords JSON: ${e.message}", e)
        }
    }

    private fun displayKeywords(keywords: List<String>) {
        linearLayout.removeAllViews() // 기존 뷰 제거

        // 이미지 표시
        val imageBytes = intent.getByteArrayExtra("imageBytes")
        if (imageBytes != null) {
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            imageView.setImageBitmap(bitmap)
        }

        keywords.forEach { keyword ->
            val textView = TextView(this).apply {
                text = keyword
                textSize = 18f
                setPadding(16, 16, 16, 16)
            }
            linearLayout.addView(textView)
        }
    }
}