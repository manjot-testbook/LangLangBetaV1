package com.example.langlangbetav1.upi

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.langlangbetav1.audio.SoundPlayer
import com.example.langlangbetav1.ui.theme.PurplePrimary
import kotlinx.coroutines.delay

// ─────────────────────────────────────────────────────────────────────────────
// Shared colours (PhonePe palette)
// ─────────────────────────────────────────────────────────────────────────────
private val PhonePeBg     = Color(0xFF13002C)
private val PhonePePurple = Color(0xFF6B21A8)
private val PhonePeLight  = Color(0xFFCE93D8)
private val OnSurface     = Color(0xFFFFFFFF)
private val OnSurface60   = Color(0x99FFFFFF)
private val SurfaceCard   = Color(0xFF1E0A3C)
private val SuccessGreen  = Color(0xFF4CAF50)

// ─────────────────────────────────────────────────────────────────────────────
// 1. UPI Payment Screen  (PhonePe-style)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun UpiPaymentScreen(navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(PhonePeBg, Color(0xFF0D001F))))
    ) {
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // ── Header ────────────────────────────────────────────────────
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text       = "phonepe",
                    color      = OnSurface,
                    fontSize   = 28.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp,
                )
                Text(
                    text  = "UPI Payment",
                    color = OnSurface60,
                    fontSize = 13.sp,
                )
            }

            // ── Merchant card ─────────────────────────────────────────────
            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceCard)
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(PurplePrimary, RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("🎓", fontSize = 36.sp)
                }

                Text(
                    text       = "LangLang Premium",
                    color      = OnSurface,
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text  = "Unlock unlimited lessons",
                    color = OnSurface60,
                    fontSize = 14.sp,
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(0.08f))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Amount", color = OnSurface60, fontSize = 15.sp)
                    Text(
                        text       = "₹499.00",
                        color      = OnSurface,
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("UPI ID", color = OnSurface60, fontSize = 15.sp)
                    Text("langlang@ybl", color = PhonePeLight, fontSize = 14.sp)
                }
            }

            // ── Action ────────────────────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick  = { navController.navigate("upi_pin") },
                    shape    = RoundedCornerShape(50),
                    colors   = ButtonDefaults.buttonColors(containerColor = PhonePePurple),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) {
                    Text(
                        text       = "Pay ₹499.00",
                        fontSize   = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White,
                    )
                }
                TextButton(onClick = { navController.navigate("signup") }) {
                    Text("Skip for now", color = OnSurface60, fontSize = 14.sp)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. UPI PIN Screen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun UpiPinScreen(navController: NavController) {
    var pin by remember { mutableStateOf("") }
    val maxPin = 6

    // Auto-advance when full PIN is entered
    LaunchedEffect(pin) {
        if (pin.length == maxPin) {
            delay(400)
            navController.navigate("payment_success")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PhonePeBg)
    ) {
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Header
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("phonepe", color = OnSurface, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(24.dp))
                Text("Enter UPI PIN", color = OnSurface, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("for SBI  •••• 4521", color = OnSurface60, fontSize = 14.sp)
            }

            // PIN dots
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                repeat(maxPin) { i ->
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .background(
                                if (i < pin.length) PhonePeLight else Color.White.copy(0.2f),
                                CircleShape,
                            )
                    )
                }
            }

            // Number pad
            Column(
                verticalArrangement   = Arrangement.spacedBy(16.dp),
                horizontalAlignment   = Alignment.CenterHorizontally,
            ) {
                val rows = listOf(
                    listOf("1","2","3"),
                    listOf("4","5","6"),
                    listOf("7","8","9"),
                    listOf("⌫","0","✓"),
                )
                rows.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        row.forEach { key ->
                            PinKey(
                                label   = key,
                                onClick = {
                                    when (key) {
                                        "⌫" -> if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                        "✓" -> { /* handled by auto-advance */ }
                                        else -> if (pin.length < maxPin) pin += key
                                    }
                                },
                                highlight = key == "✓",
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PinKey(label: String, onClick: () -> Unit, highlight: Boolean = false) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .background(
                if (highlight) PhonePePurple else Color.White.copy(0.08f),
                CircleShape,
            )
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text       = label,
            color      = if (highlight) Color.White else OnSurface,
            fontSize   = if (label.length == 1) 24.sp else 18.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. Payment Success Screen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PaymentSuccessScreen(navController: NavController) {
    var showCheck   by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf(false) }
    var showButton  by remember { mutableStateOf(false) }

    val checkScale = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        delay(300)
        showCheck = true
        checkScale.animateTo(1.3f, tween(180))
        checkScale.animateTo(1f, spring(Spring.DampingRatioLowBouncy))
        SoundPlayer.playGradeReveal()
        delay(400)
        showDetails = true
        delay(600)
        showButton = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF003020), Color(0xFF0A0A0A)))),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier            = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Animated checkmark circle
            AnimatedVisibility(visible = showCheck, enter = scaleIn()) {
                Box(
                    modifier = Modifier
                        .scale(checkScale.value)
                        .size(100.dp)
                        .background(SuccessGreen, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✓", color = Color.White, fontSize = 52.sp, fontWeight = FontWeight.Black)
                }
            }

            AnimatedVisibility(visible = showDetails, enter = fadeIn(tween(400))) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text       = "Payment Successful!",
                        color      = Color.White,
                        fontSize   = 26.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign  = TextAlign.Center,
                    )
                    Text("₹499.00", color = SuccessGreen, fontSize = 36.sp, fontWeight = FontWeight.Black)
                    Text("Paid to LangLang Premium", color = Color.White.copy(0.6f), fontSize = 15.sp)

                    Spacer(Modifier.height(8.dp))

                    // Details card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(0.07f))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        DetailRow("Status",       "✅ Success")
                        DetailRow("Amount",       "₹499.00")
                        DetailRow("Merchant",     "LangLang Premium")
                        DetailRow("Transaction",  "UPI2026031600001")
                        DetailRow("Date",         "Mar 16, 2026")
                    }

                    Text(
                        text  = "🎓 LangLang Premium Activated!",
                        color = Color(0xFFFFD700),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            AnimatedVisibility(visible = showButton, enter = fadeIn(tween(300))) {
                Button(
                    onClick  = { navController.navigate("signup") },
                    shape    = RoundedCornerShape(50),
                    colors   = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                ) {
                    Text("Continue to LangLang  →", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = Color.White.copy(0.5f), fontSize = 13.sp)
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. Signup Screen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SignupScreen(navController: NavController) {
    var name  by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0D001F), Color(0xFF0A0A0A))))
    ) {
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Branding
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🎓", fontSize = 60.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    text       = "LangLang",
                    color      = Color.White,
                    fontSize   = 36.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp,
                )
                Text(
                    text  = "Create Your Profile",
                    color = Color.White.copy(0.55f),
                    fontSize = 16.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = "Join 50,000+ learners worldwide",
                    color = PhonePeLight.copy(0.85f),
                    fontSize = 13.sp,
                )
            }

            // Form
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                LangLangTextField(
                    value       = name,
                    onValueChange = { name = it },
                    placeholder = "Full Name",
                    emoji       = "👤",
                )
                LangLangTextField(
                    value       = phone,
                    onValueChange = { phone = it },
                    placeholder = "Phone / Email",
                    emoji       = "📱",
                    keyboardType = KeyboardType.Phone,
                )
            }

            // Actions
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Button(
                    onClick  = { /* Would navigate to home in real app */ },
                    shape    = RoundedCornerShape(50),
                    colors   = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) {
                    Text(
                        text       = "Start Learning  →",
                        fontSize   = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White,
                    )
                }
                TextButton(onClick = {}) {
                    Text("Already have an account? Sign in", color = PhonePeLight.copy(0.7f), fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun LangLangTextField(
    value        : String,
    onValueChange: (String) -> Unit,
    placeholder  : String,
    emoji        : String,
    keyboardType : KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        modifier      = Modifier.fillMaxWidth(),
        placeholder   = { Text("$emoji  $placeholder", color = Color.White.copy(0.35f)) },
        shape         = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors        = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = PurplePrimary,
            unfocusedBorderColor = Color.White.copy(0.15f),
            focusedTextColor     = Color.White,
            unfocusedTextColor   = Color.White,
            cursorColor          = PurplePrimary,
        ),
        textStyle = TextStyle(fontSize = 16.sp),
        singleLine = true,
    )
}

