import AVFoundation
import SwiftUI

struct CameraPreview: UIViewRepresentable {
    let session: AVCaptureSession

    func makeUIView(context: Context) -> PreviewView {
        let view = PreviewView()
        view.previewLayer.session = session
        view.previewLayer.videoGravity = .resizeAspectFill
        return view
    }

    func updateUIView(_ uiView: PreviewView, context: Context) {}
}

final class PreviewView: UIView {
    override class var layerClass: AnyClass { AVCaptureVideoPreviewLayer.self }
    var previewLayer: AVCaptureVideoPreviewLayer { layer as! AVCaptureVideoPreviewLayer }
}

struct FocusCorners: View {
    var body: some View {
        GeometryReader { proxy in
            let length = proxy.size.width * 0.18
            Path { path in
                path.move(to: .init(x: 0, y: length)); path.addLine(to: .zero); path.addLine(to: .init(x: length, y: 0))
                path.move(to: .init(x: proxy.size.width - length, y: 0)); path.addLine(to: .init(x: proxy.size.width, y: 0)); path.addLine(to: .init(x: proxy.size.width, y: length))
                path.move(to: .init(x: proxy.size.width, y: proxy.size.height - length)); path.addLine(to: .init(x: proxy.size.width, y: proxy.size.height)); path.addLine(to: .init(x: proxy.size.width - length, y: proxy.size.height))
                path.move(to: .init(x: length, y: proxy.size.height)); path.addLine(to: .init(x: 0, y: proxy.size.height)); path.addLine(to: .init(x: 0, y: proxy.size.height - length))
            }
            .stroke(.white.opacity(0.9), style: StrokeStyle(lineWidth: 5, lineCap: .round, lineJoin: .round))
        }
        .aspectRatio(1, contentMode: .fit)
    }
}

