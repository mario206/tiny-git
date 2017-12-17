package hamburg.remme.tinygit.service

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.git.LocalRepository
import hamburg.remme.tinygit.git.api.Git

object StashService : Refreshable() {

    override fun onRefresh(repository: LocalRepository) {
        update(repository)
    }

    override fun onRepositoryChanged(repository: LocalRepository) {
        update(repository)
    }

    override fun onRepositoryDeselected() {
        State.stashSize.set(0)
    }

    private fun update(repository: LocalRepository) {
        State.stashSize.set(Git.stashListSize(repository))
    }

}
