package com.example.puzzle

import android.content.Intent
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException
import java.io.InputStream
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs


class PuzzleActivity : AppCompatActivity() {
    var pieces: ArrayList<PuzzlePiece>? = null
    var mCurrentPhotoPath: String? = null
    var mCurrentPhotoUri: String? = null
    private var nbEssais : Int=0
    private  var nbSecondesPassees:Int=0
    var score:Int =100
    private  lateinit var tVScore: TextView
    var i: Int=0
    private  val START_TIME_IN_MILI: Long = 80 * 1000
    private var remainingTime : Long = START_TIME_IN_MILI
    private  var isTimerRunning : Boolean=false
    private var timer: CountDownTimer? =null
    private  lateinit var  textViewEssais: TextView
    private  lateinit var textViewMessage: TextView
    private  lateinit var textViewTimer: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_puzzle)
        tVScore= findViewById(R.id.textViewScore)
        textViewEssais=findViewById(R.id.tv_move_number)
        textViewTimer= findViewById(R.id.tv_time_taken)
        if(!isTimerRunning){
            startTimer()
        }
        val layout = findViewById<RelativeLayout>(R.id.layout)
        val imageView: ImageView = findViewById(R.id.imageView)
        val intent = intent
        val assetName = intent.getStringExtra("assetName")
        mCurrentPhotoPath = intent.getStringExtra("mCurrentPhotoPath")
        mCurrentPhotoUri = intent.getStringExtra("mCurrentPhotoUri")

        // run image related code after the view was laid out
        // to have all dimensions calculated
        imageView.post(Runnable {
            if (assetName != null) {
                setPicFromAsset(assetName, imageView)
            } else if (mCurrentPhotoPath != null) {
                setPicFromPath(mCurrentPhotoPath!!, imageView)
            } else if (mCurrentPhotoUri != null) {
                imageView.setImageURI(Uri.parse(mCurrentPhotoUri))
            }
            pieces = splitImage()
            val touchListener = TouchListener(this@PuzzleActivity)
            // shuffle pieces order
            Collections.shuffle(pieces)
            for (piece in pieces!!) {
                piece.setOnTouchListener(touchListener)
                layout.addView(piece)
                // randomize position, on the bottom of the screen
                val lParams = piece.getLayoutParams() as RelativeLayout.LayoutParams
                lParams.leftMargin = Random().nextInt(layout.width - piece.pieceWidth)
                lParams.topMargin = layout.height - piece.pieceHeight
                piece.setLayoutParams(lParams)
            }
        })
    }

    private fun setPicFromAsset(assetName: String, imageView: ImageView) {
        // Get the dimensions of the View
        val targetW: Int = imageView.getWidth()
        val targetH: Int = imageView.getHeight()
        val am = assets
        try {
            val `is`: InputStream = am.open("img/$assetName")
            // Get the dimensions of the bitmap
            val bmOptions = BitmapFactory.Options()
            bmOptions.inJustDecodeBounds = true
            BitmapFactory.decodeStream(`is`, Rect(-1, -1, -1, -1), bmOptions)
            val photoW = bmOptions.outWidth
            val photoH = bmOptions.outHeight

            // Determine how much to scale down the image
            val scaleFactor = Math.min(photoW / targetW, photoH / targetH)
            `is`.reset()

            // Decode the image file into a Bitmap sized to fill the View
            bmOptions.inJustDecodeBounds = false
            bmOptions.inSampleSize = scaleFactor
            bmOptions.inPurgeable = true
            val bitmap = BitmapFactory.decodeStream(`is`, Rect(-1, -1, -1, -1), bmOptions)
            imageView.setImageBitmap(bitmap)
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show()
        }
    }

    private fun splitImage(): ArrayList<PuzzlePiece>? {
        val piecesNumber = 12
        val rows = 4
        val cols = 3
        val imageView: ImageView = findViewById(R.id.imageView)
        val pieces: ArrayList<PuzzlePiece> = ArrayList(piecesNumber)

        // Get the scaled bitmap of the source image
        val drawable = imageView.getDrawable() as BitmapDrawable
        val bitmap = drawable.bitmap
        val dimensions = getBitmapPositionInsideImageView(imageView)
        val scaledBitmapLeft = dimensions[0]
        val scaledBitmapTop = dimensions[1]
        val scaledBitmapWidth = dimensions[2]
        val scaledBitmapHeight = dimensions[3]
        val croppedImageWidth: Int = scaledBitmapWidth - 2 * abs(scaledBitmapLeft)
        val croppedImageHeight: Int = scaledBitmapHeight - 2 * abs(scaledBitmapTop)
        val scaledBitmap =
            Bitmap.createScaledBitmap(bitmap, scaledBitmapWidth, scaledBitmapHeight, true)
        val croppedBitmap = Bitmap.createBitmap(
            scaledBitmap,
            abs(scaledBitmapLeft),
            abs(scaledBitmapTop),
            croppedImageWidth,
            croppedImageHeight
        )

        // Calculate the with and height of the pieces
        val pieceWidth = croppedImageWidth / cols
        val pieceHeight = croppedImageHeight / rows

        // Create each bitmap piece and add it to the resulting array
        var yCoord = 0
        for (row in 0 until rows) {
            var xCoord = 0
            for (col in 0 until cols) {
                // calculate offset for each piece
                var offsetX = 0
                var offsetY = 0
                if (col > 0) {
                    offsetX = pieceWidth / 3
                }
                if (row > 0) {
                    offsetY = pieceHeight / 3
                }

                // apply the offset to each piece
                val pieceBitmap = Bitmap.createBitmap(
                    croppedBitmap,
                    xCoord - offsetX,
                    yCoord - offsetY,
                    pieceWidth + offsetX,
                    pieceHeight + offsetY
                )
                val piece = PuzzlePiece(applicationContext)
                piece.setImageBitmap(pieceBitmap)
                piece.xCoord = xCoord - offsetX + imageView.getLeft()
                piece.yCoord = yCoord - offsetY + imageView.getTop()
                piece.pieceWidth = pieceWidth + offsetX
                piece.pieceHeight = pieceHeight + offsetY

                // this bitmap will hold our final puzzle piece image
                val puzzlePiece = Bitmap.createBitmap(
                    pieceWidth + offsetX,
                    pieceHeight + offsetY,
                    Bitmap.Config.ARGB_8888
                )

                // draw path
                val bumpSize = pieceHeight / 4
                val canvas = Canvas(puzzlePiece)
                val path = Path()
                path.moveTo(offsetX.toFloat(), offsetY.toFloat())
                if (row == 0) {
                    // top side piece
                    path.lineTo(pieceBitmap.width.toFloat(), offsetY.toFloat())
                } else {
                    // top bump
                    path.lineTo((offsetX + (pieceBitmap.width - offsetX) / 3).toFloat(),
                        offsetY.toFloat()
                    )
                    path.cubicTo(
                        (offsetX + (pieceBitmap.width - offsetX) / 6).toFloat(),
                        (offsetY - bumpSize).toFloat(),
                        (offsetX + (pieceBitmap.width - offsetX) / 6 * 5).toFloat(),
                        (offsetY - bumpSize).toFloat(),
                        (offsetX + (pieceBitmap.width - offsetX) / 3 * 2).toFloat(),
                        offsetY.toFloat()
                    )
                    path.lineTo(pieceBitmap.width.toFloat(), offsetY.toFloat())
                }
                if (col == cols - 1) {
                    // right side piece
                    path.lineTo(pieceBitmap.width.toFloat(), pieceBitmap.height.toFloat())
                } else {
                    // right bump
                    path.lineTo(
                        pieceBitmap.width.toFloat(),
                        (offsetY + (pieceBitmap.height - offsetY) / 3).toFloat()
                    )
                    path.cubicTo(
                        (pieceBitmap.width - bumpSize).toFloat(),
                        (offsetY + (pieceBitmap.height - offsetY) / 6).toFloat(),
                        (pieceBitmap.width - bumpSize).toFloat(),
                        (offsetY + (pieceBitmap.height - offsetY) / 6 * 5).toFloat(),
                        pieceBitmap.width.toFloat(),
                        (offsetY + (pieceBitmap.height - offsetY) / 3 * 2).toFloat()
                    )
                    path.lineTo(pieceBitmap.width.toFloat(), pieceBitmap.height.toFloat())
                }
                if (row == rows - 1) {
                    // bottom side piece
                    path.lineTo(offsetX.toFloat(), pieceBitmap.height.toFloat())
                } else {
                    // bottom bump
                    path.lineTo((offsetX + (pieceBitmap.width - offsetX) / 3 * 2).toFloat(),
                        pieceBitmap.height.toFloat()
                    )
                    path.cubicTo(
                        (offsetX + (pieceBitmap.width - offsetX) / 6 * 5).toFloat(),
                        (pieceBitmap.height - bumpSize).toFloat(),
                        (offsetX + (pieceBitmap.width - offsetX) / 6).toFloat(),
                        (pieceBitmap.height - bumpSize).toFloat(),
                        (offsetX + (pieceBitmap.width - offsetX) / 3).toFloat(),
                        pieceBitmap.height.toFloat()
                    )
                    path.lineTo(offsetX.toFloat(), pieceBitmap.height.toFloat())
                }
                if (col == 0) {
                    // left side piece
                    path.close()
                } else {
                    // left bump
                    path.lineTo(offsetX.toFloat(),
                        (offsetY + (pieceBitmap.height - offsetY) / 3 * 2).toFloat()
                    )
                    path.cubicTo(
                        (offsetX - bumpSize).toFloat(),
                        (offsetY + (pieceBitmap.height - offsetY) / 6 * 5).toFloat(),
                        (offsetX - bumpSize).toFloat(),
                        (offsetY + (pieceBitmap.height - offsetY) / 6).toFloat(),
                        offsetX.toFloat(),
                        (offsetY + (pieceBitmap.height - offsetY) / 3).toFloat()
                    )
                    path.close()
                }

                // mask the piece
                val paint = Paint()
                paint.setColor(-0x1000000)
                paint.setStyle(Paint.Style.FILL)
                canvas.drawPath(path, paint)
                paint.setXfermode(PorterDuffXfermode(PorterDuff.Mode.SRC_IN))
                canvas.drawBitmap(pieceBitmap,0f,0f,paint)

                // draw a white border
                var border = Paint()
                border.setColor(-0x7f000001)
                border.setStyle(Paint.Style.STROKE)
                border.setStrokeWidth(8.0f)
                canvas.drawPath(path, border)

                // draw a black border
                border = Paint()
                border.setColor(-0x80000000)
                border.setStyle(Paint.Style.STROKE)
                border.setStrokeWidth(3.0f)
                canvas.drawPath(path, border)

                // set the resulting bitmap to the piece
                piece.setImageBitmap(puzzlePiece)
                pieces.add(piece)
                xCoord += pieceWidth
            }
            yCoord += pieceHeight
        }
        return pieces
    }

    private fun getBitmapPositionInsideImageView(imageView: ImageView?): IntArray {
        val ret = IntArray(4)
        if (imageView == null || imageView.getDrawable() == null) return ret

        // Get image dimensions
        // Get image matrix values and place them in an array
        val f = FloatArray(9)
        imageView.getImageMatrix().getValues(f)

        // Extract the scale values using the constants (if aspect ratio maintained, scaleX == scaleY)
        val scaleX = f[Matrix.MSCALE_X]
        val scaleY = f[Matrix.MSCALE_Y]

        // Get the drawable (could also get the bitmap behind the drawable and getWidth/getHeight)
        val d: Drawable = imageView.getDrawable()
        val origW = d.intrinsicWidth
        val origH = d.intrinsicHeight

        // Calculate the actual dimensions
        val actW = Math.round(origW * scaleX)
        val actH = Math.round(origH * scaleY)
        ret[2] = actW
        ret[3] = actH

        // Get image position
        // We assume that the image is centered into ImageView
        val imgViewW: Int = imageView.getWidth()
        val imgViewH: Int = imageView.getHeight()
        val top = (imgViewH - actH) / 2
        val left = (imgViewW - actW) / 2
        ret[0] = left
        ret[1] = top
        return ret
    }

    fun checkGameOver() {
        if (isGameOver()) {
            finish()
        }
    }

    private fun isGameOver(): Boolean {
        for (piece in pieces!!) {
            if (piece.canMove) {
                return false
            }
        }
        return true
    }

    private fun setPicFromPath(mCurrentPhotoPath: String, imageView: ImageView) {
        // Get the dimensions of the View
        val targetW: Int = imageView.getWidth()
        val targetH: Int = imageView.getHeight()

        // Get the dimensions of the bitmap
        val bmOptions = BitmapFactory.Options()
        bmOptions.inJustDecodeBounds = true
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions)
        val photoW = bmOptions.outWidth
        val photoH = bmOptions.outHeight

        // Determine how much to scale down the image
        val scaleFactor = Math.min(photoW / targetW, photoH / targetH)

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false
        bmOptions.inSampleSize = scaleFactor
        bmOptions.inPurgeable = true
        val bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions)
        var rotatedBitmap = bitmap

        // rotate bitmap if needed
        try {
            val ei = ExifInterface(mCurrentPhotoPath)
            val orientation: Int = ei.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotatedBitmap = rotateImage(bitmap, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotatedBitmap = rotateImage(bitmap, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotatedBitmap = rotateImage(bitmap, 270f)
            }
        } catch (e: IOException) {
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show()
        }
        imageView.setImageBitmap(rotatedBitmap)
    }

    fun rotateImage(source: Bitmap, angle: Float): Bitmap? {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(
            source, 0, 0, source.width, source.height,
            matrix, true
        )
    }
    private fun startTimer() {
        timer = object : CountDownTimer(remainingTime, 1000) {
            override fun onTick(timeleft: Long) {
                remainingTime=timeleft
                updateTimerText()
                nbSecondesPassees++

            }

            override fun onFinish() {
                textViewTimer.text = "Done!"
                isTimerRunning= false
                getScore()
                gameOver()
            }

        }.start()

        isTimerRunning=true
    }
    private fun gameOver() {
        timer?.cancel()
        getScore()
        val i = Intent(this@PuzzleActivity,GameOver::class.java);
        startActivity(i);
        finish();

    }
            private  fun getScore(){
                score= score - (nbEssais + nbSecondesPassees)
                tVScore.text = score.toString()

            }

            private fun updateTimerText(){
                val minute = remainingTime.div(1000).div(60)
                val second = remainingTime.div(1000) % 60

                val formatTime = String.format("%02d:%02d",minute,second)
                textViewTimer.text= formatTime


            }

}