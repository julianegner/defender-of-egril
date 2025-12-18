# ZIP Implementation for Web/WASM

## Overview
This document describes the ZIP file format implementation for the web/WASM platform in Defender of Egril.

## Background
The game supports downloading all save games as a single file. On Desktop and Android platforms, this uses `java.util.zip` to create proper ZIP archives. However, the web/WASM platform does not have access to Java libraries, so a custom implementation was required.

## Implementation

### Files
- `composeApp/src/wasmJsMain/kotlin/de/egril/defender/save/ZipWriter.kt` - Custom ZIP writer
- `composeApp/src/wasmJsMain/kotlin/de/egril/defender/save/FileExportImport.wasmJs.kt` - Uses ZipWriter for exports

### ZIP Format
The implementation follows the ZIP file format specification (PKZIP):

1. **Local File Header** (per file)
   - Signature: 0x04034b50
   - Version needed: 2.0
   - Flags: UTF-8 filename encoding (bit 11)
   - Compression: STORE (0 = no compression)
   - CRC-32: IEEE 802.3 polynomial
   - Sizes: compressed and uncompressed (same for STORE)
   - Filename

2. **Central Directory** (per file)
   - Signature: 0x02014b50
   - Metadata matching local header
   - Offset to local header

3. **End of Central Directory Record**
   - Signature: 0x06054b50
   - Number of entries
   - Central directory size and offset

### Features
- ✅ Uncompressed storage (STORE method)
- ✅ UTF-8 filename support
- ✅ Proper CRC-32 checksums
- ✅ Little-endian byte order
- ✅ Compatible with all standard ZIP utilities
- ✅ No external dependencies
- ✅ Works in WASM environment

### Limitations
- Only STORE method (no compression)
- No ZIP64 support (sufficient for save files)
- No encryption support
- Fixed timestamps (1980-01-01 00:00:02)

## Usage

```kotlin
val zipWriter = ZipWriter()
zipWriter.addFile("save1.json", """{"level": 1}""")
zipWriter.addFile("save2.json", """{"level": 2}""")
val zipBytes = zipWriter.build()
```

The ZIP bytes can then be converted to a Blob and downloaded via browser APIs.

## Testing
The implementation has been validated to:
- Produce correct ZIP file signatures
- Calculate accurate CRC-32 checksums
- Create archives that extract correctly with standard tools
- Preserve file contents exactly

## References
- [ZIP File Format Specification](https://pkware.cachefly.net/webdocs/casestudies/APPNOTE.TXT)
- [CRC-32 IEEE 802.3 Polynomial](https://en.wikipedia.org/wiki/Cyclic_redundancy_check)
