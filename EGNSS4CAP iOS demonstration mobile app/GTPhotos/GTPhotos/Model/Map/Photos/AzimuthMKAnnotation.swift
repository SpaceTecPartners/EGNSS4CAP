import MapKit

class AzimuthMKAnnotation: NSObject, MKAnnotation {
    
    static let azimuthAnnotationIndetifier = "azimuthAnnotationIndetifier"

    let coordinate: CLLocationCoordinate2D
    let photo: PersistPhoto
    
    init(photo: PersistPhoto) {
        self.photo = photo
        self.coordinate = CLLocationCoordinate2D(latitude: photo.lat, longitude: photo.lng)
    }
}
