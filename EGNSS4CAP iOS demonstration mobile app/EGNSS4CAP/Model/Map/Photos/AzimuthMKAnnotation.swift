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

// Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
