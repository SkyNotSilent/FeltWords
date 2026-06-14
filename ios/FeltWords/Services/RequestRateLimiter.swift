import Foundation

actor RequestRateLimiter {
    private let limit: Int
    private let interval: TimeInterval
    private var requestDates: [Date] = []

    init(limit: Int = 20, interval: TimeInterval = 60) {
        self.limit = limit
        self.interval = interval
    }

    func waitForSlot() async throws {
        while true {
            let now = Date()
            requestDates.removeAll { now.timeIntervalSince($0) >= interval }
            if requestDates.count < limit {
                requestDates.append(now)
                return
            }
            guard let oldest = requestDates.first else { continue }
            let delay = max(0.5, interval - now.timeIntervalSince(oldest) + 0.2)
            try await Task.sleep(for: .seconds(delay))
        }
    }
}

