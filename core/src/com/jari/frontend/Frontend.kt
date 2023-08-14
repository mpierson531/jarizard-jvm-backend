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
        val selectionColor = Color(0.35f, 0.35f, 1f, 0.6f)
        private val dependencySpacing = Utils.getWidth() / 2f - 85f
        private fun copyFont(font: BitmapFont) = BitmapFont(font.data, font.regions, true)
    }

    private val jarButton: GTextButton

    private val dialog: Dialog
    private val dialogLabel: GLabel
    private var showDialog = false
    private var isDialogActive = false

    private var isOnMain = true

    private val stage: Stage

    private val windowColor = Color(0.2f, 0.2f, 0.2f, 1f)
    private val backend: Backend = Backend()

    private val directories: FysList<DirectoryWidget> = FysList()
    private val dependencies: FysList<Pair<DirectoryWidget, TextField>> = FysList()

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
        val versionWidth = 75f

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

        generator.dispose()

        addStyle.over = artist.textureDrawable(addSize, addSize, Color(selectionColor), "rect", "filled")
        addStyle.pressedOffsetY = jarStyle.pressedOffsetY

        stage = Stage(ScreenViewport())

        val inDir = DirectoryWidget("Input Directory", font, false)
        directories.add(inDir)
        val outDir = DirectoryWidget("Output Directory", font, false)
        DirectoryWidget.incrementY(false)

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
            if (isOnMain) {
                val dirWidget = DirectoryWidget("Input Directory", font, false)
                stage.addActor(dirWidget)
                directories.add(dirWidget)
                outDir.y = DirectoryWidget.dirY
            } else {
                val dirWidget = DirectoryWidget("Path", font, true)
                val versionField = makeVersionField(dirWidget.x + dependencySpacing, dirWidget.y, versionWidth, DirectoryWidget.fieldHeight, font)
                stage.addActor(dirWidget)
                stage.addActor(versionField)
                dependencies.add(Pair(dirWidget, versionField))
            }
        }

        subDirButton.onClick {
            if (isOnMain) {
                if (directories.size != 1) {
                    directories.remove(directories.size - 1).addAction(Actions.removeActor())
                    DirectoryWidget.incrementY(false)
                    outDir.y = DirectoryWidget.dirY
                }
            } else if (dependencies.size != 1) {
                val dependency = dependencies.remove(dependencies.size - 1)
                dependency.first.addAction(Actions.removeActor())
                dependency.second.addAction(Actions.removeActor())
                DirectoryWidget.incrementY(true)
            }
        }

        val jarVersion = makeVersionField(manifestField.x + manifestField.width + 15f, manifestField.y, versionWidth, DirectoryWidget.fieldHeight, font)

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

        val mainButtonStyle = TextButtonStyle()
        mainButtonStyle.over = artist.textureDrawable(5f, 5f, windowColor, "rect", "filled")
        mainButtonStyle.up = mainButtonStyle.over
        mainButtonStyle.font = copyFont(font)
        mainButtonStyle.fontColor = Color(0.4f, 0.4f, 0.4f, 1f)
        mainButtonStyle.checkedFontColor = mainButtonStyle.font.color
        mainButtonStyle.overFontColor = Color(0.6f, 0.6f, 0.6f, 1f)

        val mainButton = GTextButton("Main", mainButtonStyle)
        mainButton.size = Vector2(jarButton.size)
        mainButton.position = Vector2(Utils.getWidth() / 2f - jarBtnWidth, Utils.getHeight() - jarBtnHeight)
        mainButton.setProgrammaticChangeEvents(true)
        mainButton.toggle()

        val mavenButtonStyle = TextButtonStyle()
        mavenButtonStyle.over = mainButtonStyle.over
        mavenButtonStyle.up = mainButtonStyle.over
        mavenButtonStyle.font = copyFont(font)
        mavenButtonStyle.fontColor = Color(mainButtonStyle.fontColor)
        mavenButtonStyle.checkedFontColor = mavenButtonStyle.font.color
        mavenButtonStyle.overFontColor = Color(mainButtonStyle.overFontColor)

        val mavenButton = GTextButton("Maven", mavenButtonStyle)
        mavenButton.size = Vector2(mainButton.size)
        mavenButton.position = Vector2(mainButton.x + mainButton.width, mainButton.y)

        val firstDependency = DirectoryWidget("Path", font, true)
        firstDependency.isEnabled = false
        firstDependency.isVisible = false
        val firstDependencyVersion = makeVersionField(firstDependency.x + dependencySpacing, firstDependency.y,
            versionWidth, DirectoryWidget.fieldHeight, font)
        firstDependencyVersion.isVisible = false
        firstDependencyVersion.isDisabled = true
        dependencies.add(Pair(firstDependency, firstDependencyVersion))

        val exampleLabel = GLabel("example path: org.jetbrains.kotlin.kotlin-stdlib", manifestStyle)
        exampleLabel.x = firstDependency.x + 90f
        exampleLabel.y = firstDependency.y + 80f
        exampleLabel.isVisible = false

        mainButton.onClick {
            if (!isOnMain) {
                mavenButton.toggle()
                isOnMain = true

                exampleLabel.isVisible = false

                compressionLabel.isVisible = true
                compressionCheckbox.isVisible = true
                compressionCheckbox.isDisabled = false

                manifestLabel.isVisible = true
                manifestField.isVisible = true
                manifestField.isDisabled = false

                jarVersion.isVisible = true
                jarVersion.isDisabled = false

                outDir.isEnabled = true
                outDir.isVisible = true

                for (dir in directories) {
                    dir.isEnabled = true
                    dir.isVisible = true
                }

                for (dep in dependencies) {
                    dep.first.isEnabled = false
                    dep.first.isVisible = false
                    dep.second.isVisible = false
                    dep.second.isDisabled = true
                }
            } else {
                mainButton.toggle()
            }
        }

        mavenButton.onClick {
            if (isOnMain) {
                mainButton.toggle()
                isOnMain = false

                exampleLabel.isVisible = true

                compressionLabel.isVisible = false
                compressionCheckbox.isVisible = false
                compressionCheckbox.isDisabled = true

                manifestLabel.isVisible = false
                manifestField.isVisible = false
                manifestField.isDisabled = true

                jarVersion.isVisible = false
                jarVersion.isDisabled = true

                outDir.isEnabled = false
                outDir.isVisible = false

                for (dir in directories) {
                    dir.isEnabled = false
                    dir.isVisible = false
                }

                for (dep in dependencies) {
                    dep.first.isEnabled = true
                    dep.first.isVisible = true
                    dep.second.isVisible = true
                    dep.second.isDisabled = false
                }
            } else {
                mavenButton.toggle()
            }
        }

        stage.addActor(inDir)
        stage.addActor(outDir)

        stage.addActor(jarButton)
        stage.addActor(addDirButton)
        stage.addActor(subDirButton)

        stage.addActor(manifestLabel)
        stage.addActor(manifestField)

        stage.addActor(jarVersion)

        stage.addActor(compressionLabel)
        stage.addActor(compressionCheckbox)

        stage.addActor(mainButton)
        stage.addActor(mavenButton)
        stage.addActor(firstDependency)
        stage.addActor(firstDependencyVersion)
        stage.addActor(exampleLabel)

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
        labelStyle.fontColor = font.color

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
            val filteredInput = this.directories.copy().filter { it.field.text.isNotBlank() }

            val input = if (filteredInput.size == 0) {
                arrayOf(this.directories[0].field.text)
            } else {
                Array(filteredInput.size) { filteredInput[it].field.text }
            }

            val filteredDependencies = this.dependencies.copy().filter { it.first.field.text.isNotBlank() }
            val dependencies = Array(filteredDependencies.size) {
                Pair(filteredDependencies[it].first.field.text, filteredDependencies[it].second.text)
            }

            backend.jarIt(input, outDir.field.text, dependencies, manifestField.text, jarVersion.text, !compressionCheckbox.isChecked)
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

                    val lastIndex = errors.size - 1

                    for (i in 0..lastIndex) {
                        if (i == lastIndex) {
                            text += errors[i].toString()
                            break
                        }

                        text += "${errors[i]}${System.lineSeparator()}"
                    }

                    dialogLabel.setText(text)
                }

                backend.reset()

                if (!isDialogActive) {
                    isDialogActive = true
                    dialog.show(stage, Actions.fadeIn(0.3f))
                    dialog.setPosition(Math.round((stage.width - dialog.width) / 2).toFloat(), Math.round((stage.height - dialog.height) / 2).toFloat())
                }
            } else if (!isDialogActive) {
                isDialogActive = true
                dialogLabel.setText("Jarring...")
                dialog.show(stage, Actions.fadeIn(0.3f))
                dialog.setPosition(Math.round((stage.width - dialog.width) / 2).toFloat(), Math.round((stage.height - dialog.height) / 2).toFloat())
            }
        }

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

    private inline fun makeVersionField(x: Float, y: Float, width: Float, height: Float, font: BitmapFont): TextField {
        val versionStyle = TextFieldStyle()
        versionStyle.font = copyFont(font)
        versionStyle.fontColor = font.color
        versionStyle.focusedBackground = artist.textureDrawable(width,
            DirectoryWidget.fieldHeight, selectionColor, "rect", "line")
        versionStyle.selection = DirectoryWidget.selection
        versionStyle.background = artist.textureDrawable(width,
            DirectoryWidget.fieldHeight, Color.DARK_GRAY, "rect", "filled")
        versionStyle.cursor = DirectoryWidget.cursor

        val versionField = TextField("", versionStyle)
        versionField.messageText = "Version"
        versionField.alignment = Align.center
        versionField.x = x
        versionField.y = y
        versionField.setSize(width, height)

        return versionField
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