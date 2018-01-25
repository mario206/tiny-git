package hamburg.remme.tinygit.domain.service

import hamburg.remme.tinygit.TinyGit
import hamburg.remme.tinygit.addSorted
import hamburg.remme.tinygit.asPath
import hamburg.remme.tinygit.delete
import hamburg.remme.tinygit.domain.File
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.domain.Status
import hamburg.remme.tinygit.exists
import hamburg.remme.tinygit.git.gitAdd
import hamburg.remme.tinygit.git.gitAddUpdate
import hamburg.remme.tinygit.git.gitCheckout
import hamburg.remme.tinygit.git.gitRemove
import hamburg.remme.tinygit.git.gitReset
import hamburg.remme.tinygit.git.gitStatus
import hamburg.remme.tinygit.observableList
import javafx.beans.property.SimpleStringProperty
import javafx.concurrent.Task

class WorkingCopyService : Refreshable {

    val staged = observableList<File>()
    val pending = observableList<File>()
    val modifiedPending = pending.filtered { it.status != File.Status.ADDED }!!
    val selectedStaged = observableList<File>()
    val selectedPending = observableList<File>()
    val message = SimpleStringProperty()
    private lateinit var repository: Repository
    private var task: Task<*>? = null // TODO: needed?

    fun stage() {
        gitAdd(repository)
        gitRemove(repository, pending.filter { it.status == File.Status.REMOVED })
        status()
    }

    fun stageSelected(successHandler: () -> Unit) {
        if (pending.size == selectedPending.size) {
            stage()
        } else {
            gitAdd(repository, selectedPending.filter { it.status != File.Status.REMOVED })
            gitRemove(repository, selectedPending.filter { it.status == File.Status.REMOVED })
            status(successHandler)
        }
    }

    fun update() {
        gitAddUpdate(repository)
        status()
    }

    fun unstage() {
        gitReset(repository)
        status()
    }

    fun unstageSelected(successHandler: () -> Unit) {
        if (staged.size == selectedStaged.size) {
            unstage()
        } else {
            gitReset(repository, selectedStaged)
            status(successHandler)
        }
    }

    fun delete(successHandler: () -> Unit) {
        selectedPending.forEach { repository.resolve(it).asPath().takeIf { it.exists() }?.delete() }
        status(successHandler)
    }

    fun discardChanges(successHandler: () -> Unit, errorHandler: (String) -> Unit) {
        try {
            gitCheckout(repository, selectedPending.filter { it.status != File.Status.ADDED })
            status(successHandler)
        } catch (ex: RuntimeException) { // TODO
            errorHandler.invoke(ex.message ?: "")
        }
    }

    override fun onRefresh(repository: Repository) {
        update(repository)
    }

    override fun onRepositoryChanged(repository: Repository) {
        onRepositoryDeselected() // TODO
        update(repository)
    }

    override fun onRepositoryDeselected() {
        task?.cancel()
        staged.clear()
        pending.clear()
        selectedStaged.clear()
        selectedPending.clear()
    }

    private fun update(repository: Repository) {
        this.repository = repository
        status()
    }

    private fun status(successHandler: (() -> Unit)? = null) {
        task?.cancel()
        task = object : Task<Status>() {
            override fun call() = gitStatus(repository)

            override fun succeeded() {
                staged.addSorted(value.staged.filter { staged.none(it::equals) })
                staged.removeAll(staged.filter { value.staged.none(it::equals) })
                pending.addSorted(value.pending.filter { pending.none(it::equals) })
                pending.removeAll(pending.filter { value.pending.none(it::equals) })
                successHandler?.invoke()
            }
        }.also { TinyGit.execute(it) }
    }

}