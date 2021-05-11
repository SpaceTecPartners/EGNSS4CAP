import Foundation

class SEStorage: PersistStorage {
    enum Key: String {
        case centroidActive
        case centroidCount
        case trackingInterval
    }
    
    typealias KeyEnum = Key
    
    static let defaults: [Key : Any] = [
        .centroidActive: false,
        .centroidCount: 20,
        .trackingInterval: 1
    ]
    
    static let prefix: PSPrefix = .SEStorage
    
    @item(key: Key.centroidActive)
    static var centroidActive: Bool
    
    @item(key: Key.centroidCount)
    static var centroidCount: Int
    
    @item(key: Key.trackingInterval)
    static var trackingInterval: Int
}

// Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
