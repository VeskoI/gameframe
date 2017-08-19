package com.savvasdalkitsis.gameframe.draw.view

import android.content.Context
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.util.AttributeSet

import com.savvasdalkitsis.gameframe.model.Historical
import com.savvasdalkitsis.gameframe.draw.model.Model

class LayersView : RecyclerView {

    private lateinit var layers: LayersAdapter

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    override fun onFinishInflate() {
        super.onFinishInflate()
        layers = LayersAdapter()
        layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, true)
        setHasFixedSize(true)
        val itemTouchHelper = ItemTouchHelper(MoveLayersItemHelper())
        itemTouchHelper.attachToRecyclerView(this)
        adapter = layers
    }

    fun addNewLayer() = layers.addNewLayer()

    fun bind(modelHistory: Historical<Model>) = layers.bind(modelHistory)

    private inner class MoveLayersItemHelper internal constructor() : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {

        private val ALLOW_DRAG = ItemTouchHelper.Callback.makeFlag(ItemTouchHelper.ACTION_STATE_DRAG,
                ItemTouchHelper.DOWN or ItemTouchHelper.UP or ItemTouchHelper.START or ItemTouchHelper.END)
        private var newMove = true

        override fun onMoved(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, fromPos: Int, target: RecyclerView.ViewHolder, toPos: Int, x: Int, y: Int) {
            super.onMoved(recyclerView, viewHolder, fromPos, target, toPos, x, y)
            layers.swapLayers(viewHolder, target)
        }

        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            if (newMove) {
                newMove = false
                layers.swapStarted()
            }
            return target.adapterPosition > 0
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

        override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) =
                if (viewHolder.adapterPosition > 0) ALLOW_DRAG else 0

        override fun clearView(recyclerView: RecyclerView?, viewHolder: RecyclerView.ViewHolder) {
            super.clearView(recyclerView, viewHolder)
            motionFinished()
        }

        private fun motionFinished() {
            newMove = true
            layers.swapLayersFinished()
        }
    }
}