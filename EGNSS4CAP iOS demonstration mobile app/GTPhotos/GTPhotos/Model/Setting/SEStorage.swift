import Foundation

class SEStorage: PersistStorage {
    enum Key: String {
        case centroidActive
        case centroidCount
    }
    
    typealias KeyEnum = Key
    
    static let defaults: [Key : Any] = [
        .centroidActive: false,
        .centroidCount: 20
    ]
    
    static let prefix: PSPrefix = .SEStorage
    
    @item(key: Key.centroidActive)
    static var centroidActive: Bool
    
    @item(key: Key.centroidCount)
    static var centroidCount: Int
}
