package com.jari

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle
import com.badlogic.gdx.scenes.scene2d.ui.TextArea
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldStyle
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.ScreenViewport
import geo.collections.FysList
import geo.files.FileHandler
import geo.threading.ThreadUtils
import geo.ui.Artist2D
import geo.ui.Label
import geo.ui.TextButton

class Frontend : Screen {
    private val inDirLabel: Label
    private val outDirLabel: Label
    private val inDirText: TextArea
    private val outDirText: TextArea
    private val jarButton: TextButton
    private val artist = Artist2D()
    private val stage: Stage
    private val backend = Backend()

    init {
        val generator = FreeTypeFontGenerator(FileHandler.fileHandle("ArialMonoMTProRegular.ttf"))
        val params = FreeTypeFontParameter()
        params.color = Color.WHITE
        params.mono = true
        params.genMipMaps = true
        params.gamma = 2f
        params.size = 17

        val textStyle = TextFieldStyle()
        textStyle.background = artist.getTextureRegionDrawable(200f, 50f, Color.DARK_GRAY, "rect", "filled")
        textStyle.font = generator.generateFont(params)
        textStyle.fontColor = Color.WHITE
        textStyle.cursor = artist.getTextureRegionDrawable(1.5f, 5f, Color.WHITE, "rect", "filled")
        textStyle.selection = artist.getTextureRegionDrawable(5f, 5f, Color(0.3f, 0.3f, 1f, 0.7f), "rect", "filled")
        textStyle.focusedBackground = artist.getTextureRegionDrawable(200f, 50f, Color(0.35f, 0.35f, 1f, 0.75f), "rect", "line")

        inDirText = TextArea("", textStyle)
        inDirText.x = 280f
        inDirText.y = 400f
        inDirText.setSize(250f, 30f)
        inDirText.alignment = Align.center

        val labelStyle = LabelStyle()
        labelStyle.font = generator.generateFont(params)
        labelStyle.fontColor = Color.WHITE
        inDirLabel =
            Label("Input directory", labelStyle, Vector2(inDirText.x - 160f, inDirText.y), Vector2(150f, inDirText.height))

        outDirLabel = Label("Output Directory", labelStyle)
        outDirLabel.setSize(Vector2(inDirLabel.width + 10f, inDirLabel.height))
        outDirLabel.x = inDirLabel.x
        outDirLabel.y = inDirLabel.y - inDirLabel.height - 10f

        outDirText = TextArea("", textStyle)
        outDirText.x = outDirLabel.x + outDirLabel.width + 10f
        outDirText.y = outDirLabel.y
        outDirText.setSize(inDirText.width, inDirText.height)
        outDirText.alignment = Align.center

        val jarStyle = TextButtonStyle()
        jarStyle.font = generator.generateFont(params)
        jarStyle.up = artist.getTextureRegionDrawable(50f, 40f, Color.WHITE, "rect", "line")
        jarStyle.over = artist.getTextureRegionDrawable(50f, 40f, Color(0.35f, 0.35f, 1f, 0.75f), "rect", "filled")
        jarStyle.down = artist.getTextureRegionDrawable(50f, 40f, Color(0.6f, 0.6f, 1f, 0.75f), "rect", "filled")

        jarButton = TextButton("Jar", jarStyle, Vector2(450f - 25f, 75f), Vector2(50f, 40f))

        jarButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                ThreadUtils.run {
                    val input = inDirText.text
                    backend.input.add(input)
                    backend.output = outDirText.text
                    backend.jarizard()
                }
            }
        })

        stage = Stage(ScreenViewport())
        stage.addActor(inDirLabel)
        stage.addActor(outDirLabel)
        stage.addActor(inDirText)
        stage.addActor(outDirText)
        stage.addActor(jarButton)

        Gdx.input.inputProcessor = stage

//        stage.isDebugAll = true
    }

    override fun render(delta: Float) {
        ScreenUtils.clear(Color(0.2f, 0.2f, 0.2f, 1f))
        stage.act()
        stage.draw()
    }

    override fun show() {

    }

    override fun resize(width: Int, height: Int) {

    }

    override fun pause() {

    }

    override fun resume() {

    }


    override fun hide() {

    }

    override fun dispose() {

    }

}