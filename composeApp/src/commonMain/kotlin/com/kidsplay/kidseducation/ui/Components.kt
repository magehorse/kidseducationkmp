package com.kidsplay.kidseducation.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidsplay.kidseducation.platform.LocalDeps
import org.jetbrains.compose.resources.painterResource

/** Draws the artwork called [name] (see GeneratedDrawables.kt). Unknown names draw nothing. */
@Composable
fun GameImage(name: String?, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Fit) {
    val resource = name?.let { drawableByName[it] } ?: return
    Image(painterResource(resource), contentDescription = null, modifier = modifier, contentScale = contentScale)
}

/** Each letter in a different colour — replaces Utils.setColorfulTitle. */
fun colorfulText(text: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    text.forEach { ch ->
        if (ch.isWhitespace()) append(ch) else {
            withStyle(SpanStyle(color = KidsColors.Rainbow[i % KidsColors.Rainbow.size])) { append(ch) }
            i++
        }
    }
}

@Composable
fun ColorfulTitle(text: String, modifier: Modifier = Modifier, fontSize: TextUnit = 28.sp) {
    Text(
        text = colorfulText(text),
        modifier = modifier,
        style = MaterialTheme.typography.headlineMedium.copy(fontSize = fontSize),
        textAlign = TextAlign.Center,
        maxLines = 2,
    )
}

/** Round white button with a yellow ring (same look as the floating reel button). Label is centred exactly. */
@Composable
fun RoundIconButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    val fontSize = (size.value * 0.46f).sp
    Box(
        modifier = modifier.size(size).shadow(4.dp, CircleShape).clip(CircleShape).background(Color.White)
            .border(2.dp, KidsColors.Sun, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontSize = fontSize,
            lineHeight = fontSize,
            color = KidsColors.HeaderBottom,
            fontWeight = FontWeight.Bold,
            style = TextStyle(lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)),
        )
    }
}

/** Back button: white circle, yellow ring, and an arrow drawn on a Canvas so it is perfectly centred. */
@Composable
fun BackButton(onClick: () -> Unit, modifier: Modifier = Modifier, buttonSize: Dp = 44.dp) {
    Box(
        modifier = modifier.size(buttonSize).shadow(4.dp, CircleShape).clip(CircleShape).background(Color.White)
            .border(2.dp, KidsColors.Sun, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(buttonSize * 0.5f)) {
            val cx = this.size.width / 2f
            val cy = this.size.height / 2f
            val half = this.size.minDimension * 0.42f   // half the shaft length
            val head = half * 0.75f
            val arrow = Path().apply {
                moveTo(cx + half, cy); lineTo(cx - half, cy)                                   // shaft
                moveTo(cx - half + head, cy - head); lineTo(cx - half, cy); lineTo(cx - half + head, cy + head) // head
            }
            drawPath(
                arrow, KidsColors.HeaderBottom,
                style = Stroke(width = this.size.minDimension * 0.16f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

/**
 * Top bar used by EVERY screen (incl. Home and Reels): solid blue, white title, back button on the left,
 * optional [trailing] on the right, yellow scalloped border underneath (the old app's header).
 */
@Composable
fun ScreenHeader(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth().background(KidsColors.HeaderBottom)) {
        Box(Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 10.dp)) {
            if (onBack != null) BackButton(onBack, Modifier.align(Alignment.CenterStart))
            Text(
                text = title,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 22.sp,
                    letterSpacing = 0.sp,
                    shadow = Shadow(Color(0x55000000), Offset(0f, 3f), 6f),
                ),
                modifier = Modifier.align(Alignment.Center).padding(horizontal = 64.dp),
            )
            if (trailing != null) Box(Modifier.align(Alignment.CenterEnd)) { trailing() }
        }
        GameImage("header_scallop", Modifier.fillMaxWidth().height(12.dp), ContentScale.FillBounds)
    }
}

/**
 * Standard screen: blue [ScreenHeader], page gradient, and the platform banner-ad slot at the bottom.
 * [showBanner] is false only for the bottom-tab bodies, because MainScreen already shows one banner above its tab bar.
 */
@Composable
fun ScreenFrame(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    showBanner: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    Column(modifier.fillMaxSize().background(KidsColors.PageBrush)) {
        ScreenHeader(title, onBack, trailing = trailing)
        Box(Modifier.weight(1f).fillMaxWidth(), content = content)
        if (showBanner) LocalDeps.current.ads.Banner(Modifier.fillMaxWidth())
    }
}

/** White rounded card used for every picture tile. */
@Composable
fun TileCard(modifier: Modifier = Modifier, borderColor: Color = Color.White, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    val base = Modifier.let { if (onClick != null) it.clickable(onClick = onClick) else it }
    Card(
        modifier = modifier.then(base),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(3.dp, borderColor),
    ) { content() }
}

@Composable
fun BigButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = KidsColors.HeaderTop, enabled: Boolean = true) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(30.dp))
            .background(if (enabled) color else Color.LightGray)
            .let { if (enabled) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 28.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Color.White, style = MaterialTheme.typography.titleMedium, fontSize = 18.sp)
    }
}

@Composable
fun CenteredMessage(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.padding(24.dp))
    }
}
