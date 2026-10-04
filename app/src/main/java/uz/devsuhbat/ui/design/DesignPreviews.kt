package uz.devsuhbat.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import uz.devsuhbat.ui.theme.DevSuhbatTheme
import uz.devsuhbat.ui.theme.LocalExtraColors

@Composable
private fun PreviewFrame(dark: Boolean, content: @Composable () -> Unit) {
    DevSuhbatTheme(dark = dark) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.background(MaterialTheme.colorScheme.background).padding(16.dp),
        ) { content() }
    }
}

@Composable
private fun BasicComponents() {
    val extra = LocalExtraColors.current
    SectionCard {
        Text("Bo'sh mavzular", style = MaterialTheme.typography.titleMedium)
        Text("Komponentlar va lifecycle · 20%", style = MaterialTheme.typography.bodyMedium)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile("6", "savol", Modifier.weight(1f))
        StatTile("5", "birinchi urinishda", Modifier.weight(1f), extra.successContainer, extra.onSuccessContainer)
        StatTile("1", "qayta ishlangan", Modifier.weight(1f), extra.streakContainer, extra.onStreakContainer)
    }
    ExpressiveButton("Mashqni boshlash", onClick = {}, trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward)
    ExpressiveButton("Keyingi savol", onClick = {}, tone = ButtonTone.SUCCESS)
    ExpressiveButton("Bosh sahifa", onClick = {}, tone = ButtonTone.OUTLINED)
    ExpressiveButton("Tekshirish", onClick = {}, enabled = false)
}

@Preview(name = "Basic · light", widthDp = 390)
@Composable
private fun BasicLight() = PreviewFrame(dark = false) { BasicComponents() }

@Preview(name = "Basic · dark", widthDp = 390)
@Composable
private fun BasicDark() = PreviewFrame(dark = true) { BasicComponents() }
