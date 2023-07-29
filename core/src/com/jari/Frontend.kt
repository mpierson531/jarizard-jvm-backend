package com.jari

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Dialog
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldStyle
import com.badlogic.gdx.scenes.scene2d.ui.Window.WindowStyle
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.ScreenViewport
import geo.ui.Artist2D
import geo.ui.GLabel
import geo.ui.GTextButton
import java.lang.Error

class Frontend : Screen {
    private val inDirLabel: GLabel
    private val outDirLabel: GLabel
    private val inDirText: TextField
    private val outDirText: TextField
    private val jarButton: GTextButton

    private val artist = Artist2D()

    private val dialog: Dialog
    private val dialogLabel: GLabel
    private var showDialog = false
    private var isDialogActive = false

    private val stage: Stage

    private val windowColor = Color(0.2f, 0.2f, 0.2f, 1f)
//    private var bridge = Bridge()
    private val backend: Backend = Backend()

    init {
        val textFieldSize = Vector2(250f, 30f)
        val labelSize = Vector2(150f, textFieldSize.y)

        val selectionColor = Color(0.35f, 0.35f, 1f, 0.75f)
        val clickColor = Color(0.6f, 0.6f, 1f, 0.75f)

        val generator = FreeTypeFontGenerator(Gdx.files.internal("ArialMonoMTProRegular.TTF"))
        val params = FreeTypeFontParameter()
        params.color = Color.WHITE
        params.mono = true
        params.genMipMaps = true
        params.gamma = 1f
        params.size = 17

        val textStyle = TextFieldStyle()
        textStyle.background = artist.textureDrawable(textFieldSize.x, textFieldSize.y, Color.DARK_GRAY, "rect", "filled")
        textStyle.font = generator.generateFont(params)
        textStyle.fontColor = Color.WHITE
        textStyle.cursor = artist.textureDrawable(2f, 5f, Color.WHITE, "rect", "filled")
        textStyle.selection = artist.textureDrawable(5f, 5f, Color(selectionColor), "rect", "filled")
        textStyle.focusedBackground = artist.textureDrawable(textFieldSize.x, textFieldSize.y, Color(selectionColor), "rect", "line")

        inDirText = TextField("", textStyle)
        inDirText.x = 300f
        inDirText.y = 400f
        inDirText.setSize(textFieldSize.x, textFieldSize.y)
        inDirText.alignment = Align.left

        val labelStyle = LabelStyle()
        labelStyle.font = textStyle.font
        labelStyle.fontColor = Color.WHITE
        inDirLabel =
            GLabel("Input directory", labelStyle, Vector2(120f, inDirText.y), Vector2(labelSize))

        outDirLabel = GLabel("Output Directory", labelStyle)
        outDirLabel.setSize(labelSize.x + 10f, labelSize.y)
        outDirLabel.x = inDirLabel.x
        outDirLabel.y = inDirLabel.y - inDirLabel.height - 10f

        outDirText = TextField("", textStyle)
        outDirText.x = inDirText.x
        outDirText.y = outDirLabel.y
        outDirText.setSize(textFieldSize.x, textFieldSize.y)
        outDirText.alignment = Align.left

        val jarBtnWidth = 55f
        val jarBtnHeight = 40f

        val jarStyle = TextButtonStyle()
        jarStyle.font = textStyle.font
        jarStyle.up = artist.textureDrawable(jarBtnWidth, jarBtnHeight, Color.WHITE,  "rect","line")
        jarStyle.over = artist.textureDrawable(jarBtnWidth, jarBtnHeight, Color(selectionColor),  "rect","filled")
        jarStyle.down = artist.textureDrawable(jarBtnWidth, jarBtnHeight, Color(clickColor), "rect", "filled")
        jarStyle.pressedOffsetX = -0.5f
        jarStyle.pressedOffsetY = -1.75f

        jarButton = GTextButton("Jar", jarStyle, Vector2(450f - 25f, 75f), Vector2(jarBtnWidth, jarBtnHeight))

        stage = Stage(ScreenViewport())
        stage.addActor(inDirLabel)
        stage.addActor(outDirLabel)
        stage.addActor(inDirText)
        stage.addActor(outDirText)
        stage.addActor(jarButton)

        val dialogColor = Color(0.3f, 0.3f, 0.3f, 0.85f)

        val dialogStyle = WindowStyle()
        dialogStyle.background = artist.textureDrawable(Gdx.graphics.width * 0.65f,
            Gdx.graphics.height * 0.5f, dialogColor, "rect", "filled")
        dialogStyle.titleFont = jarStyle.font
        dialogStyle.titleFontColor = jarStyle.fontColor

        dialog = Dialog("", dialogStyle)
        dialog.x = 0f
        dialog.y = Gdx.graphics.height * 0.25f
        dialog.setSize(Gdx.graphics.width * 0.65f, Gdx.graphics.height * 0.5f)
        dialog.isModal = false

        dialogLabel = GLabel("", labelStyle)
        dialog.text(dialogLabel)

        val dialogClose = GTextButton("Close", jarStyle).onClick {
            isDialogActive = false
            dialog.hide()
        }

        dialog.button(dialogClose)
        dialog.buttonTable.getCell(dialogClose).padBottom(10f)

        jarButton.onClick {
            backend.queue(arrayOf(inDirText.text), outDirText.text)
            showDialog = true
        }

//        stage.isDebugAll = true
        Gdx.input.inputProcessor = stage
    }

    private fun formatError(error: IOError): String {
        val text = if (error.isInput) "Input error:" else "Output error:"

        return if (error.state == Backend.FileState.Empty) {
            "$text *empty*, ${error.state}"
        } else {
            "$text ${error.dir}, ${error.state}"
        }
    }

    override fun render(delta: Float) {
        ScreenUtils.clear(windowColor)

        if (showDialog) {
            showDialog = false

            if (backend.isDone) {
                if (backend.isOk) {
                    dialogLabel.setText("Input Jarred!")
                } else {
                    var text = ""

                    val errors = backend.errors

                    synchronized(errors) {
                        val lastIndex = errors.size - 1

                        for (i in 0..lastIndex) {
                            if (i == lastIndex) {
                                text += errors[i].toString()
                                break
                            }

                            text += "${errors[i]}${System.lineSeparator()}"
                        }
                    }

                    dialogLabel.setText(text)
                }
            } else {
                dialogLabel.setText("Queued...")
            }

            if (!isDialogActive) {
                dialog.show(stage)
                isDialogActive = true
            }
        }

        /*if (showDialog && !isDialogActive) {
            if (backend.isDone) {
                when (backend.endState) {
                    Backend.State.Ok -> dialogLabel.setText("Input jarred!")
                    Backend.State.Empty -> dialogLabel.setText("Input was empty.")
                    Backend.State.NonExistent -> dialogLabel.setText("Input didn't exist.")
                }
            } else {
                dialogLabel.setText("Queued...")
            }

            dialog.show(stage)
            isDialogActive = true
        }*/

        stage.act(delta)
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