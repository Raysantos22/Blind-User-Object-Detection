package com.BlindDetection.feature.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.BlindDetection.R

enum class OnBoardingPage(
    @StringRes val titleResource: Int,
    @StringRes val subTitleResource: Int,
    @StringRes val descriptionResource: Int,
//    @DrawableRes val logoResource: Int
) {

    ONE(
        R.string.onboarding_slide1_title,
        R.string.onboarding_slide1_subtitle,
        R.string.onboarding_slide1_desc,
//        R.drawable.one
    ),
//    TWO(
//        R.string.onboarding_slide2_title,
//        R.string.onboarding_slide2_subtitle,
//        R.string.onboarding_slide2_desc,
//        R.drawable.second
//    ),
//    THREE(
//        R.string.onboarding_slide3_title,
//        R.string.onboarding_slide3_subtitle,
//        R.string.onboarding_slide3_desc, // Corrected the description resource ID
//        R.drawable.three
//    ),
//    FOUR( // Corrected the entry name
//        R.string.onboarding_slide4_title, // Adjusted the resource ID
//        R.string.onboarding_slide4_subtitle, // Adjusted the resource ID
//        R.string.onboarding_slide4_desc, // Adjusted the resource ID
//        R.drawable.fourth
//    ),
//   FIVE( // Corrected the entry name
//       R.string.onboarding_slide4_title, // Adjusted the resource ID
//       R.string.onboarding_slide4_subtitle, // Adjusted the resource ID
//       R.string.onboarding_slide4_desc, // Adjusted the resource ID
//        R.drawable.dis
//    ),
}