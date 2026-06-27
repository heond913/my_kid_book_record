package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.shadow

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildNameInputScreen(
    initialName: String = "",
    initialGender: String = "BOY",
    initialPhotoUri: String = "",
    initialColorHex: String = "#8B5CF6",
    initialBirthDate: String = "",
    isNewProfile: Boolean = false,
    onBack: () -> Unit,
    onComplete: ((name: String, gender: String, photoUri: String) -> Unit)? = null,
    onCompleteNew: ((name: String, gender: String, photoUri: String, colorHex: String, birthDate: String) -> Unit)? = null,
    onProfileDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(initialName) }
    var gender by remember { mutableStateOf(initialGender) } // "BOY" or "GIRL"
    var photoUri by remember { mutableStateOf(initialPhotoUri) }
    var colorHex by remember { mutableStateOf(initialColorHex) }
    var birthDate by remember { mutableStateOf(initialBirthDate) }

    // Uri chosen by the system photo picker, before we crop it
    var imageToCrop by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                imageToCrop = uri
            }
        }
    )

    val scrollState = rememberScrollState()

    // Color definitions
    val backgroundColor = Color(0xFFFDFCF0) // Cream background
    val textColor = Color(0xFF44403C) // Dark brown text
    
    val activeColor = remember(colorHex) {
        try {
            Color(android.graphics.Color.parseColor(colorHex))
        } catch (e: Exception) {
            Color(0xFF8B5CF6)
        }
    }
    val lightPurple = activeColor.copy(alpha = 0.3f) // Disabled light purple

    // Button states animation
    val isEnabled = name.isNotBlank() && birthDate.length == 8
    val buttonBgColor by animateColorAsState(
        targetValue = if (isEnabled) activeColor else lightPurple,
        animationSpec = tween(durationMillis = 300),
        label = "buttonBgColor"
    )
    val buttonTextColor by animateColorAsState(
        targetValue = if (isEnabled) Color.White else Color.White.copy(alpha = 0.7f),
        animationSpec = tween(durationMillis = 300),
        label = "buttonTextColor"
    )

    // Render Image Cropper Dialog if an image was chosen
    if (imageToCrop != null) {
        ImageCropperDialog(
            imageUri = imageToCrop!!,
            onDismiss = { imageToCrop = null },
            onCropped = { croppedUri ->
                photoUri = croppedUri.toString()
                imageToCrop = null
            }
        )
    }

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

            // 2. Interactive Circular Thumbnail Profile Preview Area (Immediately updates)
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(110.dp)
                    .shadow(elevation = 6.dp, shape = CircleShape)
                    .background(
                        if (photoUri.isNotEmpty() && !photoUri.startsWith("emoji:")) Color.Transparent
                        else when (gender) {
                            "BOY" -> Color(0xFFE0F2FE) // Soft boy blue background
                            "GIRL" -> Color(0xFFFCE7F3) // Soft girl pink background
                            else -> Color(0xFFFFF9C4)
                        },
                        CircleShape
                    )
                    .border(
                        3.dp,
                        activeColor,
                        CircleShape
                    )
                    .clip(CircleShape)
                    .clickable {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (photoUri.isNotEmpty()) {
                    if (photoUri.startsWith("emoji:")) {
                        val emoji = photoUri.removePrefix("emoji:")
                        Text(
                            text = emoji,
                            fontSize = 52.sp
                        )
                    } else {
                        AsyncImage(
                            model = photoUri,
                            contentDescription = "아이 사진 미리보기",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    Text(
                        text = if (gender == "BOY") "👦🏻" else "👧🏻",
                        fontSize = 52.sp
                    )
                }

                // Camera Badge Icon in bottom-right corner
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color.White, CircleShape)
                            .border(1.5.dp, Color(0xFFE2E8F0), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "사진 수정",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Headline Title text
            Text(
                text = if (isNewProfile) "새로운 우리 아이\n독서 프로필 등록하기 ✨" else "반가워요, 엄마!\n첫 기록을 시작할\n우리 아이의 이름은?",
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
                    focusedBorderColor = activeColor,
                    unfocusedBorderColor = textColor.copy(alpha = 0.15f),
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    cursorColor = activeColor
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

            // 4.1. Birthdate Input Field
            Text(
                text = "우리 아이 생년월일 입력 (8자리)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = birthDate,
                onValueChange = { input ->
                    val filtered = input.filter { it.isDigit() }
                    if (filtered.length <= 8) {
                        birthDate = filtered
                    }
                },
                placeholder = {
                    Text(
                        text = "예) 20200506",
                        color = textColor.copy(alpha = 0.4f),
                        fontSize = 15.sp
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = activeColor,
                    unfocusedBorderColor = textColor.copy(alpha = 0.15f),
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    cursorColor = activeColor
                ),
                trailingIcon = {
                    if (birthDate.isNotEmpty()) {
                        IconButton(onClick = { birthDate = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "입력 지우기",
                                tint = textColor.copy(alpha = 0.4f)
                            )
                        }
                    }
                },
                supportingText = {
                    if (birthDate.isNotEmpty() && birthDate.length < 8) {
                        Text(
                            text = "8자리 숫자로 입력해주세요 (현재 ${birthDate.length}자리)",
                            color = Color.Red,
                            fontSize = 12.sp
                        )
                    } else if (birthDate.length == 8) {
                        Text(
                            text = "올바른 8자리 생년월일입니다 ✨",
                            color = Color(0xFF10B981),
                            fontSize = 12.sp
                        )
                    } else {
                        Text(
                            text = "생년월일 8자리를 입력해야 프로필을 등록할 수 있습니다.",
                            color = textColor.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("child_birthdate_input")
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

            // 5.1. Accent Color Theme Picker
            Text(
                text = "우리 아이 고유 컬러 지정",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(10.dp))

            val colorOptions = listOf(
                "#8B5CF6" to "보라 💜",
                "#10B981" to "초록 💚",
                "#3B82F6" to "파랑 💙",
                "#F59E0B" to "노랑 💛",
                "#EF4444" to "빨강 ❤️",
                "#EC4899" to "핑크 🩷"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                colorOptions.forEach { (hex, label) ->
                    val isSelected = colorHex == hex
                    val col = try {
                        Color(android.graphics.Color.parseColor(hex))
                    } catch (e: Exception) {
                        Color(0xFF8B5CF6)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .background(
                                color = if (isSelected) col.copy(alpha = 0.15f) else Color.White,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) col else textColor.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { colorHex = hex },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(col, CircleShape)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = label.takeLast(2), // Emoji
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 5.2. Cute Default Character Picker
            Text(
                text = "우리 아이 기본 캐릭터 선택",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val defaultCharacters = listOf(
                    "🐼" to "팬더",
                    "🦁" to "사자",
                    "🐰" to "토끼",
                    "🦊" to "여우",
                    "🐻" to "곰",
                    "🐥" to "병아리"
                )
                defaultCharacters.forEach { (charEmoji, nameLabel) ->
                    val isSelected = photoUri == "emoji:$charEmoji"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .background(
                                color = if (isSelected) activeColor.copy(alpha = 0.15f) else Color.White,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) activeColor else textColor.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { photoUri = "emoji:$charEmoji" },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(text = charEmoji, fontSize = 24.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = nameLabel, fontSize = 10.sp, color = textColor)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 6. Child Photo Selection
            Text(
                text = "우리 아이 사진 추가 (직접 업로드)",
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
                            .background(
                                if (photoUri.isNotEmpty() && !photoUri.startsWith("emoji:")) Color.Transparent
                                else when (gender) {
                                    "BOY" -> Color(0xFFE0F2FE)
                                    "GIRL" -> Color(0xFFFCE7F3)
                                    else -> Color(0xFFFFF9C4)
                                },
                                CircleShape
                            )
                            .border(
                                1.5.dp,
                                activeColor.copy(alpha = 0.4f),
                                CircleShape
                            )
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (photoUri.isNotEmpty() && !photoUri.startsWith("emoji:")) {
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
                                tint = activeColor,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (photoUri.isNotEmpty() && !photoUri.startsWith("emoji:")) "직접 추가된 사진 ✨" else "갤러리에서 사진 등록하기",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Text(
                            text = if (photoUri.isNotEmpty() && !photoUri.startsWith("emoji:")) "클릭하여 사진 크롭 및 변경" else "앨범 속 사진을 직접 설정할 수 있습니다",
                            fontSize = 12.sp,
                            color = textColor.copy(alpha = 0.6f)
                        )
                    }

                    if (photoUri.isNotEmpty() && !photoUri.startsWith("emoji:")) {
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
                onClick = {
                    if (isEnabled) {
                        if (onCompleteNew != null) {
                            onCompleteNew(name, gender, photoUri, colorHex, birthDate)
                        } else if (onComplete != null) {
                            onComplete(name, gender, photoUri)
                        }
                    }
                },
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
                    text = if (isNewProfile) "새 프로필 등록 완료 ✨" else "포트폴리오 시작하기",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (!isNewProfile && initialName.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                var showDeleteConfirm by remember { mutableStateOf(false) }

                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFEF4444)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        text = "프로필 삭제하기",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (showDeleteConfirm) {
                    AlertDialog(
                        onDismissRequest = { showDeleteConfirm = false },
                        title = { Text("프로필 삭제") },
                        text = { Text("정말로 '${initialName}'의 모든 기록을 삭제하고 프로필을 지우시겠습니까? 이 작업은 되돌릴 수 없습니다.") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showDeleteConfirm = false
                                    onProfileDelete?.invoke()
                                }
                            ) {
                                Text("삭제", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteConfirm = false }) {
                                Text("취소")
                            }
                        }
                    )
                }
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

@Composable
fun ImageCropperDialog(
    imageUri: Uri,
    onDismiss: () -> Unit,
    onCropped: (Uri) -> Unit
) {
    val context = LocalContext.current
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val croppedUri = cropBitmap(
                        context = context,
                        sourceUri = imageUri,
                        scale = scale,
                        offsetX = offsetX,
                        offsetY = offsetY,
                        viewSizePx = 600 // High-quality resolution for crop box
                    )
                    if (croppedUri != null) {
                        onCropped(croppedUri)
                    } else {
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
            ) {
                Text("자르기 완료")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소", color = Color(0xFF44403C))
            }
        },
        title = {
            Text(
                text = "사진 자르기",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF44403C)
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "두 손가락으로 확대/축소하고, 드래그하여 원 안에 맞게 조정해 주세요.",
                    fontSize = 13.sp,
                    color = Color(0xFF44403C).copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Interactive Crop Area Viewport
                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .background(Color.Black, shape = RoundedCornerShape(12.dp))
                        .clipToBounds()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 5f)
                                offsetX += pan.x * scale
                                offsetY += pan.y * scale
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Loaded image with dynamic layout transformations
                    AsyncImage(
                        model = imageUri,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY
                            ),
                        contentScale = ContentScale.Fit
                    )

                    // Semi-transparent circle overlay using Border + Background
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                            .border(3.dp, Color.White, CircleShape)
                            .background(Color.Transparent)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom slider for absolute zoom control
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("확대", fontSize = 12.sp, color = Color(0xFF44403C), modifier = Modifier.width(36.dp))
                    Slider(
                        value = scale,
                        onValueChange = { scale = it },
                        valueRange = 1f..5f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF8B5CF6),
                            activeTrackColor = Color(0xFF8B5CF6)
                        )
                    )
                }
            }
        },
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false
        ),
        modifier = Modifier.padding(24.dp)
    )
}

fun cropBitmap(
    context: android.content.Context,
    sourceUri: Uri,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    viewSizePx: Int
): Uri? {
    var inputStream: java.io.InputStream? = null
    var rawBitmap: Bitmap? = null
    var originalBitmap: Bitmap? = null
    var croppedBitmap: Bitmap? = null
    try {
        val resolver = context.contentResolver
        
        // 1. Get EXIF Orientation flag
        val orientation = resolver.openInputStream(sourceUri)?.use { stream ->
            val exifInterface = androidx.exifinterface.media.ExifInterface(stream)
            exifInterface.getAttributeInt(
                androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
            )
        } ?: androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL

        // Map the orientation flag to rotation degrees
        val degrees = when (orientation) {
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }

        // 2. Decode raw bitmap
        inputStream = resolver.openInputStream(sourceUri)
        rawBitmap = BitmapFactory.decodeStream(inputStream) ?: return null

        // 3. Rotate bitmap if degrees != 0
        originalBitmap = if (degrees != 0) {
            val matrix = android.graphics.Matrix().apply { postRotate(degrees.toFloat()) }
            Bitmap.createBitmap(
                rawBitmap,
                0,
                0,
                rawBitmap.width,
                rawBitmap.height,
                matrix,
                true
            )
        } else {
            rawBitmap
        }

        val imgW = originalBitmap.width.toFloat()
        val imgH = originalBitmap.height.toFloat()

        // Match ContentScale.Fit inside a square viewport of viewSizePx
        val baseScale = min(viewSizePx.toFloat() / imgW, viewSizePx.toFloat() / imgH)
        val activeScale = baseScale * scale

        val displayedW = imgW * activeScale
        val displayedH = imgH * activeScale

        val initX = (viewSizePx - displayedW) / 2f
        val initY = (viewSizePx - displayedH) / 2f

        val currentImgLeft = initX + offsetX
        val currentImgTop = initY + offsetY

        // Crop window is [0, viewSizePx] relative to viewport
        val cropLeftInBitmap = (-currentImgLeft / activeScale).toInt()
        val cropTopInBitmap = (-currentImgTop / activeScale).toInt()
        val cropSizeInBitmap = (viewSizePx / activeScale).toInt()

        // Boundaries checks
        val finalX = cropLeftInBitmap.coerceIn(0, (originalBitmap.width - 1))
        val finalY = cropTopInBitmap.coerceIn(0, (originalBitmap.height - 1))
        val finalSize = cropSizeInBitmap.coerceIn(1, min(originalBitmap.width - finalX, originalBitmap.height - finalY))

        croppedBitmap = Bitmap.createBitmap(originalBitmap, finalX, finalY, finalSize, finalSize)
        
        // Save cropped image to temporary cache file
        val outputFile = File(context.cacheDir, "cropped_child_${System.currentTimeMillis()}.jpg")
        FileOutputStream(outputFile).use { out ->
            croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        
        return Uri.fromFile(outputFile)
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    } finally {
        try { inputStream?.close() } catch (ignored: Exception) {}
        if (rawBitmap != null && rawBitmap != originalBitmap) {
            rawBitmap.recycle()
        }
        originalBitmap?.recycle()
        croppedBitmap?.recycle()
    }
}

