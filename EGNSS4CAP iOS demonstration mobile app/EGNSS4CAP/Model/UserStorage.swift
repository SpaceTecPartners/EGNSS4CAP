import Foundation

class UserStorage: PersistStorage {
    enum Key: String {
        case userID
        case login
        case userName
        case userSurname
        case gpsCapable
    }
    
    typealias KeyEnum = Key
    
    static let prefix: PSPrefix = .UserStorage
    
    @item(key: Key.userID)
    static var userID: Int
    
    @item(key: Key.login)
    static var login: String?
    
    @item(key: Key.userName)
    static var userName: String?
    
    @item(key: Key.userSurname)
    static var userSurname: String?
    
    @item(key: Key.gpsCapable)
    static var gpsCapable: Bool?
    
}

// Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
