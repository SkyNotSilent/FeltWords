import AVFoundation
import UIKit

final class CameraService: NSObject, ObservableObject, @unchecked Sendable {
    let session = AVCaptureSession()
    @Published var permissionDenied = false
    @Published var capturedImage: UIImage?

    private let output = AVCapturePhotoOutput()
    private let queue = DispatchQueue(label: "feltwords.camera")

    override init() {
        super.init()
        Task { await configure() }
    }

    func start() {
        queue.async { [session] in
            if !session.isRunning { session.startRunning() }
        }
    }

    func stop() {
        queue.async { [session] in
            if session.isRunning { session.stopRunning() }
        }
    }

    func capture() {
        output.capturePhoto(with: AVCapturePhotoSettings(), delegate: self)
    }

    @MainActor
    private func configure() async {
        let status = AVCaptureDevice.authorizationStatus(for: .video)
        let granted: Bool
        if status == .authorized {
            granted = true
        } else if status == .notDetermined {
            granted = await AVCaptureDevice.requestAccess(for: .video)
        } else {
            granted = false
        }
        guard granted else {
            permissionDenied = true
            return
        }
        queue.async { [weak self] in
            guard let self else { return }
            session.beginConfiguration()
            session.sessionPreset = .photo
            guard let device = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .back),
                  let input = try? AVCaptureDeviceInput(device: device),
                  session.canAddInput(input),
                  session.canAddOutput(output) else {
                session.commitConfiguration()
                return
            }
            session.addInput(input)
            session.addOutput(output)
            session.commitConfiguration()
            session.startRunning()
        }
    }
}

extension CameraService: AVCapturePhotoCaptureDelegate {
    func photoOutput(_ output: AVCapturePhotoOutput, didFinishProcessingPhoto photo: AVCapturePhoto, error: Error?) {
        guard error == nil, let data = photo.fileDataRepresentation(), let image = UIImage(data: data) else { return }
        DispatchQueue.main.async { self.capturedImage = image }
    }
}
