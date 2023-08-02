package com.jari.frontend

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox.CheckBoxStyle
import com.badlogic.gdx.scenes.scene2d.ui.Dialog
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldStyle
import com.badlogic.gdx.scenes.scene2d.ui.Window.WindowStyle
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.jari.backend.Backend
import geo.collections.FysList
import geo.ui.Artist2D
import geo.ui.GLabel
import geo.ui.GTextButton
import geo.utils.Utils

class Frontend : Screen {
    companion object {
        val artist: Artist2D = Artist2D()
//        val selectionColor = Color(0.35f, 0.35f, 1f, 0.6f)

          val selectionColor = Color(0.35f, 0.35f, 1f, 0.6f)
        private fun copyFont(font: BitmapFont) = BitmapFont(font.data, font.regions, true)
    }

    private val jarButton: GTextButton

    private val dialog: Dialog
    private val dialogLabel: GLabel
    private var showDialog = false
    private var isDialogActive = false

    private val stage: Stage

    private val windowColor = Color(0.2f, 0.2f, 0.2f, 1f)
    private val backend: Backend = Backend()

    private val directories: FysList<DirectoryWidget> = FysList()

    init {
//        val clickColor = Color(selectionColor.r, selectionColor.g, selectionColor.b, 0.7f)
        val clickColor = Color(0.4f, 0.4f, 1f, 0.6f)

        val generator = FreeTypeFontGenerator(Gdx.files.internal("ArialMonoMTProRegular.TTF"))
        val params = FreeTypeFontParameter()
        params.color = Color.WHITE
        params.mono = true
        params.genMipMaps = true
        params.gamma = 1f
        params.size = 17

        val font = generator.generateFont(params)

        val jarBtnWidth = 55f
        val jarBtnHeight = DirectoryWidget.fieldHeight
        val checkboxSize = DirectoryWidget.fieldHeight
        val addSize = DirectoryWidget.fieldHeight

        val jarStyle = TextButtonStyle()
        jarStyle.font = copyFont(font)
        jarStyle.up = artist.textureDrawable(jarBtnWidth, jarBtnHeight, Color.WHITE,  "rect","line")
        jarStyle.over = artist.textureDrawable(jarBtnWidth, jarBtnHeight, Color(selectionColor),  "rect","filled")
        jarStyle.down = artist.textureDrawable(jarBtnWidth, jarBtnHeight, Color(clickColor), "rect", "filled")
        jarStyle.pressedOffsetY = -1.6f

        params.size = 25

        val addStyle = TextButtonStyle(
            artist.textureDrawable(addSize, addSize, Color.WHITE, "rect", "line"),
            artist.textureDrawable(addSize, addSize, Color(clickColor), "rect", "filled"),
            null,
            generator.generateFont(params)
        )

        addStyle.over = artist.textureDrawable(addSize, addSize, Color(selectionColor), "rect", "filled")
        addStyle.pressedOffsetY = jarStyle.pressedOffsetY

        generator.dispose()

        stage = Stage(ScreenViewport())

        val inDir = DirectoryWidget(font, true)
        directories.add(inDir)
        val outDir = DirectoryWidget(font, false)
        val addDirButton = GTextButton("+", addStyle, inDir.x + Utils.getWidth() / 2f - 2f, inDir.y, addSize, addSize)
        val subDirButton = GTextButton("-", addStyle, addDirButton.x + addDirButton.width + 17f, addDirButton.y, addDirButton.width, addDirButton.height)
        val jarPosition = Vector2(subDirButton.x + addSize + 17f , inDir.y)
        jarButton = GTextButton("Jar", jarStyle, jarPosition, Vector2(jarBtnWidth, jarBtnHeight))

        val manifestStyle = LabelStyle(font, Color.WHITE)
        val manifestLabel = GLabel("Main Class", manifestStyle, 2.5f, 35f,
            DirectoryWidget.labelWidth,
            DirectoryWidget.fieldHeight)
        val manifestTextStyle = TextFieldStyle(copyFont(font), Color.WHITE,
            DirectoryWidget.cursor,
            DirectoryWidget.selection,
            DirectoryWidget.background)
        manifestTextStyle.focusedBackground = DirectoryWidget.focusedBackground
        val manifestField = TextField(null, manifestTextStyle)
        manifestField.messageText = "Required for executable"
        manifestField.alignment = Align.center
        manifestField.x = manifestLabel.x + DirectoryWidget.labelToField - 30f
        manifestField.y = manifestLabel.y
        manifestField.setSize(DirectoryWidget.fieldWidth, DirectoryWidget.fieldHeight)

        addDirButton.onClick {
            val dirWidget = DirectoryWidget(font, true)
            stage.addActor(dirWidget)
            directories.add(dirWidget)
            outDir.y -= DirectoryWidget.decrement
        }

        subDirButton.onClick {
            if (directories.size != 1) {
                val widget = directories.remove(directories.size - 1)
                widget.addAction(Actions.removeActor())
                outDir.y += DirectoryWidget.decrement
                DirectoryWidget.incrementY()
            }
        }

        val versionWidth = 75f
        val versionStyle = TextFieldStyle()
        versionStyle.font = copyFont(font)
        versionStyle.fontColor = font.color
        versionStyle.focusedBackground = artist.textureDrawable(versionWidth,
            DirectoryWidget.fieldHeight, selectionColor, "rect", "line")
        versionStyle.selection = DirectoryWidget.selection
        versionStyle.background = artist.textureDrawable(versionWidth,
            DirectoryWidget.fieldHeight, Color.DARK_GRAY, "rect", "filled")
        versionStyle.cursor = DirectoryWidget.cursor

        val versionField = TextField("", versionStyle)
        versionField.messageText = "Version"
        versionField.alignment = Align.center
        versionField.x = manifestField.x + manifestField.width + 15f
        versionField.y = manifestField.y
        versionField.setSize(versionWidth, DirectoryWidget.fieldHeight)

        val compressionLabel = GLabel("No Compress", manifestStyle)

        val checkboxDrawables = getCheckboxDrawables(
            checkboxSize, Color.DARK_GRAY, clickColor,
            Color(clickColor.r, clickColor.g, clickColor.b, 0.4f),
            Color(clickColor.r, clickColor.g, clickColor.b, 0.7f)
        )

        val checkboxStyle = CheckBoxStyle()
        checkboxStyle.font = copyFont(font)
        checkboxStyle.fontColor = font.color
        checkboxStyle.checkboxOff = checkboxDrawables[0]
        checkboxStyle.checkboxOn = checkboxDrawables[1]
        checkboxStyle.checkboxOnOver = checkboxDrawables[2]
        checkboxStyle.checkboxOver = checkboxDrawables[3]
        val compressionCheckbox = CheckBox("", checkboxStyle)

        compressionLabel.x = manifestLabel.x + 5f
        compressionLabel.y = manifestLabel.y + DirectoryWidget.fieldHeight + 10f
        compressionLabel.setSize(DirectoryWidget.labelWidth, DirectoryWidget.fieldHeight)

        compressionCheckbox.x = compressionLabel.x + compressionLabel.width - 5f
        compressionCheckbox.y = compressionLabel.y
        compressionCheckbox.setSize(checkboxSize, checkboxSize)

        stage.addActor(inDir)
        stage.addActor(outDir)
        stage.addActor(jarButton)
        stage.addActor(addDirButton)
        stage.addActor(subDirButton)
        stage.addActor(manifestLabel)
        stage.addActor(manifestField)
        stage.addActor(versionField)
        stage.addActor(compressionLabel)
        stage.addActor(compressionCheckbox)

        val dialogColor = Color(0.3f, 0.3f, 0.3f, 0.85f)

        val dialogStyle = WindowStyle()
        dialogStyle.background = artist.textureDrawable(Gdx.graphics.width * 0.65f,
            Gdx.graphics.height * 0.5f, dialogColor, "rect", "filled")
        dialogStyle.titleFont = jarStyle.font
        dialogStyle.titleFontColor = jarStyle.fontColor

        dialog = Dialog("", dialogStyle)
        dialog.setSize(Gdx.graphics.width * 0.65f, Gdx.graphics.height * 0.5f)
        dialog.isModal = false

        val labelStyle = LabelStyle()
        labelStyle.font = font
        labelStyle.fontColor = Color.WHITE

        dialogLabel = GLabel("", labelStyle)
        dialog.text(dialogLabel)

        val dialogClose = GTextButton("Close", jarStyle)

        dialogClose.onClick {
            isDialogActive = false
            dialog.hide()
        }

        dialog.button(dialogClose)
        dialog.buttonTable.getCell(dialogClose).padBottom(10f)

        jarButton.onClick {
            backend.jarIt(Array(directories.size) { directories[it].text }, outDir.text,
                manifestField.text, versionField.text, !compressionCheckbox.isChecked)
            showDialog = true
        }

//        stage.isDebugAll = true
        Gdx.input.inputProcessor = stage
    }

    override fun render(delta: Float) {
        ScreenUtils.clear(windowColor)

        if (showDialog) {
            if (backend.isDone) {
                showDialog = false

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
                dialogLabel.setText("Jarring...")
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

    private fun getCheckboxDrawables(size: Float, uncheckedColor: Color, checkedColor: Color,
                                     hoverOnColor: Color, hoverOffColor: Color): Array<Drawable> {
        val sizeInt = size.toInt()
        val unchecked = artist.textureDrawable(size, size, uncheckedColor, "rect", "filled")

        val checkedPixmap = Pixmap(sizeInt, sizeInt, Pixmap.Format.RGBA8888)
        checkedPixmap.setColor(checkedColor)
        checkedPixmap.fillRectangle(0, 0, sizeInt, sizeInt)

        val checked = TextureRegionDrawable(Texture(checkedPixmap))
        checkedPixmap.dispose()

        val hoverOnPixmap = Pixmap(sizeInt, sizeInt, Pixmap.Format.RGBA8888)
        hoverOnPixmap.setColor(hoverOnColor)
        hoverOnPixmap.fillRectangle(0, 0, sizeInt, sizeInt)

        val hoverOn = TextureRegionDrawable(Texture(hoverOnPixmap))
        hoverOnPixmap.dispose()

        val hoverOffPixmap = Pixmap(sizeInt, sizeInt, Pixmap.Format.RGBA8888)
        hoverOffPixmap.setColor(hoverOffColor)
        hoverOffPixmap.fillRectangle(0, 0, sizeInt, sizeInt)

        val hoverOff = TextureRegionDrawable(Texture(hoverOffPixmap))
        hoverOffPixmap.dispose()

        return arrayOf(unchecked, checked, hoverOn, hoverOff)
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