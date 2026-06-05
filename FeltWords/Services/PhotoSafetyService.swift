import UIKit
import Vision
import ImageIO

enum PhotoSafetyService {
    /// 检测照片中是否包含人脸，用于阻止把儿童面部上传到第三方接口。
    /// 模拟器无法创建 Vision 推理上下文（依赖神经引擎），跳过检测以便开发联调；真机保留严格检测。
    static func containsFace(in image: UIImage) async throws -> Bool {
        #if targetEnvironment(simulator)
        return false
        #else
        guard let cgImage = image.cgImage else { throw AgnesError.invalidImage }
        return try await Task.detached(priority: .userInitiated) {
            let request = VNDetectFaceRectanglesRequest()
            let handler = VNImageRequestHandler(cgImage: cgImage, orientation: image.imageOrientation.cgOrientation)
            try handler.perform([request])
            return !(request.results?.isEmpty ?? true)
        }.value
        #endif
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
