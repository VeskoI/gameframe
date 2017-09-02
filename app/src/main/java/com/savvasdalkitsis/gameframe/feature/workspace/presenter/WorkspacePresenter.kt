package com.savvasdalkitsis.gameframe.feature.workspace.presenter

import com.savvasdalkitsis.gameframe.R
import com.savvasdalkitsis.gameframe.feature.composition.usecase.BlendUseCase
import com.savvasdalkitsis.gameframe.feature.gameframe.model.AlreadyExistsOnGameFrameException
import com.savvasdalkitsis.gameframe.feature.gameframe.usecase.GameFrameUseCase
import com.savvasdalkitsis.gameframe.feature.history.usecase.HistoryUseCase
import com.savvasdalkitsis.gameframe.feature.raster.usecase.BmpUseCase
import com.savvasdalkitsis.gameframe.feature.saves.model.FileAlreadyExistsException
import com.savvasdalkitsis.gameframe.feature.saves.usecase.FileUseCase
import com.savvasdalkitsis.gameframe.feature.workspace.element.grid.model.Grid
import com.savvasdalkitsis.gameframe.feature.workspace.element.grid.model.GridDisplay
import com.savvasdalkitsis.gameframe.feature.workspace.element.grid.view.GridTouchedListener
import com.savvasdalkitsis.gameframe.feature.workspace.model.Project
import com.savvasdalkitsis.gameframe.feature.workspace.model.WorkspaceModel
import com.savvasdalkitsis.gameframe.feature.workspace.usecase.WorkspaceUseCase
import com.savvasdalkitsis.gameframe.feature.workspace.view.WorkspaceView
import com.savvasdalkitsis.gameframe.infra.android.StringUseCase
import com.savvasdalkitsis.gameframe.infra.rx.RxTransformers
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import java.io.File

class WorkspacePresenter<O>(private val gameFrameUseCase: GameFrameUseCase,
                            private val fileUseCase: FileUseCase,
                            private val bmpUseCase: BmpUseCase,
                            private val blendUseCase: BlendUseCase,
                            private val workspaceUseCase: WorkspaceUseCase,
                            private val stringUseCase: StringUseCase) : GridTouchedListener {

    private lateinit var view: WorkspaceView<O>
    private lateinit var gridDisplay: GridDisplay
    private var uploading: Boolean = false
    private var tempName: String? = null
    private var project = Project(history = HistoryUseCase(WorkspaceModel()))
    private val history: HistoryUseCase<WorkspaceModel>
        get() = project.history
    private val present: WorkspaceModel
        get() = history.present
    private var displayLayoutBorders
        get() = project.displayLayoutBorders
        set(value) {
            project.displayLayoutBorders = value
        }

    fun bindView(view: WorkspaceView<O>, gridDisplay: GridDisplay) {
        this.view = view
        this.gridDisplay = gridDisplay
        view.observe(history)
        history.observe()
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    render(it)
                    view.bindPalette(it.selectedPalette)
                }
    }

    fun upload(colorGrid: Grid) {
        if (!uploading) {
            view.askForFileName(R.string.upload) { name -> upload(name, colorGrid) }
        }
    }

    fun replaceDrawing(name: String, colorGrid: Grid) {
        view.displayProgress()
        uploading = true
        fileUseCase.deleteDirectory(name)
                .concatWith { gameFrameUseCase.removeFolder(name) }
                .compose(RxTransformers.schedulers())
                .doOnTerminate { uploading = false }
                .subscribe({ upload(name, colorGrid) }, { view.operationFailed(it) })
    }

    fun saveWorkspace() {
        view.displayProgress()
        project.name?.let { tempName = it }
        tempName?.let { name ->
            tempName = null
            workspaceUseCase.saveProject(name, present)
                    .compose(RxTransformers.schedulers<File>())
                    .subscribe({ projectChangedSuccessfully(name) }, projectSaveFailed())
        } ?: view.askForFileName(R.string.save) {
            tempName = it
            saveWorkspace()
        }
    }

    fun loadProject(name: String? = null) {
        view.displayProgress()
        when {
            name != null -> workspaceUseCase.load(name)
                    .compose(RxTransformers.schedulers<WorkspaceModel>())
                    .subscribe(projectLoaded(name), { view.operationFailed(it) })
            else -> workspaceUseCase.savedProjects()
                    .compose(RxTransformers.schedulers<List<String>>())
                    .subscribe(savedProjectsLoaded {
                        view.stopProgress()
                        view.askForProjectToLoad(it)
                    }, {
                        view.operationFailed(it)
                    })
        }
    }

    fun deleteProjects(names: List<String>? = null) {
        view.displayProgress()
        if (names?.isEmpty() == false) {
            Flowable.fromIterable(names)
                    .flatMapCompletable(workspaceUseCase::deleteProject)
                    .compose(RxTransformers.schedulers())
                    .subscribe(projectsDeleted(names), { view.operationFailed(it) })
        } else {
            workspaceUseCase.savedProjects()
                    .compose(RxTransformers.schedulers<List<String>>())
                    .subscribe(savedProjectsLoaded {
                        view.stopProgress()
                        view.askForProjectsToDelete(it)
                    }, {
                        view.operationFailed(it)
                    })
        }
    }

    private fun projectsDeleted(names: List<String>) = {
        view.showSuccess()
        if (names.contains(project.name)) {
            project.name = null
            displayProjectName()
        }
    }

    private fun savedProjectsLoaded(onNonEmpty: (List<String>) -> Unit): (List<String>) -> Unit = {
        if (it.isEmpty()) {
            view.displayNoSavedProjectsExist()
        } else {
            onNonEmpty(it)
        }
    }

    private fun projectLoaded(name: String): (WorkspaceModel) -> Unit = {
        history.restartFrom(it)
        projectChangedSuccessfully(name)
    }

    private fun projectSaveFailed(): (Throwable) -> Unit = {
        view.operationFailed(it)
        displayProjectName()
    }

    private fun projectChangedSuccessfully(name: String) {
        view.showSuccess()
        project.name = name
        displayProjectName()
    }

    private fun displayProjectName() {
        view.displayProjectName(project.name ?: stringUseCase.getString(R.string.untitled))
    }

    fun changeColor(currentColor: Int, newColor: Int, paletteIndex: Int) {
        if (currentColor != newColor) {
            with(history) {
                progressTimeWithoutAnnouncing()
                present.selectedPalette.changeColor(paletteIndex, newColor)
                collapsePresentWithPastIfTheSame()
                announcePresent()
            }
        }
    }

    override fun onGridTouchStarted() {
        history.progressTime()
    }

    override fun onGridTouch(startColumn: Int, startRow: Int, column: Int, row: Int) {
        view.drawLayer(present.selectedLayer, startColumn, startRow, column, row)
        render(present)
    }

    override fun onGridTouchFinished() {
        view.finishStroke(present.selectedLayer)
        history.collapsePresentWithPastIfTheSame()
    }

    fun prepareOptions(options: O) {
        history.hasPast()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { hasPast ->
                    if (hasPast) {
                        view.enableUndo(options)
                    } else {
                        view.disableUndo(options)
                    }
                }
        history.hasFuture()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { hasFuture ->
                    if (hasFuture) {
                        view.enableRedo(options)
                    } else {
                        view.disableRedo(options)
                    }
                }
        if (displayLayoutBorders) {
            view.displayLayoutBordersEnabled(options)
        } else {
            view.displayLayoutBordersDisabled(options)
        }
    }

    fun selectedOptionBorders() {
        displayLayoutBorders = !displayLayoutBorders
        render(present)
    }

    fun selectedOptionUndo() {
        history.stepBackInTime()
    }

    fun selectedOptionRedo() {
        history.stepForwardInTime()
    }

    private fun upload(name: String, colorGrid: Grid) {
        view.displayProgress()
        uploading = true
        fileUseCase.saveFile("bmp/$name", "0.bmp", { bmpUseCase.rasterizeToBmp(colorGrid) })
                .flatMap<File> { file -> gameFrameUseCase.createFolder(name).toSingleDefault<File>(file) }
                .flatMapCompletable { gameFrameUseCase.uploadFile(it) }
                .concatWith { gameFrameUseCase.play(name) }
                .compose(RxTransformers.schedulers())
                .doOnTerminate { uploading = false }
                .subscribe({ view.showSuccess() }, { e ->
                    if (e is FileAlreadyExistsException || e is AlreadyExistsOnGameFrameException) {
                        view.drawingAlreadyExists(name, colorGrid, e)
                    } else {
                        view.operationFailed(e)
                    }
                })
    }

    private fun render(model: WorkspaceModel) {
        model.layers.forEach { blendUseCase.renderOn(it, gridDisplay) }
        val selected = model.layers.firstOrNull { it.isSelected }

        if (selected != null && displayLayoutBorders) {
            val colorGrid = selected.colorGrid
            view.displayBoundaries(colorGrid.columnTranslation, colorGrid.rowTranslation)
        } else {
            view.clearBoundaries()
        }
        view.rendered()
    }
}
