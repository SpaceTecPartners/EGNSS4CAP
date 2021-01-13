import MapKit

class PTMKAnnotation: NSObject, MKAnnotation {
    
    static let PTAnnotationIndetifier = "PTAnnotationIndetifier"
    
    let coordinate: CLLocationCoordinate2D
    
    let ptPoint: PTPoint
    let ptPointInfoView: PTPointInfoView
    
    init(ptPoint: PTPoint) {
        self.ptPoint = ptPoint
        self.coordinate = CLLocationCoordinate2D(latitude: ptPoint.lat, longitude: ptPoint.lng)
        self.ptPointInfoView = PTPointInfoView.fromNib()
        super.init()
    }

}
