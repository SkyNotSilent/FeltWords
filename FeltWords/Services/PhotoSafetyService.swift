import UIKit
import Vision
import ImageIO

enum PhotoSafetyService {
    static func containsFace(in image: UIImage) async throws -> Bool {
        guard let cgImage = image.cgImage else { throw AgnesError.invalidImage }
        return try await Task.detached(priority: .userInitiated) {
            let request = VNDetectFaceRectanglesRequest()
            let handler = VNImageRequestHandler(cgImage: cgImage, orientation: image.imageOrientation.cgOrientation)
            try handler.perform([request])
            return !(request.results?.isEmpty ?? true)
        }.value
    }
}

private extension UIImage.Orientation {
    var cgOrientation: CGImagePropertyOrientation {
        switch self {
        case .up: .up
        case .upMirrored: .upMirrored
        case .down: .down
        case .downMirrored: .downMirrored
        case .left: .left
        case .leftMirrored: .leftMirrored
        case .right: .right
        case .rightMirrored: .rightMirrored
        @unknown default: .up
        }
    }
}
