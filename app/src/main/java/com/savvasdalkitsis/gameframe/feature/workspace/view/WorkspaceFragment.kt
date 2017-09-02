package com.savvasdalkitsis.gameframe.feature.workspace.view

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.annotation.ColorInt
import android.support.design.internal.NavigationMenu
import android.support.v4.widget.DrawerLayout
import android.util.Log
import android.view.*
import butterknife.OnClick
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.color.ColorChooserDialog
import com.savvasdalkitsis.gameframe.R
import com.savvasdalkitsis.gameframe.feature.history.usecase.HistoryUseCase
import com.savvasdalkitsis.gameframe.feature.home.view.HomeActivity
import com.savvasdalkitsis.gameframe.feature.workspace.element.grid.model.Grid
import com.savvasdalkitsis.gameframe.feature.workspace.element.layer.model.Layer
import com.savvasdalkitsis.gameframe.feature.workspace.element.palette.model.Palette
import com.savvasdalkitsis.gameframe.feature.workspace.element.palette.view.AddNewPaletteSelectedListener
import com.savvasdalkitsis.gameframe.feature.workspace.element.palette.view.AddPaletteView
import com.savvasdalkitsis.gameframe.feature.workspace.element.palette.view.PaletteView
import com.savvasdalkitsis.gameframe.feature.workspace.element.swatch.view.SwatchSelectedListener
import com.savvasdalkitsis.gameframe.feature.workspace.element.swatch.view.SwatchView
import com.savvasdalkitsis.gameframe.feature.workspace.element.tools.model.Tools
import com.savvasdalkitsis.gameframe.feature.workspace.element.tools.view.ToolSelectedListener
import com.savvasdalkitsis.gameframe.feature.workspace.model.WorkspaceModel
import com.savvasdalkitsis.gameframe.feature.workspace.presenter.WorkspacePresenter
import com.savvasdalkitsis.gameframe.infra.kotlin.TypeAction
import com.savvasdalkitsis.gameframe.infra.android.BaseFragment
import com.savvasdalkitsis.gameframe.infra.android.FragmentSelectedListener
import com.savvasdalkitsis.gameframe.infra.android.Snackbars
import com.savvasdalkitsis.gameframe.injector.presenter.PresenterInjector.workspacePresenter
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import io.github.yavski.fabspeeddial.CustomFabSpeedDial
import io.github.yavski.fabspeeddial.SimpleMenuListenerAdapter
import kotlinx.android.synthetic.main.fragment_workspace.*

class WorkspaceFragment : BaseFragment(), FragmentSelectedListener,
        SwatchSelectedListener, WorkspaceView<Menu>,
        ColorChooserDialog.ColorCallback, ToolSelectedListener {

    private lateinit var fab: CustomFabSpeedDial
    private lateinit var drawer: DrawerLayout
    private val presenter = workspacePresenter()
    private var swatchToModify: SwatchView? = null
    private var selected: Boolean = false
    private var activeSwatch: SwatchView? = null

    override val layoutId: Int
        get() = R.layout.fragment_workspace

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        fab = activity.findViewById(R.id.view_fab_workspace)
        view_draw_tools.setOnToolSelectedListener(this)
        view_draw_tools_current.bind(Tools.defaultTool())

        view_draw_sliding_up_panel.addPanelSlideListener(object : SlidingUpPanelLayout.SimplePanelSlideListener() {
            override fun onPanelStateChanged(panel: View?, previousState: SlidingUpPanelLayout.PanelState?, newState: SlidingUpPanelLayout.PanelState?) {
                val scale = if (newState == SlidingUpPanelLayout.PanelState.EXPANDED) 0f else 1f
                fab.animate().scaleY(scale).scaleX(scale).start()
            }
        })
        drawer.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerOpened(drawerView: View?) {
                setFabState()
            }

            override fun onDrawerClosed(drawerView: View?) {
                setFabState()
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        drawer = view.findViewById(R.id.view_draw_drawer)
        view_draw_led_grid_view.setOnGridTouchedListener(presenter)
        presenter.bindView(this, view_draw_led_grid_view)
    }

    override fun onResume() {
        super.onResume()
        setFabState()
    }

    override fun bindPalette(selectedPalette: Palette) {
        val paletteView = drawer.findViewById<PaletteView>(R.id.view_draw_palette)
        paletteView.bind(selectedPalette)
        paletteView.setOnSwatchSelectedListener(this)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater) {
        if (selected) {
            inflater.inflate(R.menu.menu_workspace, menu)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.menu_borders -> {
            presenter.selectedOptionBorders()
            true
        }
        R.id.menu_undo -> {
            presenter.selectedOptionUndo()
            true
        }
        R.id.menu_redo -> {
            presenter.selectedOptionRedo()
            true
        }
        else -> false
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        presenter.prepareOptions(menu)
    }

    override fun displayLayoutBordersEnabled(options: Menu) {
        options.findItem(R.id.menu_borders)?.setIcon(R.drawable.ic_border_outer_white_48px)
    }

    override fun displayLayoutBordersDisabled(options: Menu) {
        options.findItem(R.id.menu_borders)?.setIcon(R.drawable.ic_border_clear_white_48px)
    }

    override fun enableUndo(options: Menu) {
        setMenuAlpha(options, R.id.menu_undo, 255)
    }

    override fun disableUndo(options: Menu) {
        setMenuAlpha(options, R.id.menu_undo, 125)
    }

    override fun enableRedo(options: Menu) {
        setMenuAlpha(options, R.id.menu_redo, 255)
    }

    override fun disableRedo(options: Menu) {
        setMenuAlpha(options, R.id.menu_redo, 125)
    }

    private fun setMenuAlpha(options: Menu, id: Int, alpha: Int) {
        options.findItem(id)?.let { item ->
            item.icon.alpha = alpha
        }
    }

    override fun onFragmentSelected() {
        selected = true
        fab.visibility = View.VISIBLE
        invalidateOptionsMenu()
    }

    override fun onFragmentUnselected() {
        selected = false
        fab.visibility = View.GONE
        invalidateOptionsMenu()
    }

    override fun observe(history: HistoryUseCase<WorkspaceModel>) {
        view_draw_layers.bind(history)
        view_draw_palettes.bind(history)
    }

    override fun onSwatchSelected(swatchView: SwatchView) {
        this.activeSwatch = swatchView
    }

    override fun onSwatchLongPressed(swatch: SwatchView) {
        swatchToModify = swatch
        ColorChooserDialog.Builder(context as HomeActivity, R.string.change_color)
                .show()
    }

    override fun onColorSelection(dialog: ColorChooserDialog, @ColorInt selectedColor: Int) {
        swatchToModify?.let {
            presenter.changeColor(it.color, selectedColor, it.index)
        }
    }

    override fun drawLayer(layer: Layer, startColumn: Int, startRow: Int, column: Int, row: Int) {
        activeSwatch?.let {
            view_draw_tools_current.drawingTool?.drawOn(layer, startColumn, startRow, column, row, it.color)
        }
    }

    override fun finishStroke(layer: Layer) {
        view_draw_tools_current.drawingTool?.finishStroke(layer)
    }

    override fun onToolSelected(tool: Tools) {
        view_draw_tools_current.bind(tool)
        view_draw_sliding_up_panel.panelState = SlidingUpPanelLayout.PanelState.HIDDEN
    }

    override fun askForFileName(positiveText: Int, nameEntered: TypeAction<String>) {
        MaterialDialog.Builder(activity)
                .input(R.string.name_of_drawing, 0, false) { _, input -> nameEntered(input.toString()) }
                .title(R.string.enter_name_for_drawing)
                .positiveText(positiveText)
                .negativeText(android.R.string.cancel)
                .build()
                .show()
    }

    override fun displayProgress() {
        startFabProgress()
    }

    override fun drawingAlreadyExists(name: String, colorGrid: Grid, e: Throwable) {
        Log.e(WorkspacePresenter::class.java.name, "Drawing already exists", e)
        Snackbars.actionError(coordinator(), R.string.already_exists, R.string.replace,
                { presenter.replaceDrawing(name, colorGrid) }).show()
        stopFabProgress()
    }

    @SuppressLint("RtlHardcoded")
    @OnClick(R.id.view_draw_open_layers)
    fun openLayers() {
        drawer.openDrawer(Gravity.RIGHT)
    }

    @SuppressLint("RtlHardcoded")
    @OnClick(R.id.view_draw_open_palette)
    fun openPalette() {
        drawer.openDrawer(Gravity.LEFT)
    }

    @OnClick(R.id.view_draw_tools_change, R.id.view_draw_tools_current)
    fun changeTool() {
        view_draw_sliding_up_panel.panelState = SlidingUpPanelLayout.PanelState.EXPANDED
    }

    @SuppressLint("RtlHardcoded")
    private fun setFabState() = with(fab) {
        when {
            drawer.isDrawerOpen(Gravity.RIGHT) -> {
                setMenuListener(addNewLayerOperation())
                setImageResource(R.drawable.ic_add_white_48px)
            }
            drawer.isDrawerOpen(Gravity.LEFT) -> {
                setMenuListener(addNewPaletteOperation())
                setImageResource(R.drawable.ic_add_white_48px)
            }
            else -> {
                setMenuListener(standardOperation())
                setImageResource(R.drawable.ic_import_export_white_48px)
            }
        }
    }

    private fun addNewPaletteOperation() = object : SimpleMenuListenerAdapter() {
        override fun onPrepareMenu(navigationMenu: NavigationMenu?): Boolean {
            AddPaletteView.show(context, drawer, object : AddNewPaletteSelectedListener {
                override fun onAddNewPalletSelected(palette: Palette) {
                    view_draw_palettes.addNewPalette(palette)
                }
            })
            return false
        }
    }

    private fun addNewLayerOperation() = object : SimpleMenuListenerAdapter() {
        override fun onPrepareMenu(navigationMenu: NavigationMenu?): Boolean {
            view_draw_layers.addNewLayer()
            return false
        }
    }

    private fun standardOperation() = object : SimpleMenuListenerAdapter() {
        override fun onMenuItemSelected(menuItem: MenuItem) = when (menuItem.itemId) {
            R.id.operation_upload -> {
                presenter.upload(view_draw_led_grid_view.colorGrid)
                true
            }
            R.id.operation_save -> {
                presenter.saveWorkspace()
                true
            }
            R.id.operation_open -> {
                presenter.loadProject()
                true
            }
            else -> false
        }
    }

    override fun askForProjectToLoad(projectNames: List<String>) {
        MaterialDialog.Builder(context)
                .title(R.string.load_project)
                .items(projectNames)
                .itemsCallbackSingleChoice(-1) { _, _, which, _ ->
                    presenter.loadProject(projectNames[which])
                    true
                }
                .show()
    }

    override fun displayNoSavedProjectsExist() {
        Snackbars.error(coordinator(), R.string.no_saved_projects).show()
        stopFabProgress()
    }

    private fun startFabProgress() {
        fab.startProgress()
    }

    private fun stopFabProgress() {
        fab.stopProgress(R.drawable.ic_import_export_white_48px)
    }

    override fun displayProjectName(name: String) {
        view_draw_project_name.text = name
    }

    private fun coordinator() = activity.findViewById<View>(R.id.view_coordinator)

    override fun displayBoundaries(col: Int, row: Int) {
        view_draw_led_grid_view.displayBoundaries(col, row)
    }

    override fun clearBoundaries() {
        view_draw_led_grid_view.clearBoundaries()
    }

    override fun rendered() {
        view_draw_led_grid_view.invalidate()
        invalidateOptionsMenu()
    }

    private fun invalidateOptionsMenu() {
        activity.invalidateOptionsMenu()
    }

    override fun showSuccess() {
        Snackbars.success(coordinator(), R.string.success).show()
        stopFabProgress()
    }

    override fun operationFailed(e: Throwable) {
        Log.e(WorkspacePresenter::class.java.name, "Workspace operation failed", e)
        Snackbars.error(coordinator(), R.string.operation_failed).show()
        stopFabProgress()
    }
}