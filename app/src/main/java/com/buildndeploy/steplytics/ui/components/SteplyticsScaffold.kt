package com.buildndeploy.steplytics.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.buildndeploy.steplytics.ui.theme.AppBackground
import com.buildndeploy.steplytics.ui.theme.AppBackgroundSecondary

@Composable
fun SteplyticsScaffold(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(AppBackground, AppBackgroundSecondary)
                )
            )
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        content()
    }
}
