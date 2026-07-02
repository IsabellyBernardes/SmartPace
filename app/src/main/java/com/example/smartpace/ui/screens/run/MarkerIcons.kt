package com.example.smartpace.ui.screens.run

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

/**
 * Gera um ícone de marcador com o emoji do alerta dentro de um círculo branco
 * com borda laranja — para o alerta aparecer visualmente no mapa, não só o pin padrão.
 */
fun emojiMarkerDescriptor(emoji: String): BitmapDescriptor {
    val size = 110
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = size / 2f
    val radius = center - 5f

    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    canvas.drawCircle(center, center, radius, fill)

    val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F97316")
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }
    canvas.drawCircle(center, center, radius, border)

    val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 56f
        textAlign = Paint.Align.CENTER
    }
    val baselineY = center - (text.descent() + text.ascent()) / 2f
    canvas.drawText(emoji, center, baselineY, text)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}
