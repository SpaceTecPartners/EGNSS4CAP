import Foundation

enum PSPrefix: String {
    case SEStorage
    case UserStorage
}

@propertyWrapper class PSItem<T, S: PersistStorage> {
    let key: String
    let keyEnum: S.KeyEnum
    public var defaultVal: T? = nil
    
    init(key: S.KeyEnum) {
        self.keyEnum = key
        self.key = S._createKey(key.rawValue)
        if (S.defaults[key] != nil) {
            self.defaultVal = S.defaults[key] as! T?
        }
    }
    
    var wrappedValue: T {
        get {
            if !S.exists(key: keyEnum) && defaultVal != nil {
                UserDefaults.standard.set(defaultVal!, forKey: key)
                return defaultVal!
            } else {
                return UserDefaults.standard.object(forKey: key) as! T
            }
        }

        set {
            UserDefaults.standard.set(newValue, forKey: key)
        }
    }
}


protocol PersistStorage {
    associatedtype KeyEnum: RawRepresentable & Hashable where KeyEnum.RawValue == String
    typealias item<T> = PSItem<T, Self>
    
    static var prefix: PSPrefix {get}
    
    static var defaults: [KeyEnum: Any] {get}
    
}
extension PersistStorage {
    
    static var defaults: [KeyEnum: Any] {
        return [:]
    }
    
    static func _createKey(_ name: String) -> String {
        return prefix.rawValue + "" + name
    }
    
    static func removeObject(key: KeyEnum) {
        UserDefaults.standard.removeObject(forKey: _createKey(key.rawValue))
    }
    
    static func exists(key: KeyEnum) -> Bool {
        return UserDefaults.standard.object(forKey: _createKey(key.rawValue)) != nil
    }

}

// Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
