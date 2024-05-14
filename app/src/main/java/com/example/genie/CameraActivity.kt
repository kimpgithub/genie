package com.example.genie

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.camera.view.PreviewView
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import kotlin.math.max
import kotlin.math.min
import android.graphics.Paint
import android.graphics.Canvas
import android.graphics.Color

class CameraActivity : AppCompatActivity() {
    private lateinit var binding: ConstraintLayout
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    private val retrofitClient: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("http://3.27.46.26:8080/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(OkHttpClient.Builder().build())
            .build()
    }

    private val apiService: ApiService by lazy {
        retrofitClient.create(ApiService::class.java)
    }

    interface ApiService {
        @Multipart
        @POST("upload")
        fun uploadImage(@Part image: MultipartBody.Part): Call<Unit>
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ConstraintLayout(this)
        setContentView(binding)

        // Apply window insets for edge-to-edge layout
        ViewCompat.setOnApplyWindowInsetsListener(binding) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        startCamera()

        val btnCapture = Button(this)
        btnCapture.text = "Capture"
        btnCapture.setOnClickListener { takePhoto() }

        val layoutParams = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.WRAP_CONTENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
        layoutParams.leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID
        layoutParams.rightToRight = ConstraintLayout.LayoutParams.PARENT_ID
        btnCapture.layoutParams = layoutParams

        binding.addView(btnCapture)

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val previewView = PreviewView(this)
            val overlayView = OverlayView(this)

            val preview = Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build().also {
                    Log.d("CameraActivity", "ImageCapture options: ${it.getCaptureMode()}")
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                Toast.makeText(this, "Failed to start camera", Toast.LENGTH_SHORT).show()
            }

            // ConstraintLayout의 크기를 확장하여 프리뷰가 전체 화면에 표시되도록 함
            val layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.MATCH_PARENT
            )
            previewView.layoutParams = layoutParams

            // 뷰의 크기가 변경될 때마다 ROI 영역을 설정하기 위해 ViewTreeObserver를 사용
            previewView.viewTreeObserver.addOnGlobalLayoutListener {
                Log.d("CameraActivity", "ViewTreeObserver 콜백 호출됨")

                // 고정 ROI 영역 설정 (5cm x 2cm 크기로 가정)
                val roiWidth = previewView.width / 2
                val roiHeight = previewView.height / 5
                val centerX = previewView.width / 2
                val centerY = previewView.height / 2
                val left = centerX - roiWidth / 2
                val top = centerY - roiHeight / 2
                val right = centerX + roiWidth / 2
                val bottom = centerY + roiHeight / 2

                overlayView.setROIRect(left, top, right, bottom)
            }
            binding.addView(previewView) // 프리뷰를 ConstraintLayout에 추
            binding.addView(overlayView)
        }, ContextCompat.getMainExecutor(this))
    }
    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        imageCapture.takePicture(ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                Log.d("CameraActivity", "onCaptureSuccess() called on thread: ${Thread.currentThread().name}")
                val buffer = image.planes[0].buffer
                val data = ByteArray(buffer.remaining())
                buffer.get(data)
                image.close()
                processCapturedImage(data)
            }

            override fun onError(exc: ImageCaptureException) {
                showToast("Photo capture failed: ${exc.message}")
            }
        })
    }

    private fun processCapturedImage(imageData: ByteArray) {
        // Convert data to Bitmap
        val capturedImage = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)

        // Extract ROI from capturedImage (same logic as getROIImage)
        val roiBitmap = getROIImage(capturedImage)

        // Upload ROI
        uploadImage(bitmapToByteArray(roiBitmap))
    }

    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        Log.d("CameraActivity", "bitmapToByteArray called")
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        return stream.toByteArray()
    }

    private fun getROIImage(image: Bitmap): Bitmap {
        Log.d("CameraActivity", "getROIImage called")
        val width = image.width
        val height = image.height

        // Calculate the size of the ROI rectangle (e.g., 5cm x 2cm size)
        val roiWidth = width / 2 // 예시로 5cm
        val roiHeight = height / 5 // 예시로 2cm

        // Calculate the center coordinates of the image
        val centerX = width / 2
        val centerY = height / 2

        // Calculate the left, top, right, and bottom coordinates of the ROI rectangle
        val left = centerX - roiWidth / 2
        val top = centerY - roiHeight / 2
        val right = centerX + roiWidth / 2
        val bottom = centerY + roiHeight / 2

        // Clip the coordinates to ensure they are within the image boundaries
        val leftPixel = max(left, 0)
        val topPixel = max(top, 0)
        val rightPixel = min(right, width)
        val bottomPixel = min(bottom, height)

        // Create the ROI bitmap using the calculated coordinates
        return Bitmap.createBitmap(image, leftPixel, topPixel, rightPixel - leftPixel, bottomPixel - topPixel)
    }

    private fun uploadImage(imageBytes: ByteArray) {
        val requestFile = imageBytes.toRequestBody("image/jpg".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("image", "image.jpg", requestFile)

        Log.d("CameraActivity", "uploadImage called")

        apiService.uploadImage(body).enqueue(object : Callback<Unit> {
            override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                Log.d("CameraActivity", "onResponse called")

                if (response.isSuccessful) {
                    Log.d("CameraActivity", "Image uploaded successfully")
                    showToast("Image uploaded successfully")
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = "Upload failed: " + (errorBody ?: "Unknown error")
                    Log.e("CameraActivity", "Image upload failed: $errorMessage")
                    showToast(errorMessage)
                }
            }

            override fun onFailure(call: Call<Unit>, t: Throwable) {
                Log.e("CameraActivity", "Image upload failed: ${t.message}", t)
                showToast("Image upload failed: ${t.message}")
            }
        })
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this@CameraActivity, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}

class OverlayView(context: Context) : View(context) {
    private val paint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private var left = 0f
    private var top = 0f
    private var right = 0f
    private var bottom = 0f

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        // 디버그 포인트 추가
        Log.d("OverlayView", "onMeasure() 호출됨: widthMeasureSpec=$widthMeasureSpec, heightMeasureSpec=$heightMeasureSpec")
    }

    fun setROIRect(left: Int, top: Int, right: Int, bottom: Int) {
        Log.d("OverlayView", "setROIRect 호출됨: left=$left, top=$top, right=$right, bottom=$bottom")

        this.left = left.toFloat()
        this.top = top.toFloat()
        this.right = right.toFloat()
        this.bottom = bottom.toFloat()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(left, top, right, bottom, paint)
    }
}

