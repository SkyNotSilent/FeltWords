import Foundation

enum GeneratedImageStore {
    static func persist(remoteURL: URL) async throws -> URL {
        let (data, response) = try await URLSession.shared.data(from: remoteURL)
        guard let http = response as? HTTPURLResponse, 200..<300 ~= http.statusCode else {
            throw AgnesError.invalidResponse
        }

        let directory = try FileManager.default.url(
            for: .applicationSupportDirectory,
            in: .userDomainMask,
            appropriateFor: nil,
            create: true
        ).appending(path: "GeneratedImages", directoryHint: .isDirectory)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        let fileURL = directory.appending(path: "\(UUID().uuidString).jpg")
        try data.write(to: fileURL, options: .atomic)
        return fileURL
    }
}

