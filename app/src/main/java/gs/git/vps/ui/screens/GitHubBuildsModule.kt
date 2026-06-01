package gs.git.vps.ui.screens

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import gs.git.vps.data.github.GHRepo
import gs.git.vps.data.github.GHWorkflow

@Composable
fun BuildsScreen(
    repo: GHRepo,
    branches: List<String>,
    workflows: List<GHWorkflow>,
    selectedBranch: String?,
    onRunSelected: (Long?) -> Unit
) {
    Text("Builds — placeholder")
}
