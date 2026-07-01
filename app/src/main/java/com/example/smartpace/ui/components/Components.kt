package com.example.smartpace.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartpace.model.LatLngPoint
import com.example.smartpace.model.Run

@Composable
fun SmartPaceLogo() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Color(0xFF3B82F6), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("~", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "SmartPace",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A)
        )
    }
}

@Composable
fun SmartPaceTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onPasswordToggle: (() -> Unit)? = null
) {
    Column {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
            color = Color(0xFF64748B)
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder, color = Color(0xFFCBD5E1)) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF3B82F6),
                unfocusedBorderColor = Color(0xFFE2E8F0),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedTextColor = Color(0xFF0F172A),
                unfocusedTextColor = Color(0xFF0F172A)
            ),
            visualTransformation = if (isPassword && !passwordVisible)
                PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = if (isPassword)
                KeyboardOptions(keyboardType = KeyboardType.Password)
            else KeyboardOptions.Default,
            trailingIcon = if (isPassword && onPasswordToggle != null) {
                {
                    IconButton(onClick = onPasswordToggle) {
                        Icon(
                            imageVector = if (passwordVisible)
                                Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8)
                        )
                    }
                }
            } else null
        )
    }
}

@Composable
fun OrDivider() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
        Text(
            text = "  ou continue com  ",
            fontSize = 12.sp,
            color = Color(0xFF94A3B8)
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
    }
}

@Composable
fun SocialButton(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
    ) {
        Text(text, color = Color(0xFF0F172A), fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

/**
 * Miniatura da rota desenhada a partir dos pontos GPS salvos.
 * Normaliza lat/lng para o quadro preservando a proporção — leve o bastante
 * para renderizar em listas roláveis sem carregar tiles de mapa.
 */
@Composable
fun RouteThumbnail(
    points: List<LatLngPoint>,
    modifier: Modifier = Modifier,
    lineColor: Color = Color(0xFF3B82F6),
    background: Color = Color(0xFFEFF6FF)
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        if (points.size < 2) {
            Text("~", color = lineColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            return@Box
        }

        Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
            val lats = points.map { it.lat }
            val lngs = points.map { it.lng }
            val minLat = lats.min(); val maxLat = lats.max()
            val minLng = lngs.min(); val maxLng = lngs.max()
            val spanLat = (maxLat - minLat).takeIf { it > 0 } ?: 1e-6
            val spanLng = (maxLng - minLng).takeIf { it > 0 } ?: 1e-6

            // Escala uniforme para não distorcer o traçado da rota
            val scale = minOf(size.width / spanLng, size.height / spanLat)
            val offsetX = (size.width - spanLng * scale) / 2f
            val offsetY = (size.height - spanLat * scale) / 2f

            fun toX(lng: Double) = (offsetX + (lng - minLng) * scale).toFloat()
            // Latitude cresce para cima, então invertemos o eixo Y
            fun toY(lat: Double) = (offsetY + (maxLat - lat) * scale).toFloat()

            val path = Path()
            points.forEachIndexed { i, p ->
                val x = toX(p.lng); val y = toY(p.lat)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, color = lineColor, style = Stroke(width = 3f, cap = StrokeCap.Round))

            // Marca início (verde) e fim (vermelho) do percurso
            drawCircle(Color(0xFF22C55E), radius = 4f, center = Offset(toX(points.first().lng), toY(points.first().lat)))
            drawCircle(Color(0xFFEF4444), radius = 4f, center = Offset(toX(points.last().lng), toY(points.last().lat)))
        }
    }
}

@Composable
fun RunCard(run: Run) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(Color(0xFFEFF6FF), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("~", color = Color(0xFF3B82F6), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("${run.distance} km", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                Text(run.date, fontSize = 12.sp, color = Color(0xFF94A3B8))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(run.duration, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                Text("${run.pace}/km", fontSize = 12.sp, color = Color(0xFF94A3B8))
            }
        }
    }
}
