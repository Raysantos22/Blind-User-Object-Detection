package com.BlindDetection.feature.home

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.BlindDetection.databinding.OnboardingActivityBinding

class OnBoardingActivity : AppCompatActivity() {
    


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = OnboardingActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)


        }
    }



