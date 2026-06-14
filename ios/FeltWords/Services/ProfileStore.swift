import UIKit

/// 头像本地持久化：固定文件名存到 Application Support/Profile/avatar.jpg。
enum ProfileStore {
    private static func directory() throws -> URL {
        let directory = try FileManager.default.url(
            for: .applicationSupportDirectory,
            in: .userDomainMask,
            appropriateFor: nil,
            create: true
        ).appending(path: "Profile", directoryHint: .isDirectory)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        return directory
    }

    private static func avatarURL() throws -> URL {
        try directory().appending(path: "avatar.jpg")
    }

    static func loadAvatar() -> UIImage? {
        guard let url = try? avatarURL() else { return nil }
        return UIImage(contentsOfFile: url.path)
    }

    static func saveAvatar(_ image: UIImage) {
        guard let url = try? avatarURL(),
              let data = image.jpegData(compressionQuality: 0.85) else { return }
        try? data.write(to: url, options: .atomic)
    }

    static func deleteAvatar() {
        guard let url = try? avatarURL() else { return }
        try? FileManager.default.removeItem(at: url)
    }
}
