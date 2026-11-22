// swift-tools-version: 6.0
import PackageDescription

let package = Package(
    name: "HtetLingoMac",
    platforms: [
        .macOS(.v14)
    ],
    products: [
        .executable(name: "HtetLingoMac", targets: ["HtetLingoMac"])
    ],
    targets: [
        .executableTarget(
            name: "HtetLingoMac",
            linkerSettings: [
                .linkedLibrary("sqlite3")
            ]
        )
    ]
)
