import Foundation

class Weak<T: AnyObject> {
    
    weak var val: T?
    
    init(_ val: T?) {
        self.val = val
    }
}

// Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
