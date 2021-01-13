import Foundation

class CHPoint: Equatable{
    static func == (lhs: CHPoint, rhs: CHPoint) -> Bool {
        return lhs === rhs
    }
    
    var x: Double!
    var y: Double!
    
    init() {
    }
    
    init(x: Int, y: Int) {
        self.x = Double(x)
        self.y = Double(y)
    }
    
    init (x: Double, y: Double) {
        self.x = x
        self.y = y
    }
}
