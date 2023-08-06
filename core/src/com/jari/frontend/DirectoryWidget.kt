package com.jari.frontend

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldStyle
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import geo.ui.GLabel

open class DirectoryWidget(text: String, font: BitmapFont, isDependency: Boolean) : WidgetGroup() {
    companion object {
        const val fieldWidth = 250f
        const val fieldHeight = 30f
        const val labelWidth = 150f
        const val labelToField = labelWidth + 30f
        const val decrement = fieldHeight + 10f

        val background: TextureRegionDrawable = Frontend.artist.textureDrawable(fieldWidth, fieldHeight, Color.DARK_GRAY, "rect", "filled")
        val cursor: TextureRegionDrawable = Frontend.artist.textureDrawable(2f, 5f, Color.WHITE, "rect", "filled")
        val selection: TextureRegionDrawable = Frontend.artist.textureDrawable(5f, 5f,
            Frontend.selectionColor, "rect", "filled")
        val focusedBackground: TextureRegionDrawable

        var dirY = 450f
            private set
        var depY = 450f
            private set

        fun incrementY(isDependency: Boolean) = incrementY(isDependency, 1)
        fun decrementY(isDependency: Boolean) = decrementY(isDependency, 1)

        fun incrementY(isDependency: Boolean, times: Int) {
            if (isDependency) {
                depY += decrement * times
            } else {
                dirY += decrement * times
            }
        }

        fun decrementY(isDependency: Boolean, times: Int) {
            if (isDependency) {
                depY -= decrement * times
            } else {
                dirY -= decrement * times
            }
        }

        init {
            val focusedBackgroundColor = Color(Frontend.selectionColor.r, Frontend.selectionColor.g, Frontend.selectionColor.b, 0.7f)
            focusedBackground = Frontend.artist.textureDrawable(fieldWidth, fieldHeight, focusedBackgroundColor, "rect", "line")
        }
    }

    val label: GLabel
    val field: TextField

    var isEnabled: Boolean = true
        set(value) {
            field = value
            this.field.isDisabled = !value
        }

    init {
        val y: Float
        val labelToField: Float

        if (isDependency) {
            y = depY
            labelToField = Companion.labelToField - 75f
            decrementY(true)
        } else {
            y = dirY
            labelToField = Companion.labelToField
            decrementY(false)
        }

        val labelStyle = LabelStyle(font, font.color)
        val textFieldStyle = TextFieldStyle(font, font.color, cursor, selection, background)

        label = GLabel(text, labelStyle, 135f, y, labelWidth, fieldHeight)
        textFieldStyle.focusedBackground = focusedBackground
        field = TextField(null, textFieldStyle)
        field.alignment = Align.left
        field.setPosition(x + labelToField, y)
        field.setSize(fieldWidth, fieldHeight)

        super.addActor(label)
        super.addActor(field)
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