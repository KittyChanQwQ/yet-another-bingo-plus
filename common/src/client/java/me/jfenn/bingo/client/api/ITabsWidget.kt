package me.jfenn.bingo.client.api

import me.jfenn.bingo.common.utils.IEventListener
import net.minecraft.client.gui.widget.TabNavigationWidget
import net.minecraft.text.Text

interface ITabsWidget {
    var width: Int
    val height: Int
    val widget: TabNavigationWidget

    var currentTab: Int
    val onTabChanged: IEventListener<Int>
}

interface ITabsWidgetFactory {
    fun create(tabs: List<Text>): ITabsWidget
}
