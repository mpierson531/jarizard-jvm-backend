package com.jari

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldStyle
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import geo.ui.GLabel

class DirectoryWidget(font: BitmapFont, isInput: Boolean) : WidgetGroup() {
    companion object {
        const val fieldWidth = 250f
        const val fieldHeight = 30f
        const val labelWidth = 150f
        const val labelToField = labelWidth + 30f
        const val decrement = fieldHeight + 10f

        val background: TextureRegionDrawable = Frontend.artist.textureDrawable(fieldWidth, fieldHeight, Color.DARK_GRAY, "rect", "filled")
        val cursor: TextureRegionDrawable = Frontend.artist.textureDrawable(2f, 5f, Color.WHITE, "rect", "filled")
        val selection: TextureRegionDrawable = Frontend.artist.textureDrawable(5f, 5f, Frontend.selectionColor, "rect", "filled")
        val focusedBackground: TextureRegionDrawable
        var y = 450f

        private fun decrementY() {
            y -= decrement
        }

        fun incrementY() {
            y += decrement
        }

        init {
            val focusedBackgroundColor = Color(Frontend.selectionColor.r, Frontend.selectionColor.g, Frontend.selectionColor.b, 0.7f)
            focusedBackground = Frontend.artist.textureDrawable(fieldWidth, fieldHeight, focusedBackgroundColor, "rect", "line")
        }
    }

    private val label: GLabel
    private val field: TextField

    val text: String get() = this.field.text

    init {
        val labelStyle = LabelStyle(font, font.color)
        val textFieldStyle = TextFieldStyle(font, font.color, cursor, selection, background)

        label = GLabel(if (isInput) "Input Directory" else "Output Directory", labelStyle, 135f, Companion.y, labelWidth, fieldHeight)
        textFieldStyle.focusedBackground = focusedBackground
        field = TextField(null, textFieldStyle)
        field.alignment = Align.left
        field.setPosition(label.x + labelToField, Companion.y)
        field.setSize(fieldWidth, fieldHeight)

        super.addActor(label)
        super.addActor(field)

        if (isInput) {
            Companion.decrementY()
        }
    }

    override fun setX(x: Float) {
        label.x = x
        field.x = x + labelToField
    }

    override fun setY(y: Float) {
        label.y = y
        field.y = y
    }

    override fun getX(): Float {
        return label.x
    }

    override fun getY(): Float {
        return label.y
    }
}