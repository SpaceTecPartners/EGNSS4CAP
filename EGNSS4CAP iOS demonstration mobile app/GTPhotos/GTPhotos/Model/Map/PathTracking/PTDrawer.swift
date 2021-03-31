import Foundation
import MapKit

class PTPolyline: MKPolyline {
}
class PTPolygon: MKPolygon {
}

class PTDrawer {
    unowned private var ptManager: PTManager!
    
    private var pointAnnotations: [PTMKAnnotation] = []
    private var polyline: MKPolyline?
    private var polygon: MKPolygon?
    
    init (ptManager: PTManager) {
        self.ptManager = ptManager
        reinit()
    }
    
    func reinit(ptPath: PTPath? = nil) {
        ptManager.ptDelegat?.mkMapView.register(PTMKAnnotationView.self, forAnnotationViewWithReuseIdentifier: PTMKAnnotation.PTAnnotationIndetifier)
        removeAll()
        if ptManager.isTracking && ptPath != nil {
            drawPoints(ptPoints: ptPath!.points!.array as! [PTPoint])
        }
    }
    
    func removeAll() {
        removePath()
        removePolygon()
    }
    
    func removePath() {
        ptManager.ptDelegat?.mkMapView.removeAnnotations(pointAnnotations.map{$0 as MKAnnotation})
        pointAnnotations.removeAll()
        removePolyline()
    }
    
    private func removePolyline() {
        if polyline != nil {
            ptManager.ptDelegat?.mkMapView.removeOverlay(polyline!)
            polyline = nil
        }
    }
    
    func removePolygon() {
        if polygon != nil {
            ptManager.ptDelegat?.mkMapView.removeOverlay(polygon!)
        }
    }
    
    func drawPoints(ptPoints: [PTPoint]) {
        guard ptManager.ptDelegat != nil else {
            return
        }
        
        for ptPoint in ptPoints {
            drawPoint(ptPoint: ptPoint, withRefreshPolyline: false)
        }
        refreshPolyline()
    }
    
    
    func drawPoint(ptPoint: PTPoint, withRefreshPolyline: Bool = true) {
        guard let ptDelegate = ptManager.ptDelegat else {
            return
        }
        
        let ann = PTMKAnnotation(ptPoint: ptPoint)
        pointAnnotations.append(ann)
        ptDelegate.mkMapView.addAnnotation(ann)
        
        if (withRefreshPolyline) {
            refreshPolyline()
        }
    }
    
    func refreshPolyline() {
        guard let ptDelegate = ptManager.ptDelegat else {
            return
        }
        
        var coordinates = pointAnnotations.map{$0.coordinate}
        removePolyline()
        polyline = PTPolyline(coordinates: &coordinates, count: pointAnnotations.count)
        ptDelegate.mkMapView.addOverlay(polyline!)
    }
    
    func createAnnotationView(annotation: MKAnnotation) -> MKAnnotationView? {
        guard let ptDelegate = ptManager.ptDelegat else {
            return nil
        }
        
        return ptDelegate.mkMapView.dequeueReusableAnnotationView(withIdentifier: PTMKAnnotation.PTAnnotationIndetifier, for: annotation)
    }
    
    func createPolylineRenderer(overlay: MKOverlay) -> MKOverlayRenderer {
        let renderer =  MKPolylineRenderer(overlay: overlay)
        renderer.strokeColor = .blue
        renderer.lineWidth = 3
        return renderer
    }
    
    func drawPolygon(ptPath: PTPath) {
        guard let ptDelegate = ptManager.ptDelegat else {
            return
        }
        
        removeAll()
        
        let points = ptPath.points!.array.map{$0 as! PTPoint}
        drawPoints(ptPoints: points)
        var coordinates = points.map{CLLocationCoordinate2D(latitude: $0.lat, longitude: $0.lng)}
        polygon = PTPolygon(coordinates: &coordinates, count: coordinates.count)
        ptDelegate.mkMapView.addOverlay(polygon!)
        moveToPolygon()
    }
    
    func moveToPolygon() {
        guard let ptDelegate = ptManager.ptDelegat else {
            return
        }
        
        ptDelegate.mkMapView.setRegion(MKCoordinateRegion(polygon!.boundingMapRect), animated: true)
    }
    
    func createPolygonRenderer(overlay: MKOverlay) -> MKOverlayRenderer {
        let renderer =  MKPolygonRenderer(overlay: overlay)
        renderer.strokeColor = .blue
        renderer.lineWidth = 3
        renderer.fillColor = UIColor.blue.withAlphaComponent(0.5)
        return renderer
    }
    
    func createOverlayRenderer(overlay: MKOverlay) -> MKOverlayRenderer {
        switch overlay {
        case is PTPolyline:
            return createPolylineRenderer(overlay: overlay)
        case is PTPolygon:
            return createPolygonRenderer(overlay: overlay)
        default:
            return MKOverlayRenderer()
        }
    }
    
}

// Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
