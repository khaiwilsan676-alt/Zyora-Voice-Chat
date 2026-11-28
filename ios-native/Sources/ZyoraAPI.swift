import Foundation

/// Shared Zyora backend transport. Both mobile clients use the same production API;
/// wallet, room and game state remains server-authoritative.
enum ZyoraAPIError: LocalizedError {
    case invalidURL
    case invalidResponse
    case httpStatus(Int, String)

    var errorDescription: String? {
        switch self {
        case .invalidURL:
            return "The Zyora server URL is invalid."
        case .invalidResponse:
            return "The Zyora server returned an invalid response."
        case let .httpStatus(code, message):
            return message.isEmpty ? "Zyora request failed (HTTP \(code))." : message
        }
    }
}

final class ZyoraAPI {
    static let shared = ZyoraAPI()
    static let baseURL = URL(string: "https://zyora-server.onrender.com")!

    private let session: URLSession

    private init(session: URLSession = .shared) {
        self.session = session
    }

    func request(
        path: String,
        method: String = "GET",
        query: [URLQueryItem] = [],
        body: Data? = nil,
        idToken: String? = nil
    ) async throws -> Data {
        guard var components = URLComponents(url: Self.baseURL.appendingPathComponent(path), resolvingAgainstBaseURL: false) else {
            throw ZyoraAPIError.invalidURL
        }
        if !query.isEmpty { components.queryItems = query }
        guard let url = components.url else { throw ZyoraAPIError.invalidURL }

        var request = URLRequest(url: url)
        request.httpMethod = method
        request.httpBody = body
        request.timeoutInterval = 60
        request.cachePolicy = .reloadIgnoringLocalCacheData
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.setValue("no-cache", forHTTPHeaderField: "Cache-Control")
        if body != nil { request.setValue("application/json", forHTTPHeaderField: "Content-Type") }
        if let idToken, !idToken.isEmpty {
            request.setValue("Bearer \(idToken)", forHTTPHeaderField: "Authorization")
        }

        let (data, response) = try await session.data(for: request)
        guard let http = response as? HTTPURLResponse else { throw ZyoraAPIError.invalidResponse }
        guard (200...299).contains(http.statusCode) else {
            let message = (try? JSONSerialization.jsonObject(with: data) as? [String: Any])?["error"] as? String ?? ""
            throw ZyoraAPIError.httpStatus(http.statusCode, message)
        }
        return data
    }

    func fetchRooms(sinceUpdatedAt: Int64 = 0, idToken: String? = nil) async throws -> ZyoraRoomsResponse {
        let data = try await request(
            path: "api/rooms",
            query: [URLQueryItem(name: "sinceUpdatedAt", value: String(sinceUpdatedAt))],
            idToken: idToken
        )
        return try JSONDecoder().decode(ZyoraRoomsResponse.self, from: data)
    }
}

struct ZyoraRoomsResponse: Decodable {
    let rooms: [ZyoraRoom]
    let serverTime: Int64?
}

struct ZyoraRoom: Decodable, Identifiable {
    let roomId: String?
    let accountId: String?
    let legacyId: String?
    let roomName: String?
    let name: String?
    let dp: String?
    let image: String?
    let roomDp: String?
    let roomImage: String?
    let announcement: String?
    let message: String?
    let countryName: String?
    let country: String?
    let countryCode: String?
    let isLocked: Bool?

    enum CodingKeys: String, CodingKey {
        case roomId, accountId, roomName, name, dp, image, roomDp, roomImage
        case announcement, message, countryName, country, countryCode, isLocked
        case legacyId = "id"
    }

    var stableID: String { roomId ?? accountId ?? legacyId ?? "" }
    var id: String { stableID }
    var displayName: String { roomName ?? name ?? "Room" }
    var displayImage: String? { dp ?? image ?? roomDp ?? roomImage }
    var displayAnnouncement: String { announcement ?? message ?? "" }
    var displayCountry: String { countryName ?? country ?? countryCode ?? "" }
    var isRoomLocked: Bool { isLocked ?? false }
}
