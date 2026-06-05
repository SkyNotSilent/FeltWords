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

            if camera.permissionDenied || camera.cameraUnavailable {
                Color.black.opacity(0.55).ignoresSafeArea()
                VStack(spacing: 14) {
                    Image(systemName: "camera.fill").font(.largeTitle)
                    Text("让毛毛看看你发现的物品").font(.headline)
                    Text(camera.permissionDenied
                         ? "请在系统设置中允许相机权限，或者从相册选择一张照片。"
                         : "这台设备暂时用不了相机，先从相册选择一张照片试试吧。")
                        .font(.subheadline)
                        .multilineTextAlignment(.center)
                    if camera.permissionDenied {
                        Button("打开设置") {
                            guard let url = URL(string: UIApplication.openSettingsURLString) else { return }
                            UIApplication.shared.open(url)
                        }
                        .buttonStyle(FeltButtonStyle(color: FeltTheme.yellow))
                    }
                    PhotosPicker(selection: $selectedPhoto, matching: .images) {
                        Label("从相册选择", systemImage: "photo.fill")
                            .frame(maxWidth: .infinity, minHeight: 52)
                            .background(FeltTheme.cream, in: RoundedRectangle(cornerRadius: 20))
                    }
                }
                .foregroundStyle(FeltTheme.ink)
                .padding(28)
                .background(.white, in: RoundedRectangle(cornerRadius: 24))
                .padding(28)
            }
        }
        .toolbar(.hidden, for: .navigationBar)
        .toolbar(.hidden, for: .tabBar)
        .onAppear { camera.start() }
        .onDisappear { camera.stop() }
        .onChange(of: camera.capturedImage) { _, image in
            guard let image else { return }
            recognize(image)
        }
        .onChange(of: selectedPhoto) { _, item in
            guard let item else { return }
            Task {
                do {
                    guard let data = try await item.loadTransferable(type: Data.self),
                          let image = UIImage(data: data) else {
                        errorMessage = "这张照片读不出来，换一张试试吧。"
                        return
                    }
                    recognize(image)
                } catch {
                    errorMessage = "照片加载失败了，请再选一次。"
                }
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
                if try await PhotoSafetyService.containsFace(in: image) {
                    throw AgnesError.server("照片里好像有人，请只拍物品再试一次。")
                }
                let result = try await model.agnes.recognize(image: image)
                model.latestResult = result
                resultRoute = result
            } catch let error as AgnesError {
                errorMessage = error.localizedDescription
            } catch {
                // 系统底层错误（如 Vision 推理失败）不直接把英文抛给孩子，统一用友好中文兜底。
                errorMessage = "毛毛刚才没看清楚，请再试一次吧。"
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
