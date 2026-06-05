import PhotosUI
import SwiftUI

struct CameraScreen: View {
    @EnvironmentObject private var model: AppModel
    @StateObject private var camera = CameraService()
    @State private var selectedPhoto: PhotosPickerItem?
    @State private var isProcessing = false
    @State private var errorMessage: String?
    @State private var resultRoute: RecognitionResult?

    var body: some View {
        ZStack {
            CameraPreview(session: camera.session).ignoresSafeArea()
            Color.black.opacity(0.12).ignoresSafeArea()

            VStack {
                Spacer()
                FocusCorners()
                    .frame(width: 270, height: 270)
                Text("把物品放进小框里")
                    .font(.system(.headline, design: .rounded, weight: .bold))
                    .foregroundStyle(.white)
                    .padding(.horizontal, 20)
                    .padding(.vertical, 10)
                    .background(.black.opacity(0.25), in: Capsule())
                    .padding(.top, 34)
                Spacer()

                HStack {
                    Button { model.selectedTab = .home } label: {
                        Image(systemName: "xmark").cameraControl()
                    }
                    Spacer()
                    Button { camera.capture() } label: { ShutterButton() }
                        .disabled(isProcessing)
                    Spacer()
                    PhotosPicker(selection: $selectedPhoto, matching: .images) {
                        Image(systemName: "photo").cameraControl()
                    }
                }
                .padding(.horizontal, 40)
                .padding(.bottom, 24)
            }

            if isProcessing {
                Color.black.opacity(0.35).ignoresSafeArea()
                VStack(spacing: 14) {
                    ProgressView().tint(FeltTheme.orange).scaleEffect(1.4)
                    Text("毛毛正在找单词…").font(.headline)
                }
                .padding(28)
                .background(.white, in: RoundedRectangle(cornerRadius: 24))
            }
        }
        .toolbar(.hidden, for: .navigationBar)
        .onAppear { camera.start() }
        .onDisappear { camera.stop() }
        .onChange(of: camera.capturedImage) { _, image in
            guard let image else { return }
            recognize(image)
        }
        .onChange(of: selectedPhoto) { _, item in
            guard let item else { return }
            Task {
                guard let data = try? await item.loadTransferable(type: Data.self),
                      let image = UIImage(data: data) else { return }
                recognize(image)
            }
        }
        .navigationDestination(item: $resultRoute) { result in
            WordResultView(result: result, originalImage: model.capturedImage)
        }
        .alert("再试一次", isPresented: .constant(errorMessage != nil)) {
            Button("好的") { errorMessage = nil }
        } message: {
            Text(errorMessage ?? "")
        }
    }

    private func recognize(_ image: UIImage) {
        model.capturedImage = image
        isProcessing = true
        Task {
            do {
                let result = try await model.agnes.recognize(image: image)
                model.latestResult = result
                resultRoute = result
            } catch {
                errorMessage = error.localizedDescription
            }
            isProcessing = false
        }
    }
}

private struct ShutterButton: View {
    var body: some View {
        ZStack {
            Circle().fill(.white).frame(width: 84, height: 84)
            Circle().stroke(FeltTheme.orange, lineWidth: 6).frame(width: 66, height: 66)
        }
        .shadow(color: .black.opacity(0.18), radius: 8, y: 4)
    }
}

private extension View {
    func cameraControl() -> some View {
        self
            .font(.title2.bold())
            .foregroundStyle(.white)
            .frame(width: 54, height: 54)
            .background(.white.opacity(0.24), in: Circle())
    }
}

