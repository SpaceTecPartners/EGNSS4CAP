import Foundation

class CHCluster {
    var points: [CHPoint] = []
    var quickHull = CHQuickHull()
    
    func reset() {
        points.removeAll()
    }
    
    func addPoint(point: CHPoint) {
        points.append(point)
    }
    
    func removePoint(point: CHPoint){
        points.remove(at: points.firstIndex(of: point)!)
    }
    
    func getSize() -> Int {
        points.count
    }
    
    func computeCentroid(lastPerimeter: inout [CHPoint]?) -> CHPoint? {
        if (points.count == 0) {
            return nil
        }
        let centroid = CHPoint()
        var hull = quickHull.quickHull(poin: points)
        var lastHull: [CHPoint] = []
        var remainPoints = points
        while hull.count > 0 {
            lastHull = hull
            for hullPoint in hull {
                remainPoints.remove(at: remainPoints.firstIndex(of: hullPoint)!)
            }
            hull = quickHull.quickHull(poin: remainPoints)
        }
        
        var centroidX: Double = 0
        var centroidY: Double = 0
        for hullPoint in lastHull {
            centroidX += hullPoint.x
            centroidY += hullPoint.y
        }
        centroidX = centroidX / Double(lastHull.count)
        centroidY = centroidY / Double(lastHull.count)
        centroid.x = centroidX
        centroid.y = centroidY
        
        lastPerimeter?.removeAll()
        lastPerimeter?.append(contentsOf: lastHull)
        
        return centroid
    }
}

// Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
