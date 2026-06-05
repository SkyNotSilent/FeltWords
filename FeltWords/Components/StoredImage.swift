import SwiftUI

struct StoredImage: View {
    let url: URL

    var body: some View {
        if url.isFileURL, let image = UIImage(contentsOfFile: url.path) {
            Image(uiImage: image)
                .resizable()
        } else {
            AsyncImage(url: url) { image in
                image.resizable()
            } placeholder: {
                ProgressView()
            }
        }
    }
}

