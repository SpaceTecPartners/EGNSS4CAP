import Foundation

class CHQuickHull {
    
    private func pointLocation(A: CHPoint, B: CHPoint, P: CHPoint) -> Int {
        let cp1_1: Double = (B.x - A.x) * (P.y - A.y)
        let cp1_2: Double = (B.y - A.y) * (P.x - A.x)
        let cp1 = cp1_1 - cp1_2
        if cp1 > 0 {
            return 1
        } else if cp1 == 0 {
            return 0
        } else {
            return -1
        }
    }
    
    private func distance(A: CHPoint, B:CHPoint, C:CHPoint) -> Double {
        let ABx = B.x - A.x
        let ABy = B.y - A.y
        let num1 = ABx * (A.y - C.y)
        let num2 = ABy * (A.x - C.x)
        var num = num1 - num2
        if num < 0 {
            num = -num
        }
        return num
    }
    
    private func hullSet(A: CHPoint, B: CHPoint, set: inout [CHPoint], hull: inout [CHPoint]) {
        let insertPosition = hull.firstIndex(of: B)!
        if set.count == 0 {
            return
        }
        if set.count == 1 {
            let p = set[0]
            set.remove(at: 0)
            hull.insert(p, at: insertPosition)
            return
        }
        var dist: Double = .leastNormalMagnitude
        var furthestPoint: Int = -1
        for (i, p) in set.enumerated() {
            let distanc = distance(A: A, B: B, C: p)
            if distanc > dist {
                dist = distanc
                furthestPoint = i
            }
        }
        let P = set[furthestPoint]
        set.remove(at: furthestPoint)
        hull.insert(P, at: insertPosition)
        
        var letSetAP: [CHPoint] = []
        for M in set {
            if (pointLocation(A: A, B: P, P: M) == 1) {
                letSetAP.append(M)
            }
        }
        
        var letSetPB: [CHPoint] = []
        for M in set {
            if (pointLocation(A: P, B: B, P: M) == 1) {
                letSetPB.append(M)
            }
        }
        
        hullSet(A: A, B: P, set: &letSetAP, hull: &hull)
        hullSet(A: P, B: B, set: &letSetPB, hull: &hull)
    }
    
    func quickHull(poin ps: [CHPoint]) -> [CHPoint] {
        var points = ps
        var convexHull: [CHPoint] = []
        if (points.count < 3) {
            return points
        }
        
        var minPoint: Int = -1
        var maxPoint: Int = -1
        var minX: Double = Double.greatestFiniteMagnitude
        var maxX: Double = Double.leastNormalMagnitude
        for (i, P) in points.enumerated() {
            if (P.x < minX) {
                minX = P.x
                minPoint = i
            }
            if (P.x  > maxX) {
                maxX = P.x
                maxPoint = i
            }
        }
        let A = points[minPoint]
        let B = points[maxPoint]
        convexHull.append(A)
        convexHull.append(B)
        points.remove(at: points.firstIndex(of: A)!)
        points.remove(at: points.firstIndex(of: B)!)
        
        var leftSet: [CHPoint] = []
        var rightSet: [CHPoint] = []
        for P in points {
            if pointLocation(A: A, B: B, P: P) == -1 {
                leftSet.append(P)
            } else if pointLocation(A: A, B: B, P: P) == 1 {
                rightSet.append(P)
            }
        }
        
        hullSet(A: A, B: B, set: &rightSet, hull: &convexHull)
        hullSet(A: B, B: A, set: &leftSet, hull: &convexHull)
        
        return convexHull
    }
}
