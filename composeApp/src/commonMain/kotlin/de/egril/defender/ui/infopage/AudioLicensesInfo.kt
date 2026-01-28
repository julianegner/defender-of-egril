@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package de.egril.defender.ui.infopage

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.*

/**
 * Composable displaying audio files sources and licenses
 */
@Composable
fun AudioLicensesInfo() {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(Res.string.audio_licenses_title),
            style = MaterialTheme.typography.displayMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            // Background Music Section
            AudioSection(
                title = stringResource(Res.string.audio_background_music_title)
            ) {
                // Fantasy Ambience
                AudioItem(
                    name = stringResource(Res.string.audio_fantasy_ambience_name),
                    file = "2021-02-23_-_Fantasy_Ambience_-_David_Fesliyan.mp3",
                    source = "https://www.fesliyanstudios.com/royalty-free-music/download/fantasy-ambience/1702",
                    license = stringResource(Res.string.audio_fantasy_ambience_license)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // The Dark Castle
                AudioItem(
                    name = stringResource(Res.string.audio_dark_castle_name),
                    file = "2017-06-16_-_The_Dark_Castle_-_David_Fesliyan.mp3",
                    source = "https://www.fesliyanstudios.com/",
                    license = stringResource(Res.string.audio_fesliyan_license)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Fantasy Orchestral Music
                AudioItem(
                    name = stringResource(Res.string.audio_fantasy_orchestral_name),
                    file = "atmosphere-mystic-fantasy-orchestral-music-335263.mp3",
                    source = "https://pixabay.com/music/mystery-atmosphere-mystic-fantasy-orchestral-music-335263/",
                    license = stringResource(Res.string.audio_pixabay_license)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Sound Effects Section
            AudioSection(
                title = stringResource(Res.string.audio_sound_effects_title)
            ) {
                // Tower Attacks
                Text(
                    text = stringResource(Res.string.audio_tower_attacks_subtitle),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
                )
                
                AudioItem(
                    name = stringResource(Res.string.audio_melee_attack_name),
                    file = "342397__christopherderp__swords-clash-w-swing-1.wav",
                    source = "https://freesound.org/people/Christopherderp/sounds/342397/",
                    license = "Creative Commons 0"
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                AudioItem(
                    name = stringResource(Res.string.audio_ranged_attack_name),
                    file = "649334__sonofxaudio__arrow_loose02.wav",
                    source = "https://freesound.org/people/SonoFxAudio/sounds/649334/",
                    license = "Attribution 4.0"
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                AudioItem(
                    name = stringResource(Res.string.audio_fireball_attack_name),
                    file = "564041__robinhood76__10056-giant-fireball-blow.wav",
                    source = "https://freesound.org/people/Robinhood76/sounds/564041/",
                    license = "Attribution NonCommercial 4.0"
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                AudioItem(
                    name = stringResource(Res.string.audio_acid_attack_name),
                    file = "202094__spookymodem__acid-bubbling.wav",
                    source = "https://freesound.org/people/spookymodem/sounds/202094/",
                    license = "Creative Commons 0"
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Enemy Events
                Text(
                    text = stringResource(Res.string.audio_enemy_events_subtitle),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
                )
                
                AudioItem(
                    name = stringResource(Res.string.audio_enemy_spawn_name),
                    file = "384898__ali_6868__knight-left-footstep-forestgrass-3-with-chainmail.wav",
                    source = "https://freesound.org/people/Ali_6868/sounds/384898/",
                    license = "Creative Commons 0"
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                AudioItem(
                    name = stringResource(Res.string.audio_enemy_destroyed_name),
                    file = "656726__paladinvii__deathsfx.wav",
                    source = "https://freesound.org/people/PaladinVII/sounds/656726/",
                    license = "Attribution 4.0"
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Mine Events
                Text(
                    text = stringResource(Res.string.audio_mine_events_subtitle),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
                )
                
                AudioItem(
                    name = stringResource(Res.string.audio_mine_dig_name),
                    file = "240801__ryanconway__pickaxe-mining-stone.wav",
                    source = "https://freesound.org/people/ryanconway/sounds/240801/",
                    license = "Attribution 4.0"
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                AudioItem(
                    name = stringResource(Res.string.audio_mine_coin_name),
                    file = "761495__paul-sinnett__coin-clink.wav",
                    source = "https://freesound.org/people/Paul%20Sinnett/sounds/761495/",
                    license = "Attribution 4.0"
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                AudioItem(
                    name = stringResource(Res.string.audio_mine_trap_name),
                    file = "537430__wavecal22__wood-misc-6.wav",
                    source = "https://freesound.org/people/wavecal22/sounds/537430/",
                    license = "Creative Commons 0"
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                AudioItem(
                    name = stringResource(Res.string.audio_mine_dragon_name),
                    file = "676474__neartheatmoshphere__beast-1.wav",
                    source = "https://freesound.org/people/NearTheAtmoshphere/sounds/676474/",
                    license = "Creative Commons 0"
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Other Game Events
                Text(
                    text = stringResource(Res.string.audio_other_events_subtitle),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
                )
                
                AudioItem(
                    name = stringResource(Res.string.audio_trap_trigger_name),
                    file = "434898__thebuilder15__trap-switch.wav",
                    source = "https://freesound.org/people/TheBuilder15/sounds/434898/",
                    license = "Creative Commons 0"
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                AudioItem(
                    name = stringResource(Res.string.audio_life_lost_name),
                    file = "221544__joseppujol__wounded-man-scream.mp3",
                    source = "https://freesound.org/people/joseppujol/sounds/221544/",
                    license = "Creative Commons 0"
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                AudioItem(
                    name = stringResource(Res.string.audio_dragon_eat_name),
                    file = "389638__stubb__growl-7.wav",
                    source = "https://freesound.org/people/_stubb/sounds/389638/",
                    license = "Creative Commons 0"
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                AudioItem(
                    name = stringResource(Res.string.audio_tower_upgraded_name),
                    file = "810754__mokasza__level-up-02.mp3",
                    source = "https://freesound.org/people/mokasza/sounds/810754/",
                    license = "Attribution 4.0"
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                AudioItem(
                    name = stringResource(Res.string.audio_battle_start_name),
                    file = "188815__porphyr__battle-horn.wav",
                    source = "https://freesound.org/people/Porphyr/sounds/188815/",
                    license = "Attribution 4.0"
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Attribution note
            Text(
                text = stringResource(Res.string.audio_attribution_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
            )
        }
    }
}

@Composable
private fun AudioSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        content()
    }
}

@Composable
private fun AudioItem(
    name: String,
    file: String,
    source: String,
    license: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            Text(
                text = stringResource(Res.string.audio_file_label, file),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            
            Text(
                text = stringResource(Res.string.audio_source_label, source),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            
            Text(
                text = stringResource(Res.string.audio_license_label, license),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
