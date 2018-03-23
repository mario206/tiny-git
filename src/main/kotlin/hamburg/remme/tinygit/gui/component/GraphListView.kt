package hamburg.remme.tinygit.gui.component

import hamburg.remme.tinygit.TinyGit
import hamburg.remme.tinygit.domain.Branch
import hamburg.remme.tinygit.domain.Commit
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.hbox
import hamburg.remme.tinygit.gui.builder.label
import hamburg.remme.tinygit.gui.builder.vbox
import hamburg.remme.tinygit.gui.component.skin.GraphListViewSkin
import hamburg.remme.tinygit.shortDateTimeFormat
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.layout.HBox
import javafx.scene.text.Text

/**
 * This view has some heavy interaction with [GraphListViewSkin] but is still loosely coupled, as it would
 * work with the default list skin, just without Git log graph.
 *
 * The actual log graph is calculated asynchronously by [TinyGit.commitLogService] when the log changes.
 */
class GraphListView(commits: ObservableList<Commit>) : ListView<Commit>(commits) {

    var graphWidth: Double
        get() = graphPadding.get().left
        set(value) = graphPadding.set(Insets(0.0, 0.0, 0.0, value))
    var isGraphVisible
        get() = graphVisible.get()
        set(value) = graphVisible.set(value)
    val logGraph = TinyGit.commitLogService.logGraph
    private val service = TinyGit.branchService
    private val graphVisible = object : SimpleBooleanProperty(true) {
        override fun invalidated() = refresh()
    }
    private val graphPadding = SimpleObjectProperty<Insets>(Insets.EMPTY)

    init {
        addClass("graph-view")
        setCellFactory { CommitLogListCell() }
        service.head.addListener { _ -> refresh() }
        service.branches.addListener(ListChangeListener { refresh() })
    }

    override fun createDefaultSkin() = GraphListViewSkin(this)

    /**
     * This rather complex list cell is displaying brief information about the commit.
     * It will show its ID, commit time, message and author.
     *
     * It will also display any branch pointing to the commit.
     *
     * The [ListCell] will have a left padding bound to [GraphListView.padding] to leave space for the graph
     * that is drawn by the [GraphListViewSkin].
     *
     * @todo branches all have the same color which is not synchronized with the log graph
     */
    private inner class CommitLogListCell : ListCell<Commit>() {

        private val MAX_LENGTH = 60
        private val commitId = Text().addClass("commitId")
        private val date = Text().addClass("date")
        private val badges = HBox().addClass("branches")
        private val message = Text().addClass("message")
        private val author = Text().addClass("author")

        init {
            graphic = vbox {
                addClass("graph-view-cell")
                paddingProperty().bind(graphPadding)
                +hbox {
                    alignment = Pos.CENTER_LEFT
                    +commitId
                    +date
                    +badges
                }
                +hbox {
                    alignment = Pos.CENTER_LEFT
                    +message
                    +author
                }
            }
        }

        override fun updateItem(item: Commit?, empty: Boolean) {
            super.updateItem(item, empty)
            graphic.isVisible = !empty
            item?.let { c ->
                commitId.text = c.shortId
                date.text = c.date.format(shortDateTimeFormat)
                badges.children.setAll(service.branches.filter { it.id == c.id }.toBadges())
                message.text = c.shortMessage
                author.text = " ― ${c.authorName}"
            }
        }

        private fun List<Branch>.toBadges(): List<Node> {
            return map {
                label {
                    addClass("branch-badge")
                    if (service.isDetached(it)) addClass("detached")
                    else if (service.isHead(it)) addClass("current")
                    +it.name.abbrev()
                    +if (service.isDetached(it)) Icons.locationArrow() else Icons.codeFork()
                }
            }
        }

        private fun String.abbrev() = if (length > MAX_LENGTH) "${substring(0, MAX_LENGTH)}..." else this

    }

}
