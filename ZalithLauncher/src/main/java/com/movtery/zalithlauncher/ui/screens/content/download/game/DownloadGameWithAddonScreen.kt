package com.movtery.zalithlauncher.ui.screens.content.download.game

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.addons.modloader.AddonVersion
import com.movtery.zalithlauncher.game.addons.modloader.ModLoader
import com.movtery.zalithlauncher.game.addons.modloader.ResponseTooShortException
import com.movtery.zalithlauncher.game.addons.modloader.fabriclike.fabric.FabricVersion
import com.movtery.zalithlauncher.game.addons.modloader.fabriclike.fabric.FabricVersions
import com.movtery.zalithlauncher.game.addons.modloader.fabriclike.quilt.QuiltVersion
import com.movtery.zalithlauncher.game.addons.modloader.fabriclike.quilt.QuiltVersions
import com.movtery.zalithlauncher.game.addons.modloader.forgelike.forge.ForgeVersion
import com.movtery.zalithlauncher.game.addons.modloader.forgelike.forge.ForgeVersions
import com.movtery.zalithlauncher.game.addons.modloader.forgelike.neoforge.NeoForgeVersion
import com.movtery.zalithlauncher.game.addons.modloader.forgelike.neoforge.NeoForgeVersions
import com.movtery.zalithlauncher.game.addons.modloader.optifine.OptiFineVersion
import com.movtery.zalithlauncher.game.addons.modloader.optifine.OptiFineVersions
import com.movtery.zalithlauncher.game.download.game.GameDownloadInfo
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.state.MutableStates
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.screens.content.DOWNLOAD_SCREEN_TAG
import com.movtery.zalithlauncher.ui.screens.content.download.DOWNLOAD_GAME_SCREEN_TAG
import com.movtery.zalithlauncher.ui.screens.content.elements.isFilenameInvalid
import com.movtery.zalithlauncher.utils.animation.getAnimateTween
import com.movtery.zalithlauncher.utils.animation.swapAnimateDpAsState
import com.movtery.zalithlauncher.utils.string.StringUtils
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.serialization.SerializationException
import java.net.ConnectException
import java.net.UnknownHostException

const val DOWNLOAD_GAME_WITH_ADDON_SCREEN_TAG = "DownloadGameWithAddonScreen"

private class AddonList {
    //版本列表
    var optifineList by mutableStateOf<List<OptiFineVersion>?>(null)
    var forgeList by mutableStateOf<List<ForgeVersion>?>(null)
    var neoforgeList by mutableStateOf<List<NeoForgeVersion>?>(null)
    var fabricList by mutableStateOf<List<FabricVersion>?>(null)
    var quiltList by mutableStateOf<List<QuiltVersion>?>(null)

    //重新加载
    var reloadOptiFine by mutableStateOf(false)
    var reloadForge by mutableStateOf(false)
    var reloadNeoForge by mutableStateOf(false)
    var reloadFabric by mutableStateOf(false)
    var reloadQuilt by mutableStateOf(false)
}

private class CurrentAddon {
    //当前选择版本
    var optifineVersion by mutableStateOf<OptiFineVersion?>(null)
    var forgeVersion by mutableStateOf<ForgeVersion?>(null)
    var neoforgeVersion by mutableStateOf<NeoForgeVersion?>(null)
    var fabricVersion by mutableStateOf<FabricVersion?>(null)
    var quiltVersion by mutableStateOf<QuiltVersion?>(null)

    //加载状态
    var optifineState by mutableStateOf<AddonState>(AddonState.None)
    var forgeState by mutableStateOf<AddonState>(AddonState.None)
    var neoforgeState by mutableStateOf<AddonState>(AddonState.None)
    var fabricState by mutableStateOf<AddonState>(AddonState.None)
    var quiltState by mutableStateOf<AddonState>(AddonState.None)

    //不兼容列表 利用Set集合不可重复
    var incompatibleWithOptiFine by mutableStateOf<Set<ModLoader>>(emptySet())
    var incompatibleWithForge by mutableStateOf<Set<ModLoader>>(emptySet())
    var incompatibleWithNeoForge by mutableStateOf<Set<ModLoader>>(emptySet())
    var incompatibleWithFabric by mutableStateOf<Set<ModLoader>>(emptySet())
    var incompatibleWithQuilt by mutableStateOf<Set<ModLoader>>(emptySet())

    /**
     * 获取当前所有已选择的 Addon 版本
     */
    fun getAllAddons(): List<AddonVersion> {
        return listOfNotNull(
            optifineVersion,
            forgeVersion,
            neoforgeVersion,
            fabricVersion,
            quiltVersion
        )
    }
}

@Composable
fun DownloadGameWithAddonScreen(
    gameVersion: String,
    onInstall: (GameDownloadInfo) -> Unit = {}
) {
    val addonList = AddonList()
    val currentAddon = CurrentAddon()

    BaseScreen(
        Triple(DOWNLOAD_SCREEN_TAG, MutableStates.mainScreenTag, true),
        Triple(DOWNLOAD_GAME_SCREEN_TAG, MutableStates.downloadScreenTag, false),
        Triple(DOWNLOAD_GAME_WITH_ADDON_SCREEN_TAG, DownloadGameScreenStates.screenTag, true),
    ) { isVisible ->
        val yOffset by swapAnimateDpAsState(
            targetValue = (-40).dp,
            swapIn = isVisible
        )

        Card(
            modifier = Modifier
                .padding(all = 12.dp)
                .fillMaxSize()
                .offset { IntOffset(x = 0, y = yOffset.roundToPx()) },
            shape = MaterialTheme.shapes.extraLarge,
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
        ) {
            ScreenHeader(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                gameVersion = gameVersion,
                currentAddon = currentAddon,
                onInstall = { customVersionName ->
                    val info = GameDownloadInfo(
                        gameVersion = gameVersion,
                        customVersionName = customVersionName,
                        addons = currentAddon.getAllAddons()
                    )
                    onInstall(info)
                }
            )

            Column(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .verticalScroll(state = rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                val yOffset1 by swapAnimateDpAsState(targetValue = (-40).dp, swapIn = isVisible)
                OptiFineList(
                    modifier = Modifier.offset { IntOffset(x = 0, y = yOffset1.roundToPx()) },
                    gameVersion = gameVersion,
                    currentAddon = currentAddon,
                    addonList = addonList
                )

                val yOffset2 by swapAnimateDpAsState(targetValue = (-40).dp, swapIn = isVisible, delayMillis = 50)
                ForgeList(
                    modifier = Modifier.offset { IntOffset(x = 0, y = yOffset2.roundToPx()) },
                    gameVersion = gameVersion,
                    currentAddon = currentAddon,
                    addonList = addonList
                )

                val yOffset3 by swapAnimateDpAsState(targetValue = (-40).dp, swapIn = isVisible, delayMillis = 100)
                NeoForgeList(
                    modifier = Modifier.offset { IntOffset(x = 0, y = yOffset3.roundToPx()) },
                    gameVersion = gameVersion,
                    currentAddon = currentAddon,
                    addonList = addonList
                )

                val yOffset4 by swapAnimateDpAsState(targetValue = (-40).dp, swapIn = isVisible, delayMillis = 150)
                FabricList(
                    modifier = Modifier.offset { IntOffset(x = 0, y = yOffset4.roundToPx()) },
                    gameVersion = gameVersion,
                    currentAddon = currentAddon,
                    addonList = addonList
                )

                val yOffset5 by swapAnimateDpAsState(targetValue = (-40).dp, swapIn = isVisible, delayMillis = 200)
                QuiltList(
                    modifier = Modifier.offset { IntOffset(x = 0, y = yOffset5.roundToPx()) },
                    gameVersion = gameVersion,
                    currentAddon = currentAddon,
                    addonList = addonList
                )
            }
        }
    }
}

@Composable
private fun ScreenHeader(
    modifier: Modifier = Modifier,
    gameVersion: String,
    currentAddon: CurrentAddon,
    onInstall: (String) -> Unit = {}
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(modifier = Modifier.width(8.dp))

            VersionIconPreview(
                modifier = Modifier.size(28.dp),
                currentAddon = currentAddon
            )

            var nameValue by remember { mutableStateOf(gameVersion) }
            var errorMessage by remember { mutableStateOf("") }

            val isError = nameValue.isEmpty().also {
                errorMessage = stringResource(R.string.generic_cannot_empty)
            } || isFilenameInvalid(nameValue) { message ->
                errorMessage = message
            } || VersionsManager.validateVersionName(nameValue, null) { message ->
                errorMessage = message
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp)
                    .animateContentSize(animationSpec = getAnimateTween())
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = 4.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = RoundedCornerShape(50f),
                    shadowElevation = 2.dp
                ) {
                    BasicTextField(
                        modifier = Modifier
                            .weight(1f)
                            .height(28.dp)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        value = nameValue,
                        onValueChange = {
                            nameValue = it
                        },
                        textStyle = TextStyle(color = LocalContentColor.current).copy(fontSize = 12.sp),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            Box(
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (nameValue.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.download_game_version_name),
                                        style = TextStyle(color = LocalContentColor.current).copy(fontSize = 12.sp)
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }

                if (isError) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            IconButton(
                onClick = {
                    if (!isError) {
                        onInstall(nameValue)
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = stringResource(R.string.download_install)
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun VersionIconPreview(
    currentAddon: CurrentAddon,
    modifier: Modifier = Modifier
) {
    val iconRes = when {
        currentAddon.optifineVersion != null && currentAddon.forgeVersion != null -> R.drawable.ic_anvil //OptiFine & Forge 同时选择
        currentAddon.optifineVersion != null -> R.drawable.ic_optifine
        currentAddon.forgeVersion != null -> R.drawable.ic_anvil
        currentAddon.neoforgeVersion != null -> R.drawable.ic_neoforge
        currentAddon.fabricVersion != null -> R.drawable.ic_fabric
        currentAddon.quiltVersion != null -> R.drawable.ic_quilt
        else -> R.drawable.ic_minecraft
    }

    Image(
        modifier = modifier,
        painter = painterResource(id = iconRes),
        contentDescription = null
    )
}

@Composable
private fun OptiFineList(
    modifier: Modifier = Modifier,
    gameVersion: String,
    currentAddon: CurrentAddon,
    addonList: AddonList
) {
    AddonListLayout(
        modifier = modifier,
        state = currentAddon.optifineState,
        title = ModLoader.OPTIFINE.displayName,
        iconPainter = painterResource(R.drawable.ic_optifine),
        items = addonList.optifineList,
        itemsFilter = if (currentAddon.forgeVersion != null) {
            {
                //选择 Forge 之后，过滤为当前 OptiFine 列表内能够匹配的版本
                when {
                    it.forgeVersion == null -> false
                    it.forgeVersion.isEmpty() -> true
                    else -> StringUtils.compareClassVersions(currentAddon.forgeVersion!!.version.toString(), it.forgeVersion) == 0
                }
            }
        } else {
            null
        },
        current = currentAddon.optifineVersion,
        incompatibleSet = currentAddon.incompatibleWithOptiFine,
        getItemText = { it.displayName },
        summary = { OptiFineVersionSummary(it) },
        onValueChange = { version ->
            val ofType = listOf(ModLoader.OPTIFINE)
            if (version == null) {
                currentAddon.optifineVersion = null
                currentAddon.incompatibleWithForge -= ofType
                currentAddon.incompatibleWithNeoForge -= ofType
                currentAddon.incompatibleWithFabric -= ofType
                currentAddon.incompatibleWithQuilt -= ofType
            } else {
                val forgeVersion = currentAddon.forgeVersion
                //检查与 Forge 的兼容性
                if (forgeVersion != null) {
                    if (isOptiFineCompatibleWithForge(version, forgeVersion)) {
                        currentAddon.incompatibleWithForge -= ofType
                    } else {
                        currentAddon.incompatibleWithForge += ofType
                        currentAddon.forgeVersion = null
                    }
                } else {
                    if (isOptiFineCompatibleWithForgeList(version, addonList.forgeList)) {
                        currentAddon.incompatibleWithForge -= ofType
                    } else {
                        currentAddon.incompatibleWithForge += ofType
                        currentAddon.forgeVersion = null
                    }
                }
                currentAddon.optifineVersion = version
                currentAddon.neoforgeVersion = null
                currentAddon.fabricVersion = null
                currentAddon.quiltVersion = null
                currentAddon.incompatibleWithNeoForge += ofType
                currentAddon.incompatibleWithFabric += ofType
                currentAddon.incompatibleWithQuilt += ofType
            }
        },
        onReload = { addonList.reloadOptiFine = !addonList.reloadOptiFine }
    )

    LaunchedEffect(addonList.reloadOptiFine) {
        runWithState({ currentAddon.optifineState = it }) {
            OptiFineVersions.fetchOptiFineList()?.filter { it.inherit == gameVersion }
        }.also {
            addonList.optifineList = it
        }
    }
}

@Composable
private fun ForgeList(
    modifier: Modifier = Modifier,
    gameVersion: String,
    currentAddon: CurrentAddon,
    addonList: AddonList
) {
    AddonListLayout(
        modifier = modifier,
        state = currentAddon.forgeState,
        title = ModLoader.FORGE.displayName,
        iconPainter = painterResource(R.drawable.ic_anvil),
        items = addonList.forgeList,
        itemsFilter = if (currentAddon.optifineVersion != null) {
            {
                //选择 OptiFine 之后，根据 OptiFine 需求的 Forge 版本进行过滤
                val requiredVersion = currentAddon.optifineVersion!!.forgeVersion
                when {
                    requiredVersion == null -> false
                    requiredVersion.isEmpty() -> true
                    else -> StringUtils.compareClassVersions(it.version.toString(), requiredVersion) == 0
                }
            }
        } else {
            null
        },
        current = currentAddon.forgeVersion,
        incompatibleSet = currentAddon.incompatibleWithForge,
        error = checkForgeCompatibilityError(addonList.forgeList),
        getItemText = { it.versionName },
        summary = { ForgeVersionSummary(it) },
        onValueChange = { version ->
            val forgeType = listOf(ModLoader.FORGE)
            if (version == null) {
                currentAddon.forgeVersion = null
                currentAddon.incompatibleWithOptiFine -= forgeType
                currentAddon.incompatibleWithNeoForge -= forgeType
                currentAddon.incompatibleWithFabric -= forgeType
                currentAddon.incompatibleWithQuilt -= forgeType
            } else {
                val optiFineVersion = currentAddon.optifineVersion
                //检查与 OptiFine 的兼容性
                if (optiFineVersion != null) {
                    if (isOptiFineCompatibleWithForge(optiFineVersion, version)) {
                        currentAddon.incompatibleWithOptiFine -= forgeType
                    } else {
                        currentAddon.incompatibleWithOptiFine += forgeType
                        currentAddon.optifineVersion = null
                    }
                } else {
                    if (isForgeCompatibleWithOptiFineList(version, addonList.optifineList)) {
                        currentAddon.incompatibleWithForge -= forgeType
                    } else {
                        currentAddon.incompatibleWithOptiFine += forgeType
                        currentAddon.optifineVersion = null
                    }
                }
                currentAddon.forgeVersion = version
                currentAddon.neoforgeVersion = null
                currentAddon.fabricVersion = null
                currentAddon.quiltVersion = null
                currentAddon.incompatibleWithNeoForge += forgeType
                currentAddon.incompatibleWithFabric += forgeType
                currentAddon.incompatibleWithQuilt += forgeType
            }
        },
        onReload = { addonList.reloadForge = !addonList.reloadForge }
    )

    LaunchedEffect(addonList.reloadForge) {
        runWithState({ currentAddon.forgeState = it }) {
            ForgeVersions.fetchForgeList(gameVersion)
        }.also {
            addonList.forgeList = it
        }
    }
}

@Composable
private fun NeoForgeList(
    modifier: Modifier = Modifier,
    gameVersion: String,
    currentAddon: CurrentAddon,
    addonList: AddonList
) {
    AddonListLayout(
        modifier = modifier,
        state = currentAddon.neoforgeState,
        title = ModLoader.NEOFORGE.displayName,
        iconPainter = painterResource(R.drawable.ic_neoforge),
        items = addonList.neoforgeList,
        current = currentAddon.neoforgeVersion,
        incompatibleSet = currentAddon.incompatibleWithNeoForge,
        getItemText = { it.versionName },
        summary = { NeoForgeSummary(it) },
        onValueChange = { version ->
            val neoforgeType = listOf(ModLoader.NEOFORGE)
            if (version == null) {
                currentAddon.neoforgeVersion = null
                currentAddon.incompatibleWithOptiFine -= neoforgeType
                currentAddon.incompatibleWithForge -= neoforgeType
                currentAddon.incompatibleWithFabric -= neoforgeType
                currentAddon.incompatibleWithQuilt -= neoforgeType
            } else {
                currentAddon.neoforgeVersion = version
                currentAddon.optifineVersion = null
                currentAddon.forgeVersion = null
                currentAddon.fabricVersion = null
                currentAddon.quiltVersion = null
                currentAddon.incompatibleWithOptiFine += neoforgeType
                currentAddon.incompatibleWithForge += neoforgeType
                currentAddon.incompatibleWithFabric += neoforgeType
                currentAddon.incompatibleWithQuilt += neoforgeType
            }
        },
        onReload = { addonList.reloadNeoForge = !addonList.reloadNeoForge }
    )

    LaunchedEffect(addonList.reloadNeoForge) {
        runWithState({ currentAddon.neoforgeState = it }) {
            NeoForgeVersions.fetchNeoForgeList()?.filter { it.inherit == gameVersion }
        }.also {
            addonList.neoforgeList = it
        }
    }
}

@Composable
private fun FabricList(
    modifier: Modifier = Modifier,
    gameVersion: String,
    currentAddon: CurrentAddon,
    addonList: AddonList
) {
    AddonListLayout(
        modifier = modifier,
        state = currentAddon.fabricState,
        title = ModLoader.FABRIC.displayName,
        iconPainter = painterResource(R.drawable.ic_fabric),
        items = addonList.fabricList,
        current = currentAddon.fabricVersion,
        incompatibleSet = currentAddon.incompatibleWithFabric,
        getItemText = { it.version },
        summary = { FabricSummary(it) },
        onValueChange = { version ->
            val fabricType = listOf(ModLoader.FABRIC)
            if (version == null) {
                currentAddon.fabricVersion = null
                currentAddon.incompatibleWithOptiFine -= fabricType
                currentAddon.incompatibleWithForge -= fabricType
                currentAddon.incompatibleWithNeoForge -= fabricType
                currentAddon.incompatibleWithQuilt -= fabricType
            } else {
                currentAddon.fabricVersion = version
                currentAddon.optifineVersion = null
                currentAddon.forgeVersion = null
                currentAddon.neoforgeVersion = null
                currentAddon.quiltVersion = null
                currentAddon.incompatibleWithOptiFine += fabricType
                currentAddon.incompatibleWithForge += fabricType
                currentAddon.incompatibleWithNeoForge += fabricType
                currentAddon.incompatibleWithQuilt += fabricType
            }
        },
        onReload = { addonList.reloadFabric = !addonList.reloadFabric }
    )

    LaunchedEffect(addonList.reloadFabric) {
        runWithState({ currentAddon.fabricState = it }) {
            FabricVersions.fetchFabricLoaderList(gameVersion)
        }.also {
            addonList.fabricList = it
        }
    }
}

@Composable
private fun QuiltList(
    modifier: Modifier = Modifier,
    gameVersion: String,
    currentAddon: CurrentAddon,
    addonList: AddonList
) {
    AddonListLayout(
        modifier = modifier,
        state = currentAddon.quiltState,
        title = ModLoader.QUILT.displayName,
        iconPainter = painterResource(R.drawable.ic_quilt),
        items = addonList.quiltList,
        current = currentAddon.quiltVersion,
        incompatibleSet = currentAddon.incompatibleWithQuilt,
        getItemText = { it.version },
        summary = { QuiltSummary(it) },
        onValueChange =  { version ->
            val quiltType = listOf(ModLoader.QUILT)
            if (version == null) {
                currentAddon.quiltVersion = null
                currentAddon.incompatibleWithOptiFine -= quiltType
                currentAddon.incompatibleWithForge -= quiltType
                currentAddon.incompatibleWithNeoForge -= quiltType
                currentAddon.incompatibleWithFabric -= quiltType
            } else {
                currentAddon.quiltVersion = version
                currentAddon.optifineVersion = null
                currentAddon.forgeVersion = null
                currentAddon.neoforgeVersion = null
                currentAddon.fabricVersion = null
                currentAddon.incompatibleWithOptiFine += quiltType
                currentAddon.incompatibleWithForge += quiltType
                currentAddon.incompatibleWithNeoForge += quiltType
                currentAddon.incompatibleWithFabric += quiltType
            }
        },
        onReload = { addonList.reloadQuilt = !addonList.reloadQuilt }
    )

    LaunchedEffect(addonList.reloadQuilt) {
        runWithState({ currentAddon.quiltState = it }) {
            QuiltVersions.fetchQuiltLoaderList(gameVersion)
        }.also {
            addonList.quiltList = it
        }
    }
}

private fun isOptiFineCompatibleWithForge(
    optifine: OptiFineVersion,
    forge: ForgeVersion
): Boolean {
                                    //没有声明需要的 Forge 版本，视为不兼容
    val requiredVersion = optifine.forgeVersion ?: return false

    if (requiredVersion.isBlank()) return true

    return if ('.' in requiredVersion) {
        StringUtils.compareClassVersions(forge.version.toString(), requiredVersion) == 0
    } else {
        forge.version.revision.toString() == requiredVersion
    }
}

private fun isOptiFineCompatibleWithForgeList(
    optifine: OptiFineVersion,
    forgeList: List<ForgeVersion>?
): Boolean {
    return when {
        optifine.forgeVersion == null -> false //为null则表示不兼容
        optifine.forgeVersion.isEmpty() -> true //为空则表示不要求，兼容
        else -> forgeList?.any {
            StringUtils.compareClassVersions(it.version.toString(), optifine.forgeVersion) == 0
        } == true
    }
}

private fun isForgeCompatibleWithOptiFineList(
    forge: ForgeVersion,
    optifineList: List<OptiFineVersion>?
): Boolean {
    val forgeVersion = forge.version.toString()

    optifineList?.forEach { optifine ->
        val ofVersion = optifine.forgeVersion ?: return@forEach //null: 不兼容，跳过
        if (ofVersion.isEmpty()) return true    //空字符串表示兼容所有
        if (StringUtils.compareClassVersions(forgeVersion, ofVersion) == 0) return true
    }

    return false //没有匹配项
}

@Composable
private fun checkForgeCompatibilityError(
    forgeList: List<ForgeVersion>?
): String? {
    return when {
        forgeList == null -> null //保持默认的“不可用”
        forgeList.any { forgeVersion -> forgeVersion.category == "universal" || forgeVersion.category == "client" } -> {
            //跳过无法自动安装的版本
            stringResource(R.string.download_game_addon_not_installable)
        }
        else -> null
    }
}

private suspend fun <T> runWithState(
    updateState: (AddonState) -> Unit,
    block: suspend () -> T?
): T? {
    updateState(AddonState.Loading)
    return runCatching {
        block().also {
            updateState(AddonState.None)
        }
    }.onFailure { e ->
        when (e) {
            is ResponseTooShortException -> {
                //忽略，判定为不可用
                updateState(AddonState.None)
            }
            is HttpRequestTimeoutException -> updateState(AddonState.Error(R.string.error_timeout))
            is UnknownHostException -> {
                AddonState.Error(R.string.error_network_unreachable)
            }
            is ConnectException -> {
                AddonState.Error(R.string.error_connection_failed)
            }
            is SerializationException -> {
                AddonState.Error(R.string.error_parse_failed)
            }
            else -> {
                val errorMessage = e.localizedMessage ?: e.message ?: e::class.qualifiedName ?: "Unknown error"
                AddonState.Error(R.string.error_unknown, arrayOf(errorMessage))
            }
        }
    }.getOrNull()
}