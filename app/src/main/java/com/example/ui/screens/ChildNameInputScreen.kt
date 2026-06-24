package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildNameInputScreen(
    onBack: () -> Unit,
    onComplete: (name: String, gender: String, photoUri: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("BOY") } // "BOY" or "GIRL"
    var photoUri by remember { mutableStateOf("") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                photoUri = uri.toString()
            }
        }
    )

    val scrollState = rememberScrollState()

    // Color definitions
    val backgroundColor = Color(0xFFFDFCF0) // Cream background
    val textColor = Color(0xFF44403C) // Dark brown text
    val warmPurple = Color(0xFF8B5CF6) // Warm purple accent
    val lightPurple = warmPurple.copy(alpha = 0.3f) // Disabled light purple

    // Button states animation
    val isEnabled = name.isNotBlank()
    val buttonBgColor by animateColorAsState(
        targetValue = if (isEnabled) warmPurple else lightPurple,
        animationSpec = tween(durationMillis = 300),
        label = "buttonBgColor"
    )
    val buttonTextColor by animateColorAsState(
        targetValue = if (isEnabled) Color.White else Color.White.copy(alpha = 0.7f),
        animationSpec = tween(durationMillis = 300),
        label = "buttonTextColor"
    )

    Scaffold(
        containerColor = backgroundColor,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Back button (top left alignment)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onBack() }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "뒤로가기",
                        tint = textColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "뒤로가기",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Center Illustration (Book stack with sprout)
            SproutBookStack(modifier = Modifier.padding(vertical = 8.dp))

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Headline Title text
            Text(
                text = "반가워요, 엄마!\n첫 기록을 시작할\n우리 아이의 이름은?",
                style = MaterialTheme.typography.headlineMedium.copy(
                    lineHeight = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textColor
                ),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 4. OutlinedTextField (Rounded shape, white bg, clear icon)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = {
                    Text(
                        text = "아이 이름을 입력해주세요 (예: 서준)",
                        color = textColor.copy(alpha = 0.4f),
                        fontSize = 15.sp
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = warmPurple,
                    unfocusedBorderColor = textColor.copy(alpha = 0.15f),
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    cursorColor = warmPurple
                ),
                trailingIcon = {
                    if (name.isNotEmpty()) {
                        IconButton(onClick = { name = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "입력 지우기",
                                tint = textColor.copy(alpha = 0.4f)
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("child_name_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Guide subtext under textfield
            Text(
                text = "*이름은 언제든 설정에서 변경할 수 있어요.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = textColor.copy(alpha = 0.6f),
                    fontSize = 13.sp
                ),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 5. Gender Selection
            Text(
                text = "우리 아이 성별 선택",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Boy Chip
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clickable { gender = "BOY" },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (gender == "BOY") Color(0xFFE0F2FE) else Color(0xFFFFFDF5)
                    ),
                    border = BorderStroke(
                        width = 1.5.dp,
                        color = if (gender == "BOY") Color(0xFF0284C7) else textColor.copy(alpha = 0.1f)
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "남아 👦🏻",
                            fontSize = 15.sp,
                            fontWeight = if (gender == "BOY") FontWeight.Bold else FontWeight.Medium,
                            color = if (gender == "BOY") Color(0xFF0284C7) else textColor
                        )
                    }
                }

                // Girl Chip
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clickable { gender = "GIRL" },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (gender == "GIRL") Color(0xFFFCE7F3) else Color(0xFFFFFDF5)
                    ),
                    border = BorderStroke(
                        width = 1.5.dp,
                        color = if (gender == "GIRL") Color(0xFFDB2777) else textColor.copy(alpha = 0.1f)
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "여아 👧🏻",
                            fontSize = 15.sp,
                            fontWeight = if (gender == "GIRL") FontWeight.Bold else FontWeight.Medium,
                            color = if (gender == "GIRL") Color(0xFFDB2777) else textColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 6. Child Photo Selection
            Text(
                text = "우리 아이 사진 추가 (선택)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, textColor.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color(0xFFFFF9C4), CircleShape)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (photoUri.isNotEmpty()) {
                            AsyncImage(
                                model = photoUri,
                                contentDescription = "아이 사진",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = "사진 추가",
                                tint = Color(0xFF8B5CF6),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (photoUri.isNotEmpty()) "사진이 등록되었습니다 ✨" else "아이 사진 등록하기",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Text(
                            text = if (photoUri.isNotEmpty()) "클릭하여 사진 변경" else "포트폴리오에 예쁘게 노출됩니다",
                            fontSize = 12.sp,
                            color = textColor.copy(alpha = 0.6f)
                        )
                    }

                    if (photoUri.isNotEmpty()) {
                        TextButton(
                            onClick = { photoUri = "" }
                        ) {
                            Text(
                                text = "삭제",
                                color = Color.Red,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // 7. CTA Button with color animations
            Button(
                onClick = { if (isEnabled) onComplete(name, gender, photoUri) },
                enabled = isEnabled,
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonBgColor,
                    contentColor = buttonTextColor,
                    disabledContainerColor = buttonBgColor,
                    disabledContentColor = buttonTextColor
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = if (isEnabled) 4.dp else 0.dp,
                    pressedElevation = 8.dp,
                    disabledElevation = 0.dp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("start_portfolio_button")
            ) {
                Text(
                    text = "포트폴리오 시작하기",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SproutBookStack(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(160.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy((-14).dp)
        ) {
            // Cute watercolor style green sprout with leaves
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .padding(bottom = 6.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Draw a cute organic curved stem
                    val path = Path().apply {
                        moveTo(w / 2f, h)
                        quadraticTo(w / 2f - 10f, h / 2f + 5f, w / 2f - 2f, h / 2f - 10f)
                        quadraticTo(w / 2f + 4f, h / 4f, w / 2f, 15f)
                    }
                    drawPath(
                        path = path,
                        color = Color(0xFF4CAF50),
                        style = Stroke(width = 5f)
                    )

                    // Draw left round leaf
                    drawOval(
                        color = Color(0xFF81C784),
                        topLeft = Offset(w / 2f - 26f, 12f),
                        size = Size(24f, 15f)
                    )

                    // Draw right round leaf
                    drawOval(
                        color = Color(0xFF48BB78),
                        topLeft = Offset(w / 2f + 2f, 7f),
                        size = Size(26f, 16f)
                    )
                }
            }

            // Book 1 (Top): Warm golden yellow
            Card(
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 4.dp, bottomStart = 8.dp, bottomEnd = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF176)),
                border = BorderStroke(1.dp, Color(0xFFE5C158).copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .width(135.dp)
                    .height(20.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Page edge block
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(12.dp)
                            .background(Color.White)
                            .align(Alignment.CenterEnd)
                    )
                }
            }

            // Book 2 (Middle): Warm pastel orange/coral
            Card(
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 4.dp, bottomStart = 8.dp, bottomEnd = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFAB91)),
                border = BorderStroke(1.dp, Color(0xFFD38B74).copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier
                    .width(155.dp)
                    .height(24.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Page edge block
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(14.dp)
                            .background(Color.White)
                            .align(Alignment.CenterEnd)
                    )
                }
            }

            // Book 3 (Bottom): Soft tan/beige
            Card(
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 4.dp, bottomStart = 8.dp, bottomEnd = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFD7CCC8)),
                border = BorderStroke(1.dp, Color(0xFFA1887F).copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .width(175.dp)
                    .height(28.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Page edge block
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(16.dp)
                            .background(Color.White)
                            .align(Alignment.CenterEnd)
                    )
                }
            }
        }
    }
}
