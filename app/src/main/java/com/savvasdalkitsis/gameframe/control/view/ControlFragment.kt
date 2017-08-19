package com.savvasdalkitsis.gameframe.control.view

import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.SeekBar
import butterknife.OnClick
import butterknife.OnItemSelected
import com.savvasdalkitsis.gameframe.R
import com.savvasdalkitsis.gameframe.infra.view.BaseFragment
import com.savvasdalkitsis.gameframe.infra.view.FragmentSelectedListener
import com.savvasdalkitsis.gameframe.infra.view.Snackbars
import com.savvasdalkitsis.gameframe.injector.infra.navigation.NavigatorInjector
import com.savvasdalkitsis.gameframe.injector.presenter.PresenterInjector
import com.savvasdalkitsis.gameframe.ip.model.IpAddress
import com.savvasdalkitsis.gameframe.model.*
import kotlinx.android.synthetic.main.fragment_control.*

class ControlFragment : BaseFragment(), ControlView, FragmentSelectedListener {
    private val navigator = NavigatorInjector.navigator()

    private val presenter = PresenterInjector.controlPresenter()
    private lateinit var fab: FloatingActionButton

    override val layoutId: Int
        get() = R.layout.fragment_control

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        fab = activity.findViewById(R.id.view_fab)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view_brightness.setOnSeekBarChangeListener(BrightnessChangedListener())
        view_playback_mode.adapter = adapter(R.array.playback_mode)
        view_cycle_interval.adapter = adapter(R.array.cycle_interval)
        view_display_mode.adapter = adapter(R.array.display_mode)
        view_clock_face.adapter = adapter(R.array.clock_face)
        presenter.bindView(this)
    }

    override fun onResume() {
        super.onResume()
        presenter.loadIpAddress()
    }

    override fun onFragmentSelected() {
        presenter.loadIpAddress()
        fab.setImageResource(R.drawable.ic_power_settings_new_white_48px)
        fab.setOnClickListener { presenter.togglePower() }
    }

    override fun onFragmentUnselected() {}

    @OnClick(R.id.view_menu)
    fun menu() {
        presenter.menu()
    }

    @OnClick(R.id.view_next)
    fun next() {
        presenter.next()
    }

    @OnClick(R.id.view_control_setup)
    fun setup() {
        navigator.navigateToIpSetup()
    }

    @OnClick(R.id.view_brightness_low)
    fun brightnessLow() {
        view_brightness.incrementProgressBy(-1)
    }

    @OnClick(R.id.view_brightness_high)
    fun brightnessHigh() {
        view_brightness.incrementProgressBy(1)
    }

    @OnItemSelected(R.id.view_playback_mode)
    fun playbackMode(position: Int) {
        presenter.changePlaybackMode(PlaybackMode.from(position))
    }

    @OnItemSelected(R.id.view_cycle_interval)
    fun cycleInterval(position: Int) {
        presenter.changeCycleInterval(CycleInterval.from(position))
    }

    @OnItemSelected(R.id.view_display_mode)
    fun displayMode(position: Int) {
        presenter.changeDisplayMode(DisplayMode.from(position))
    }

    @OnItemSelected(R.id.view_clock_face)
    fun clockFace(position: Int) {
        presenter.changeClockFace(ClockFace.from(position))
    }

    override fun operationSuccess() = Snackbars.success(activity.findViewById(R.id.view_coordinator), R.string.success).show()

    override fun operationFailure(e: Throwable) {
        Log.e(ControlFragment::class.java.name, "Operation failure", e)
        Snackbars.error(activity.findViewById(R.id.view_coordinator), R.string.operation_failed).show()
    }

    override fun ipAddressLoaded(ipAddress: IpAddress) {
        view_ip.text = String.format("Game Frame IP: %s", ipAddress.toString())
        view_control_error.visibility = View.GONE
        view_control_content.visibility = View.VISIBLE
    }

    override fun ipCouldNotBeFound(throwable: Throwable) {
        Log.e(ControlFragment::class.java.name, "Could not find ip", throwable)
        view_control_error.visibility = View.VISIBLE
        view_control_content.visibility = View.GONE
    }

    private fun adapter(data: Int): ArrayAdapter<CharSequence> {
        val adapter = ArrayAdapter.createFromResource(context, data, android.R.layout.simple_spinner_item)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        return adapter
    }

    private inner class BrightnessChangedListener : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, level: Int, b: Boolean) {
            presenter.changeBrightness(Brightness.from(level))
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {}

        override fun onStopTrackingTouch(seekBar: SeekBar) {}
    }
}
