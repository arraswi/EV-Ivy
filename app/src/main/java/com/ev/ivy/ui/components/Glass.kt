package com.ev.ivy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ev.ivy.ui.theme.EvBlue
import com.ev.ivy.ui.theme.EvBlueSecondary
import com.ev.ivy.ui.theme.EvGray
import com.ev.ivy.ui.theme.EvWhite

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF161C29).copy(alpha = 0.92f), Color(0xFF0C1017))
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(Color(0x44FFFFFF), Color(0x0AFFFFFF), Color(0x33FFFFFF))
                ),
                shape = shape
            )
            .padding(contentPadding),
        content = content
    )
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        color = EvBlue,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 2.sp
    )
}

@Composable
fun CardTitle(text: String) {
    Text(text = text, color = EvWhite, fontSize = 17.sp, fontWeight = FontWeight.Bold)
}

@Composable
fun CardSubtitle(text: String) {
    Text(text = text, color = EvGray, fontSize = 13.sp)
}

@Composable
fun GradientAccentBar(modifier: Modifier = Modifier) {
    Spacer(
        modifier = modifier
            .height(3.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(2.dp))
            .background(Brush.horizontalGradient(listOf(EvBlue, EvBlueSecondary, Color.Transparent)))
    )
}
