package com.savvasdalkitsis.gameframe.widget.view

import com.savvasdalkitsis.gameframe.R

class NextWidgetProvider : ClickableWidgetProvider() {

    override fun layoutResId() = R.layout.widget_next

    override fun onClick() {
        presenter.next()
    }
}
