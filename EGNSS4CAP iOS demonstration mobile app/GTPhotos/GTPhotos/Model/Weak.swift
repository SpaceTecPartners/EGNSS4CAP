import Foundation

class Weak<T: AnyObject> {
    
    weak var val: T?
    
    init(_ val: T?) {
        self.val = val
    }
}
