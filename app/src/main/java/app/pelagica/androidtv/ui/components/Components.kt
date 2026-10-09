package app.pelagica.androidtv.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import app.pelagica.androidtv.R
import app.pelagica.androidtv.ui.theme.ButtonContainer
import app.pelagica.androidtv.ui.theme.ButtonContent
import app.pelagica.androidtv.ui.theme.FocusRing
import app.pelagica.androidtv.ui.theme.Geist
import app.pelagica.androidtv.ui.theme.InputBorder
import app.pelagica.androidtv.ui.theme.OnBackground
import app.pelagica.androidtv.ui.theme.Placeholder
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

private val CornerRadius = 12.dp
private val RingWidth = 3.dp
private val RingGap = 3.dp

/**
 * Draws the focus ring *outside* the control: the ring sits [RingGap] away from the
 * control's edge. Space for the ring is always reserved so focusing never shifts layout.
 */
@Composable
private fun Modifier.focusRing(focused: Boolean, radius: Dp = CornerRadius): Modifier {
    val color by animateColorAsState(
        targetValue = if (focused) FocusRing else Color.Transparent,
        label = "focusRing",
    )
    val inset = RingWidth + RingGap
    return this
        .border(RingWidth, color, RoundedCornerShape(radius + inset))
        .padding(inset)
}

enum class ButtonStyle { Primary, Secondary }

@Composable
fun PelagicaButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ButtonStyle = ButtonStyle.Primary,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(CornerRadius)
    val container = when (style) {
        ButtonStyle.Primary -> ButtonContainer
        ButtonStyle.Secondary -> if (focused) Color(0x1FFFFFFF) else Color.Transparent
    }

    Box(
        modifier = modifier
            .focusRing(focused)
            .clip(shape)
            .background(container)
            .then(if (style == ButtonStyle.Secondary) Modifier.border(1.dp, InputBorder, shape) else Modifier)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
fun PelagicaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ButtonStyle = ButtonStyle.Primary,
    loading: Boolean = false,
) {
    val contentColor = if (style == ButtonStyle.Primary) ButtonContent else OnBackground
    // Stay enabled while loading so the button keeps focus; just ignore the click.
    PelagicaButton(onClick = { if (!loading) onClick() }, modifier = modifier, style = style) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (loading) Spinner(color = contentColor)
            Text(text = text, color = contentColor, fontSize = 18.sp, fontWeight = FontWeight.Medium)
        }
    }
}

/**
 * A D-pad friendly text field
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PelagicaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
) {
    var editing by remember { mutableStateOf(false) }
    var containerFocused by remember { mutableStateOf(false) }
    var fieldFocused by remember { mutableStateOf(false) }
    val fieldFocusRequester = remember { FocusRequester() }
    val shape = RoundedCornerShape(CornerRadius)

    LaunchedEffect(editing) {
        if (editing) fieldFocusRequester.requestFocus()
    }

    // Dismissing the keyboard with Back doesn't unfocus the field, which would leave it
    // stuck in edit mode. Treat the keyboard closing as the end of editing.
    val imeVisible = WindowInsets.isImeVisible
    var imeShown by remember { mutableStateOf(false) }
    LaunchedEffect(imeVisible, editing) {
        if (!editing) {
            imeShown = false
        } else if (imeVisible) {
            imeShown = true
        } else if (imeShown) {
            editing = false
            focusRequester.requestFocus()
        }
    }

    Box(
        modifier = modifier
            .focusRing(containerFocused || fieldFocused)
            .clip(shape)
            .border(1.dp, InputBorder, shape)
            .focusRequester(focusRequester)
            .onFocusChanged { containerFocused = it.isFocused }
            .onKeyEvent {
                val isConfirm = it.key == Key.DirectionCenter || it.key == Key.Enter || it.key == Key.NumPadEnter
                if (!editing && isConfirm && it.type == KeyEventType.KeyUp) {
                    editing = true
                    true
                } else {
                    false
                }
            }
            .focusable()
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = OnBackground, fontSize = 18.sp, fontFamily = Geist),
            cursorBrush = SolidColor(OnBackground),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (isPassword) KeyboardType.Password else keyboardType,
                imeAction = imeAction,
            ),
            keyboardActions = KeyboardActions(onAny = {
                editing = false
                focusRequester.requestFocus()
                onImeAction()
            }),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(fieldFocusRequester)
                .focusProperties { canFocus = editing }
                .onFocusChanged {
                    fieldFocused = it.isFocused
                    if (!it.isFocused) editing = false
                },
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty()) {
                        Text(text = placeholder, color = Placeholder, fontSize = 18.sp)
                    }
                    innerTextField()
                }
            },
        )
    }
}

@Composable
fun Spinner(modifier: Modifier = Modifier, color: Color = OnBackground) {
    val transition = rememberInfiniteTransition(label = "spinner")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 900, easing = LinearEasing)),
        label = "spinnerAngle",
    )
    Canvas(modifier.size(16.dp)) {
        val stroke = 2.dp.toPx()
        rotate(angle) {
            drawArc(
                color = color,
                startAngle = 0f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(stroke / 2, stroke / 2),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
    }
}

@Composable
fun PelagicaLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.pelagica_logo),
        contentDescription = "Pelagica",
        modifier = modifier.size(56.dp),
    )
}

/** Renders [content] as a QR code: black modules on a white, rounded card. */
@Composable
fun QrCode(content: String, modifier: Modifier = Modifier, codeSize: Dp = 200.dp) {
    val matrix = remember(content) {
        QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, mapOf(EncodeHintType.MARGIN to 0))
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(CornerRadius))
            .background(Color.White)
            .padding(12.dp),
    ) {
        Canvas(Modifier.size(codeSize)) {
            val cell = size.width / matrix.width
            for (y in 0 until matrix.height) {
                for (x in 0 until matrix.width) {
                    if (matrix[x, y]) {
                        // Slight overlap avoids hairline gaps between modules.
                        drawRect(Color.Black, Offset(x * cell, y * cell), Size(cell + 0.5f, cell + 0.5f))
                    }
                }
            }
        }
    }
}

val FormWidth = 520.dp

fun Modifier.formWidth(): Modifier = this.width(FormWidth)
