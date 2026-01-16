package com.example.autismstroller.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.autismstroller.R

@Composable
fun StartLoadingScreen(){
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.stroller_loading_animated))
    val progress by animateLottieCompositionAsState(composition, iterations = LottieConstants.IterateForever)

    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.Center)
    )
}