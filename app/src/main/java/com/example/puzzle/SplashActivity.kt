package com.example.puzzle

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import com.bumptech.glide.Glide
import java.util.*

class SplashActivity : AppCompatActivity() {
    lateinit var imgLoader: ImageView;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        showGif()
        Timer().schedule(object : TimerTask(){
            override fun run() {
                val i = Intent(this@SplashActivity,MainActivity::class.java);
                startActivity(i);
                finish();
            }

        },5500L)

    }

    fun showGif(){
        imgLoader = findViewById(R.id.loader)
        Glide.with(this).load(R.drawable.jigsaw_puzzle_unscreen).into(imgLoader)
    }

}
