package com.sentinela.camtv.data.update

import android.content.Context
import com.sentinela.camtv.config.ProjectLinks
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GitHubReleaseUpdateRepository(
    context: Context,
    private val latestReleaseUrl: String = DEFAULT_LATEST_RELEASE_URL,
) : UpdateRepository {
    private val appContext = context.applicationContext
    private val updatesDir: File = File(appContext.cacheDir, "updates")

    override suspend fun checkForUpdate(
        currentVersionName: String,
        supportedAbis: List<String>,
    ): Result<UpdateCheckResult> = withContext(Dispatchers.IO) {
        runCatching {
            val release = GitHubReleaseParser.parse(
                json = getText(latestReleaseUrl),
            )
            val remoteVersionName = AppVersion.normalize(release.tagName)
            if (remoteVersionName.isBlank() || !AppVersion.isNewer(remoteVersionName, currentVersionName)) {
                return@runCatching UpdateCheckResult.UpToDate
            }

            val asset = ReleaseAssetSelector.selectApk(
                assets = release.assets,
                supportedAbis = supportedAbis,
            ) ?: error("Nenhum APK compatível foi encontrado nesta versão.")
            val checksumAsset = ReleaseAssetSelector.selectSha256Sums(release.assets)

            UpdateCheckResult.Available(
                AvailableUpdate(
                    versionName = remoteVersionName,
                    assetName = asset.name,
                    downloadUrl = asset.downloadUrl,
                    checksumUrl = checksumAsset?.downloadUrl,
                    releasePageUrl = release.htmlUrl,
                    changelog = release.body,
                ),
            )
        }.recoverCatching { error ->
            throw IllegalStateException(error.toUserFacingMessage())
        }
    }

    override suspend fun downloadUpdate(update: AvailableUpdate): Result<DownloadedUpdate> =
        withContext(Dispatchers.IO) {
            runCatching {
                updatesDir.mkdirs()
                updatesDir.listFiles()
                    ?.filter { file -> file.extension.equals("apk", ignoreCase = true) }
                    ?.forEach { file -> file.delete() }

                val targetFile = targetFileFor(update)
                val temporaryFile = File(updatesDir, "${targetFile.name}.download")
                downloadToFile(
                    sourceUrl = update.downloadUrl,
                    targetFile = temporaryFile,
                )
                validateSha256(
                    update = update,
                    downloadedFile = temporaryFile,
                )
                if (targetFile.exists()) targetFile.delete()
                check(temporaryFile.renameTo(targetFile)) {
                    "Não foi possível preparar o APK baixado."
                }

                DownloadedUpdate(
                    update = update,
                    filePath = targetFile.absolutePath,
                )
            }.recoverCatching { error ->
                throw IllegalStateException(error.toUserFacingMessage())
            }
        }

    override suspend fun findDownloadedUpdate(update: AvailableUpdate): Result<DownloadedUpdate?> =
        withContext(Dispatchers.IO) {
            runCatching {
                val targetFile = targetFileFor(update)
                if (!targetFile.isFile) {
                    return@runCatching null
                }

                runCatching {
                    val checksumUrl = update.checksumUrl
                        ?: error("SHA256SUMS.txt não foi encontrado nesta versão.")
                    val downloaded = DownloadedUpdateCache.validDownloadedUpdateOrNull(
                        update = update,
                        downloadedFile = targetFile,
                        sumsText = getText(checksumUrl),
                    ) ?: error("Falha ao validar o APK baixado.")
                    downloaded
                }.getOrElse {
                    targetFile.delete()
                    null
                }
            }
        }

    private fun validateSha256(
        update: AvailableUpdate,
        downloadedFile: File,
    ) {
        val checksumUrl = update.checksumUrl
            ?: error("SHA256SUMS.txt não foi encontrado nesta versão.")
        val checksums = getText(checksumUrl)
        val expectedSha256 = Sha256Sums.expectedHashFor(
            sumsText = checksums,
            fileName = update.assetName,
        ) ?: error("SHA-256 do APK não foi encontrado no SHA256SUMS.txt.")
        val actualSha256 = Sha256Sums.sha256(downloadedFile)
        check(expectedSha256.equals(actualSha256, ignoreCase = true)) {
            "Falha ao validar o APK baixado."
        }
    }

    private fun getText(url: String): String {
        val connection = openConnection(url)
        return connection.inputStream.bufferedReader().use { reader ->
            reader.readText()
        }
    }

    private fun downloadToFile(
        sourceUrl: String,
        targetFile: File,
    ) {
        val connection = openConnection(sourceUrl)
        connection.inputStream.use { input ->
            targetFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun targetFileFor(update: AvailableUpdate): File =
        DownloadedUpdateCache.targetFile(
            updatesDir = updatesDir,
            assetName = update.assetName,
        )

    private fun openConnection(url: String): HttpURLConnection {
        val parsedUrl = URL(url)
        check(parsedUrl.protocol.equals("https", ignoreCase = true)) {
            "Atualização exige conexão HTTPS."
        }
        val connection = parsedUrl.openConnection() as HttpURLConnection
        connection.connectTimeout = NETWORK_TIMEOUT_MS
        connection.readTimeout = NETWORK_TIMEOUT_MS
        connection.setRequestProperty("User-Agent", "SentinelaCamTV")
        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
            connection.disconnect()
            error("O site respondeu com HTTP $responseCode.")
        }
        return connection
    }

    private fun Throwable.toUserFacingMessage(): String {
        val details = message.orEmpty()
        return when {
            details.contains("HTTP 404") -> "Nenhuma versão pública foi encontrada no site."
            details.contains("HTTP 403") -> "O site limitou temporariamente a busca por atualização."
            details.contains("APK compatível") -> details
            details.contains("Unable to resolve host", ignoreCase = true) -> "Não foi possível acessar o site."
            else -> details.ifBlank { "Falha ao buscar atualização." }
        }
    }

    private companion object {
        private const val DEFAULT_LATEST_RELEASE_URL = ProjectLinks.GITHUB_LATEST_RELEASE_API_URL
        private const val NETWORK_TIMEOUT_MS = 15_000
    }
}
