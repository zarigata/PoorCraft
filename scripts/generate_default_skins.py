import struct
import zlib
from pathlib import Path

PNG_SIGNATURE = b"\x89PNG\r\n\x1a\n"


def _chunk(tag: bytes, data: bytes) -> bytes:
    return (
        struct.pack(">I", len(data))
        + tag
        + data
        + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)
    )


def write_placeholder_skin(path: Path, width: int, height: int, primary_rgba: tuple[int, int, int, int], secondary_rgba: tuple[int, int, int, int] | None = None) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)

    primary_pixel = bytes(primary_rgba)
    secondary_pixel = bytes(secondary_rgba) if secondary_rgba else None

    raw = bytearray()
    for y in range(height):
        raw.append(0)  # filter type 0 (None)
        if secondary_pixel:
            row = bytearray()
            for x in range(width):
                row.extend(primary_pixel if (x + y) % 2 == 0 else secondary_pixel)
            raw.extend(row)
        else:
            raw.extend(primary_pixel * width)

    ihdr = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
    idat = zlib.compress(bytes(raw), level=9)
    png_bytes = PNG_SIGNATURE + _chunk(b"IHDR", ihdr) + _chunk(b"IDAT", idat) + _chunk(b"IEND", b"")

    path.write_bytes(png_bytes)


def main() -> None:
    base = Path("src/main/resources/skins/default")
    write_placeholder_skin(
        base / "steve.png",
        width=64,
        height=64,
        primary_rgba=(125, 173, 255, 255),
        secondary_rgba=(43, 91, 178, 255),
    )
    write_placeholder_skin(
        base / "alex.png",
        width=64,
        height=64,
        primary_rgba=(214, 166, 133, 255),
        secondary_rgba=(170, 110, 66, 255),
    )


if __name__ == "__main__":
    main()
