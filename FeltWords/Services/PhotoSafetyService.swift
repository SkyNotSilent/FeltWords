import UIKit
import Vision

enum PhotoSafetyService {
    static func containsFace(in image: UIImage) async throws -> Bool {
        guard let cgImage = image.cgImage else { throw AgnesError.invalidImage }
        return try await Task.detached(priority: .userInitiated) {
            let request = VNDetectFaceRectanglesRequest()
            let handler = VNImageRequestHandler(cgImage: cgImage, orientation: .up)
            try handler.perform([request])
            return !(request.results?.isEmpty ?? true)
        }.value
    }
}

