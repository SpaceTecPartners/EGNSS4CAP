import Foundation
import MapKit
import CoreData

protocol PTManagerDelegate: AnyObject {
    var mkMapView: MKMapView {get}
    
    func PTSavePathError(error: Error)
    
    func PTNoPointError()
}

class PTManager: NSObject, CLLocationManagerDelegate {
    
    let kEarthRadius = 6378137.0
    
    private static var managers: [String: PTManager] = [:]
    
    static func acquire(indetifier: String, ptManagerDelegate: PTManagerDelegate) -> PTManager {
        var manager = managers[indetifier]
        if manager == nil {
            manager = PTManager(ptManagerDelegate: ptManagerDelegate)
            managers[indetifier] = manager
        } else {
            manager?.reinit(ptDelegate: ptManagerDelegate)
        }
        return manager!
    }
    
    private static let locationAccuracy = kCLLocationAccuracyBestForNavigation
    //private static let locationRefreshIntervalMils = 1000 * SEStorage.trackingInterval
    
    weak var ptDelegat: PTManagerDelegate?
    private var _ptDrawer: PTDrawer!
    private var ptDrawer: PTDrawer {
        get {return _ptDrawer}
        set {_ptDrawer = newValue}
    }
    
    private let locationManager =  CLLocationManager()
    private var locationTimer: DispatchSourceTimer?
    private var locations: [CLLocation] = []
    private var lastLocation: CLLocation?
    private let locationDB = DB()
    private var ptPath: PTPath?
    private var centroidWaitAlert:UIAlertController!
    private var centroidTimer:Timer?
    // zajišťuje jedinou lokaci během intervalu
    private var isLocationUpdated = false
    private var _isTracking = false
    var centroid = false
    var isTracking: Bool {
        return locationDQ.sync {
            self._isTracking
        }
    }
    private var locationDQ = DispatchQueue(label: "locationDQ")
    
    private init(ptManagerDelegate: PTManagerDelegate) {
        self.ptDelegat = ptManagerDelegate
        
        super.init()
        
        self.ptDrawer = PTDrawer(ptManager: self)
        
        locationManager.delegate = self
        locationManager.desiredAccuracy = Self.locationAccuracy
        locationManager.distanceFilter = kCLDistanceFilterNone
    }
    
    private func reinit(ptDelegate: PTManagerDelegate) {
        self.ptDelegat = ptDelegate
        ptDrawer.reinit(ptPath: acquirePTPathMainThread())
    }
    
    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        locationDQ.async {
            guard self._isTracking else {
                return
            }
            guard !self.isLocationUpdated else {
                return
            }
            guard let newLocation = self.locationManager.location else {
                return
            }
            if self.lastLocation != nil && self.lastLocation!.timestamp == newLocation.timestamp {
              return
            }
            if self.centroid && self.lastLocation != nil && self.lastLocation!.coordinate.latitude == newLocation.coordinate.latitude {
              return
            }
            
            self.isLocationUpdated = true
            self.lastLocation = newLocation
            
            if (self.centroid) {
                self.locations.append(newLocation)
                DispatchQueue.main.async {
                    self.centroidWaitAlert.message = self.locations.count.description + "/" + SEStorage.centroidCount.description
                }
                
                if (self.locations.count == SEStorage.centroidCount) {                    
                    DispatchQueue.main.async {
                        var points: [CHPoint] = []
                        let cluster = CHCluster()
                        for loc in self.locations {
                            points.append(CHPoint(x: loc.coordinate.latitude, y: loc.coordinate.longitude))
                        }
                        cluster.points = points
                        var lastPerimeter: [CHPoint]? = nil
                        let centroidPoint = cluster.computeCentroid(lastPerimeter: &lastPerimeter)
                        
                        let centroidPointX = centroidPoint?.x ?? -1000
                        let centroidPointY = centroidPoint?.y ?? -1000
                        
                        if (centroidPointX > -1000 && centroidPointY > -1000) {
                            let ptPoint = PTPoint(context: self.locationDB.privateMOC)
                            ptPoint.lat = centroidPointX
                            ptPoint.lng = centroidPointY
                            ptPoint.created = newLocation.timestamp
                            ptPoint.path = self.ptPath

                            DispatchQueue.main.async {
                                self.ptDrawer.drawPoint(ptPoint: self.acquirePTPointMainThread(ptPoint: ptPoint)!)
                            }
                        }
                        self.centroidWaitAlert.dismiss(animated: true, completion: nil)
                        self.centroid = false
                    }
                    
                } else {
                    DispatchQueue.main.async {
                        self.centroidTimer = Timer.scheduledTimer(timeInterval: 1, target: self, selector: #selector(self.centroidGetPoint), userInfo: nil, repeats: false)
                    }
                }
                
            } else {
                let ptPoint = PTPoint(context: self.locationDB.privateMOC)
                ptPoint.lat = newLocation.coordinate.latitude
                ptPoint.lng = newLocation.coordinate.longitude
                ptPoint.created = newLocation.timestamp
                ptPoint.path = self.ptPath

                DispatchQueue.main.async {
                    self.ptDrawer.drawPoint(ptPoint: self.acquirePTPointMainThread(ptPoint: ptPoint)!)
                }
            }
        }
    }
    
    private func savePath() {
        guard self.ptPath != nil else {
            return
        }

        do {
            try locationDB.privateMOC.save()
        } catch {
            ptDelegat?.PTSavePathError(error: error)
        }
        
    }
    
    private func acquirePTPointMainThread(ptPoint: PTPoint) -> PTPoint? {
        return locationDQ.sync {
            guard self.ptPath != nil else {
                return nil
            }
            
            self.savePath()
            locationDB.mainMOC.refreshAllObjects()
            return locationDB.mainMOC.object(with: ptPoint.objectID) as? PTPoint
        }
    }
    
    private func _acquirePTPathMainThread() -> PTPath? {
        guard self.ptPath != nil else {
            return nil
        }
        
        self.savePath()
        locationDB.mainMOC.refreshAllObjects()
        return locationDB.mainMOC.object(with: self.ptPath!.objectID) as? PTPath
    }
    
    // MARK: - controls
    
    func startTrack(pathName: String) {
                
        self.locationManager.requestWhenInUseAuthorization()
        locationDQ.sync {
            guard (self.locationTimer == nil || self.locationTimer!.isCancelled) && !self._isTracking else {
                return
            }
            
            self._isTracking = true
            
            self.ptDrawer.removeAll()
            
            self.ptPath = PTPath(context: self.locationDB.privateMOC)
            self.ptPath!.name = pathName
            self.ptPath!.start = Date()
            self.ptPath!.userId = Int64(UserStorage.userID)
            
            self.locationTimer = DispatchSource.makeTimerSource(flags: .strict, queue: self.locationDQ)
            self.locationTimer?.schedule(deadline: .now(), repeating: Double(SEStorage.trackingInterval))
            self.locationTimer?.setEventHandler(handler: {
                self.isLocationUpdated = false
                self.locationManager.stopUpdatingLocation()
                self.locationManager.startUpdatingLocation()
            })
            self.locationTimer?.activate()
        }
    }
    
    func stopTrack() -> PTPath? {
        locationDQ.sync {
            guard _isTracking else {
                return nil
            }
            guard let lcTimer = locationTimer else {
                return nil
            }
            /*guard !lcTimer.isCancelled else {
                locationTimer = nil
                return nil
            }*/
            
            lcTimer.cancel()
            locationTimer = nil
            locationManager.stopUpdatingLocation()
            
            let ptPath = self.ptPath!
            if ptPath.points?.count ?? 0 > 0 {
                ptPath.end = Date()
            } else {
                locationDB.privateMOC.delete(ptPath)
                savePath()
                self.ptPath = nil
                ptDelegat?.PTNoPointError()
            }
            if ptPath.points?.count ?? 0 > 2 {
                let points = ptPath.points!.array.map{$0 as! PTPoint}
                let coordinates = points.map{CLLocationCoordinate2D(latitude: $0.lat, longitude: $0.lng)}
                
                ptPath.area = round(regionArea(locations: coordinates))
            }                       
            
            _isTracking = false
            return self._acquirePTPathMainThread()
        }
    }
    
    func pauseTrack() {
        locationDQ.sync {
            guard _isTracking else {
                return
            }
            guard let lcTimer = locationTimer else {
                return
            }
            guard !lcTimer.isCancelled else {
                return
            }
            
            lcTimer.cancel()
        }
    }
    
    func unpauseTrack() {
        locationDQ.sync {
            guard _isTracking else {
                return
            }
            guard let lcTimer = locationTimer else {
                return
            }
            guard lcTimer.isCancelled else {
                return
            }
            
            self.locationTimer = DispatchSource.makeTimerSource(flags: .strict, queue: self.locationDQ)
            self.locationTimer?.schedule(deadline: .now(), repeating: Double(SEStorage.trackingInterval))
            self.locationTimer?.setEventHandler(handler: {
                self.isLocationUpdated = false
                self.locationManager.stopUpdatingLocation()
                self.locationManager.startUpdatingLocation()
            })
            self.locationTimer?.activate()
        }
    }
    
    func manualPointCapture() {
        locationDQ.sync {
            guard _isTracking else {
                return
            }
            guard let lcTimer = locationTimer else {
                return
            }
            guard lcTimer.isCancelled else {
                return
            }
            
            self.isLocationUpdated = false
            self.locationManager.stopUpdatingLocation()
            self.locationManager.startUpdatingLocation()
        }
    }
    
    func deleteLastPoint () {
        locationDQ.async {
            let lastPoint = self.ptPath?.points?.lastObject as! PTPoint
            let lastPointObj = self.locationDB.privateMOC.object(with: lastPoint.objectID)
            
            self.locationDB.privateMOC.delete(lastPointObj)
            
            DispatchQueue.main.async {
                self.ptDrawer.reinit(ptPath: self.acquirePTPathMainThread())
            }
        }
    }
    
    func startCentroidCapture (alert: UIAlertController) {
        locationDQ.sync {
            guard _isTracking else {
                return
            }
            guard let lcTimer = locationTimer else {
                return
            }
            guard lcTimer.isCancelled else {
                return
            }
            
            centroid = true
            self.centroidWaitAlert = alert
            self.locations.removeAll()
            centroidGetPoint()
        }
    }
    
    func cancelCentroidcapture() {
        locationDQ.sync {
            centroid = false
            self.isLocationUpdated = true
            self.centroidTimer?.invalidate()
        }
    }
    
    @objc func centroidGetPoint() {
        self.isLocationUpdated = false
        self.locationManager.stopUpdatingLocation()
        self.locationManager.startUpdatingLocation()
    }
    
    func getPointsCount() -> Int {
        return ptPath?.points?.count ?? 0
    }
    
    func drawPathPolygon(ptPath: PTPath?) {
        if (isTracking) {
            return
        }
        if ptPath != nil {
            ptDrawer.drawPolygon(ptPath: ptPath!)
        } else {
            ptDrawer.removeAll()
        }
    }
    
    func moveToPolygon() {
        if (isTracking) {
            return
        }
        ptDrawer.moveToPolygon()
    }
    
    func mapView(_ mapView: MKMapView, viewFor annotation: MKAnnotation) -> MKAnnotationView? {
        return ptDrawer.createAnnotationView(annotation: annotation)
    }
    
    func mapView(_ mapView: MKMapView, rendererFor overlay: MKOverlay) -> MKOverlayRenderer {
        return ptDrawer.createOverlayRenderer(overlay: overlay)
    }
    
    func acquirePTPathMainThread() -> PTPath? {
        return locationDQ.sync {
            return _acquirePTPathMainThread()
        }
    }
    
    func radians(degrees: Double) -> Double {
        return degrees * .pi / 180
    }

    func regionArea(locations: [CLLocationCoordinate2D]) -> Double {

        guard locations.count > 2 else { return 0 }
        var area = 0.0

        for i in 0..<locations.count {
            let p1 = locations[i > 0 ? i - 1 : locations.count - 1]
            let p2 = locations[i]

            area += radians(degrees: p2.longitude - p1.longitude) * (2 + sin(radians(degrees: p1.latitude)) + sin(radians(degrees: p2.latitude)) )
        }
        area = -(area * kEarthRadius * kEarthRadius / 2)
        return max(area, -area) // In order not to worry about is polygon clockwise or counterclockwise defined.
    }
}

// Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
