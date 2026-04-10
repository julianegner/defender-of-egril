package de.egril.defender

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicReference
import javax.sql.DataSource

private val analyticsLogger = LoggerFactory.getLogger("Analytics")
private val savefileLogger = LoggerFactory.getLogger("Savefiles")
private val communityLogger = LoggerFactory.getLogger("Community")
private val userDataLogger = LoggerFactory.getLogger("UserData")
private val settingsLogger = LoggerFactory.getLogger("Settings")
private val iamLogger = LoggerFactory.getLogger("IAM")

fun Application.configureRouting(dataSourceRef: AtomicReference<DataSource?>) {
    routing {
        get("/") {
            call.respondText("Defender of Egril Backend", ContentType.Text.Plain)
        }

        /**
         * Health endpoint.
         * Returns HTTP 200 {"status":"UP","database":"connected"} when the DB is reachable.
         * Returns HTTP 503 {"status":"DOWN","database":"unavailable"} when dataSource is null.
         *
         * Docker uses this endpoint as the backend container's healthcheck so that
         * `restart: on-failure` (or `restart: always`) kicks in automatically when the
         * DB was not ready at startup and the old image (without retry logic) was used.
         */
        get("/health") {
            val dataSource = dataSourceRef.get()
            if (dataSource == null) {
                call.respondText(
                    """{"status":"DOWN","database":"unavailable"}""",
                    ContentType.Application.Json,
                    HttpStatusCode.ServiceUnavailable
                )
                return@get
            }
            try {
                dataSource.connection.use { conn ->
                    conn.prepareStatement("SELECT 1").use { stmt ->
                        stmt.executeQuery().use { /* connection verified */ }
                    }
                }
                call.respondText(
                    """{"status":"UP","database":"connected"}""",
                    ContentType.Application.Json,
                    HttpStatusCode.OK
                )
            } catch (e: Exception) {
                val safeMessage = (e.message ?: "error")
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t")
                call.respondText(
                    """{"status":"DOWN","database":"$safeMessage"}""",
                    ContentType.Application.Json,
                    HttpStatusCode.ServiceUnavailable
                )
            }
        }

        /**
         * Keycloak Back-Channel Logout endpoint (OpenID Connect Back-Channel Logout spec).
         *
         * Called by Keycloak when a user's SSO session is terminated (e.g. the user logs out
         * from any client connected to the same realm). Keycloak sends an HTTP POST with
         * Content-Type: application/x-www-form-urlencoded containing a signed `logout_token` JWT.
         *
         * This endpoint must be registered as `backchannel.logout.url` in the Keycloak client
         * configuration (egril-realm.json).
         *
         * Since the backend uses stateless JWT authentication there are no server-side sessions
         * to invalidate. The endpoint acknowledges the notification with 200 OK so that Keycloak
         * considers the backchannel logout successful and completes the user's own SSO logout.
         * Without this endpoint Keycloak may fail the logout flow and the user remains logged in.
         *
         * Note on JWT signature verification: the OIDC spec recommends validating the logout_token
         * signature against Keycloak's JWKS. Since the backend holds no server-side sessions the
         * worst-case impact of a spoofed request is a no-op 200 OK with a log entry. Full JWKS
         * verification can be added in the future if the backend starts maintaining sessions.
         */
        post("/api/backchannel-logout") {
            val params = try {
                call.receiveParameters()
            } catch (e: Exception) {
                iamLogger.warn("Backchannel logout: failed to parse form parameters: ${e.message}")
                call.respond(HttpStatusCode.BadRequest, "Invalid request parameters")
                return@post
            }
            val logoutToken = params["logout_token"]
            if (logoutToken == null) {
                iamLogger.warn("Backchannel logout: missing logout_token parameter")
                call.respond(HttpStatusCode.BadRequest, "Missing logout_token parameter")
                return@post
            }
            // Verify the token has three dot-separated parts (header.payload.signature).
            // This rejects obviously malformed values without a full JWKS round-trip.
            if (logoutToken.split(".").size != 3) {
                iamLogger.warn("Backchannel logout: malformed logout_token (expected 3 JWT parts)")
                call.respond(HttpStatusCode.BadRequest, "Malformed logout_token")
                return@post
            }
            // The backend uses stateless JWT; there are no server-side sessions to invalidate.
            // Acknowledge the notification so Keycloak completes the logout successfully.
            iamLogger.info("Backchannel logout received")
            call.respond(HttpStatusCode.OK)
        }

        post("/api/events") {
            val event = try {
                call.receive<GameEvent>()
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, "Invalid event payload: ${e.message}")
                return@post
            }

            // Optionally extract the authenticated username from the Bearer token.
            // Authentication is not required – the token is only used for audit logging.
            val authUser = extractUsernameFromBearerToken(call.request.header(HttpHeaders.Authorization))

            val message = buildString {
                append("[${event.event}] platform=${event.platform}")
                if (event.platformExtended != null) append(" platformExtended=${event.platformExtended}")
                if (event.levelName != null) append(" levelName=${event.levelName}")
                if (event.versionName != null) append(" version=${event.versionName}")
                if (event.commitHash != null) append(" commit=${event.commitHash}")
                if (event.turnNumber != null) append(" turn=${event.turnNumber}")
                if (authUser != null) append(" user=$authUser")
            }
            analyticsLogger.info(message)

            dataSourceRef.get()?.connection?.use { conn ->
                try {
                    conn.prepareStatement(
                        "INSERT INTO events (event_type, platform, platform_long, platform_extended, level_name, version_name, commit_hash, user_name, turn_number) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
                    ).use { stmt ->
                        stmt.setString(1, event.event)
                        stmt.setString(2, event.platform)
                        stmt.setString(3, event.platformLong)
                        stmt.setString(4, event.platformExtended)
                        stmt.setString(5, event.levelName)
                        stmt.setString(6, event.versionName)
                        stmt.setString(7, event.commitHash)
                        stmt.setString(8, authUser)
                        if (event.turnNumber != null) stmt.setInt(9, event.turnNumber) else stmt.setNull(9, java.sql.Types.INTEGER)
                        stmt.executeUpdate()
                    }
                } catch (e: Exception) {
                    analyticsLogger.error("Failed to persist event to database: ${e.message}", e)
                }
            }

            call.respond(HttpStatusCode.OK)
        }

        // ---------------------------------------------------------------------------
        // Savefile crossplay endpoints
        // ---------------------------------------------------------------------------

        /**
         * Upload (create or replace) a savefile for the authenticated user.
         * Requires a valid Bearer token in the Authorization header.
         * Returns 401 if no valid user can be extracted from the token.
         */
        post("/api/savefiles") {
            val userId = extractUserIdFromBearerToken(call.request.header(HttpHeaders.Authorization))
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, "Authentication required")
                return@post
            }

            val request = try {
                call.receive<SavefileUploadRequest>()
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, "Invalid payload: ${e.message}")
                return@post
            }

            val ds = dataSourceRef.get() ?: run {
                call.respond(HttpStatusCode.ServiceUnavailable, "Database not available")
                return@post
            }
            ds.connection.use { conn ->
                try {
                    conn.prepareStatement(
                        """
                        INSERT INTO savefiles (user_id, save_id, data, platform, platform_long, version_name, commit_hash, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, NOW())
                        ON CONFLICT (user_id, save_id)
                        DO UPDATE SET data = EXCLUDED.data, platform = EXCLUDED.platform, platform_long = EXCLUDED.platform_long, version_name = EXCLUDED.version_name, commit_hash = EXCLUDED.commit_hash, updated_at = NOW()
                        """.trimIndent()
                    ).use { stmt ->
                        stmt.setString(1, userId)
                        stmt.setString(2, request.saveId)
                        stmt.setString(3, request.data)
                        stmt.setString(4, request.platform)
                        stmt.setString(5, request.platformLong)
                        stmt.setString(6, request.versionName)
                        stmt.setString(7, request.commitHash)
                        stmt.executeUpdate()
                    }
                    savefileLogger.info("Savefile uploaded: userId=$userId saveId=${request.saveId}")
                    call.respond(HttpStatusCode.OK)
                } catch (e: Exception) {
                    savefileLogger.error("Failed to store savefile: ${e.message}", e)
                    call.respond(HttpStatusCode.InternalServerError, "Failed to store savefile")
                }
            }
        }

        /**
         * List all savefiles for the authenticated user.
         * Requires a valid Bearer token in the Authorization header.
         * Returns 401 if no valid user can be extracted from the token.
         */
        get("/api/savefiles") {
            val userId = extractUserIdFromBearerToken(call.request.header(HttpHeaders.Authorization))
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, "Authentication required")
                return@get
            }

            val ds = dataSourceRef.get() ?: run {
                call.respond(HttpStatusCode.ServiceUnavailable, "Database not available")
                return@get
            }
            ds.connection.use { conn ->
                try {
                    val savefiles = mutableListOf<SavefileMetadata>()
                    conn.prepareStatement(
                        "SELECT save_id, data, updated_at FROM savefiles WHERE user_id = ? ORDER BY updated_at DESC"
                    ).use { stmt ->
                        stmt.setString(1, userId)
                        stmt.executeQuery().use { rs ->
                            while (rs.next()) {
                                savefiles.add(
                                    SavefileMetadata(
                                        saveId = rs.getString("save_id"),
                                        data = rs.getString("data"),
                                        updatedAt = rs.getTimestamp("updated_at").toInstant().toString()
                                    )
                                )
                            }
                        }
                    }
                    call.respond(savefiles)
                } catch (e: Exception) {
                    savefileLogger.error("Failed to retrieve savefiles: ${e.message}", e)
                    call.respond(HttpStatusCode.InternalServerError, "Failed to retrieve savefiles")
                }
            }
        }

        // ---------------------------------------------------------------------------
        // User data endpoints (abilities, level progress, local username)
        // ---------------------------------------------------------------------------

        /**
         * Upload (create or replace) general user data for the authenticated user.
         * Stores abilities, level progress, and local username separately from save files.
         * Requires a valid Bearer token in the Authorization header.
         * Returns 401 if no valid user can be extracted from the token.
         */
        post("/api/userdata") {
            val userId = extractUserIdFromBearerToken(call.request.header(HttpHeaders.Authorization))
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, "Authentication required")
                return@post
            }

            val request = try {
                call.receive<UserDataUploadRequest>()
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, "Invalid payload: ${e.message}")
                return@post
            }

            val ds = dataSourceRef.get() ?: run {
                call.respond(HttpStatusCode.ServiceUnavailable, "Database not available")
                return@post
            }
            ds.connection.use { conn ->
                try {
                    conn.prepareStatement(
                        """
                        INSERT INTO userdata (user_id, data, platform, platform_long, version_name, commit_hash, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, NOW())
                        ON CONFLICT (user_id)
                        DO UPDATE SET data = EXCLUDED.data, platform = EXCLUDED.platform, platform_long = EXCLUDED.platform_long, version_name = EXCLUDED.version_name, commit_hash = EXCLUDED.commit_hash, updated_at = NOW()
                        """.trimIndent()
                    ).use { stmt ->
                        stmt.setString(1, userId)
                        stmt.setString(2, request.data)
                        stmt.setString(3, request.platform)
                        stmt.setString(4, request.platformLong)
                        stmt.setString(5, request.versionName)
                        stmt.setString(6, request.commitHash)
                        stmt.executeUpdate()
                    }
                    userDataLogger.info("User data uploaded: userId=$userId")
                    call.respond(HttpStatusCode.OK)
                } catch (e: Exception) {
                    userDataLogger.error("Failed to store user data: ${e.message}", e)
                    call.respond(HttpStatusCode.InternalServerError, "Failed to store user data")
                }
            }
        }

        /**
         * Fetch the general user data for the authenticated user.
         * Returns 401 if unauthenticated, 404 if no data has been uploaded yet.
         */
        get("/api/userdata") {
            val userId = extractUserIdFromBearerToken(call.request.header(HttpHeaders.Authorization))
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, "Authentication required")
                return@get
            }

            val ds = dataSourceRef.get() ?: run {
                call.respond(HttpStatusCode.ServiceUnavailable, "Database not available")
                return@get
            }
            ds.connection.use { conn ->
                try {
                    var result: UserDataResponse? = null
                    conn.prepareStatement(
                        "SELECT data, updated_at FROM userdata WHERE user_id = ?"
                    ).use { stmt ->
                        stmt.setString(1, userId)
                        stmt.executeQuery().use { rs ->
                            if (rs.next()) {
                                result = UserDataResponse(
                                    data = rs.getString("data"),
                                    updatedAt = rs.getTimestamp("updated_at").toInstant().toString()
                                )
                            }
                        }
                    }
                    if (result == null) {
                        call.respond(HttpStatusCode.NotFound, "No user data found")
                    } else {
                        call.respond(result)
                    }
                } catch (e: Exception) {
                    userDataLogger.error("Failed to retrieve user data: ${e.message}", e)
                    call.respond(HttpStatusCode.InternalServerError, "Failed to retrieve user data")
                }
            }
        }

        // ---------------------------------------------------------------------------
        // Player settings endpoints (separate from game save files and user data)
        // ---------------------------------------------------------------------------

        /**
         * Upload (create or replace) player settings for the authenticated user.
         * Stores application settings (dark mode, language, sound, etc.) in a
         * dedicated table independent from userdata and save files.
         * Requires a valid Bearer token in the Authorization header.
         * Returns 401 if no valid user can be extracted from the token.
         */
        post("/api/settings") {
            val userId = extractUserIdFromBearerToken(call.request.header(HttpHeaders.Authorization))
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, "Authentication required")
                return@post
            }

            val request = try {
                call.receive<SettingsUploadRequest>()
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, "Invalid payload: ${e.message}")
                return@post
            }

            val ds = dataSourceRef.get() ?: run {
                call.respond(HttpStatusCode.ServiceUnavailable, "Database not available")
                return@post
            }
            ds.connection.use { conn ->
                try {
                    conn.prepareStatement(
                        """
                        INSERT INTO player_settings (user_id, data, platform, platform_long, version_name, commit_hash, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, NOW())
                        ON CONFLICT (user_id)
                        DO UPDATE SET data = EXCLUDED.data, platform = EXCLUDED.platform, platform_long = EXCLUDED.platform_long, version_name = EXCLUDED.version_name, commit_hash = EXCLUDED.commit_hash, updated_at = NOW()
                        """.trimIndent()
                    ).use { stmt ->
                        stmt.setString(1, userId)
                        stmt.setString(2, request.data)
                        stmt.setString(3, request.platform)
                        stmt.setString(4, request.platformLong)
                        stmt.setString(5, request.versionName)
                        stmt.setString(6, request.commitHash)
                        stmt.executeUpdate()
                    }
                    settingsLogger.info("Player settings uploaded: userId=$userId")
                    call.respond(HttpStatusCode.OK)
                } catch (e: Exception) {
                    settingsLogger.error("Failed to store player settings: ${e.message}", e)
                    call.respond(HttpStatusCode.InternalServerError, "Failed to store player settings")
                }
            }
        }

        /**
         * Fetch the player settings for the authenticated user.
         * Returns 401 if unauthenticated, 404 if no settings have been uploaded yet.
         */
        get("/api/settings") {
            val userId = extractUserIdFromBearerToken(call.request.header(HttpHeaders.Authorization))
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, "Authentication required")
                return@get
            }

            val ds = dataSourceRef.get() ?: run {
                call.respond(HttpStatusCode.ServiceUnavailable, "Database not available")
                return@get
            }
            ds.connection.use { conn ->
                try {
                    var result: SettingsResponse? = null
                    conn.prepareStatement(
                        "SELECT data, updated_at FROM player_settings WHERE user_id = ?"
                    ).use { stmt ->
                        stmt.setString(1, userId)
                        stmt.executeQuery().use { rs ->
                            if (rs.next()) {
                                result = SettingsResponse(
                                    data = rs.getString("data"),
                                    updatedAt = rs.getTimestamp("updated_at").toInstant().toString()
                                )
                            }
                        }
                    }
                    if (result == null) {
                        call.respond(HttpStatusCode.NotFound, "No settings found")
                    } else {
                        call.respond(result)
                    }
                } catch (e: Exception) {
                    settingsLogger.error("Failed to retrieve player settings: ${e.message}", e)
                    call.respond(HttpStatusCode.InternalServerError, "Failed to retrieve player settings")
                }
            }
        }

        // ---------------------------------------------------------------------------
        // Community maps/levels endpoints
        // ---------------------------------------------------------------------------

        /**
         * Upload (create or replace) a community map or level.
         * Requires a valid Bearer token in the Authorization header.
         * A user can only update files they originally uploaded.
         * Returns 401 if unauthenticated, 403 if trying to update another user's file.
         */
        post("/api/community/files") {
            val userId = extractUserIdFromBearerToken(call.request.header(HttpHeaders.Authorization))
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, "Authentication required")
                return@post
            }
            val username = extractUsernameFromBearerToken(call.request.header(HttpHeaders.Authorization)) ?: userId

            val request = try {
                call.receive<CommunityFileUploadRequest>()
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, "Invalid payload: ${e.message}")
                return@post
            }

            if (request.fileType != "MAP" && request.fileType != "LEVEL") {
                call.respond(HttpStatusCode.BadRequest, "fileType must be MAP or LEVEL")
                return@post
            }

            val ds = dataSourceRef.get() ?: run {
                call.respond(HttpStatusCode.ServiceUnavailable, "Database not available")
                return@post
            }
            ds.connection.use { conn ->
                try {
                    // Check if file already exists and if the user is the owner
                    val existingUserId: String? = conn.prepareStatement(
                        "SELECT user_id FROM community_files WHERE file_type = ? AND file_id = ?"
                    ).use { stmt ->
                        stmt.setString(1, request.fileType)
                        stmt.setString(2, request.fileId)
                        stmt.executeQuery().use { rs ->
                            if (rs.next()) rs.getString("user_id") else null
                        }
                    }

                    if (existingUserId != null && existingUserId != userId) {
                        call.respond(HttpStatusCode.Forbidden, "Cannot update another user's community file")
                        return@use
                    }

                    conn.prepareStatement(
                        """
                        INSERT INTO community_files (user_id, username, file_type, file_id, data, description, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, NOW())
                        ON CONFLICT (file_type, file_id)
                        DO UPDATE SET data = EXCLUDED.data, username = EXCLUDED.username, description = EXCLUDED.description, updated_at = NOW()
                        """.trimIndent()
                    ).use { stmt ->
                        stmt.setString(1, userId)
                        stmt.setString(2, username)
                        stmt.setString(3, request.fileType)
                        stmt.setString(4, request.fileId)
                        stmt.setString(5, request.data)
                        stmt.setString(6, request.description)
                        stmt.executeUpdate()
                    }

                    // For MAP uploads, generate and persist a server-side PNG image so that
                    // no user-supplied image data is ever stored or served.
                    if (request.fileType == "MAP") {
                        try {
                            val mapData = parseBackendMapData(request.data)
                            if (mapData != null) {
                                val (pixels, w, h) = BackendMapImageGenerator.generatePixels(mapData)
                                val png = BackendMapImageGenerator.encodeToPng(pixels, w, h)
                                if (png != null) {
                                    conn.prepareStatement(
                                        "UPDATE community_files SET map_image = ? WHERE file_type = 'MAP' AND file_id = ?"
                                    ).use { stmt ->
                                        stmt.setBytes(1, png)
                                        stmt.setString(2, request.fileId)
                                        stmt.executeUpdate()
                                    }
                                    communityLogger.info("Map image generated for MAP ${request.fileId} (${w}x${h})")
                                }
                            }
                        } catch (imgEx: Exception) {
                            // Image generation failure is non-fatal – the map data was already stored
                            communityLogger.warn("Failed to generate map image for ${request.fileId}: ${imgEx.message}")
                        }
                    }

                    communityLogger.info("Community file uploaded: userId=$userId fileType=${request.fileType} fileId=${request.fileId}")
                    call.respond(HttpStatusCode.OK)
                } catch (e: Exception) {
                    communityLogger.error("Failed to store community file: ${e.message}", e)
                    call.respond(HttpStatusCode.InternalServerError, "Failed to store community file")
                }
            }
        }

        /**
         * List all community files. Optionally filter by fileType (MAP or LEVEL).
         * This endpoint is public (no authentication required).
         */
        get("/api/community/files") {
            val fileType = call.request.queryParameters["fileType"]

            val ds = dataSourceRef.get() ?: run {
                call.respond(HttpStatusCode.ServiceUnavailable, "Database not available")
                return@get
            }
            ds.connection.use { conn ->
                try {
                    val files = mutableListOf<CommunityFileMetadata>()
                    val query = if (fileType != null) {
                        "SELECT file_type, file_id, user_id, username, description, updated_at, created_at FROM community_files WHERE file_type = ? ORDER BY updated_at DESC"
                    } else {
                        "SELECT file_type, file_id, user_id, username, description, updated_at, created_at FROM community_files ORDER BY updated_at DESC"
                    }
                    conn.prepareStatement(query).use { stmt ->
                        if (fileType != null) stmt.setString(1, fileType)
                        stmt.executeQuery().use { rs ->
                            while (rs.next()) {
                                files.add(
                                    CommunityFileMetadata(
                                        fileType = rs.getString("file_type"),
                                        fileId = rs.getString("file_id"),
                                        authorUsername = rs.getString("username"),
                                        authorId = rs.getString("user_id"),
                                        updatedAt = rs.getTimestamp("updated_at").toInstant().toString(),
                                        uploadedAt = rs.getTimestamp("created_at").toInstant().toString(),
                                        description = rs.getString("description") ?: ""
                                    )
                                )
                            }
                        }
                    }
                    call.respond(files)
                } catch (e: Exception) {
                    communityLogger.error("Failed to retrieve community files: ${e.message}", e)
                    call.respond(HttpStatusCode.InternalServerError, "Failed to retrieve community files")
                }
            }
        }

        /**
         * Download the data for a specific community file.
         * This endpoint is public (no authentication required).
         */
        get("/api/community/files/{fileType}/{fileId}") {
            val fileType = call.parameters["fileType"]
            val fileId = call.parameters["fileId"]

            if (fileType == null || fileId == null) {
                call.respond(HttpStatusCode.BadRequest, "Missing fileType or fileId")
                return@get
            }
            if (fileType != "MAP" && fileType != "LEVEL") {
                call.respond(HttpStatusCode.BadRequest, "fileType must be MAP or LEVEL")
                return@get
            }

            val ds = dataSourceRef.get() ?: run {
                call.respond(HttpStatusCode.ServiceUnavailable, "Database not available")
                return@get
            }
            ds.connection.use { conn ->
                try {
                    var result: CommunityFileData? = null
                    conn.prepareStatement(
                        "SELECT file_type, file_id, user_id, username, data, description, updated_at, created_at FROM community_files WHERE file_type = ? AND file_id = ?"
                    ).use { stmt ->
                        stmt.setString(1, fileType)
                        stmt.setString(2, fileId)
                        stmt.executeQuery().use { rs ->
                            if (rs.next()) {
                                result = CommunityFileData(
                                    fileType = rs.getString("file_type"),
                                    fileId = rs.getString("file_id"),
                                    authorUsername = rs.getString("username"),
                                    authorId = rs.getString("user_id"),
                                    data = rs.getString("data"),
                                    updatedAt = rs.getTimestamp("updated_at").toInstant().toString(),
                                    uploadedAt = rs.getTimestamp("created_at").toInstant().toString(),
                                    description = rs.getString("description") ?: ""
                                )
                            }
                        }
                    }
                    if (result == null) {
                        call.respond(HttpStatusCode.NotFound, "Community file not found")
                    } else {
                        call.respond(result)
                    }
                } catch (e: Exception) {
                    communityLogger.error("Failed to retrieve community file: ${e.message}", e)
                    call.respond(HttpStatusCode.InternalServerError, "Failed to retrieve community file")
                }
            }
        }

        /**
         * Delete a community map or level.
         * Requires a valid Bearer token with the `community_admin` role on the
         * `defender-of-egril` Keycloak client.
         * Returns 401 if unauthenticated, 403 if the caller lacks the required role,
         * 400 for an invalid fileType, 404 if the file does not exist, 200 on success.
         */
        delete("/api/community/files/{fileType}/{fileId}") {
            val authHeader = call.request.header(HttpHeaders.Authorization)
            val userId = extractUserIdFromBearerToken(authHeader)
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, "Authentication required")
                return@delete
            }
            if (!hasClientRole(authHeader, "defender-of-egril", "community_admin")) {
                call.respond(HttpStatusCode.Forbidden, "community_admin role required")
                return@delete
            }

            val fileType = call.parameters["fileType"]
            val fileId = call.parameters["fileId"]

            if (fileType == null || fileId == null) {
                call.respond(HttpStatusCode.BadRequest, "Missing fileType or fileId")
                return@delete
            }
            if (fileType != "MAP" && fileType != "LEVEL") {
                call.respond(HttpStatusCode.BadRequest, "fileType must be MAP or LEVEL")
                return@delete
            }

            val ds = dataSourceRef.get() ?: run {
                call.respond(HttpStatusCode.ServiceUnavailable, "Database not available")
                return@delete
            }
            ds.connection.use { conn ->
                try {
                    val deletedRows = conn.prepareStatement(
                        "DELETE FROM community_files WHERE file_type = ? AND file_id = ?"
                    ).use { stmt ->
                        stmt.setString(1, fileType)
                        stmt.setString(2, fileId)
                        stmt.executeUpdate()
                    }
                    if (deletedRows == 0) {
                        call.respond(HttpStatusCode.NotFound, "Community file not found")
                    } else {
                        communityLogger.info("Community file deleted by admin $userId: fileType=$fileType fileId=$fileId")
                        call.respond(HttpStatusCode.OK)
                    }
                } catch (e: Exception) {
                    communityLogger.error("Failed to delete community file: ${e.message}", e)
                    call.respond(HttpStatusCode.InternalServerError, "Failed to delete community file")
                }
            }
        }

        /**
         * Download the server-generated PNG image for a community map.
         * This endpoint is public (no authentication required).
         * The image is generated by the backend when the MAP is uploaded and stored in the
         * map_image column of community_files. No user-supplied image data is ever served.
         */
        get("/api/community/files/MAP/{mapId}/image") {
            val mapId = call.parameters["mapId"]
            if (mapId == null) {
                call.respond(HttpStatusCode.BadRequest, "Missing mapId")
                return@get
            }

            val ds = dataSourceRef.get() ?: run {
                call.respond(HttpStatusCode.ServiceUnavailable, "Database not available")
                return@get
            }
            ds.connection.use { conn ->
                try {
                    var imageBytes: ByteArray? = null
                    conn.prepareStatement(
                        "SELECT map_image FROM community_files WHERE file_type = 'MAP' AND file_id = ?"
                    ).use { stmt ->
                        stmt.setString(1, mapId)
                        stmt.executeQuery().use { rs ->
                            if (rs.next()) imageBytes = rs.getBytes("map_image")
                        }
                    }
                    if (imageBytes == null) {
                        call.respond(HttpStatusCode.NotFound, "Map image not found")
                    } else {
                        call.respondBytes(imageBytes!!, ContentType.Image.PNG)
                    }
                } catch (e: Exception) {
                    communityLogger.error("Failed to retrieve map image for $mapId: ${e.message}", e)
                    call.respond(HttpStatusCode.InternalServerError, "Failed to retrieve map image")
                }
            }
        }
    }
}

/**
 * Extracts the `preferred_username` (or `sub`) from a JWT Bearer token without
 * requiring a full JOSE library. Returns null if the header is absent or malformed.
 * This is for audit-logging only – no signature verification is performed.
 */
private fun extractUsernameFromBearerToken(authHeader: String?): String? {
    if (authHeader == null || !authHeader.startsWith("Bearer ")) return null
    return try {
        val token = authHeader.removePrefix("Bearer ")
        val payload = token.split(".").getOrNull(1) ?: return null
        val padded = payload + "=".repeat((4 - payload.length % 4) % 4)
        val decoded = java.util.Base64.getUrlDecoder().decode(padded).toString(Charsets.UTF_8)
        extractJsonStringValue(decoded, "preferred_username")
            ?: extractJsonStringValue(decoded, "sub")
    } catch (_: Exception) {
        null
    }
}

/**
 * Extracts the stable user ID (`sub` claim) from a JWT Bearer token.
 * Falls back to `preferred_username` if `sub` is not present.
 * Returns null if the header is absent, malformed, or contains no usable identity claim.
 * No signature verification is performed – authentication is delegated to the Keycloak proxy.
 */
private fun extractUserIdFromBearerToken(authHeader: String?): String? {
    if (authHeader == null || !authHeader.startsWith("Bearer ")) return null
    return try {
        val token = authHeader.removePrefix("Bearer ")
        val payload = token.split(".").getOrNull(1) ?: return null
        val padded = payload + "=".repeat((4 - payload.length % 4) % 4)
        val decoded = java.util.Base64.getUrlDecoder().decode(padded).toString(Charsets.UTF_8)
        extractJsonStringValue(decoded, "sub")
            ?: extractJsonStringValue(decoded, "preferred_username")
    } catch (_: Exception) {
        null
    }
}

/**
 * Returns true if the JWT Bearer token contains [role] in
 * `resource_access.[clientId].roles`.
 * No signature verification is performed – authentication is delegated to the Keycloak proxy.
 *
 * Keycloak embeds client roles in the token payload as:
 * ```
 * "resource_access": { "<clientId>": { "roles": ["<role>", ...] } }
 * ```
 * The regex matches that structure. `[^}]*` is safe here because the `roles` array
 * only contains plain strings (no nested JSON objects), so `}` cannot appear before
 * the closing brace of the client object.
 */
internal fun hasClientRole(authHeader: String?, clientId: String, role: String): Boolean {
    if (authHeader == null || !authHeader.startsWith("Bearer ")) return false
    return try {
        val token = authHeader.removePrefix("Bearer ")
        val payload = token.split(".").getOrNull(1) ?: return false
        val padded = payload + "=".repeat((4 - payload.length % 4) % 4)
        val decoded = java.util.Base64.getUrlDecoder().decode(padded).toString(Charsets.UTF_8)
        Regex(""""${Regex.escape(clientId)}"\s*:\s*\{[^}]*"roles"\s*:\s*\[[^\]]*"${Regex.escape(role)}"[^\]]*\]""")
            .containsMatchIn(decoded)
    } catch (_: Exception) {
        // Malformed token – deny access
        false
    }
}

internal fun extractJsonStringValue(json: String, key: String): String? =
    Regex("\"${Regex.escape(key)}\"\\s*:\\s*\"([^\"]+)\"").find(json)?.groupValues?.get(1)
