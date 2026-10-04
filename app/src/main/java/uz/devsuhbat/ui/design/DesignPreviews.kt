package uz.devsuhbat.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import uz.devsuhbat.ui.common.InlineCodeText
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

@Composable
private fun ProgressComponents() {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        ReadinessRing(fraction = 0.5f, centerText = "50%")
        Box(Modifier.background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.large)) {
            ReadinessRing(fraction = 0.5f, centerText = "50%", colors = RingDefaults.onPrimaryColors())
        }
    }
    WavyProgress(fraction = 0.17f)
    WavyProgress(fraction = 0.83f)
}

@Preview(name = "Progress · light", widthDp = 390)
@Composable
private fun ProgressLight() = PreviewFrame(dark = false) { ProgressComponents() }

@Preview(name = "Progress · dark", widthDp = 390)
@Composable
private fun ProgressDark() = PreviewFrame(dark = true) { ProgressComponents() }

@Composable
private fun QuestionComponents() {
    val options = listOf(
        "Klassni avtomatik `Serializable` qiladi" to OptionState.WRONG,
        "Klassdan faqat bitta nusxa (singleton) yaratilishini kompilyator darajasida ta'minlaydi" to OptionState.IDLE,
        "`equals`, `hashCode`, `toString`, `copy` va `componentN` ni generatsiya qiladi" to OptionState.SELECTED,
        "Obyektni avtomatik ravishda o'zgarmas (immutable) qiladi" to OptionState.DIMMED,
    )
    options.forEachIndexed { i, (text, state) ->
        OptionCard(text, 'A' + i, state, multi = false, onClick = {})
    }
    OptionCard("`equals`, `hashCode` ni generatsiya qiladi", 'C', OptionState.CORRECT, multi = true, onClick = {})
    FeedbackSheet(FeedbackTone.WRONG, "Bu emas. Yana urinib ko'ring") {
        InlineCodeText("Serializatsiya uchun alohida interfeys yoki annotatsiya kerak.")
    }
    FeedbackSheet(FeedbackTone.CORRECT, "To'g'ri!") {
        InlineCodeText("Kompilyator nusxalash (`copy`) va destrukturizatsiya (`componentN`) funksiyalarini yozib beradi.")
    }
}

@Preview(name = "Question · light", widthDp = 390)
@Composable
private fun QuestionLight() = PreviewFrame(dark = false) { QuestionComponents() }

@Preview(name = "Question · dark", widthDp = 390)
@Composable
private fun QuestionDark() = PreviewFrame(dark = true) { QuestionComponents() }

@Preview(name = "Confetti", widthDp = 390, heightDp = 400)
@Composable
private fun ConfettiPreview() = DevSuhbatTheme(dark = false) {
    Confetti(play = true, modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
}

@Preview(name = "Basic · light", widthDp = 390)
@Composable
private fun BasicLight() = PreviewFrame(dark = false) { BasicComponents() }

@Preview(name = "Basic · dark", widthDp = 390)
@Composable
private fun BasicDark() = PreviewFrame(dark = true) { BasicComponents() }
