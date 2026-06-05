import Foundation
import UIKit

enum GeneratedImageStore {
    /// 当前沙盒下的图片目录。注意：iOS 沙盒容器在 App 重装后路径会变，
    /// 所以只能持久化文件名，渲染时按文件名重新解析到当前目录（见 resolve(filename:)）。
    static func directory() throws -> URL {
        let directory = try FileManager.default.url(
            for: .applicationSupportDirectory,
            in: .userDomainMask,
            appropriateFor: nil,
            create: true
        ).appending(path: "GeneratedImages", directoryHint: .isDirectory)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        return directory
    }

    /// 按文件名在当前沙盒目录定位文件，解决重装后旧绝对路径失效但文件仍在的问题。
    static func resolve(filename: String) -> URL? {
        guard let directory = try? directory() else { return nil }
        let candidate = directory.appending(path: filename)
        return FileManager.default.fileExists(atPath: candidate.path) ? candidate : nil
    }

    static func persist(remoteURL: URL) async throws -> URL {
        let (data, response) = try await URLSession.shared.data(from: remoteURL)
        guard let http = response as? HTTPURLResponse, 200..<300 ~= http.statusCode else {
            throw AgnesError.invalidResponse
        }

        let fileURL = try directory().appending(path: "\(UUID().uuidString).jpg")
        try data.write(to: fileURL, options: .atomic)
        return fileURL
    }

    static func persist(image: UIImage) throws -> URL {
        guard let data = image.jpegData(compressionQuality: 0.88) else {
            throw CocoaError(.fileWriteUnknown)
        }
        let fileURL = try directory().appending(path: "\(UUID().uuidString).jpg")
        try data.write(to: fileURL, options: .atomic)
        return fileURL
    }
}
