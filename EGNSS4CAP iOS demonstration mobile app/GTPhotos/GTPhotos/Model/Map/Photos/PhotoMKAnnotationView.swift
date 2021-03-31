import UIKit
import MapKit

class PhotoMKAnnotationView: MKAnnotationView {
    

    override var annotation: MKAnnotation? {
        willSet {
            guard let annotation = newValue as? PhotoMKAnnotation else {
                return
            }
            
            canShowCallout = true
            var photo: UIImage? = nil
            if (annotation.persistPhoto.photo != nil) {
                photo = UIImage(data: annotation.persistPhoto.photo!)?.rotateCameraImageToProperOrientation()
            }
            let img = createIcon(photo: photo)
            image = img
            let uiView = annotation.photoMapInfoView
            detailCalloutAccessoryView = uiView
            let persistPhoto = annotation.persistPhoto
            uiView.longitudeLabel.text = String(format: "%f", persistPhoto.lng)
            uiView.latitudeLabel.text = String(format: "%f", persistPhoto.lat)
            uiView.altitudeLabel.text = String(format: "%.0f", persistPhoto.altitude)
            uiView.createdLabel.text =  persistPhoto.created == nil ? "unknown" : Util.prettyDate(date: persistPhoto.created!) + " " + Util.prettyTime(date: persistPhoto.created!)
            centerOffset = CGPoint(x: 0, y: (-img.size.height / 2) + 5)
            calloutOffset = CGPoint(x: -5, y: 5)
            let detailButton = UIButton(type: .custom)
            detailButton.setImage(#imageLiteral(resourceName: "icon_find"), for: .normal)
            detailButton.frame = CGRect(x: 0, y: 0, width: 30, height: 30)
            detailButton.addTarget(self, action: #selector(gotToDetail), for: .touchUpInside)
            rightCalloutAccessoryView = detailButton
            
        }
    }
    
    private func createIcon(photo: UIImage?) -> UIImage {
        let outerIcon = #imageLiteral(resourceName: "icon_photo.png")
        let frame = CGRect(origin: CGPoint(x: 0, y: 0), size: outerIcon.size)
        let photoFrame = CGRect(x: 2, y: 2 + (frame.height / 2), width: frame.width - 4, height: (frame.height / 2) - 4)
        let renderer = UIGraphicsImageRenderer(size: frame.size)
        let image = renderer.image(actions: { context in
            context.cgContext.translateBy(x: 0, y: frame.size.height)
            context.cgContext.scaleBy(x: 1, y: -1)
            context.cgContext.draw(outerIcon.cgImage!, in: frame)
            UIColor.black.setFill()
            context.fill(photoFrame)
            if photo != nil {
                context.cgContext.draw(photo!.cgImage!, in: Util.scaleCGRectToCenter(origSize: photo!.size, dest: photoFrame))
            }
        })
        return image
    }
    
    @objc func gotToDetail() {
        guard let annotation = annotation as? PhotoMKAnnotation else {
            return
        }
        guard let parent = annotation.parentViewController else {
            return
        }
        guard let detail = parent.storyboard?.instantiateViewController(withIdentifier: "PhotoDetailViewController") as? PhotoDetailViewController else {
            return
        }
        detail.persistPhoto = annotation.persistPhoto
        parent.present(detail, animated: true)
        
    }

}

// Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
