import UIKit
import MapKit
import CoreLocation

class PhotoMKAnnotation: NSObject, MKAnnotation {
    
    static let photoAnnotationIndetifier = "photoAnnotationIndetifier"
    
    let coordinate: CLLocationCoordinate2D
    
    weak var parentViewController: UIViewController?
    let photoMapInfoView: PhotoMapInfoView
    let persistPhoto: PersistPhoto
    
    init(parentViewController: UIViewController, persistPhoto: PersistPhoto) {
        self.parentViewController = parentViewController
        self.persistPhoto = persistPhoto
        self.coordinate = CLLocationCoordinate2D(latitude: persistPhoto.lat, longitude: persistPhoto.lng)
        self.photoMapInfoView = PhotoMapInfoView.fromNib()
        super.init()
    }
}

// Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
