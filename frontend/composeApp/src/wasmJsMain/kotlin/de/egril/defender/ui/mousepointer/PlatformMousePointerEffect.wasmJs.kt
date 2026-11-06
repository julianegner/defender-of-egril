@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package de.egril.defender.ui.mousepointer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import de.egril.defender.audio.createBlob
import de.egril.defender.audio.createObjectURL
import de.egril.defender.audio.createUint8Array
import de.egril.defender.audio.setUint8ArrayValue
import de.egril.defender.ui.settings.AppSettings
import de.egril.defender.ui.settings.MousePointerSource
import defender_of_egril.composeapp.generated.resources.Res

@JsFun(
    """
    (source, direction, scale, brightness, blobUrl, textBlobUrl) => {
        // A native CSS cursor (cursor: url(...)) is not reliable for this app:
        // - Compose for wasmJs renders dialogs/popups (e.g. achievement notifications, the
        //   villains preview) on separate DOM subtrees/canvases, which don't reliably inherit a
        //   cursor style set on document.body/documentElement.
        // - Browsers silently fall back to the system arrow when a custom cursor bitmap would
        //   be clipped by the viewport edge (e.g. near the right/bottom of the window).
        // To sidestep both issues, an absolutely positioned <img> overlay is used instead: it is
        // moved to the mouse position on every mousemove, sits above everything via a very high
        // z-index, and the real system cursor is hidden everywhere via a forced stylesheet rule.
        if (!window.__defenderPointerInit) {
            window.__defenderPointerInit = true;
            const style = document.createElement("style");
            style.id = "__defenderPointerStyle";
            document.head.appendChild(style);
            const img = document.createElement("img");
            img.id = "__defenderPointerImg";
            img.alt = "";
            img.draggable = false;
            img.style.position = "fixed";
            img.style.left = "0px";
            img.style.top = "0px";
            img.style.zIndex = "2147483647";
            img.style.pointerEvents = "none";
            img.style.display = "none";
            img.style.willChange = "transform";
            document.body.appendChild(img);
            window.__defenderPointerHotspot = { x: 0, y: 0 };
            window.__defenderPointerActive = false;
            window.__defenderPointerVariants = null;
            window.__defenderRequestedCursor = "default";
            window.__defenderPointerLastClient = null;
            window.__defenderPointerObservedCanvases = [];

            // Applies whichever variant (normal hand/gauntlet pointer, or the text I-beam
            // pointer) matches the cursor style Compose most recently requested on the
            // underlying canvas, updating the overlay image immediately (not waiting for the
            // next mousemove) so size/color/variant changes are reflected right away.
            const updatePointerVariant = () => {
                const variants = window.__defenderPointerVariants;
                if (!variants) return;
                const wantText = window.__defenderRequestedCursor === "text";
                const variant = (wantText && variants.text) ? variants.text : variants.normal;
                if (!variant) return;
                if (img.src !== variant.src) {
                    img.src = variant.src;
                }
                img.style.width = variant.width + "px";
                img.style.height = variant.height + "px";
                window.__defenderPointerHotspot = variant.hotspot;
                const last = window.__defenderPointerLastClient;
                if (last) {
                    img.style.transform =
                        "translate(" + (last.x - variant.hotspot.x) + "px, " + (last.y - variant.hotspot.y) + "px)";
                }
            };
            window.__defenderUpdatePointerVariant = updatePointerVariant;

            document.addEventListener("mousemove", (event) => {
                window.__defenderPointerLastClient = { x: event.clientX, y: event.clientY };
                const hotspot = window.__defenderPointerHotspot;
                img.style.transform =
                    "translate(" + (event.clientX - hotspot.x) + "px, " + (event.clientY - hotspot.y) + "px)";
                if (window.__defenderPointerActive) {
                    img.style.display = "block";
                }
            }, true);
            // Hide the overlay once the mouse leaves the browser viewport entirely (relatedTarget
            // becomes null/undefined when moving out to the OS desktop or another window), and
            // show it again once the mouse re-enters, so it doesn't stay hanging outside the app.
            document.addEventListener("mouseout", (event) => {
                if (!event.relatedTarget && window.__defenderPointerActive) {
                    img.style.display = "none";
                }
            }, true);
            document.addEventListener("mouseover", (event) => {
                if (window.__defenderPointerActive) {
                    img.style.display = "block";
                }
            }, true);
            window.addEventListener("blur", () => {
                img.style.display = "none";
            });

            // Compose maps PointerIcon.Text (used for markable/selectable text, just like on
            // desktop) to a "text" CSS cursor set directly on the underlying canvas. Compose for
            // wasmJs may render that canvas inside a shadow DOM subtree (e.g. for dialogs/popups
            // such as the achievement notification or the villains preview), so a document-level
            // stylesheet rule cannot see or override it - each canvas has to be found and reacted
            // to individually via a MutationObserver on its "style" attribute.
            const captureAndHide = (canvas) => {
                const value = canvas.style.cursor;
                if (value && value !== "none") {
                    if (window.__defenderRequestedCursor !== value) {
                        window.__defenderRequestedCursor = value;
                        updatePointerVariant();
                    }
                    if (window.__defenderPointerActive) {
                        canvas.style.setProperty("cursor", "none", "important");
                    }
                }
            };
            const observeCanvas = (canvas) => {
                if (window.__defenderPointerObservedCanvases.indexOf(canvas) !== -1) return;
                window.__defenderPointerObservedCanvases.push(canvas);
                new MutationObserver(() => captureAndHide(canvas)).observe(canvas, {
                    attributes: true,
                    attributeFilter: ["style"],
                });
                captureAndHide(canvas);
            };
            const scanForCanvases = (root) => {
                const elements = root.querySelectorAll ? root.querySelectorAll("*") : [];
                for (const element of elements) {
                    if (element.tagName === "CANVAS") {
                        observeCanvas(element);
                    }
                    if (element.shadowRoot) {
                        scanForCanvases(element.shadowRoot);
                    }
                }
            };
            window.setInterval(() => {
                scanForCanvases(document);
                if (window.__defenderPointerActive) {
                    for (const canvas of window.__defenderPointerObservedCanvases) {
                        captureAndHide(canvas);
                    }
                }
            }, 300);
        }

        const style = document.getElementById("__defenderPointerStyle");
        const img = document.getElementById("__defenderPointerImg");

        const resetCursor = () => {
            window.__defenderPointerActive = false;
            window.__defenderPointerVariants = null;
            window.__defenderRequestedCursor = "default";
            style.textContent = "";
            img.style.display = "none";
        };
        if (source === "SYSTEM" || !blobUrl) {
            resetCursor();
            return;
        }

        const revision = (window.__defenderPointerRevision || 0) + 1;
        window.__defenderPointerRevision = revision;

        const applyBrightness = (context, width, height) => {
            if (source === "GAME" && brightness !== 0) {
                const imageData = context.getImageData(0, 0, width, height);
                const factor = 1 + brightness;
                for (let index = 0; index < imageData.data.length; index += 4) {
                    const red = imageData.data[index];
                    const green = imageData.data[index + 1];
                    const blue = imageData.data[index + 2];
                    if (!(red < 120 || green < 70 || blue < 50 || red < green || green < blue)) {
                        imageData.data[index] = Math.min(255, Math.round(red * factor));
                        imageData.data[index + 1] = Math.min(255, Math.round(green * factor));
                        imageData.data[index + 2] = Math.min(255, Math.round(blue * factor));
                    }
                }
                context.putImageData(imageData, 0, 0);
            } else if (source === "GAUNTLET") {
                const imageData = context.getImageData(0, 0, width, height);
                const factor = 1.3;
                for (let index = 0; index < imageData.data.length; index += 4) {
                    imageData.data[index] = Math.min(255, Math.round(imageData.data[index] * factor));
                    imageData.data[index + 1] = Math.min(255, Math.round(imageData.data[index + 1] * factor));
                    imageData.data[index + 2] = Math.min(255, Math.round(imageData.data[index + 2] * factor));
                }
                context.putImageData(imageData, 0, 0);
            }
        };

        const buildVariant = (image, baseWidth, hotspotSourceX, hotspotSourceY) =>
            new Promise((resolve) => {
                const cursorWidth = Math.min(baseWidth, Math.max(16, Math.round(baseWidth * scale)));
                const cursorHeight = Math.max(16, Math.round(image.height * cursorWidth / image.width));
                const cursorCanvas = document.createElement("canvas");
                cursorCanvas.width = cursorWidth;
                cursorCanvas.height = cursorHeight;
                const context = cursorCanvas.getContext("2d");
                context.drawImage(image, 0, 0, cursorWidth, cursorHeight);
                applyBrightness(context, cursorWidth, cursorHeight);

                const hotspotX = Math.round(hotspotSourceX * cursorWidth / image.width);
                const hotspotY = Math.round(hotspotSourceY * cursorHeight / image.height);
                resolve({
                    src: cursorCanvas.toDataURL("image/png"),
                    width: cursorWidth,
                    height: cursorHeight,
                    hotspot: { x: hotspotX, y: hotspotY },
                });
            });

        const loadImage = (url) =>
            new Promise((resolve, reject) => {
                if (!url) {
                    resolve(null);
                    return;
                }
                const image = new Image();
                image.onload = () => resolve(image);
                image.onerror = () => resolve(null);
                image.src = url;
            });

        Promise.all([loadImage(blobUrl), loadImage(textBlobUrl)]).then(([normalImage, textImage]) => {
            if (window.__defenderPointerRevision !== revision) return;
            if (!normalImage) {
                resetCursor();
                if (blobUrl) URL.revokeObjectURL(blobUrl);
                if (textBlobUrl) URL.revokeObjectURL(textBlobUrl);
                return;
            }

            const sourceHotspotX =
                source === "GAUNTLET"
                    ? (direction === "RIGHT" ? 307 : 772)
                    : (direction === "RIGHT" ? 306 : 771);

            Promise.all([
                buildVariant(normalImage, 160, sourceHotspotX, 140),
                textImage ? buildVariant(textImage, 64, 540, 294) : Promise.resolve(null),
            ]).then(([normalVariant, textVariant]) => {
                if (window.__defenderPointerRevision !== revision) return;
                window.__defenderPointerVariants = { normal: normalVariant, text: textVariant };
                window.__defenderPointerActive = true;
                img.style.display = "block";
                window.__defenderUpdatePointerVariant();
                // Hide the real system cursor everywhere (including inside dialogs/popups) so
                // only the overlay image is visible.
                style.textContent = "*, *:hover { cursor: none !important; }";
                if (blobUrl) URL.revokeObjectURL(blobUrl);
                if (textBlobUrl) URL.revokeObjectURL(textBlobUrl);
            });
        });
    }
    """,
)
private external fun applyWebMousePointer(
    source: String,
    direction: String,
    scale: Float,
    brightness: Float,
    blobUrl: String?,
    textBlobUrl: String?,
)

private fun resourceFileNameFor(
    source: MousePointerSource,
    direction: String,
): String? =
    when (source) {
        MousePointerSource.SYSTEM -> null
        MousePointerSource.GAUNTLET ->
            if (direction == "RIGHT") "mouse_pointer_gauntlet_right.png" else "mouse_pointer_gauntlet_left.png"
        MousePointerSource.GAME ->
            if (direction == "RIGHT") "mouse_pointer_hand_right.png" else "mouse_pointer_hand_left.png"
    }

private suspend fun createBlobUrlForResource(fileName: String): String? =
    try {
        // Loading via Res.readBytes() (rather than guessing the static asset URL) is the
        // pattern already proven to work for wasmJs resource loading elsewhere in this
        // codebase (see FileSoundManager.wasmJs.kt).
        val bytes = Res.readBytes("drawable/$fileName")
        val uint8Array = createUint8Array(bytes.size)
        bytes.forEachIndexed { index, byte -> setUint8ArrayValue(uint8Array, index, byte) }
        val blob = createBlob(uint8Array, "image/png")
        createObjectURL(blob)
    } catch (e: Exception) {
        println("Could not load mouse pointer image: $fileName - ${e.message}")
        null
    }

@Composable
actual fun PlatformMousePointerEffect() {
    val source by AppSettings.mousePointerSource
    val direction by AppSettings.mousePointerDirection
    val size by AppSettings.mousePointerSize
    val brightness by AppSettings.mousePointerSkinBrightness

    LaunchedEffect(source, direction, size, brightness) {
        val directionName = direction.name
        val fileName = resourceFileNameFor(source, directionName)
        val blobUrl = fileName?.let { createBlobUrlForResource(it) }
        val textBlobUrl =
            if (source != MousePointerSource.SYSTEM) createBlobUrlForResource("mouse_pointer_text.png") else null
        applyWebMousePointer(
            source = source.name,
            direction = directionName,
            scale = size.scale,
            brightness = if (source == MousePointerSource.GAME) brightness else 0f,
            blobUrl = blobUrl,
            textBlobUrl = textBlobUrl,
        )
    }
}
