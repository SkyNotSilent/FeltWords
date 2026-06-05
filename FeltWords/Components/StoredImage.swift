import SwiftUI

struct StoredImage: View {
    let url: URL

    var body: some View {
        if let image = Self.loadLocal(url) {
            Image(uiImage: image).resizable()
        } else if url.isFileURL {
            // 本地文件确实找不到（容器路径变化且无法按文件名重定位）：给友好占位，不无限转圈。
            ImagePlaceholder()
        } else {
            AsyncImage(url: url) { phase in
                switch phase {
                case .success(let image): image.resizable()
                case .failure: ImagePlaceholder()
                case .empty: ProgressView()
                @unknown default: ImagePlaceholder()
                }
            }
        }
    }

    /// 本地文件优先按原路径读；失败则按文件名在当前沙盒目录重新定位，
    /// 解决 App 重装后容器 UUID 变化导致已存绝对路径失效（但文件仍在）的问题。
    private static func loadLocal(_ url: URL) -> UIImage? {
        guard url.isFileURL else { return nil }
        if let image = UIImage(contentsOfFile: url.path) { return image }
        guard let resolved = GeneratedImageStore.resolve(filename: url.lastPathComponent) else { return nil }
        return UIImage(contentsOfFile: resolved.path)
    }
}

private struct ImagePlaceholder: View {
    var body: some View {
        ZStack {
            FeltTheme.cream
            Image(systemName: "photo")
                .font(.largeTitle)
                .foregroundStyle(FeltTheme.secondary)
        }
    }
}
