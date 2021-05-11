import CoreLocation
import CoreMotion
import UIKit

class PhotoDataController: NSObject, CLLocationManagerDelegate {
    
    typealias LocationReceiver = (_ location: CLLocation) -> Void
    typealias HeadingReceiver = (_ heading: CLHeading) -> Void
    typealias MotionReceiver = (_ att: CMAttitude?) -> Void
    typealias CentroidReceiver = (_ count: Int, _ centroidLat: Double?, _ centroidLng: Double?) -> Void
    
    private static let locationAccuracy = kCLLocationAccuracyBestForNavigation
    private static let maxAgeLifeLocationMils = 7000
    private static let locationRefreshIntervalMils = 1000
    private static let maxDelayLocationMils = 5000
    private static let maxDistanceMeter = 20
    private static let motionUpdateIntervalMils = 100
    private static let centroidUpdateIntervalMils = 1000
    
    private var isCentroid = false
    private var isRunning = false
    
    private let locationManager = CLLocationManager()
    private var locations: [CLLocation] = []
    private var lastLocation: CLLocation?
    private let locationDQ = DispatchQueue(label: "locationDQ")
    private var isLocRefreshRunning: Bool = false
    private var headings: [CLHeading] = []
    private var lastHeading: CLHeading?
    private let headingDQ = DispatchQueue(label: "headingDQ")
    private var isHeadingRefreshRunning: Bool = false
    
    private var centroidMaxCount: Int?
    private var centroids: [CLLocation] = []
    private var centroidCount: Int {
        return centroids.count
    }
    private let cluster = CHCluster()
    private var lastCentroid: CHPoint?
    private var centroidDQ = DispatchQueue(label: "centroidDQ")
    private var centroidTimer: Timer?
    
    private let motionManager = CMMotionManager()
    private var lastAttitude: CMAttitude?
    private var motionsOQ = OperationQueue()
    private var motionDQ = DispatchQueue(label: "motionDQ")
    private var orientation: UIInterfaceOrientation?
    
    /// Hodnoty se považují za správné i v případě ž vypršela jejich platnost (maxAgeLifeLocationMils) = žádné nejsou v bufferu, existuje poslední známá hodnota
    var isSoftCorrection = true
    
    var locationReceiver: LocationReceiver?
    var headingReceiver: HeadingReceiver?
    var motionReceiver: MotionReceiver?
    var centroidReceiver: CentroidReceiver?
    
    
    override init() {
        super.init()
        initLocationManager()
        initMotionManager()
    }

    private func initLocationManager() {
        locationManager.requestWhenInUseAuthorization()
        guard CLLocationManager.locationServicesEnabled() else {
            return
        }
        locationManager.delegate = self
        locationManager.desiredAccuracy = Self.locationAccuracy
        locationManager.distanceFilter = kCLDistanceFilterNone
    }
    
    private func initMotionManager() {
        if !motionManager.isDeviceMotionAvailable {
           return
        }
        motionManager.deviceMotionUpdateInterval = Double(Self.motionUpdateIntervalMils) / 1000
    }

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        locationDQ.async {
            guard let location = manager.location else {
                return
            }
            
            self.locations.append(location)
            self.lastLocation = location
            if (self.locationReceiver != nil) {
                DispatchQueue.main.async {
                    self.locationReceiver!(location)
                }
            }
            print("Adding location. Remains: \(self.locations.count)")
        }
        
        if (isCentroid) {
            centroidDQ.async {
                var locations = locations
                for cenLoc in self.centroids {
                    locations = locations.filter{
                        if $0.coordinate.latitude == cenLoc.coordinate.latitude {
                            print("Lost centroid location: \(cenLoc)")
                            return false
                        } else {
                            return true
                        }
                        
                    }
                }
                let remove = self.centroids.count + locations.count - self.centroidMaxCount!
                if remove > 0 {
                    self.centroids.removeSubrange(0..<remove)
                }
                self.centroids.append(contentsOf: locations)
                if (self.centroids.count == self.centroidMaxCount) {
                    var points: [CHPoint] = []
                    for loc in self.centroids {
                        points.append(CHPoint(x: loc.coordinate.latitude, y: loc.coordinate.longitude))
                    }
                    self.cluster.points = points
                    var lastPerimeter: [CHPoint]? = nil
                    self.lastCentroid = self.cluster.computeCentroid(lastPerimeter: &lastPerimeter)
                }
                DispatchQueue.main.async {
                    var count: Int = 0
                    var centroid: CHPoint? = nil
                    self.centroidDQ.sync {
                        count = self.centroids.count
                        centroid = self.lastCentroid
                    }
                    self.centroidReceiver!(count, centroid?.x, centroid?.y)
                }
            }
        }
    }
    
    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        print("Location error: \(error)")
    }
    
    func locationManager(_ manager: CLLocationManager, didUpdateHeading newHeading: CLHeading) {
        headingDQ.sync {
            self.headings.append(newHeading)
            self.lastHeading = newHeading
            if (self.headingReceiver != nil) {
                DispatchQueue.main.async {
                    self.headingReceiver!(newHeading)
                }
            }
            print("Adding heading. Remains: \(self.headings.count)")
        }
    }
    
    private func refreshLocations() {
        locationDQ.asyncAfter(deadline: .now() + .milliseconds(Self.locationRefreshIntervalMils), execute: {
            guard self.isLocRefreshRunning else {
                return
            }
            for (i, location) in self.locations.enumerated().reversed() {
                let life = location.timestamp.addingTimeInterval(TimeInterval(Double(Self.maxAgeLifeLocationMils) / Double(1000))).timeIntervalSince1970
                let now =  Date().timeIntervalSince1970
                if life < now {
                    self.locations.remove(at: i)
                    print("Removing location. Remains: \(self.locations.count)")
                } else {
                    print("Leaving location:  Remains: \(self.locations.count)")
                }
            }
            
            self.refreshLocations()
        })
    }
    
    private func refreshHeadings() {
        headingDQ.asyncAfter(deadline: .now() + .milliseconds(Self.locationRefreshIntervalMils), execute: {
            guard self.isHeadingRefreshRunning else {
                return
            }
            for (i, heading) in self.headings.enumerated().reversed() {
                let life = heading.timestamp.addingTimeInterval(Double(Self.maxAgeLifeLocationMils) / Double(1000)).timeIntervalSince1970
                let now = Date().timeIntervalSince1970
                if life < now {
                    self.headings.remove(at: i)
                    print("Removing heading. Remains: \(self.headings.count)")
                } else {
                    print("Leaving heading:  Remains: \(self.headings.count)")
                }
            }
            self.refreshHeadings()
        })
    }
    
    private func isDelayCorrect(refTimeArr: [TimeInterval]) -> Bool {
        if (refTimeArr.count == 1){
            return true
        }
        var sumTime: Double = 0
        for i in 0...refTimeArr.count - 2 {
            sumTime += refTimeArr[i + 1] - refTimeArr[i]
        }
        let correct = (sumTime / Double(refTimeArr.count)) * 1000 <= Double(Self.maxDelayLocationMils)
        return correct
    }
    
    private func isLocationCorrect() -> Bool {
        locationDQ.sync {
            if locations.count == 0 {
                return isSoftCorrection && lastLocation != nil
            }
            var sumDistance: Double = 0
            if (locations.count > 1) {
                for location1 in locations {
                    for location2 in locations {
                        sumDistance += location1.distance(from: location2)
                    }
                }
                if (sumDistance / Double(locations.count * locations.count - 1)) > Double(Self.maxDistanceMeter) {
                    return false
                }
            }
            let locTimeRef = locations.map{$0.timestamp.timeIntervalSince1970}
            return isDelayCorrect(refTimeArr: locTimeRef)
        }
    }
    
    private func isHeadingCorrect() -> Bool {
        headingDQ.sync {
            if headings.count == 0 {
                return isSoftCorrection && lastHeading != nil
            }
            
            let headTimeRef = headings.map{$0.timestamp.timeIntervalSince1970}
            return isDelayCorrect(refTimeArr: headTimeRef)
        }
    }
    
    private func startLocations() {
        isLocRefreshRunning = true
        if (isCentroid) {
            centroidTimer = Timer.scheduledTimer(withTimeInterval: Double(Self.centroidUpdateIntervalMils) / 1000, repeats: true) {timer in
                // násilné vynucení updatu
                self.locationManager.stopUpdatingLocation()
                self.locationManager.startUpdatingLocation()
            }
        } else {
            locationManager.startUpdatingLocation()
        }
        refreshLocations()
    }
    
    
    private func stopLocations() {
        locationManager.stopUpdatingLocation()
        centroidTimer?.invalidate()
        isLocRefreshRunning = false
    }
    
    private func startHeadings() {
        isHeadingRefreshRunning = true
        locationManager.startUpdatingHeading()
        refreshHeadings()
    }
    
    private func stopHeadings() {
        locationManager.stopUpdatingHeading()
        isHeadingRefreshRunning = false
    }
    
    private func startMotions() {
        motionManager.startDeviceMotionUpdates(using: .xArbitraryZVertical, to: motionsOQ, withHandler: { motion, error in
            self.motionDQ.async {
                self.lastAttitude = motion?.attitude
                if (self.motionReceiver != nil) {
                    DispatchQueue.main.async {
                        self.motionReceiver!(self.lastAttitude)
                    }
                }
            }
        })
    }
    
    private func stopMotions() {
        motionManager.stopDeviceMotionUpdates()
    }
    
    // nelze volat při běhu
    private func reset() {
        locationDQ.sync {
            locations.removeAll()
            lastLocation = nil
        }
        headingDQ.sync {
            headings.removeAll()
            lastHeading = nil
        }
        motionDQ.sync {
            lastAttitude = nil
        }
        isSoftCorrection = true
    }
    
    // MARK: - Controls
    
    func start() {
        if (isRunning) {
            return
        }
        reset()
        isRunning = true
        startLocations()
        startHeadings()
        startMotions()
    }
    
    func stop() {
        if (!isRunning) {
            return
        }
        stopLocations()
        stopHeadings()
        stopMotions()
        isRunning = false
    }
    
    func restart() {
        stop()
        start()
    }
    
    func turnOnCentroid(count: Int) {
        if (isRunning) {
            return
        }
        centroidMaxCount = count
        isCentroid = true
    }
    
    func turnOffCentroid() {
        if (isRunning) {
            return
        }
        isCentroid = false
    }
    
    func isDataLocationCorrect() -> Bool {
        let locCorect = isLocationCorrect()
        let headCorrect = isHeadingCorrect()
        return locCorect && headCorrect
    }
    
    func isCentroidCorrect() -> Bool {
        centroidDQ.sync {
            if isCentroid {
                return centroids.count >= centroidMaxCount!
            } else {
                return true
            }
        }
    }
    
    func isAllDataCorrect() -> Bool {
        return isDataLocationCorrect() && isCentroidCorrect()
    }
    
    func getLastLocation() -> CLLocation? {
        locationDQ.sync {
            if (locations.count == 0) {
                return nil
            } else {
                return locations[locations.count - 1]
            }
        }
    }
    
    func getLastHeading() -> CLHeading? {
        headingDQ.sync {
            if (headings.count == 0) {
                return nil
            } else {
                return headings[headings.count - 1]
            }
        }
    }
    
    func setOrientation(orientation: UIInterfaceOrientation?) {
        if (isRunning) {
            self.orientation = orientation
        }
    }
    
    func getOrientation() -> UIInterfaceOrientation? {
        return self.orientation
    }
    
    func getLastAttitude() -> CMAttitude? {
        motionDQ.sync {
            return self.lastAttitude
        }
    }
    
    func getLastCentroid() -> CHPoint? {
        centroidDQ.sync {
            return self.lastCentroid
        }
    }
    
    func computeTilt() -> Double? {
        var att: CMAttitude?
        motionDQ.sync {
            att = self.lastAttitude
        }
        if att == nil {
            return nil
        }
        if (orientation == nil) {
            return nil
        }
        let pitch = Util.rad2deg(rads: att!.pitch)
        let roll = Util.rad2deg(rads: att!.roll)
        let acuteAngle = abs(roll) >= 90 && abs(roll) <= 270 ? false : true
        var tilt: Double?
        switch orientation {
        case .landscapeLeft:
            tilt = roll
        case .landscapeRight:
            tilt = -roll
        case .portrait:
            if (acuteAngle) {
                tilt = pitch
            } else {
                tilt = 180 - pitch
            }
        case.portraitUpsideDown:
            if (acuteAngle) {
                tilt = -pitch
            } else {
                tilt = 180 + pitch
            }
        default:
            tilt = nil
        }
        
        return tilt

    }
    
    
    func computePhotoHeading() -> Double? {
        guard let heading = getLastHeading() else {
            return nil
        }
        let azim = heading.magneticHeading
        guard let orientation = orientation else {
            return azim
        }
        switch orientation {
        case .landscapeLeft:
            return Util.degreeCycle(degree: azim - 90)
        case .landscapeRight:
            return Util.degreeCycle(degree: azim + 90)
        case .portrait:
            return azim
        case .portraitUpsideDown:
            return Util.degreeCycle(degree: azim + 180)
        case .unknown:
            return azim
        default:
            return azim
        }
    }
    
}

// Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
